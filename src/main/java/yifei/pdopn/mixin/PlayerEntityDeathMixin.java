package yifei.pdopn.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yifei.pdopn.callback.PdopnPlayerDeathCallback;

/**
 * 拦截玩家死亡，触发口渴 / 体温重置回调。
 *
 * <p><b>为什么必须注入 {@link ServerPlayerEntity} 而不是 {@code PlayerEntity}：</b>
 * 1.20.1 中 {@code ServerPlayerEntity#onDeath} 是 {@code @Override} 且
 * <b>从不调用 {@code super.onDeath(...)}</b>（它直接实现自己的死亡流程），
 * 因此挂在 {@code PlayerEntity#onDeath} 上的注入对真实玩家永远不会触发，
 * 会导致“死亡后口渴值不重置”。
 *
 * <p>注意：{@code PlayerEntity#onDeath} 内部会调用 {@code super.onDeath}（即
 * {@code LivingEntity#onDeath}），所以这里不会与父类注入重复触发。
 */
@Mixin(ServerPlayerEntity.class)
public class PlayerEntityDeathMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void pdopn$onPlayerDeath(DamageSource damageSource, CallbackInfo ci) {
        PdopnPlayerDeathCallback.fire((ServerPlayerEntity) (Object) this);
    }
}
