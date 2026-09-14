package yifei.pdopn.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 全局配置管理器。
 * 存储在 .minecraft/config/pdopn/pdopn.json，跨存档共享。
 * 温度/口渴系统的可调参数均在此处，修改后重启生效。
 */
public class PdopnConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("PDoPN-Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PdopnConfig instance;

    /**
     * 当前配置文件结构版本。
     * 用途：字段增删或改名后（Gson 会默默丢弃无法映射的字段）可以据此识别旧配置并迁移，
     * 而不是让玩家在升级模组后发现配置被重置且毫无提示。
     *
     * <p><b>每次往配置里新增字段都必须递增此值</b>，否则旧配置文件不会触发迁移与回写，
     * 新字段虽然内存里存在（Gson 保留默认值）、但用户打开 pdopn.json 根本看不到。
     *
     * <p>版本历史：
     * <ul>
     *   <li>1 — 初始 temperature / thirst</li>
     *   <li>2 — 新增 entity.whitelist / entity.blacklist</li>
     *   <li>3 — 新增口渴降温相关项（*Cooling / coolantDurationTicks）</li>
     * </ul>
     */
    public static final int CURRENT_CONFIG_VERSION = 3;

    /** 配置文件结构版本（由文件内容读入；缺失视为 0，即 v1 之前的旧配置） */
    public int configVersion = CURRENT_CONFIG_VERSION;

    /**
     * 是否需要把内存中的配置回写到文件。
     * 载入到旧版本配置时置位，由主类在服务端首次 tick 时执行回写
     * （那时才确定模组已真正装载，避免在服务端之外触发文件写入）。
     */
    private static boolean needsRewrite = false;

    /**
     * 待回写的文件内容（文件原有值 + 补齐的默认项）。
     *
     * <p>回写必须写入「合并后的 JSON」而不是重新序列化配置对象：
     * 后者会丢掉用户手写在文件里、但类中没有对应字段的键，也会丢掉原文件里的注释性内容。
     */
    private static JsonObject mergeFile = null;

    /* ══════════ 温度系统配置 ══════════ */
    public TemperatureConfig temperature = new TemperatureConfig();

    /* ══════════ 口渴系统配置 ══════════ */
    public ThirstConfig thirst = new ThirstConfig();

    /* ══════════ 实体增强配置 ══════════ */
    public EntityConfig entity = new EntityConfig();

    /* ══════════ 内部类：实体增强配置 ══════════ */
    public static class EntityConfig {
        /**
         * 允许增强的实体 ID 白名单。
         * 为空（默认）表示「除黑名单外全部允许」，与原行为一致。
         * 填写后仅增强列表内的实体，例如 ["minecraft:zombie", "minecraft:skeleton"]。
         */
        public java.util.List<String> whitelist = new java.util.ArrayList<>();

        /**
         * 禁止增强的实体 ID 黑名单，优先级高于白名单。
         * 用于避让同样改写生物属性的模组，例如 ["modid:elite_zombie"]。
         */
        public java.util.List<String> blacklist = new java.util.ArrayList<>();
    }

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

        /* ── 饮水降温（°C，正数表示降温） ── */

        /** 净水瓶降温量 */
        public double pureWaterBottleCooling = 6.0;
        /** 净水桶降温量 */
        public double pureWaterBucketCooling = 20.0;
        /** 直接饮用淡水降温量 */
        public double freshwaterDrinkCooling = 3.0;
        /** 其他水体（海水 / 咸水湖 / 普通水）降温量：能降温但容易脱水 */
        public double unsafeDrinkCooling = 2.0;
        /** 降温效果持续时间（tick），默认 10 秒 */
        public int coolantDurationTicks = 200;
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
        Path configDir;
        try {
            configDir = FabricLoader.getInstance().getConfigDir().resolve("pdopn");
        } catch (Throwable t) {
            // 配置目录都拿不到时不应让整个模组初始化失败，用默认值继续并在日志中留痕
            LOGGER.error("[PDoPN] 无法获取配置目录，将使用默认配置（不会写入文件）: {}", t.toString());
            return new PdopnConfig();
        }
        Path configFile = configDir.resolve("pdopn.json");

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                // 先保留文件原始 JSON：合并必须以「文件里实际写的内容」为基准，
                // 否则用户显式写出的 null / 被 Gson 丢弃的未知键都会丢失
                JsonObject fromFile = JsonParser.parseReader(reader).getAsJsonObject();
                PdopnConfig config = GSON.fromJson(fromFile, PdopnConfig.class);
                if (config != null) {
                    int loadedVersion = config.configVersion;
                    normalize(config);

                    // 以「文件实际内容」为基准，把默认值里缺失的键补进去。
                    // 方向很重要：补齐结果写回 fromFile（即将写盘的对象），而不是写入 config 的序列化结果，
                    // 否则用户已改过的值会被默认值覆盖。
                    List<String> added = new ArrayList<>();
                    JsonObject defaults = GSON.toJsonTree(config).getAsJsonObject();
                    mergeDefaults(fromFile, defaults, "", added);

                    // 版本号是模组管理的元数据，不由用户维护，必须显式提升。
                    // 否则文件里原有的旧版本号会一直被保留，导致每次启动都判定为「需要回写」。
                    if (loadedVersion != CURRENT_CONFIG_VERSION) {
                        fromFile.addProperty("configVersion", CURRENT_CONFIG_VERSION);
                        added.add("configVersion");
                    }

                    mergeFile = fromFile;
                    needsRewrite = !added.isEmpty();

                    LOGGER.info("[PDoPN] Config loaded from {} (version {} -> {}, 补充 {} 个缺失项)",
                        configFile, loadedVersion, config.configVersion, added.size());
                    if (!added.isEmpty()) {
                        LOGGER.info("[PDoPN] 已补齐缺失配置项: {}", added);
                    }
                    return config;
                }
            } catch (Exception e) {
                LOGGER.warn("[PDoPN] Failed to load config, using defaults: {}", e.getMessage());
            }
        }

        // 不存在或加载失败 → 创建默认配置
        LOGGER.info("[PDoPN] 未找到配置文件，正在生成默认配置：{}", configFile);
        PdopnConfig config = new PdopnConfig();
        save(config);
        return config;
    }

    /**
     * 把 {@code defaults} 中「{@code target} 里不存在」的键补进 {@code target}。
     *
     * <p>这是「升级时补齐新增配置项」的核心：新增字段会被写入文件，而用户已经改过的值
     * 一律保留 —— 绝不用默认值覆盖既有值。
     *
     * <p>方向说明：{@code target} 是<b>即将写盘的文件内容</b>，{@code defaults} 是完整的
     * 默认结构。补齐必须写进 target，否则新增键不会落盘。
     *
     * <p>嵌套对象会被递归处理；当 target 中某个分组的类型不是对象时（例如用户手误写成数字），
     * 直接整体替换为默认结构。
     *
     * @param target   文件内容（就地修改）
     * @param defaults 完整默认结构
     * @param path     当前路径，仅用于日志展示
     * @param added    收集被补齐的键路径
     */
    static void mergeDefaults(JsonObject target, JsonObject defaults, String path, List<String> added) {
        for (Map.Entry<String, JsonElement> entry : new ArrayList<>(defaults.entrySet())) {
            String key = entry.getKey();
            String childPath = path.isEmpty() ? key : path + "." + key;
            JsonElement defaultValue = entry.getValue();

            if (!target.has(key) || target.get(key).isJsonNull()) {
                // 文件里缺少该项（或为 null）→ 用默认值补齐
                target.add(key, defaultValue.deepCopy());
                // 缺失的是整个分组时，按叶子键逐个登记，日志才能直接告诉用户「新增了哪些配置项」
                collectPaths(defaultValue, childPath, added);
                continue;
            }

            JsonElement targetValue = target.get(key);
            if (defaultValue.isJsonObject()) {
                if (targetValue.isJsonObject()) {
                    mergeDefaults(targetValue.getAsJsonObject(), defaultValue.getAsJsonObject(),
                        childPath, added);
                } else {
                    // 类型不符 → 以默认结构替换
                    target.add(key, defaultValue.deepCopy());
                    collectPaths(defaultValue, childPath, added);
                }
            }
            // 基本类型且文件里已存在 → 保留用户的值，不动
        }
    }

    /** 递归收集某个 JSON 结构中的所有叶子键路径（仅用于日志展示）。 */
    private static void collectPaths(JsonElement element, String path, List<String> out) {
        if (element != null && element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                collectPaths(entry.getValue(), path + "." + entry.getKey(), out);
            }
        } else {
            out.add(path);
        }
    }

    /**
     * 规范化配置对象：补齐缺失 / 被显式写成 null 的字段，并把版本号更新为当前版本。
     *
     * <p>Gson 对文件中不存在的字段会保留字段初始值，因此新字段在内存里总是可用的；
     * 但用户的<b>文件</b>里依然缺少这些键。此方法负责把内存状态补齐到当前结构，
     * 由调用方决定是否回写。
     *
     * @return true 表示确实做了填充（对象状态发生了变化）
     */
    static boolean normalize(PdopnConfig config) {
        boolean changed = false;

        if (config.temperature == null) {
            config.temperature = new TemperatureConfig();
            changed = true;
        }
        if (config.thirst == null) {
            config.thirst = new ThirstConfig();
            changed = true;
        }
        if (config.entity == null) {
            config.entity = new EntityConfig();
            changed = true;
        }
        if (config.entity.whitelist == null) {
            config.entity.whitelist = new java.util.ArrayList<>();
            changed = true;
        }
        if (config.entity.blacklist == null) {
            config.entity.blacklist = new java.util.ArrayList<>();
            changed = true;
        }

        if (config.configVersion != CURRENT_CONFIG_VERSION) {
            config.configVersion = CURRENT_CONFIG_VERSION;
            changed = true;
        }
        return changed;
    }

    /**
     * 是否有待回写的配置（载入到旧版本文件时为 true）。
     * 由主类在服务端首次 tick 时查询，随后调用 {@link #save()} 完成回写。
     */
    public static boolean needsRewrite() {
        return needsRewrite;
    }

    /**
     * 把配置写入文件。
     * 设为 public 以便主类在检测到旧版本文件时主动触发一次回写。
     */
    public static void save(PdopnConfig config) {
        Path configDir;
        try {
            configDir = FabricLoader.getInstance().getConfigDir().resolve("pdopn");
        } catch (Throwable t) {
            // 绝不让配置写入失败拖垮服务端 tick
            LOGGER.error("[PDoPN] 无法获取配置目录，已跳过写入: {}", t.toString());
            return;
        }
        Path configFile = configDir.resolve("pdopn.json");
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                // 优先写入「合并后的文件内容」：它保留了用户原有取值，
                // 并补上了新增/缺失的键；直接序列化配置对象会丢掉文件中无对应字段的键
                if (mergeFile != null) {
                    GSON.toJson(mergeFile, writer);
                } else {
                    GSON.toJson(config, writer);
                }
            }
            // 已回写则不重复标记
            needsRewrite = false;
            mergeFile = null;
            LOGGER.info("[PDoPN] Config saved to {}", configFile);
        } catch (Exception e) {
            // 捕获范围放宽到 Exception：权限不足、磁盘只读等都可能以非 IOException 形式出现
            LOGGER.error("[PDoPN] 配置写入失败（路径 {}）: {}", configFile, e.toString());
        }
    }

    /** 保存当前配置 */
    public void save() {
        save(this);
    }
}
