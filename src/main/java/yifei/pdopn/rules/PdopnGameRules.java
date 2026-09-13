package yifei.pdopn.rules;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

/**
 * 模组自定义游戏规则。
 *
 * <p>为什么用游戏规则而不是继续加配置项：
 * <ul>
 *   <li>{@code pdopn.json} 位于 {@code .minecraft/config/}，对同一服务器的所有存档生效；
 *       而温度为每个玩家、模式与偏移为每个存档的状态，天然更适合 per-world 控制。</li>
 *   <li>游戏规则自带原生的 {@code /gamerule} 命令、OP 权限校验、世界存档持久化，
 *       以及 Fabric 提供的自动客户端同步与游戏规则界面入口，无需自行实现。</li>
 * </ul>
 *
 * <p>与配置的分工：{@code pdopn.json} 提供「全局默认值」（如伤害倍率、扫描范围），
 * 游戏规则提供「每存档覆盖」。规则一律取「关闭 / 启用」语义，
 * 具体的数值调整仍由配置承担，避免两套数值来源互相打架。
 *
 * @see PdopnSettings 解析后的生效设置
 */
public final class PdopnGameRules {

    private PdopnGameRules() {}

    /* ══════════ 系统总开关 ══════════ */

    /** 温度系统总开关 */
    public static final GameRules.Key<GameRules.BooleanRule> TEMPERATURE_SYSTEM =
        GameRuleRegistry.register("pdopnTemperature", GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true));

    /** 口渴系统总开关 */
    public static final GameRules.Key<GameRules.BooleanRule> THIRST_SYSTEM =
        GameRuleRegistry.register("pdopnThirst", GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true));

    /* ══════════ 温度相关 ══════════ */

    /**
     * 偏移累加开关。
     * 关闭后永昼 / 永夜的时间锁定仍然生效，只是不再逐日累加偏移，
     * 适合只想要「永昼风景」而不想要生存压力的玩法。
     */
    public static final GameRules.Key<GameRules.BooleanRule> DRIFT_ACCUMULATION =
        GameRuleRegistry.register("pdopnDrift", GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true));

    /**
     * 偏移绝对值上限（°C），0 表示不限。
     * 默认 100（与体温致死线一致）：约第 100 天后封顶，
     * 之后温度维持极端但可生存，使降温手段与保暖手段有意义。
     */
    public static final GameRules.Key<GameRules.IntRule> MAX_DRIFT =
        GameRuleRegistry.register("pdopnMaxDrift", GameRules.Category.MISC,
            GameRuleFactory.createIntRule(100, 0, 10000));

    /** 附近危险方块（岩浆 / 火 / 冰等）的温度影响开关 */
    public static final GameRules.Key<GameRules.BooleanRule> BLOCK_TEMPERATURE =
        GameRuleRegistry.register("pdopnBlockTemp", GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true));

    /** 温度致死与脱水致死的伤害开关（关闭后仍会中暑 / 脱水但不会死） */
    public static final GameRules.Key<GameRules.BooleanRule> LETHAL_DAMAGE =
        GameRuleRegistry.register("pdopnLethalDamage", GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true));

    /* ══════════ 实体与 HUD ══════════ */

    /** 敌对生物属性增强开关 */
    public static final GameRules.Key<GameRules.BooleanRule> ENHANCE_MOBS =
        GameRuleRegistry.register("pdopnMobBoost", GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true));

    /** 中立生物（末影人 / 蜘蛛等）主动追踪玩家开关 */
    public static final GameRules.Key<GameRules.BooleanRule> NEUTRAL_AGGRESSION =
        GameRuleRegistry.register("pdopnNeutralAggro", GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true));

    /** 新玩家加入时 HUD 的默认开关状态 */
    public static final GameRules.Key<GameRules.BooleanRule> HUD_DEFAULT =
        GameRuleRegistry.register("pdopnHudDefault", GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(true));

    /**
     * 是否把 {@code maxDays} 从「仅预警」升级为强制：达到设定天数后自动切回正常循环。
     */
    public static final GameRules.Key<GameRules.BooleanRule> MAX_DAYS_ENFORCE =
        GameRuleRegistry.register("pdopnMaxDaysEnforce", GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(false));

    /** 已注册的规则数量（供启动日志确认注册成功） */
    public static int ruleCount() {
        return 10;
    }
}
