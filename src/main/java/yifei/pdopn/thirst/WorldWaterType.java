package yifei.pdopn.thirst;

/**
 * 世界水体类型。
 * 用于区分直接饮水时的口渴判定结果。
 * 设计：开闭原则 — 新增水体类型只需扩展枚举，无需修改判定逻辑。
 */
public enum WorldWaterType {
    /** 淡水湖：直接饮用安全，恢复口渴值 */
    FRESHWATER_LAKE,
    /** 海洋：含盐量高，饮水易脱水 */
    OCEAN,
    /** 咸水湖：异变水体，饮水易脱水 */
    SALT_LAKE,
    /** 普通水：池塘/自流水等非候选群系水体 */
    NORMAL_WATER
}
