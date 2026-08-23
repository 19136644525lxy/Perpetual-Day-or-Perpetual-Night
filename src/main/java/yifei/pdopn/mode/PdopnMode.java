package yifei.pdopn.mode;

/**
 * 模组运行模式枚举。
 * 每种模式关联其对应的时间刻度，用于服务端时间锁定。
 */
public enum PdopnMode {
    /** 正常昼夜循环 */
    NORMAL(-1L),
    /** 永昼 — 时间锁定在正午 (6000 ticks) */
    PERPETUAL_DAY(6000L),
    /** 永夜 — 时间锁定在午夜 (18000 ticks) */
    PERPETUAL_NIGHT(18000L);

    /** 该模式对应的世界时间刻度，-1 表示不锁定 */
    private final long targetTime;

    PdopnMode(long targetTime) {
        this.targetTime = targetTime;
    }

    /** 获取该模式锁定的世界时间，-1 表示跟随正常循环 */
    public long getTargetTime() {
        return targetTime;
    }

    /** 该模式是否需要锁定时间 */
    public boolean isTimeLocked() {
        return targetTime >= 0;
    }
}
