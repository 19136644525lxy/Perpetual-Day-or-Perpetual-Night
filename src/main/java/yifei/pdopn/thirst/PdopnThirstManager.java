package yifei.pdopn.thirst;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yifei.pdopn.config.PdopnConfig;
import yifei.pdopn.temperature.PdopnTemperatureManager;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    /** 服务端引用（用于持久化路径） */
    private MinecraftServer serverRef;

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

    /** 玩家加入时加载数据 */
    public void onPlayerJoin(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        loadPlayerData(id);
    }

    /** 玩家离开时保存并清理 */
    public void onPlayerLeave(UUID playerId) {
        savePlayerData(playerId);
        hydrationMap.remove(playerId);
        lastHydration.remove(playerId);
        drinkCooldowns.remove(playerId);
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
        // 海洋类：优先判定（含 frozen_ocean）
        if (biomeId.contains("ocean")) {
            return WorldWaterType.OCEAN;
        }
        // 咸水湖
        if (SaltLakeDetector.isSaltLake(pos, biomeKey)) {
            return WorldWaterType.SALT_LAKE;
        }
        // 淡水湖（候选群系且非咸水湖）
        if (SaltLakeDetector.isFreshwaterLake(pos, biomeKey)) {
            return WorldWaterType.FRESHWATER_LAKE;
        }
        // 普通水（其他群系的水体）
        return WorldWaterType.NORMAL_WATER;
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

    /** 计算环境消耗（群系系数 + 体温联动） */
    private double calcEnvironmentDrain(ServerPlayerEntity player, ServerWorld world) {
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;
        double base = cfg.baseDrainRate;

        // 群系系数
        double biomeFactor = getBiomeThirstFactor(player);

        // 维度系数
        double dimFactor = getDimensionThirstFactor(player);

        // 体温联动
        double tempFactor = getTemperatureFactor(player);

        return base * (biomeFactor * dimFactor * tempFactor - 1.0);
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

    /** 计算环境恢复（雨/水） */
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
                if (biomeId.contains("ocean") || biomeId.contains("frozen_ocean")) {
                    // 海水：强脱水
                    restore -= cfg.seawaterStandingDrain;
                } else if (SaltLakeDetector.isSaltLake(player.getBlockPos(), biomeKey)) {
                    // 咸水湖：中等脱水（比海洋弱）
                    // saltLakeDrinkDrain 原为"直接饮用脱水值"，按系数 0.0002 折算为"站在水中速率"
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

    /** 处理玩家使用物品后的口渴变化 */
    public void onItemUsed(ServerPlayerEntity player, net.minecraft.item.Item item) {
        PdopnConfig.ThirstConfig cfg = PdopnConfig.getInstance().thirst;

        // 检查饮品
        if (ThirstData.isDrinkable(item)) {
            addHydration(player.getUuid(), ThirstData.getDrinkRestore(item));
            return;
        }

        // 检查含水食物
        if (ThirstData.hasFoodRestore(item)) {
            addHydration(player.getUuid(), ThirstData.getFoodRestore(item));
            return;
        }

        // 检查脱水食物
        if (ThirstData.isDehydrating(item)) {
            addHydration(player.getUuid(), ThirstData.getDehydration(item));
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

    private Path getDataDir() {
        if (serverRef == null) return null;
        return serverRef.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("pdopn");
    }

    private void loadPlayerData(UUID playerId) {
        Path dir = getDataDir();
        if (dir == null) {
            hydrationMap.put(playerId, PdopnConfig.getInstance().thirst.initialValue);
            return;
        }
        File file = dir.resolve("thirst.properties").toFile();
        if (!file.exists()) {
            hydrationMap.put(playerId, PdopnConfig.getInstance().thirst.initialValue);
            return;
        }
        try (InputStream in = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(in);
            String val = props.getProperty(playerId.toString());
            double hydration = val != null ? Double.parseDouble(val) : PdopnConfig.getInstance().thirst.initialValue;
            hydrationMap.put(playerId, Math.max(0.0, Math.min(PdopnConfig.getInstance().thirst.maxValue, hydration)));
        } catch (IOException | NumberFormatException e) {
            LOGGER.warn("Failed to load thirst for {}: {}", playerId, e.getMessage());
            hydrationMap.put(playerId, PdopnConfig.getInstance().thirst.initialValue);
        }
    }

    private void savePlayerData(UUID playerId) {
        Path dir = getDataDir();
        if (dir == null) return;
        try {
            dir.toFile().mkdirs();
            File file = dir.resolve("thirst.properties").toFile();

            Properties props = new Properties();
            if (file.exists()) {
                try (InputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
            }

            props.setProperty(playerId.toString(),
                String.valueOf(hydrationMap.getOrDefault(playerId, 0.0)));

            try (OutputStream out = new FileOutputStream(file)) {
                props.store(out, "PDoPN Thirst Data");
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to save thirst for {}: {}", playerId, e.getMessage());
        }
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
