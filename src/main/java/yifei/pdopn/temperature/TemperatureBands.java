package yifei.pdopn.temperature;

/**
 * 温度系统的纯数值逻辑。
 *
 * <p>刻意不引用任何 Minecraft 类型：这些计算（档位、偏移累加/衰减、时间与海拔修正）
 * 是模组的核心手感来源，也是最容易出错的边界逻辑，因此集中在此以便直接单元测试。
 *
 * <p>{@link TemperatureData} 中的群系 / 方块 / 物品表依赖 Minecraft 注册表，
 * 不适合在无游戏环境下加载；纯数学部分则完全没有这个限制。
 */
public final class TemperatureBands {

    private TemperatureBands() {}

    /* ══════════ 体温常量 ══════════ */

    /** 体温下限（触发冻死） */
    public static final double MIN_TEMP = -100.0;
    /** 体温上限（触发烧死） */
    public static final double MAX_TEMP = 100.0;
    /** 默认体温 */
    public static final double DEFAULT_BODY_TEMP = 0.0;

    /**
     * 体温档位分界（°C，取绝对值）。
     * 效果施加、HUD 颜色、预警音效共同以此为准，
     * 避免出现「文档 / 效果 / 显示」三套阈值不一致的历史问题。
     */
    public static final double[] TEMP_BANDS = {10.0, 25.0, 45.0, 70.0, 85.0};

    /** 舒适区上限（低于此值无任何效果） */
    public static final double COMFORT_BAND_MAX = 10.0;

    /** 一天对应的游戏刻数 */
    public static final long TICKS_PER_DAY = 24000L;

    /* ══════════ 档位判定 ══════════ */

    /**
     * 返回体温（绝对值）所处档位。
     * 0 = 舒适，1 = 轻度，2 = 中度，3 = 重度，4 = 危险，5 = 极限。
     */
    public static int bandOf(double absTemp) {
        double abs = Math.abs(absTemp);
        int band = 0;
        for (double edge : TEMP_BANDS) {
            if (abs >= edge) band++;
        }
        return band;
    }

    /** 是否处于危险档及以上（会致死的区间） */
    public static boolean isCritical(double absTemp) {
        return bandOf(absTemp) >= 4;
    }

    /* ══════════ 偏移累加 / 衰减 ══════════ */

    /**
     * 每 tick 的偏移增量。
     *
     * @param dailyDriftAmount 配置的每日偏移量（°C）
     * @param perpetualDay     true = 永昼（向热），false = 永夜（向冷）
     */
    public static double driftPerTick(double dailyDriftAmount, boolean perpetualDay) {
        double perTick = dailyDriftAmount / TICKS_PER_DAY;
        return perpetualDay ? perTick : -perTick;
    }

    /**
     * 正常模式下把偏移向 0 衰减一步（不会越过 0）。
     */
    public static double decayDrift(double current, double driftDecayRate) {
        if (current > 0) return Math.max(0, current - driftDecayRate);
        if (current < 0) return Math.min(0, current + driftDecayRate);
        return 0;
    }

    /* ══════════ 时间修正 ══════════ */

    /** 根据世界时间 (0-24000) 返回时间温度修正 */
    public static double getTimeModifier(long worldTime) {
        long t = worldTime % 24000;
        if (t >= 6000 && t < 16000) return 5.0;      // 白天
        if (t >= 16000 && t < 18000) return 2.0;     // 黄昏
        if (t >= 4000 && t < 6000) return -2.0;      // 黎明
        return -5.0;                                   // 夜晚
    }

    /* ══════════ 海拔修正 ══════════ */

    /** 海拔修正：Y > 120 每格降温，Y < 0 每格升温 */
    public static double getAltitudeModifier(int y) {
        if (y > 120) return -(y - 120) * 0.05;
        if (y < 0) return -y * 0.03;
        return 0.0;
    }
}
