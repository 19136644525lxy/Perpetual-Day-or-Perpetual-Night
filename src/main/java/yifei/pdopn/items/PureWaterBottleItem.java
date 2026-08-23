package yifei.pdopn.items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemUsage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import yifei.pdopn.PerpetualDayOrPerpetualNight;
import yifei.pdopn.config.PdopnConfig;

/**
 * 净水瓶 — 熔炉烧炼脏水瓶获得。
 * 可安全饮用，恢复口渴值。
 */
public class PureWaterBottleItem extends Item {

    public PureWaterBottleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        super.finishUsing(stack, world, user);

        if (user instanceof ServerPlayerEntity player) {
            double restore = PdopnConfig.getInstance().thirst.pureWaterBottleRestore;
            PerpetualDayOrPerpetualNight.getThirstManager().addHydration(player.getUuid(), restore);
        }

        // 喝完返回空玻璃瓶
        if (user instanceof PlayerEntity playerEntity) {
            return ItemUsage.exchangeStack(stack, playerEntity, new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
}
