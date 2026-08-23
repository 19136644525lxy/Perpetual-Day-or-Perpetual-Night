package yifei.pdopn.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yifei.pdopn.thirst.ThirstData;

/**
 * 拦截原版水瓶（PotionItem）的使用动作。
 * 阻止玩家直接饮用未净化的水瓶，提示需要烧炼。
 */
@Mixin(PotionItem.class)
public class PotionItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void pdopn$onUse(World world, PlayerEntity user, Hand hand,
                             CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            ItemStack stack = player.getStackInHand(hand);
            if (ThirstData.isBlockedDrink(stack.getItem())) {
                player.sendMessage(
                    Text.translatable("pdopn.thirst.need_purify")
                        .formatted(net.minecraft.util.Formatting.RED),
                    true
                );
                cir.setReturnValue(TypedActionResult.fail(stack));
            }
        }
    }
}
