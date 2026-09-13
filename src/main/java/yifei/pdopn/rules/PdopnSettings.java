package yifei.pdopn.rules;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;

/**
 * 解析后的生效设置。
 *
 * <p>游戏规则是「每存档」的，而状态会在服务端主世界与各管理器之间共享，
 * 因此统一在每 tick 开始处解析一次，再传递给各系统，
 * 避免每个玩家 / 每个实体各查一遍 {@code getGameRules()}。
 *
 * <p>本类只负责「开关与上限」；具体数值仍来自 {@code pdopn.json}，
 * 两套来源不会互相覆盖，语义清晰。
 *
 * @see PdopnGameRules
 */
public record PdopnSettings(
    boolean temperatureSystem,
    boolean thirstSystem,
    boolean driftAccumulation,
    int maxDrift,
    boolean blockTemperature,
    boolean lethalDamage,
    boolean enhanceMobs,
    boolean neutralAggression,
    boolean hudDefault,
    boolean maxDaysEnforce
) {

    /** 全部开启 / 默认值的设置，用于服务端尚未就绪时兜底 */
    public static final PdopnSettings DEFAULT = new PdopnSettings(
        true, true, true, 100, true, true, true, true, true, false);

    /** 从服务端主世界解析游戏规则（主世界与世界无关的规则在所有维度共享） */
    public static PdopnSettings resolve(MinecraftServer server) {
        if (server == null) return DEFAULT;

        ServerWorld overworld = server.getOverworld();
        if (overworld == null) return DEFAULT;

        GameRules rules = overworld.getGameRules();
        return new PdopnSettings(
            rules.getBoolean(PdopnGameRules.TEMPERATURE_SYSTEM),
            rules.getBoolean(PdopnGameRules.THIRST_SYSTEM),
            rules.getBoolean(PdopnGameRules.DRIFT_ACCUMULATION),
            rules.getInt(PdopnGameRules.MAX_DRIFT),
            rules.getBoolean(PdopnGameRules.BLOCK_TEMPERATURE),
            rules.getBoolean(PdopnGameRules.LETHAL_DAMAGE),
            rules.getBoolean(PdopnGameRules.ENHANCE_MOBS),
            rules.getBoolean(PdopnGameRules.NEUTRAL_AGGRESSION),
            rules.getBoolean(PdopnGameRules.HUD_DEFAULT),
            rules.getBoolean(PdopnGameRules.MAX_DAYS_ENFORCE)
        );
    }

    /** 偏移上限是否生效（0 = 不限） */
    public boolean hasDriftCap() {
        return maxDrift > 0;
    }

    /**
     * 把累加后的偏移按上限钳制。
     * 上限为 0 时原样返回，保持「无限累加」的旧行为。
     */
    public double clampDrift(double drift) {
        if (!hasDriftCap()) return drift;
        double cap = maxDrift;
        return Math.max(-cap, Math.min(cap, drift));
    }
}
