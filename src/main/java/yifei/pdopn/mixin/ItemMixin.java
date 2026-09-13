package yifei.pdopn.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yifei.pdopn.PerpetualDayOrPerpetualNight;
import yifei.pdopn.thirst.PdopnThirstManager;

/**
 * 在物品食用/饮用完成后同步口渴值。
 *
 * <p>覆盖蜂蜜瓶、炖汤、含水食物、脱水食物以及生水（水桶）等所有经由
 * {@link Item#finishUsing} 完成的消耗行为。此前 {@code PdopnThirstManager#onItemUsed}
 * 没有任何调用方，这些效果实际上是失效的。
 *
 * <p>净水瓶 / 净水桶继承自 {@link Item} 并会调用 {@code super.finishUsing}，
 * 因此也会走这里，但它们在自身类中已按各自配置恢复，{@code ThirstData} 中
 * 不含它们的条目，不会重复结算。
 */
@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "finishUsing", at = @At("TAIL"))
    private void pdopn$afterFinishUsing(ItemStack stack, World world, LivingEntity user,
                                        CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) return;

        PdopnThirstManager thirstManager = PerpetualDayOrPerpetualNight.getThirstManager();
        if (thirstManager == null) return;

        // 使用原始 stack（消耗后的 item 仍是同一物品，用于区分含水药水）
        thirstManager.onItemUsed(player, stack);
    }
}
