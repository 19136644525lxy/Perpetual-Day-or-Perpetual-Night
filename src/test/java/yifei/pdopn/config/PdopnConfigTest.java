package yifei.pdopn.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
