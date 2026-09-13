package yifei.pdopn.temperature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 温度系统纯数值逻辑测试。
 *
 * <p>被测对象是 {@link TemperatureBands}——它刻意不引用任何 Minecraft 类型，
 * 因此无需启动游戏即可验证。
 *
 * <p>这些用例锁定的是曾经真实出过错的地方：档位阈值曾与文档、HUD 三处不一致；
 * 偏移累加与衰减的边界行为也缺少保护。
 */
class TemperatureBandsTest {

    /* ────────── 档位判定 ────────── */

    @Test
    @DisplayName("档位边界与 TEMP_BANDS 一致")
    void bandBoundaries() {
        assertEquals(0, TemperatureBands.bandOf(0.0));
        assertEquals(0, TemperatureBands.bandOf(9.999));
        assertEquals(1, TemperatureBands.bandOf(10.0));
        assertEquals(1, TemperatureBands.bandOf(24.999));
        assertEquals(2, TemperatureBands.bandOf(25.0));
        assertEquals(2, TemperatureBands.bandOf(44.999));
        assertEquals(3, TemperatureBands.bandOf(45.0));
        assertEquals(3, TemperatureBands.bandOf(69.999));
        assertEquals(4, TemperatureBands.bandOf(70.0));
        assertEquals(4, TemperatureBands.bandOf(84.999));
        assertEquals(5, TemperatureBands.bandOf(85.0));
        assertEquals(5, TemperatureBands.bandOf(100.0));
    }

    @Test
    @DisplayName("档位判定使用绝对值，冷热对称")
    void bandIsSymmetric() {
        for (double value : new double[] {5, 15, 30, 50, 75, 95}) {
            assertEquals(TemperatureBands.bandOf(value), TemperatureBands.bandOf(-value),
                "冷热应当落在同一档: +-" + value);
        }
    }

    @Test
    @DisplayName("危险档判定阈值为 70 度")
    void isCriticalAtSeventy() {
        assertFalse(TemperatureBands.isCritical(69.9));
        assertTrue(TemperatureBands.isCritical(70.0));
        assertTrue(TemperatureBands.isCritical(-70.0));
    }

    /* ────────── 偏移累加 / 衰减 ────────── */

    @Test
    @DisplayName("永昼每天恰好累加 dailyDriftAmount")
    void driftAccumulatesPerDay() {
        double perDay = 1.0;
        double perTick = TemperatureBands.driftPerTick(perDay, true);
        double afterOneDay = perTick * TemperatureBands.TICKS_PER_DAY;
        assertEquals(perDay, afterOneDay, 1.0e-9);
    }

    @Test
    @DisplayName("永夜向冷方向累加")
    void nightDriftIsNegative() {
        assertTrue(TemperatureBands.driftPerTick(1.0, false) < 0);
        assertTrue(TemperatureBands.driftPerTick(1.0, true) > 0);
    }

    @Test
    @DisplayName("偏移衰减不会越过 0")
    void decayNeverOvershootsZero() {
        // 正向衰减
        assertEquals(0.0, TemperatureBands.decayDrift(0.01, 0.02), 1.0e-9);
        // 负向衰减
        assertEquals(0.0, TemperatureBands.decayDrift(-0.01, 0.02), 1.0e-9);
        // 正常衰减
        assertEquals(0.5, TemperatureBands.decayDrift(0.52, 0.02), 1.0e-9);
        assertEquals(-0.5, TemperatureBands.decayDrift(-0.52, 0.02), 1.0e-9);
        // 已为 0
        assertEquals(0.0, TemperatureBands.decayDrift(0.0, 0.02), 1.0e-9);
    }

    /* ────────── 环境修正 ────────── */

    @Test
    @DisplayName("时间修正覆盖白天/黄昏/黎明/夜晚")
    void timeModifiers() {
        assertEquals(5.0, TemperatureBands.getTimeModifier(6000));
        assertEquals(5.0, TemperatureBands.getTimeModifier(15999));
        assertEquals(2.0, TemperatureBands.getTimeModifier(16000));
        assertEquals(2.0, TemperatureBands.getTimeModifier(17999));
        assertEquals(-5.0, TemperatureBands.getTimeModifier(18000));
        assertEquals(-2.0, TemperatureBands.getTimeModifier(4000));
        assertEquals(-5.0, TemperatureBands.getTimeModifier(0));
    }

    @Test
    @DisplayName("时间修正对超过一天的世界时间取模")
    void timeModifierWrapsAround() {
        assertEquals(TemperatureBands.getTimeModifier(6000),
            TemperatureBands.getTimeModifier(6000 + 24000 * 3), 1.0e-9);
    }

    @Test
    @DisplayName("海拔修正：120 以上降温、0 以下升温")
    void altitudeModifier() {
        assertEquals(0.0, TemperatureBands.getAltitudeModifier(64), 1.0e-9);
        assertEquals(0.0, TemperatureBands.getAltitudeModifier(120), 1.0e-9);
        assertEquals(-1.0, TemperatureBands.getAltitudeModifier(140), 1.0e-9);
        // Y < 0：按 -y * 0.03 升温
        assertEquals(0.3, TemperatureBands.getAltitudeModifier(-10), 1.0e-9);
    }
}
