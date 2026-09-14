package yifei.pdopn.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置规范化逻辑测试。
 *
 * <p>锁定的是一个真实报告过的问题：README 里写了 {@code entity.whitelist} /
 * {@code entity.blacklist} / {@code configVersion}，但用户打开 {@code pdopn.json}
 * 根本看不到这些键。
 *
 * <p>成因有两层：
 * <ol>
 *   <li>Gson 对文件中缺失的字段只保留内存默认值，不会新增到文件里；</li>
 *   <li>回写只在版本落后时触发，而新增字段时忘记递增 {@code CURRENT_CONFIG_VERSION}，
 *       于是文件永远停留在旧结构。</li>
 * </ol>
 *
 * <p>{@link PdopnConfig#normalize} 是纯粹的字段补齐逻辑，不触碰文件系统，
 * 因此可以在无游戏环境下直接验证。
 */
class PdopnConfigTest {

    @Test
    @DisplayName("null 的嵌套配置会被补齐，避免下游 NPE")
    void normalizesNullSections() {
        PdopnConfig config = new PdopnConfig();
        config.temperature = null;
        config.thirst = null;
        config.entity = null;

        assertTrue(PdopnConfig.normalize(config), "应当报告发生了变更");

        assertNotNull(config.temperature);
        assertNotNull(config.thirst);
        assertNotNull(config.entity);
        assertNotNull(config.entity.whitelist);
        assertNotNull(config.entity.blacklist);
    }

    @Test
    @DisplayName("显式写成 null 的实体列表会被补齐")
    void normalizesNullEntityLists() {
        PdopnConfig config = new PdopnConfig();
        config.entity.whitelist = null;
        config.entity.blacklist = null;

        assertTrue(PdopnConfig.normalize(config));

        assertNotNull(config.entity.whitelist);
        assertNotNull(config.entity.blacklist);
        assertTrue(config.entity.whitelist.isEmpty(), "默认应为空列表而非 null");
    }

    @Test
    @DisplayName("旧版本号会被提升到当前版本")
    void upgradesOldVersion() {
        PdopnConfig config = new PdopnConfig();
        config.configVersion = 1;

        assertTrue(PdopnConfig.normalize(config));
        assertEquals(PdopnConfig.CURRENT_CONFIG_VERSION, config.configVersion);
    }

    @Test
    @DisplayName("缺失版本号（旧文件没有该键，Gson 读出 0）也会被提升")
    void upgradesMissingVersion() {
        PdopnConfig config = new PdopnConfig();
        config.configVersion = 0;

        assertTrue(PdopnConfig.normalize(config));
        assertEquals(PdopnConfig.CURRENT_CONFIG_VERSION, config.configVersion);
    }

    @Test
    @DisplayName("已是最新且字段完整时不报告变更（避免每次启动都回写）")
    void noChangeWhenAlreadyCurrent() {
        PdopnConfig config = new PdopnConfig();

        assertFalse(PdopnConfig.normalize(config),
            "全新默认配置不应被判定为需要迁移");
        assertEquals(PdopnConfig.CURRENT_CONFIG_VERSION, config.configVersion);
    }

    @Test
    @DisplayName("新增字段后版本号必须大于历史版本，否则旧文件不会触发回写")
    void versionIsAtLeastThree() {
        // 版本 2 = entity.*，版本 3 = 口渴降温相关项。
        // 若将来新增字段却忘记递增，这里会失败，提醒更新版本号。
        assertTrue(PdopnConfig.CURRENT_CONFIG_VERSION >= 3,
            "新增配置字段后必须递增 CURRENT_CONFIG_VERSION");
    }

    /* ────────── 文件级合并（新增键补齐、既有值不覆盖） ────────── */

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    @DisplayName("缺失的键会被补进文件内容")
    void mergeAddsMissingKeys() {
        // target = 即将写盘的文件内容，defaults = 完整默认结构
        JsonObject target = parse("{\"a\":1}");
        JsonObject defaults = parse("{\"a\":1,\"b\":{\"c\":2,\"d\":3}}");

        List<String> added = new ArrayList<>();
        PdopnConfig.mergeDefaults(target, defaults, "", added);

        // 补齐的记录应为「具体配置项」级（叶子键），日志才能直接告诉用户新增了什么
        assertEquals(2, added.size(), "实际登记=" + added);
        assertTrue(added.contains("b.c"), "实际登记=" + added);
        assertTrue(added.contains("b.d"), "实际登记=" + added);
        assertTrue(target.has("b"), "缺失的分组必须写进文件内容");
        assertEquals(2, target.getAsJsonObject("b").get("c").getAsInt());
        assertEquals(3, target.getAsJsonObject("b").get("d").getAsInt());
    }

    @Test
    @DisplayName("用户已改过的值绝不被默认值覆盖")
    void mergeNeverOverwritesExistingValues() {
        // 用户把 a 改成 99、把 b.c 改成 42
        JsonObject target = parse("{\"a\":99,\"b\":{\"c\":42}}");
        JsonObject defaults = parse("{\"a\":1,\"b\":{\"c\":2}}");

        List<String> added = new ArrayList<>();
        PdopnConfig.mergeDefaults(target, defaults, "", added);

        assertTrue(added.isEmpty(), "没有任何键缺失时不应报告补齐");
        assertEquals(99, target.get("a").getAsInt(), "a 必须保留用户的 99");
        assertEquals(42, target.getAsJsonObject("b").get("c").getAsInt(), "b.c 必须保留用户的 42");
    }

    @Test
    @DisplayName("补齐缺失键的同时保留同分组内用户改过的值（核心场景）")
    void mergeAddsNewKeysWhileKeepingUserValues() {
        // 用户改了 maxValue，同时文件里缺少后来新增的降温项
        JsonObject target = parse("{\"thirst\":{\"maxValue\":250.0}}");
        JsonObject defaults = parse(
            "{\"thirst\":{\"maxValue\":100.0,\"pureWaterBottleCooling\":6.0,\"coolantDurationTicks\":200}}");

        List<String> added = new ArrayList<>();
        PdopnConfig.mergeDefaults(target, defaults, "", added);

        JsonObject thirst = target.getAsJsonObject("thirst");
        assertEquals(250.0, thirst.get("maxValue").getAsDouble(), 1.0e-9,
            "用户改过的 maxValue 不能被重置");
        assertEquals(6.0, thirst.get("pureWaterBottleCooling").getAsDouble(), 1.0e-9,
            "新增项应被补上");
        assertEquals(200, thirst.get("coolantDurationTicks").getAsInt());
        assertEquals(2, added.size(), "只应报告两个新增项");
    }

    @Test
    @DisplayName("文件里显式写成 null 的项按缺失处理并补齐")
    void mergeTreatsNullAsMissing() {
        JsonObject target = parse("{\"entity\":null}");
        JsonObject defaults = parse("{\"entity\":{\"whitelist\":[],\"blacklist\":[]}}");

        List<String> added = new ArrayList<>();
        PdopnConfig.mergeDefaults(target, defaults, "", added);

        assertFalse(added.isEmpty());
        assertTrue(target.getAsJsonObject("entity").has("whitelist"));
        assertTrue(target.getAsJsonObject("entity").has("blacklist"));
    }

    @Test
    @DisplayName("分组的类型被写错时以默认结构替换")
    void mergeReplacesWrongTypedSection() {
        JsonObject target = parse("{\"thirst\":5}");
        JsonObject defaults = parse("{\"thirst\":{\"maxValue\":100.0}}");

        List<String> added = new ArrayList<>();
        PdopnConfig.mergeDefaults(target, defaults, "", added);

        // 类型错误的分组被整体替换后，其子项按叶子键登记
        assertTrue(added.contains("thirst.maxValue"),
            "被替换分组的子项应被登记，实际=" + added);
        assertTrue(target.get("thirst").isJsonObject(), "应以默认结构替换错误类型");
        assertEquals(100.0, target.getAsJsonObject("thirst").get("maxValue").getAsDouble(), 1.0e-9);
    }

    @Test
    @DisplayName("值相等的键不会被误报为补齐")
    void mergeDoesNotReportUnchanged() {
        JsonObject target = parse("{\"a\":1.0,\"b\":{\"c\":true}}");
        JsonObject defaults = parse("{\"a\":1.0,\"b\":{\"c\":true}}");

        List<String> added = new ArrayList<>();
        PdopnConfig.mergeDefaults(target, defaults, "", added);

        assertTrue(added.isEmpty(), "内容完全一致时不应报告任何补齐项");
    }

    @Test
    @DisplayName("合并不会修改默认结构本身（避免跨调用污染）")
    void mergeDoesNotMutateDefaults() {
        JsonObject target = parse("{\"a\":1}");
        JsonObject defaults = parse("{\"a\":1,\"b\":{\"c\":2}}");

        PdopnConfig.mergeDefaults(target, defaults, "", new ArrayList<>());

        assertTrue(target.has("b"));
        assertFalse(defaults.getAsJsonObject("b").has("b"), "默认结构不应被写入自身");
    }
}
