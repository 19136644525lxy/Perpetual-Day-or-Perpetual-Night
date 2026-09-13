package yifei.pdopn.thirst;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yifei.pdopn.config.PdopnConfig;
import yifei.pdopn.storage.PlayerDataStore;
import yifei.pdopn.temperature.PdopnTemperatureManager;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 口渴系统核心管理器。
 * 职责：玩家口渴追踪、消耗计算、效果施加、数据暴露。
 * HUD 渲染由 PdopnHudRenderer 负责，本类仅暴露必要数据。
 */
public final class PdopnThirstManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("PDoPN-Thirst");

    /** 玩家口渴值：UUID → 口渴值（ConcurrentHashMap 保证线程安全） */
    private final Map<UUID, Double> hydrationMap = new ConcurrentHashMap<>();

    /** 口渴变化趋势（上一次 tick 的值，用于计算方向箭头） */
    private final Map<UUID, Double> lastHydration = new ConcurrentHashMap<>();

    /** 直接饮水冷却表：UUID → 上次饮水的 tick 数（防止快速连击） */
    private final Map<UUID, Long> drinkCooldowns = new ConcurrentHashMap<>();

    /**
     * 待加载数据的玩家队列。
     * 玩家 JOIN 事件早于服务端首个 tick，此时尚未持有 serverRef 与存档路径，
     * 直接加载会永远读不到存档（旧实现的持久化失效根因）。
     * 因此把加载推迟到 tick() 中 serverRef 就绪之后执行。
     */
    private final Queue<PendingLoad> pendingLoads = new ConcurrentLinkedQueue<>();

    /**
     * 等待重生重置的玩家（UUID）。
     * 死亡时登记，重生后（重新恢复存活）由 tick() 兜底重置；
     * 即使 onDeath 注入因版本变化等原因失效，口渴值也不会永久卡在 0。
     */
    private final Set<UUID> awaitingRespawn = ConcurrentHashMap.newKeySet();

    /** 服务端引用（用于持久化路径） */
    private MinecraftServer serverRef;

    /** 玩家数据加载任务 */
    private record PendingLoad(UUID playerId, ServerPlayerEntity player) {}

    /** Tick 计数器 */
    private int tickCount = 0;

    /** 温度管理器引用（用于联动计算） */
    private PdopnTemperatureManager temperatureManager;

    public void setTemperatureManager(PdopnTemperatureManager manager) {
        this.temperatureManager = manager;
    }

    /* ══════════ 公开 API ══════════ */

    public double getHydration(UUID playerId) {
        return hydrationMap.getOrDefault(playerId, PdopnConfig.getInstance().thirst.initialValue);
    }

    /** 获取上一次 tick 的口渴值，供 HUD 渲染器计算趋势箭头 */
    public double getPreviousHydration(UUID playerId) {
        return lastHydration.getOrDefault(playerId, getHydration(playerId));
    }

    public void setHydration(UUID playerId, double value) {
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;
        hydrationMap.put(playerId, Math.max(0.0, Math.min(cfg.maxValue, value)));
    }

    /** 增加口渴值（正数=恢复，负数=脱水） */
    public void addHydration(UUID playerId, double amount) {
        setHydration(playerId, getHydration(playerId) + amount);
    }

    /** 玩家加入时：排队等待数据加载（真正的加载在 tick() 中执行） */
    public void onPlayerJoin(ServerPlayerEntity player) {
        pendingLoads.add(new PendingLoad(player.getUuid(), player));
    }

    /** 玩家离开时保存并清理 */
    public void onPlayerLeave(UUID playerId) {
        savePlayerData(playerId);
        hydrationMap.remove(playerId);
        lastHydration.remove(playerId);
        drinkCooldowns.remove(playerId);
        awaitingRespawn.remove(playerId);
    }

    /**
     * 玩家死亡时重置口渴值为初始值（100）。
     * 同时登记等待重生重置，作为二次保险。
     */
    public void onPlayerDeath(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        awaitingRespawn.add(id);
        resetHydration(id);
    }

    /** 将口渴值重置为配置的初始值，并清理趋势/冷却状态 */
    private void resetHydration(UUID id) {
        double initial = PdopnConfig.getInstance().thirst.initialValue;
        setHydration(id, initial);
        lastHydration.put(id, initial);
        drinkCooldowns.remove(id);
    }

    /* ══════════ 直接饮水 API ══════════ */

    /**
     * 玩家右键水中方块时，由 UseBlockCallback 调用。
     * 根据水体类型计算口渴变化：淡水湖恢复，其他水体按 unsafeDrinkChance 概率脱水。
     *
     * @param player 服务器玩家
     * @param world  世界
     * @param pos    玩家视线指向的水方块坐标
     * @return true 表示已处理饮水动作（应取消原右键动作）
     */
    public boolean onDrinkWaterFromWorld(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        UUID id = player.getUuid();
        long currentTick = serverRef != null ? serverRef.getTicks() : 0L;

        // 冷却检查（防止快速连击）
        int cooldown = PdopnConfig.getInstance().thirst.drinkCooldownTicks;
        Long last = drinkCooldowns.get(id);
        if (last != null && currentTick - last < cooldown) {
            return false; // 仍在冷却中，不处理也不提示
        }
        drinkCooldowns.put(id, currentTick);

        // 判定水体类型
        WorldWaterType waterType = detectWaterType(pos, world);
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;
        boolean isUnsafe = waterType != WorldWaterType.FRESHWATER_LAKE;
        boolean dehydrated = false;

        if (isUnsafe) {
            // 非淡水湖：75% 概率脱水
            if (player.getRandom().nextDouble() < cfg.unsafeDrinkChance) {
                double drain = getUnsafeWaterDrain(waterType, cfg);
                addHydration(id, -drain);
                dehydrated = true;
            } else {
                // 25% 概率少量恢复
                addHydration(id, cfg.normalWaterDrinkRestore * 0.5);
            }
        } else {
            // 淡水湖：安全恢复
            addHydration(id, cfg.freshwaterDrinkRestore);
        }

        // 播放原版喝水音效 + 挥手动画（参照 LegendarySurvivalOverhaul 实现）
        player.getWorld().playSound(
            null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENTITY_GENERIC_DRINK,
            SoundCategory.PLAYERS, 1.0f, 1.0f
        );
        player.swingHand(Hand.MAIN_HAND, true);

        // 发送反馈（Actionbar，遵循用户偏好）
        Text feedback = buildDrinkFeedback(waterType, dehydrated);
        player.sendMessage(feedback, true);
        return true;
    }

    /** 检测当前位置的水体类型 */
    private WorldWaterType detectWaterType(BlockPos pos, ServerWorld world) {
        var biomeKey = world.getBiome(pos).getKey().orElse(null);
        if (biomeKey == null) return WorldWaterType.NORMAL_WATER;

        String biomeId = biomeKey.getValue().toString();
        return SaltLakeDetector.classify(biomeId, SaltLakeDetector.isSaltLake(pos, biomeKey));
    }

    /** 不安全水体的脱水量 */
    private double getUnsafeWaterDrain(WorldWaterType type, PdopnConfig.ThirstConfig cfg) {
        return switch (type) {
            case OCEAN -> Math.abs(cfg.seawaterDrinkDrain);
            case SALT_LAKE -> Math.abs(cfg.saltLakeDrinkDrain);
            default -> Math.abs(cfg.saltLakeDrinkDrain) * 0.5; // 普通水轻度脱水
        };
    }

    /** 构建饮水反馈文本 */
    private Text buildDrinkFeedback(WorldWaterType type, boolean dehydrated) {
        return switch (type) {
            case FRESHWATER_LAKE -> Text.translatable("pdopn.thirst.drink.freshwater")
                .formatted(Formatting.AQUA);
            case OCEAN -> dehydrated
                ? Text.translatable("pdopn.thirst.drink.ocean_bad").formatted(Formatting.RED)
                : Text.translatable("pdopn.thirst.drink.unsafe_ok").formatted(Formatting.YELLOW);
            case SALT_LAKE -> dehydrated
                ? Text.translatable("pdopn.thirst.drink.saltlake_bad").formatted(Formatting.RED)
                : Text.translatable("pdopn.thirst.drink.unsafe_ok").formatted(Formatting.YELLOW);
            case NORMAL_WATER -> dehydrated
                ? Text.translatable("pdopn.thirst.drink.normal_bad").formatted(Formatting.RED)
                : Text.translatable("pdopn.thirst.drink.unsafe_ok").formatted(Formatting.YELLOW);
        };
    }

    /* ══════════ 每 Tick 更新 ══════════ */

    /** 由主类在 END_SERVER_TICK 中调用 */
    public void tick(MinecraftServer server) {
        serverRef = server;
        tickCount++;
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;

        // 服务端就绪后补做玩家数据加载（JOIN 时还没有存档路径）
        if (!pendingLoads.isEmpty()) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                pendingLoads.removeIf(pending -> {
                    if (!pending.playerId().equals(player.getUuid())) return false;
                    loadPlayerData(pending.playerId());
                    // 加载前若已死亡，则立即重置，避免把 0 写回
                    if (awaitingRespawn.contains(pending.playerId())) {
                        resetHydration(pending.playerId());
                    }
                    return true;
                });
            }
        }

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                UUID id = player.getUuid();
                double current = getHydration(id);

                // 记录上一次值（用于趋势箭头）
                lastHydration.put(id, current);

                // 创造/旁观模式不消耗
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }

                // 死亡状态下不消耗口渴（玩家死亡后到重生前，实体仍在 world.getPlayers() 中）
                // 重置由 onPlayerDeath 负责；此处仅做登记，供重生时兜底
                if (player.isDead() || player.getHealth() <= 0.0f) {
                    awaitingRespawn.add(id);
                    continue;
                }

                // 重生兜底：曾死亡且此刻已恢复存活 → 重置口渴
                // 即使 onDeath 注入失效（例如版本变更导致注入点不再存在），也不会卡在 0
                if (awaitingRespawn.remove(id)) {
                    resetHydration(id);
                    current = getHydration(id);
                }

                // 计算口渴变化
                double delta = 0.0;

                // 1. 基础消耗
                delta -= cfg.baseDrainRate;

                // 2. 环境消耗（群系 + 体温联动）
                delta -= calcEnvironmentDrain(player, world);

                // 3. 行为消耗
                delta -= calcBehaviorDrain(player);

                // 4. 环境恢复（雨/水）
                delta += calcEnvironmentRestore(player, world);

                // 5. 食物效果（每 20 tick 检测一次正在使用的物品）
                // 食物效果在 Mixin 中处理，此处不重复

                // 应用变化
                double newHydration = Math.max(0.0, Math.min(cfg.maxValue, current + delta));
                hydrationMap.put(id, newHydration);

                // 每 40 tick 施加效果
                if (tickCount % 40 == 0) {
                    applyEffects(player, newHydration);
                }

                // 致死检测
                if (newHydration <= 0.0) {
                    player.damage(player.getDamageSources().starve(),
                        player.getMaxHealth() * 0.15f);
                }
            }
        }

        // 每 6000 tick（约 5 分钟）自动保存
        if (tickCount % 6000 == 0) {
            saveAllPlayerData();
        }
    }

    /* ══════════ 消耗计算 ══════════ */

    /**
     * 计算环境消耗。
     * 返回值语义：正数 = 本 tick 应从口渴值中扣除的量（调用方用 {@code delta -=}）。
     *
     * <p>旧实现返回 {@code base * (系数 - 1)}，在寒冷群系（系数 0.5 &lt; 1）会得到负数，
     * 再被 {@code delta -=} 减去后变成“环境补水”，导致雪原等地口渴不降反升。
     * 现在统一返回非负的消耗量。
     */
    private double calcEnvironmentDrain(ServerPlayerEntity player, ServerWorld world) {
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;
        double base = cfg.baseDrainRate;

        // 群系系数
        double biomeFactor = getBiomeThirstFactor(player);

        // 维度系数
        double dimFactor = getDimensionThirstFactor(player);

        // 体温联动
        double tempFactor = getTemperatureFactor(player);

        // 寒冷环境系数小于 1 → 消耗更慢，但绝不补水
        return base * biomeFactor * dimFactor * tempFactor;
    }

    /** 获取群系口渴系数 */
    private double getBiomeThirstFactor(ServerPlayerEntity player) {
        var biomeEntry = ((ServerWorld) player.getWorld()).getBiome(player.getBlockPos());
        var biomeKey = biomeEntry.getKey().orElse(null);
        if (biomeKey == null) return 1.0;

        String biomeId = biomeKey.getValue().toString();

        // 极热
        if (biomeId.contains("desert") || biomeId.contains("badlands") || biomeId.contains("soul_sand"))
            return 3.0;
        // 炎热
        if (biomeId.contains("savanna") || biomeId.contains("jungle") || biomeId.contains("nether_wastes"))
            return 2.0;
        // 温暖
        if (biomeId.contains("swamp") || biomeId.contains("mushroom"))
            return 1.3;
        // 寒冷
        if (biomeId.contains("taiga") || biomeId.contains("stony_shore"))
            return 0.7;
        // 极寒
        if (biomeId.contains("snowy") || biomeId.contains("frozen") || biomeId.contains("ice"))
            return 0.5;

        return 1.0; // 温和
    }

    /** 获取维度口渴系数 */
    private double getDimensionThirstFactor(ServerPlayerEntity player) {
        World world = player.getWorld();
        if (world.getRegistryKey() == World.NETHER) return 2.5;
        if (world.getRegistryKey() == World.END) return 0.8;
        return 1.0;
    }

    /** 获取体温联动系数 */
    private double getTemperatureFactor(ServerPlayerEntity player) {
        if (temperatureManager == null) return 1.0;
        double bodyTemp = Math.abs(temperatureManager.getBodyTemp(player.getUuid()));

        if (bodyTemp <= 10.0) return 1.0;
        if (bodyTemp <= 25.0) return 1.3;
        if (bodyTemp <= 45.0) return 1.6;
        if (bodyTemp <= 70.0) return 2.0;
        return 2.5;
    }

    /** 计算行为消耗 */
    private double calcBehaviorDrain(ServerPlayerEntity player) {
        double drain = 0.0;

        if (player.isSprinting()) {
            drain += 0.005;
        } else if (player.getVelocity().horizontalLengthSquared() > 0.01) {
            drain += 0.001;
        }

        if (player.isSwimming()) {
            drain += 0.002;
        }

        // 挖掘中
        if (player.isUsingItem() && player.getMainHandStack() != null) {
            // 简单判断：正在使用物品时额外消耗
            drain += 0.001;
        }

        // 火中/岩浆中
        if (player.isOnFire()) {
            drain += 0.010;
        }

        return drain;
    }

    /**
     * 计算环境恢复。
     * 返回值语义：正数 = 补充口渴，负数 = 额外脱水（调用方用 {@code delta +=}）。
     */
    private double calcEnvironmentRestore(ServerPlayerEntity player, ServerWorld world) {
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;
        double restore = 0.0;

        // 站在雨中
        if (world.isRaining() && world.isSkyVisible(player.getBlockPos().up())) {
            restore += cfg.rainRestore;
        }

        // 站在水中（检测脚下是否为水方块）
        if (player.isTouchingWater()) {
            var biomeEntry = world.getBiome(player.getBlockPos());
            var biomeKey = biomeEntry.getKey().orElse(null);
            if (biomeKey != null) {
                String biomeId = biomeKey.getValue().toString();
                if (biomeId.contains("ocean")) {
                    // 海水：强脱水（restore 为负 → 口渴下降）
                    restore -= cfg.seawaterStandingDrain;
                } else if (SaltLakeDetector.isSaltLake(player.getBlockPos(), biomeKey)) {
                    // 咸水湖：中等脱水（比海洋弱）
                    // saltLakeDrinkDrain 原为“直接饮用脱水值”，按系数 0.0002 折算为“站在水中速率”
                    restore -= Math.abs(cfg.saltLakeDrinkDrain) * 0.0002;
                } else {
                    // 淡水：缓慢恢复
                    restore += cfg.freshwaterStandingRestore;
                }
            }
        }

        return restore;
    }

    /* ══════════ 食物/饮品使用处理（由 Mixin 调用） ══════════ */

    /**
     * 处理玩家使用物品后的口渴变化。由 {@code ItemMixin} 注入 {@code Item#finishUsing} 调用。
     *
     * <p>注意：本方法此前无任何调用方，导致 ThirstData 中定义的食物 / 饮品恢复
     * 全部失效；现已通过 Item / Bucket 两个 Mixin 接回。
     *
     * @param stack 被使用的物品（用于区分含水药水与普通药水）
     */
    public void onItemUsed(ServerPlayerEntity player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        net.minecraft.item.Item item = stack.getItem();
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;
        UUID id = player.getUuid();

        // 1. 生水（水桶 / 含水水瓶）→ 脱水，需先烧炼净化
        if (ThirstData.isDehydratingDrink(item, stack)) {
            addHydration(id, cfg.seawaterDrinkDrain);
            return;
        }

        // 2. 其他饮品（蜂蜜瓶 / 炖汤等）
        if (ThirstData.isDrinkable(item)) {
            double restore = ThirstData.getDrinkRestore(item);
            // 蜂蜜瓶恢复量跟随配置
            if (item == net.minecraft.item.Items.HONEY_BOTTLE) {
                restore = cfg.honeyRestore;
            }
            addHydration(id, restore);
            return;
        }

        // 3. 含水食物
        if (ThirstData.hasFoodRestore(item)) {
            addHydration(id, ThirstData.getFoodRestore(item));
            return;
        }

        // 4. 脱水食物
        if (ThirstData.isDehydrating(item)) {
            addHydration(id, ThirstData.getDehydration(item));
        }
    }

    /* ══════════ 效果施加 ══════════ */

    private void applyEffects(ServerPlayerEntity player, double hydration) {
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;

        if (hydration >= cfg.comfortZoneLow) return; // 舒适区间，无效果

        if (hydration >= cfg.lightThirstLow) {
            // 轻度口渴：饥饿加速
            player.addExhaustion(0.3f);
        } else if (hydration >= cfg.mediumDehydrationLow) {
            // 中度脱水：饥饿加速 + 缓慢 I
            player.addExhaustion(0.5f);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 0, false, false));
        } else if (hydration >= cfg.heavyDehydrationLow) {
            // 重度脱水：饥饿加速 + 缓慢 I + 虚弱 I
            player.addExhaustion(0.8f);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 0, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0, false, false));
        } else if (hydration > 0.0) {
            // 危险：缓慢 II + 虚弱 II + 反胃 + 持续伤害
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1, false, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));
            if (tickCount % 40 == 0) {
                player.damage(player.getDamageSources().starve(), 1.0f);
            }
        } else {
            // 致死：凋零
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0, false, false));
        }
    }

    /* ══════════ HUD 显示 ══════════
     * HUD 渲染职责已迁移至 yifei.pdopn.hud.PdopnHudRenderer，
     * 本类仅通过 getHydration / getPreviousHydration 暴露数据。
     */

    /* ══════════ 持久化 ══════════ */

    /** 口渴数据文件名 */
    private static final String THIRST_FILE = "thirst.properties";

    private void loadPlayerData(UUID playerId) {
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;
        double value = PlayerDataStore.loadDouble(
            PlayerDataStore.resolveFile(serverRef, THIRST_FILE),
            playerId,
            cfg.initialValue,
            v -> Math.max(0.0, Math.min(cfg.maxValue, v))
        );
        hydrationMap.put(playerId, value);
    }

    private void savePlayerData(UUID playerId) {
        PlayerDataStore.saveDouble(
            PlayerDataStore.resolveFile(serverRef, THIRST_FILE),
            playerId,
            hydrationMap.getOrDefault(playerId, PdopnConfig.getInstance().thirst.initialValue)
        );
    }

    public void saveAllPlayerData() {
        if (serverRef == null) return;
        for (ServerWorld world : serverRef.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                savePlayerData(player.getUuid());
            }
        }
    }
}
