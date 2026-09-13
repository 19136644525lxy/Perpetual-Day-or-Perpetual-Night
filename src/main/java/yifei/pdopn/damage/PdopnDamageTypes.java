package yifei.pdopn.damage;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import yifei.pdopn.PerpetualDayOrPerpetualNight;

/**
 * 模组自定义伤害类型。
 *
 * <p>死亡消息文本由 {@code "death.attack." + DamageType.msgId()} 决定
 * （见 {@code DamageSource.getDeathMessage}），所以只要注册一个自定义
 * {@link DamageType}，就能让体温致死显示专属死因，而无需覆盖原版逻辑。
 *
 * <p>类型本身是数据驱动的，定义在
 * {@code data/pdopn/damage_type/heat.json} 与 {@code cold.json}；
 * Fabric 会把模组内置数据包同步给客户端，因此死亡界面与聊天栏都能正确显示。
 */
public final class PdopnDamageTypes {

    /** 高温致死（msgId = pdopn.heat → 文本键 death.attack.pdopn.heat） */
    public static final RegistryKey<DamageType> HEAT =
        RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier(PerpetualDayOrPerpetualNight.MOD_ID, "heat"));

    /** 严寒致死（msgId = pdopn.cold → 文本键 death.attack.pdopn.cold） */
    public static final RegistryKey<DamageType> COLD =
        RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier(PerpetualDayOrPerpetualNight.MOD_ID, "cold"));

    /** 脱水致死（msgId = pdopn.dehydration → 文本键 death.attack.pdopn.dehydration） */
    public static final RegistryKey<DamageType> DEHYDRATION =
        RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier(PerpetualDayOrPerpetualNight.MOD_ID, "dehydration"));

    private PdopnDamageTypes() {}

    /** 构造高温致死伤害来源 */
    public static DamageSource heat(ServerPlayerEntity player) {
        return player.getDamageSources().create(HEAT);
    }

    /** 构造严寒致死伤害来源 */
    public static DamageSource cold(ServerPlayerEntity player) {
        return player.getDamageSources().create(COLD);
    }

    /** 构造脱水致死伤害来源 */
    public static DamageSource dehydration(ServerPlayerEntity player) {
        return player.getDamageSources().create(DEHYDRATION);
    }
}
