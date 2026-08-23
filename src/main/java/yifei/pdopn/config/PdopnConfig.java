package yifei.pdopn.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 全局配置管理器。
 * 存储在 .minecraft/config/pdopn/pdopn.json，跨存档共享。
 * 温度/口渴系统的可调参数均在此处，修改后重启生效。
 */
public class PdopnConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("PDoPN-Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PdopnConfig instance;

    /* ══════════ 温度系统配置 ══════════ */
    public TemperatureConfig temperature = new TemperatureConfig();

    /* ══════════ 口渴系统配置 ══════════ */
    public ThirstConfig thirst = new ThirstConfig();

    /* ══════════ 内部类：温度配置 ══════════ */
    public static class TemperatureConfig {
        /** 基础环境系数（体温趋向环境温度的速率） */
        public double baseEnvRate = 0.005;
        /** 每天偏移量 (°C)：永昼/永夜模式下每天累加的偏移值 */
        public double dailyDriftAmount = 1.0;
        /** 默认最大生存天数（仅用于指令参考，不影响偏移计算） */
        public int defaultMaxDays = 1000;
        /** 偏移衰减速率（正常模式下每 tick 衰减的偏移值） */
        public double driftDecayRate = 0.02;
        /** 正常模式环境温度安全范围 (°C)：纯环境温度被 clamp 到 [-safe, +safe]，不会致死 */
        public double normalSafeRange = 60.0;
        /** 下界基础温度 */
        public double netherBaseTemp = 40.0;
        /** 末地基础温度 */
        public double endBaseTemp = -20.0;
        /** 天气：下雨修正 */
        public double rainModifier = -3.0;
        /** 天气：雷暴修正 */
        public double thunderModifier = -5.0;
    }

    /* ══════════ 内部类：口渴配置 ══════════ */
    public static class ThirstConfig {
        /** 基础消耗（每 tick） */
        public double baseDrainRate = 0.005;
        /** 满值 */
        public double maxValue = 100.0;
        /** 初始值 */
        public double initialValue = 100.0;
        /** 舒适区间下限 */
        public double comfortZoneLow = 60.0;
        /** 轻度口渴下限 */
        public double lightThirstLow = 40.0;
        /** 中度脱水下限 */
        public double mediumDehydrationLow = 25.0;
        /** 重度脱水下限 */
        public double heavyDehydrationLow = 10.0;
        /** 净水瓶恢复量 */
        public double pureWaterBottleRestore = 15.0;
        /** 净水桶恢复量 */
        public double pureWaterBucketRestore = 25.0;
        /** 蜂蜜瓶恢复量 */
        public double honeyRestore = 20.0;
        /** 淡水直接饮用恢复量 */
        public double freshwaterDrinkRestore = 15.0;
        /** 海水直接饮用脱水值（负数） */
        public double seawaterDrinkDrain = -15.0;
        /** 咸水湖直接饮用脱水值（负数） */
        public double saltLakeDrinkDrain = -10.0;
        /** 普通水源直接饮用恢复量 */
        public double normalWaterDrinkRestore = 5.0;
        /** 站在淡水中恢复（每 tick） */
        public double freshwaterStandingRestore = 0.001;
        /** 站在海水中脱水（每 tick） */
        public double seawaterStandingDrain = 0.003;
        /** 雨中恢复（每 tick） */
        public double rainRestore = 0.002;
        /** 咸水湖异变概率 (0.0~1.0) */
        public double saltLakeChance = 0.25;
        /** 直接饮水冷却（tick 数，1 秒 = 20 tick） */
        public int drinkCooldownTicks = 40;
        /** 非淡水湖水体饮水口渴概率 (0.0~1.0)：海水/咸水湖/普通水均有此概率脱水 */
        public double unsafeDrinkChance = 0.75;
    }

    /* ══════════ 加载 / 保存 ══════════ */

    public static PdopnConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /** 强制重新加载配置（指令 /reload 时调用） */
    public static PdopnConfig reload() {
        instance = load();
        return instance;
    }

    private static PdopnConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("pdopn");
        Path configFile = configDir.resolve("pdopn.json");

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                PdopnConfig config = GSON.fromJson(reader, PdopnConfig.class);
                if (config != null) {
                    LOGGER.info("[PDoPN] Config loaded from {}", configFile);
                    return config;
                }
            } catch (Exception e) {
                LOGGER.warn("[PDoPN] Failed to load config, using defaults: {}", e.getMessage());
            }
        }

        // 不存在或加载失败 → 创建默认配置
        PdopnConfig config = new PdopnConfig();
        save(config);
        return config;
    }

    private static void save(PdopnConfig config) {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("pdopn");
        Path configFile = configDir.resolve("pdopn.json");
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(config, writer);
            }
            LOGGER.info("[PDoPN] Config saved to {}", configFile);
        } catch (IOException e) {
            LOGGER.warn("[PDoPN] Failed to save config: {}", e.getMessage());
        }
    }

    /** 保存当前配置 */
    public void save() {
        save(this);
    }
}
