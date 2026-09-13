package yifei.pdopn.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 游戏规则解析结果（{@link PdopnSettings}）的纯逻辑测试。
 *
 * <p>{@code PdopnSettings} 刻意不引用 Minecraft 类型，只承载解析后的布尔/整数开关，
 * 因此偏移上限这类容易写错的钳制逻辑可以直接验证。
 */
class PdopnSettingsTest {

    private static PdopnSettings withMaxDrift(int maxDrift) {
        return new PdopnSettings(true, true, true, maxDrift, true, true, true, true, true, false);
    }

    @Test
    @DisplayName("maxDrift = 0 表示不限，偏移原样返回")
    void zeroCapMeansUnlimited() {
        PdopnSettings settings = withMaxDrift(0);
        assertFalse(settings.hasDriftCap());
        assertEquals(500.0, settings.clampDrift(500.0), 1.0e-9);
        assertEquals(-500.0, settings.clampDrift(-500.0), 1.0e-9);
    }

    @Test
    @DisplayName("正负两侧都被钳制到上限（对称）")
    void capIsSymmetric() {
        PdopnSettings settings = withMaxDrift(100);
        assertTrue(settings.hasDriftCap());
        assertEquals(100.0, settings.clampDrift(250.0), 1.0e-9);
        assertEquals(-100.0, settings.clampDrift(-250.0), 1.0e-9);
    }

    @Test
    @DisplayName("未超过上限时不受影响")
    void belowCapIsUntouched() {
        PdopnSettings settings = withMaxDrift(100);
        assertEquals(60.0, settings.clampDrift(60.0), 1.0e-9);
        assertEquals(-60.0, settings.clampDrift(-60.0), 1.0e-9);
        assertEquals(100.0, settings.clampDrift(100.0), 1.0e-9);
        assertEquals(-100.0, settings.clampDrift(-100.0), 1.0e-9);
    }

    @Test
    @DisplayName("默认设置：全系统开启、偏移上限 ±100、不强制 maxDays")
    void defaultsAreSane() {
        PdopnSettings d = PdopnSettings.DEFAULT;
        assertTrue(d.temperatureSystem());
        assertTrue(d.thirstSystem());
        assertTrue(d.driftAccumulation());
        assertEquals(100, d.maxDrift());
        assertTrue(d.lethalDamage());
        assertFalse(d.maxDaysEnforce());
    }
}
