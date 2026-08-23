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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yifei.pdopn.config.PdopnConfig;
import yifei.pdopn.mode.PdopnMode;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    /** HUD 显示开关：默认开启 */
    private final Set<UUID> hudEnabled = Collections.synchronizedSet(new HashSet<>());

    /** 最大生存天数（可指令修改，存档级别） */
    private int maxDays = PdopnConfig.getInstance().temperature.defaultMaxDays;

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
    public int getMaxDays() { return maxDays; }
    public void setMaxDays(int days) { this.maxDays = Math.max(1, days); }

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

    /** 玩家加入时加载体温数据 */
    public void onPlayerJoin(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        loadPlayerData(id);
        hudEnabled.add(id); // 默认显示 HUD
    }

    /** 玩家离开时保存并清理 */
    public void onPlayerLeave(UUID playerId) {
        savePlayerData(playerId);
        bodyTemps.remove(playerId);
        lastEnvTemps.remove(playerId);
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

        // ── 偏移累加/衰减逻辑 ──
        // 永昼/永夜：每天累加 dailyDriftAmount（每 tick 累加一小部分），持续不衰减
        // 正常模式：偏移按 driftDecayRate 衰减回 0
        PdopnConfig.TemperatureConfig tcfg = PdopnConfig.getInstance().temperature;
        if (currentMode != PdopnMode.NORMAL) {
            perpetualTicks++;
            // 每 tick 累加 dailyDriftAmount / 24000（=每天加 dailyDriftAmount）
            double perTickDrift = tcfg.dailyDriftAmount / 24000.0;
            accumulatedDrift += (currentMode == PdopnMode.PERPETUAL_DAY) ? perTickDrift : -perTickDrift;
        } else if (Math.abs(accumulatedDrift) > 0.001) {
            // 正常模式：偏移衰减
            double decay = tcfg.driftDecayRate;
            if (accumulatedDrift > 0) {
                accumulatedDrift = Math.max(0, accumulatedDrift - decay);
            } else {
                accumulatedDrift = Math.min(0, accumulatedDrift + decay);
            }
        }

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                UUID id = player.getUuid();
                double currentTemp = getBodyTemp(id);

                // 创造/旁观模式不受温度影响
                boolean immune = player.isCreative() || player.isSpectator();

                // 计算环境温度（含偏移）
                double envTemp = calcEnvironmentTemp(player, currentMode);

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

                // 致死检测
                if (newTemp >= TemperatureData.MAX_TEMP) {
                    player.damage(player.getDamageSources().onFire(),
                        player.getMaxHealth() * 0.25f);
                } else if (newTemp <= TemperatureData.MIN_TEMP) {
                    player.damage(player.getDamageSources().freeze(),
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

    /* ══════════ 环境温度计算 ══════════ */

    /**
     * 计算玩家所在位置的完整环境温度（含偏移）。
     * 正常模式：纯环境温度 clamp 到 ±normalSafeRange，方块影响不 clamp
     * 永昼/永夜：环境温度 + 累计偏移，无 clamp
     */
    public double calcEnvironmentTemp(ServerPlayerEntity player, PdopnMode mode) {
        ServerWorld world = (ServerWorld) player.getWorld();
        BlockPos pos = player.getBlockPos();
        PdopnConfig.TemperatureConfig tcfg = PdopnConfig.getInstance().temperature;

        // 1. 维度基础温度
        double dimTemp = getDimensionTemp(player);

        // 2. 群系基础温度
        Biome biome = world.getBiome(pos).value();
        RegistryKey<Biome> biomeKey = world.getBiome(pos).getKey().orElse(null);
        double biomeTemp = (biomeKey != null)
            ? TemperatureData.getBiomeTemp(biomeKey, biome)
            : -20.0 + biome.getTemperature() * 35.0;

        // 下界/末地维度使用维度温度替代群系温度
        double baseTemp = (dimTemp != 0.0) ? dimTemp : biomeTemp;

        // 3. 时间修正
        double timeMod = TemperatureData.getTimeModifier(world.getTimeOfDay());

        // 4. 天气修正
        double weatherMod = 0.0;
        if (world.isRaining()) {
            weatherMod = world.isThundering()
                ? tcfg.thunderModifier
                : tcfg.rainModifier;
        }

        // 5. 海拔修正
        double altMod = TemperatureData.getAltitudeModifier(pos.getY());

        // 6. 附近方块修正（不参与安全 clamp，可导致致死）
        double blockMod = calcNearbyBlockEffect(world, pos);

        // 7. 室外检测（头顶无方块遮挡时时间/天气影响全效，室内减弱）
        boolean outdoors = world.isSkyVisible(pos.up());
        double exposureFactor = outdoors ? 1.0 : 0.3;

        // 纯环境温度（不含方块影响）
        double pureEnvTemp = baseTemp + (timeMod + weatherMod) * exposureFactor + altMod;

        if (mode == PdopnMode.NORMAL) {
            // 正常模式：纯环境温度 clamp 到安全范围，不会致死
            double safe = tcfg.normalSafeRange;
            pureEnvTemp = Math.max(-safe, Math.min(safe, pureEnvTemp));
            // 方块影响叠加（不 clamp）
            return pureEnvTemp + blockMod;
        } else {
            // 永昼/永夜：环境温度 + 累计偏移 + 方块影响，无 clamp
            return pureEnvTemp + accumulatedDrift + blockMod;
        }
    }

    /** 获取维度基础温度（下界/末地为非零值） */
    private double getDimensionTemp(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
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

    /* ══════════ HUD 显示 ══════════
     * HUD 渲染职责已迁移至 yifei.pdopn.hud.PdopnHudRenderer，
     * 本类仅通过 getBodyTemp / getLastEnvTemp 暴露数据。
     */

    /* ══════════ 工具方法 ══════════ */

    private static double clamp(double temp) {
        return Math.max(TemperatureData.MIN_TEMP, Math.min(TemperatureData.MAX_TEMP, temp));
    }

    /* ══════════ 温度持久化 ══════════ */

    /** 获取数据目录 */
    private Path getDataDir() {
        if (serverRef == null) return null;
        return serverRef.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("pdopn");
    }

    /** 加载单个玩家的体温数据 */
    private void loadPlayerData(UUID playerId) {
        Path dir = getDataDir();
        if (dir == null) {
            bodyTemps.put(playerId, TemperatureData.DEFAULT_BODY_TEMP);
            return;
        }
        File file = dir.resolve("temperatures.properties").toFile();
        if (!file.exists()) {
            bodyTemps.put(playerId, TemperatureData.DEFAULT_BODY_TEMP);
            return;
        }
        try (InputStream in = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(in);
            String val = props.getProperty(playerId.toString());
            double temp = val != null ? Double.parseDouble(val) : TemperatureData.DEFAULT_BODY_TEMP;
            bodyTemps.put(playerId, clamp(temp));
        } catch (IOException | NumberFormatException e) {
            LOGGER.warn("Failed to load temperature for {}: {}", playerId, e.getMessage());
            bodyTemps.put(playerId, TemperatureData.DEFAULT_BODY_TEMP);
        }
    }

    /** 保存单个玩家的体温数据 */
    private void savePlayerData(UUID playerId) {
        Path dir = getDataDir();
        if (dir == null) return;
        try {
            dir.toFile().mkdirs();
            File file = dir.resolve("temperatures.properties").toFile();

            // 加载现有数据
            Properties props = new Properties();
            if (file.exists()) {
                try (InputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
            }

            // 更新该玩家的数据
            props.setProperty(playerId.toString(), String.valueOf(bodyTemps.getOrDefault(playerId, 0.0)));

            // 写回文件
            try (OutputStream out = new FileOutputStream(file)) {
                props.store(out, "PDoPN Temperature Data");
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to save temperature for {}: {}", playerId, e.getMessage());
        }
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

    /** 保存全局偏移数据（accumulatedDrift + perpetualTicks） */
    private void saveGlobalData() {
        Path dir = getDataDir();
        if (dir == null) return;
        try {
            dir.toFile().mkdirs();
            File file = dir.resolve("drift.properties").toFile();
            Properties props = new Properties();
            props.setProperty("accumulatedDrift", String.valueOf(accumulatedDrift));
            props.setProperty("perpetualTicks", String.valueOf(perpetualTicks));
            try (OutputStream out = new FileOutputStream(file)) {
                props.store(out, "PDoPN Global Drift Data");
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to save global drift data: {}", e.getMessage());
        }
    }

    /** 加载全局偏移数据（服务器启动时调用） */
    public void loadGlobalData() {
        Path dir = getDataDir();
        if (dir == null) return;
        File file = dir.resolve("drift.properties").toFile();
        if (!file.exists()) return;
        try (InputStream in = new FileInputStream(file)) {
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
}
