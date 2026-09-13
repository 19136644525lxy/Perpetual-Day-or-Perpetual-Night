package yifei.pdopn.temperature;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yifei.pdopn.config.PdopnConfig;
import yifei.pdopn.damage.PdopnDamageTypes;
import yifei.pdopn.mode.PdopnMode;
import yifei.pdopn.storage.PlayerDataStore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 温度系统核心管理器。
 * 职责：玩家体温追踪、环境温度计算、效果施加、数据暴露。
 * HUD 渲染由 PdopnHudRenderer 负责，本类仅暴露必要数据。
 */
public final class PdopnTemperatureManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("PDoPN-Temperature");

    /** 玩家体温记录：UUID → 体温值（ConcurrentHashMap 保证线程安全） */
    private final Map<UUID, Double> bodyTemps = new ConcurrentHashMap<>();

    /** 最近一次环境温度缓存：UUID → 环境温度（含漂移），供 HUD 渲染器读取 */
    private final Map<UUID, Double> lastEnvTemps = new ConcurrentHashMap<>();

    /**
     * 纯环境温度缓存：UUID → (计算结果, 上次计算 tick, 上次计算所在方块)。
     * 维度 / 群系 / 时间 / 天气 / 海拔变化极慢，无需逐 tick 重算。
     */
    private final Map<UUID, EnvCache> envTempCache = new ConcurrentHashMap<>();

    /**
     * 附近方块温度影响缓存：UUID → (计算结果, 上次计算 tick)。
     * 该项每 tick 要扫描 5×5×5 = 125 个方块，是整个温度系统最重的开销。
     * 玩家静止时几乎不变，因此按 {@link #BLOCK_SCAN_INTERVAL} 节流。
     */
    private final Map<UUID, BlockCache> blockTempCache = new ConcurrentHashMap<>();

    /** 方块扫描节流间隔（tick）；20 tick = 1 秒 */
    private static final int BLOCK_SCAN_INTERVAL = 20;

    /** 纯环境温度重算间隔（tick），玩家移动到新方块时会立即失效 */
    private static final int ENV_RECALC_INTERVAL = 20;

    /** 纯环境温度缓存条目 */
    private record EnvCache(double value, int computedAtTick, BlockPos pos) {}

    /** 方块温度影响缓存条目 */
    private record BlockCache(double value, int computedAtTick) {}

    /**
     * 玩家上次所处的温度档位：UUID → 档位序号。
     * 用于在跨入更危险档位时只提示一次，避免每 2 秒刷屏。
     */
    private final Map<UUID, Integer> lastBand = new ConcurrentHashMap<>();

    /** 已提示过「接近最大生存天数」的玩家 */
    private final Set<UUID> maxDaysWarned = ConcurrentHashMap.newKeySet();

    /** HUD 显示开关：默认开启 */
    private final Set<UUID> hudEnabled = Collections.synchronizedSet(new HashSet<>());

    /**
     * 待加载数据的玩家队列。
     * 玩家 JOIN 事件早于服务端首个 tick，此时尚未持有 serverRef 与存档路径，
     * 直接加载会永远读不到存档（旧实现的持久化失效根因）。
     * 因此把加载推迟到 tick() 中 serverRef 就绪之后执行。
     */
    private final Queue<PendingLoad> pendingLoads = new ConcurrentLinkedQueue<>();

    /** 玩家数据加载任务 */
    private record PendingLoad(UUID playerId, ServerPlayerEntity player) {}

    /** 当前模式引用（由主类每 tick 更新） */
    private PdopnMode currentMode = PdopnMode.NORMAL;

    /** Tick 计数器 */
    private int tickCount = 0;

    /** 服务端引用（用于持久化路径） */
    private MinecraftServer serverRef;

    /** 永昼/永夜模式下的累计 tick（用于漂移计算，因为世界时间被锁定） */
    private long perpetualTicks = 0;

    /** 累计偏移值（永昼正向累加，永夜负向累加，正常模式衰减回 0） */
    private double accumulatedDrift = 0.0;

    /* ══════════ 公开 API ══════════ */

    public void setCurrentMode(PdopnMode mode) {
        // 永昼↔永夜直接切换：偏移值取反，保留累计天数
        // 例如永昼累计了 +50°C，切换到永夜后变为 -50°C，继续向冷方向累加
        boolean isPerpetualSwitch = (currentMode == PdopnMode.PERPETUAL_DAY && mode == PdopnMode.PERPETUAL_NIGHT)
                                  || (currentMode == PdopnMode.PERPETUAL_NIGHT && mode == PdopnMode.PERPETUAL_DAY);
        if (isPerpetualSwitch) {
            accumulatedDrift = -accumulatedDrift;
        }
        // 切换到正常模式：不取反，由 tick() 中的衰减逻辑自然回到 0
        this.currentMode = mode;
    }
    public int getMaxDays() {
        // 直接从配置读取，避免字段与配置文件脱节
        return PdopnConfig.getInstance().temperature.defaultMaxDays;
    }

    /**
     * 设置最大生存天数。
     * 旧实现只改内存字段，重启后丢失且与配置文件不一致；现在写回配置。
     */
    public void setMaxDays(int days) {
        PdopnConfig.getInstance().temperature.defaultMaxDays = Math.max(1, days);
        PdopnConfig.getInstance().save();
    }

    public double getBodyTemp(UUID playerId) {
        return bodyTemps.getOrDefault(playerId, TemperatureData.DEFAULT_BODY_TEMP);
    }

    public void setBodyTemp(UUID playerId, double temp) {
        bodyTemps.put(playerId, clamp(temp));
    }

    /** 获取最近一次环境温度（含漂移），供 HUD 渲染器读取 */
    public double getLastEnvTemp(UUID playerId) {
        return lastEnvTemps.getOrDefault(playerId, 0.0);
    }

    /** 玩家加入时：排队等待数据加载（真正的加载在 tick() 中执行） */
    public void onPlayerJoin(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        pendingLoads.add(new PendingLoad(id, player));
        hudEnabled.add(id); // 默认显示 HUD
    }

    /** 玩家离开时保存并清理 */
    public void onPlayerLeave(UUID playerId) {
        savePlayerData(playerId);
        bodyTemps.remove(playerId);
        lastEnvTemps.remove(playerId);
        envTempCache.remove(playerId);
        blockTempCache.remove(playerId);
        lastBand.remove(playerId);
        maxDaysWarned.remove(playerId);
        // 注意：hudEnabled 故意不清理，否则玩家关闭 HUD 后重新登录会被重置为开启
    }

    /** 玩家死亡时重置体温为默认值（偏移值保持全局累计，不重置） */
    public void onPlayerDeath(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        bodyTemps.put(id, TemperatureData.DEFAULT_BODY_TEMP);
        lastEnvTemps.put(id, TemperatureData.DEFAULT_BODY_TEMP);
    }

    /* ══════════ HUD 开关 ══════════ */

    /** 切换玩家 HUD 显示状态 */
    public boolean toggleHud(UUID playerId) {
        if (hudEnabled.contains(playerId)) {
            hudEnabled.remove(playerId);
            return false;
        } else {
            hudEnabled.add(playerId);
            return true;
        }
    }

    /** 玩家 HUD 是否显示 */
    public boolean isHudEnabled(UUID playerId) {
        return hudEnabled.contains(playerId);
    }

    /* ══════════ 每 Tick 更新 ══════════ */

    /** 由主类在 END_SERVER_TICK 中调用 */
    public void tick(MinecraftServer server) {
        serverRef = server;
        tickCount++;

        // 服务端就绪后补做玩家数据加载（JOIN 时还没有存档路径）
        if (!pendingLoads.isEmpty()) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                pendingLoads.removeIf(pending -> {
                    if (!pending.playerId().equals(player.getUuid())) return false;
                    loadPlayerData(pending.playerId());
                    return true;
                });
            }
        }

        // ── 偏移累加/衰减逻辑 ──
        // 永昼/永夜：每天累加 dailyDriftAmount（每 tick 累加一小部分），持续不衰减
        // 正常模式：偏移按 driftDecayRate 衰减回 0
        PdopnConfig.TemperatureConfig tcfg = PdopnConfig.getInstance().temperature;
        if (currentMode != PdopnMode.NORMAL) {
            perpetualTicks++;
            // 每 tick 累加 dailyDriftAmount / 24000（= 每天累加 dailyDriftAmount）
            accumulatedDrift += TemperatureBands.driftPerTick(
                tcfg.dailyDriftAmount, currentMode == PdopnMode.PERPETUAL_DAY);
        } else if (Math.abs(accumulatedDrift) > 0.001) {
            accumulatedDrift = TemperatureBands.decayDrift(accumulatedDrift, tcfg.driftDecayRate);
        }

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                UUID id = player.getUuid();
                double currentTemp = getBodyTemp(id);

                // 创造/旁观模式不受温度影响
                boolean immune = player.isCreative() || player.isSpectator();

                // 死亡状态下不消耗体温（玩家死亡后到重生前，实体仍在 world.getPlayers() 中）
                // 体温重置由 onPlayerDeath 负责，全局偏移不受影响继续累计
                if (player.isDead() || player.getHealth() <= 0.0f) {
                    continue;
                }

                // 计算环境温度（含偏移）——带缓存节流，见 envCacheFor
                double envTemp = envCacheFor(player, currentMode);
                double blockTemp = blockTempCacheFor(player);

                // 装备隔热系数
                double insulation = calcInsulation(player);

                // 手持物品温度调节（每 20 tick 施加一次，避免效果过强）
                double itemEffect = (tickCount % 20 == 0) ? calcHeldItemEffect(player) : 0.0;

                // 体温变化：趋向目标温度（受隔热影响）+ 物品直接调节
                double envRate = tcfg.baseEnvRate;
                double rate = envRate * (1.0 - insulation);
                double delta = (envTemp - currentTemp) * rate + itemEffect;
                double newTemp = clamp(currentTemp + delta);

                bodyTemps.put(id, newTemp);
                // 缓存环境温度供 HUD 渲染器读取
                lastEnvTemps.put(id, envTemp);

                // 创造/旁观模式跳过效果和伤害
                if (immune) continue;

                // 每 40 tick 施加效果
                if (tickCount % 40 == 0) {
                    applyEffects(player, newTemp);
                }

                // 致死检测：使用模组自定义伤害类型，以显示专属死因
                if (newTemp >= TemperatureData.MAX_TEMP) {
                    player.damage(PdopnDamageTypes.heat(player),
                        player.getMaxHealth() * 0.25f);
                } else if (newTemp <= TemperatureData.MIN_TEMP) {
                    player.damage(PdopnDamageTypes.cold(player),
                        player.getMaxHealth() * 0.25f);
                }
            }
        }

        // 每 6000 tick（约 5 分钟）自动保存所有玩家温度数据 + 全局偏移
        if (tickCount % 6000 == 0) {
            saveAllPlayerData();
            saveGlobalData();
        }
    }

    /* ══════════ 环境温度缓存（性能节流） ══════════
     * 环境温度原来每个玩家每 tick 全量重算一次，其中最重的是 125 次
     * world.getBlockState 扫描。这些量的实际变化频率远低于 tick 频率，
     * 因此拆成两部分分别缓存：
     *   - 纯环境温度：每 20 tick 重算，或玩家离开当前方块时立即重算
     *   - 附近方块影响：每 20 tick 重算
     * 玩家站在岩浆旁等场景最迟 1 秒（20 tick）内生效，
     * 而 bodyTemp 每 tick 仍照常用缓存值趋向，体感无差别。
     */

    /**
     * 获取（可能来自缓存的）纯环境温度。
     * 无缓存、玩家换了方块、或距上次计算超过 {@link #ENV_RECALC_INTERVAL} tick 时重算。
     */
    private double envCacheFor(ServerPlayerEntity player, PdopnMode mode) {
        UUID id = player.getUuid();
        ServerWorld world = (ServerWorld) player.getWorld();
        BlockPos pos = player.getBlockPos();

        EnvCache cached = envTempCache.get(id);
        boolean movedToNewBlock = cached == null || !pos.equals(cached.pos());
        boolean stale = cached == null || (tickCount - cached.computedAtTick()) >= ENV_RECALC_INTERVAL;

        double pure;
        if (movedToNewBlock || stale) {
            pure = calcPureEnvTemp(world, pos);
            envTempCache.put(id, new EnvCache(pure, tickCount, pos.toImmutable()));
        } else {
            pure = cached.value();
        }

        return combineEnvTemp(pure, blockTempCacheFor(player), mode);
    }

    /**
     * 获取（可能来自缓存的）附近方块温度影响。
     * 这是温度系统唯一的重量级计算（125 次方块状态读取）。
     */
    private double blockTempCacheFor(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        BlockCache cached = blockTempCache.get(id);
        if (cached != null && (tickCount - cached.computedAtTick()) < BLOCK_SCAN_INTERVAL) {
            return cached.value();
        }
        double value = calcNearbyBlockEffect((ServerWorld) player.getWorld(), player.getBlockPos());
        blockTempCache.put(id, new BlockCache(value, tickCount));
        return value;
    }

    /* ══════════ 环境温度计算 ══════════ */

    /**
     * 计算玩家所在位置的完整环境温度（含偏移）。
     * 正常模式：纯环境温度 clamp 到 ±normalSafeRange，方块影响不 clamp
     * 永昼/永夜：环境温度 + 累计偏移，无 clamp
     *
     * <p>注意：本方法每次调用都会做完整计算（含 5×5×5 方块扫描）。
     * 逐 tick 的调用方应改用 {@link #envCacheFor} + {@link #blockTempCacheFor}。
     */
    public double calcEnvironmentTemp(ServerPlayerEntity player, PdopnMode mode) {
        ServerWorld world = (ServerWorld) player.getWorld();
        BlockPos pos = player.getBlockPos();
        return combineEnvTemp(calcPureEnvTemp(world, pos), calcNearbyBlockEffect(world, pos), mode);
    }

    /**
     * 计算「纯环境温度」：维度 / 群系 / 时间 / 天气 / 海拔，不含累积偏移与附近方块。
     */
    private double calcPureEnvTemp(ServerWorld world, BlockPos pos) {
        PdopnConfig.TemperatureConfig tcfg = PdopnConfig.getInstance().temperature;

        // 1. 维度基础温度（非零表示下界 / 末地，覆盖群系温度）
        double dimTemp = getDimensionTemp(world);

        // 2. 群系基础温度
        RegistryKey<Biome> biomeKey = world.getBiome(pos).getKey().orElse(null);
        double biomeTemp = (biomeKey != null)
            ? TemperatureData.getBiomeTemp(biomeKey, world.getBiome(pos).value())
            : -20.0 + world.getBiome(pos).value().getTemperature() * 35.0;

        double baseTemp = (dimTemp != 0.0) ? dimTemp : biomeTemp;

        // 3. 时间修正
        double timeMod = TemperatureData.getTimeModifier(world.getTimeOfDay());

        // 4. 天气修正（读取配置，旧实现绕过了配置导致 rainModifier/thunderModifier 改了不起作用）
        double weatherMod = 0.0;
        if (world.isRaining()) {
            weatherMod = world.isThundering() ? tcfg.thunderModifier : tcfg.rainModifier;
        }

        // 5. 海拔修正
        double altMod = TemperatureData.getAltitudeModifier(pos.getY());

        // 6. 室外检测（头顶无方块遮挡时时间/天气影响全效，室内减弱至 30%）
        double exposureFactor = world.isSkyVisible(pos.up()) ? 1.0 : 0.3;

        return baseTemp + (timeMod + weatherMod) * exposureFactor + altMod;
    }

    /** 按模式把纯环境温度、累积偏移与方块影响合成为最终环境温度 */
    private double combineEnvTemp(double pureEnvTemp, double blockMod, PdopnMode mode) {
        if (mode == PdopnMode.NORMAL) {
            // 正常模式：纯环境温度 clamp 到安全范围（方块影响不 clamp，仍可致死）
            double safe = PdopnConfig.getInstance().temperature.normalSafeRange;
            return Math.max(-safe, Math.min(safe, pureEnvTemp)) + blockMod;
        }
        // 永昼/永夜：环境温度 + 累计偏移 + 方块影响，无 clamp
        return pureEnvTemp + accumulatedDrift + blockMod;
    }

    /** 获取维度基础温度（下界/末地为非零值） */
    private double getDimensionTemp(ServerWorld world) {
        PdopnConfig.TemperatureConfig tcfg = PdopnConfig.getInstance().temperature;
        if (world.getRegistryKey() == World.NETHER) return tcfg.netherBaseTemp;
        if (world.getRegistryKey() == World.END) return tcfg.endBaseTemp;
        return 0.0;
    }

    /** 计算 5×5×5 范围内方块的总温度影响 */
    private double calcNearbyBlockEffect(ServerWorld world, BlockPos center) {
        double total = 0.0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = world.getBlockState(mutable);
                    Block block = state.getBlock();

                    if (TemperatureData.hasBlockTemp(block)) {
                        double blockTemp = TemperatureData.getBlockTemp(block);
                        int radius = TemperatureData.getBlockRadius(block);
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        // 距离衰减：范围内全效，超出范围线性衰减
                        double factor = (dist <= radius) ? 1.0 : Math.max(0, 1.0 - (dist - radius));
                        total += blockTemp * factor;
                    }
                }
            }
        }
        return total;
    }

    /* ══════════ 装备与物品 ══════════ */

    /** 计算玩家装备的总隔热系数 (0.0~1.0) */
    private double calcInsulation(ServerPlayerEntity player) {
        double total = 0.0;
        for (ItemStack stack : player.getArmorItems()) {
            if (!stack.isEmpty()) {
                total += TemperatureData.getArmorInsulation(stack.getItem());
            }
        }
        // 海龟壳水下额外隔热
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        if (helmet.getItem() == net.minecraft.item.Items.TURTLE_HELMET
            && player.isTouchingWater()) {
            total += 0.20;
        }
        return Math.min(total, 0.9);
    }

    /** 计算主手+副手物品的温度调节 */
    private double calcHeldItemEffect(ServerPlayerEntity player) {
        double total = 0.0;
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        if (!mainHand.isEmpty()) {
            total += TemperatureData.getItemTemp(mainHand.getItem());
        }
        if (!offHand.isEmpty()) {
            total += TemperatureData.getItemTemp(offHand.getItem());
        }
        return total;
    }

    /* ══════════ 温度漂移 ══════════
     * 偏移机制已重构为累加式：
     * - 永昼/永夜：每 tick 累加 dailyDriftAmount / 24000，持续不衰减
     * - 正常模式：偏移按 driftDecayRate 衰减回 0
     * 累计偏移值存储在 accumulatedDrift 字段，由 tick() 更新。
     */

    /* ══════════ 效果施加 ══════════ */

    /** 根据体温施加状态效果 */
    private void applyEffects(ServerPlayerEntity player, double temp) {
        double abs = Math.abs(temp);
        boolean isHot = temp > 0;

        // 跨档位预警（只在首次进入更危险档位时提示，避免刷屏）
        warnOnBandEntry(player, abs, isHot);
        warnOnMaxDays(player);

        if (abs < 10.0) return; // 舒适区间，无效果

        if (abs >= 10.0 && abs < 25.0) {
            // 轻度
            if (isHot) {
                player.addExhaustion(0.5f);
            } else {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 0, false, false));
            }
        } else if (abs >= 25.0 && abs < 45.0) {
            // 中度
            if (isHot) {
                player.addExhaustion(1.0f);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 0, false, false));
            } else {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 100, 0, false, false));
            }
        } else if (abs >= 45.0 && abs < 70.0) {
            // 重度
            if (isHot) {
                player.addExhaustion(2.0f);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0, false, false));
            } else {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 100, 1, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 100, 1, false, false));
            }
        } else if (abs >= 70.0 && abs < 85.0) {
            // 危险
            if (isHot) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));
            } else {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1, false, false));
            }
        } else {
            // 极限 (85~100)
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0, false, false));
        }
    }

    /* ══════════ 危险预警 ══════════ */

    /**
     * 玩家跨入更危险档位时给出声画预警。
     *
     * <p>此前体温跨过 70/85 只有状态效果，玩家在死亡前几乎没有任何预警；
     * 这里在「危险」(4) 与「极限」(5) 两档入口播放音效 + ActionBar 提示。
     */
    private void warnOnBandEntry(ServerPlayerEntity player, double abs, boolean isHot) {
        UUID id = player.getUuid();
        int band = TemperatureBands.bandOf(abs);
        Integer previous = lastBand.put(id, band);

        // 仅在档位上升时提示，且只提示危险档以上
        if (previous != null && band <= previous) return;
        if (band < 4) return;

        String key = isHot ? "pdopn.warn.heat" : "pdopn.warn.cold";
        player.sendMessage(Text.translatable(key)
            .formatted(isHot ? Formatting.RED : Formatting.AQUA), true);

        // 危险档用较轻的提示音，极限档换成更急促的音效
        var sound = isHot
            ? (band >= 5 ? SoundEvents.ENTITY_BLAZE_HURT : SoundEvents.BLOCK_FIRE_EXTINGUISH)
            : (band >= 5 ? SoundEvents.ENTITY_PLAYER_HURT_FREEZE : SoundEvents.BLOCK_GLASS_BREAK);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
            sound, SoundCategory.PLAYERS, 0.8f, isHot ? 1.0f : 0.8f);
    }

    /**
     * 达到配置的最大生存天数时提示一次。
     * 该配置此前完全是装饰性的（无任何代码读取），这里至少让它具备可见的提示作用。
     */
    private void warnOnMaxDays(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (maxDaysWarned.contains(id)) return;
        if (currentMode == PdopnMode.NORMAL) return;

        long days = getPerpetualDays();
        if (days >= getMaxDays()) {
            maxDaysWarned.add(id);
            player.sendMessage(Text.translatable("pdopn.warn.maxdays")
                .append(Text.literal(" (" + days + ")").formatted(Formatting.YELLOW))
                .formatted(Formatting.GOLD), true);
        }
    }

    /* ══════════ HUD 显示 ══════════
     * HUD 渲染职责已迁移至 yifei.pdopn.hud.PdopnHudRenderer，
     * 本类仅通过 getBodyTemp / getLastEnvTemp 暴露数据。
     */

    /* ══════════ 工具方法 ══════════ */

    private static double clamp(double temp) {
        return Math.max(TemperatureData.MIN_TEMP, Math.min(TemperatureData.MAX_TEMP, temp));
    }

    /* ══════════ 温度持久化 ══════════ */

    /** 体温数据文件名 */
    private static final String TEMP_FILE = "temperatures.properties";

    /** 加载单个玩家的体温数据 */
    private void loadPlayerData(UUID playerId) {
        double temp = PlayerDataStore.loadDouble(
            PlayerDataStore.resolveFile(serverRef, TEMP_FILE),
            playerId,
            TemperatureData.DEFAULT_BODY_TEMP,
            PdopnTemperatureManager::clamp
        );
        bodyTemps.put(playerId, temp);
    }

    /** 保存单个玩家的体温数据 */
    private void savePlayerData(UUID playerId) {
        PlayerDataStore.saveDouble(
            PlayerDataStore.resolveFile(serverRef, TEMP_FILE),
            playerId,
            bodyTemps.getOrDefault(playerId, TemperatureData.DEFAULT_BODY_TEMP)
        );
    }

    /** 保存所有在线玩家数据（定期调用） */
    public void saveAllPlayerData() {
        if (serverRef == null) return;
        for (ServerWorld world : serverRef.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                savePlayerData(player.getUuid());
            }
        }
    }

    /* ══════════ 全局偏移持久化 ══════════ */

    /** 全局偏移数据文件名 */
    private static final String DRIFT_FILE = "drift.properties";

    /** 保存全局偏移数据（accumulatedDrift + perpetualTicks） */
    public void saveGlobalData() {
        Path path = PlayerDataStore.resolveFile(serverRef, DRIFT_FILE);
        if (path == null) return;
        try {
            Files.createDirectories(path.getParent());
            Properties props = new Properties();
            props.setProperty("accumulatedDrift", String.valueOf(accumulatedDrift));
            props.setProperty("perpetualTicks", String.valueOf(perpetualTicks));
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "PDoPN Global Drift Data");
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to save global drift data: {}", e.getMessage());
        }
    }

    /** 加载全局偏移数据（服务器启动时调用） */
    public void loadGlobalData() {
        Path path = PlayerDataStore.resolveFile(serverRef, DRIFT_FILE);
        if (path == null || !Files.exists(path)) return;
        try (InputStream in = Files.newInputStream(path)) {
            Properties props = new Properties();
            props.load(in);
            accumulatedDrift = Double.parseDouble(props.getProperty("accumulatedDrift", "0.0"));
            perpetualTicks = Long.parseLong(props.getProperty("perpetualTicks", "0"));
        } catch (IOException | NumberFormatException e) {
            LOGGER.warn("Failed to load global drift data: {}", e.getMessage());
        }
    }

    /** 获取当前累计偏移值（供指令/HUD 查询） */
    public double getAccumulatedDrift() {
        return accumulatedDrift;
    }

    /** 获取当前运行模式（供 HUD 显示） */
    public PdopnMode getCurrentMode() {
        return currentMode;
    }

    /** 永昼 / 永夜模式已持续的完整天数（供 HUD 显示；1 天 = 24000 tick） */
    public long getPerpetualDays() {
        return perpetualTicks / 24000L;
    }

    /**
     * 清空累计偏移与持续计数。
     * 之前偏移只增不减且无任何缓解手段，中后期存档无法挽回，只能改配置重启。
     */
    public void resetAccumulatedDrift() {
        accumulatedDrift = 0.0;
        perpetualTicks = 0L;
    }
}
