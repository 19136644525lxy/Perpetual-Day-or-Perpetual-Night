package yifei.pdopn.thirst;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 口渴系统所有静态数据常量。
 * 包含物品恢复/脱水值映射。
 */
public final class ThirstData {

    private ThirstData() {}

    /* ══════════ 饮品恢复量 ══════════ */

    private static final Map<Item, Double> DRINK_RESTORE = new HashMap<>();

    static {
        // 蜂蜜瓶
        DRINK_RESTORE.put(Items.HONEY_BOTTLE, 20.0);
        // 迷之炖汤
        DRINK_RESTORE.put(Items.SUSPICIOUS_STEW, 10.0);
        // 蘑菇煲
        DRINK_RESTORE.put(Items.MUSHROOM_STEW, 8.0);
    }

    /** 获取饮品恢复量（不含净水瓶/桶，由主类通过配置读取） */
    public static double getDrinkRestore(Item item) {
        return DRINK_RESTORE.getOrDefault(item, 0.0);
    }

    public static boolean isDrinkable(Item item) {
        return DRINK_RESTORE.containsKey(item);
    }

    /* ══════════ 会加剧口渴的饮品（生水等） ══════════ */

    /** 直接饮用会脱水的饮品：原版水桶、脏水瓶（含水药水） */
    private static final Set<Item> DEHYDRATING_DRINKS = Set.of(
        Items.WATER_BUCKET,
        Items.POTION
    );

    /**
     * 判断该饮品是否会导致脱水。
     * 原版水瓶（POTION）中只有“含水”的水瓶才算脏水，其余药水不受影响。
     */
    public static boolean isDehydratingDrink(Item item, net.minecraft.item.ItemStack stack) {
        if (item == Items.WATER_BUCKET) return true;
        if (item == Items.POTION) {
            return PotionUtil.getPotion(stack) == Potions.WATER;
        }
        return false;
    }

    /* ══════════ 含水食物恢复量 ══════════ */

    private static final Map<Item, Double> FOOD_RESTORE = new HashMap<>();

    static {
        FOOD_RESTORE.put(Items.MELON_SLICE, 6.0);
        FOOD_RESTORE.put(Items.SWEET_BERRIES, 4.0);
        FOOD_RESTORE.put(Items.GLOW_BERRIES, 3.0);
        FOOD_RESTORE.put(Items.APPLE, 3.0);
        FOOD_RESTORE.put(Items.GOLDEN_APPLE, 10.0);
        FOOD_RESTORE.put(Items.ENCHANTED_GOLDEN_APPLE, 15.0);
        FOOD_RESTORE.put(Items.CARROT, 2.0);
        FOOD_RESTORE.put(Items.GOLDEN_CARROT, 5.0);
        FOOD_RESTORE.put(Items.BEETROOT, 3.0);
        FOOD_RESTORE.put(Items.BEETROOT_SOUP, 12.0);
        FOOD_RESTORE.put(Items.PUMPKIN_PIE, 5.0);
        FOOD_RESTORE.put(Items.CAKE, 2.0);
        FOOD_RESTORE.put(Items.COOKIE, 1.0);
        FOOD_RESTORE.put(Items.DRIED_KELP, 1.0);
    }

    public static double getFoodRestore(Item item) {
        return FOOD_RESTORE.getOrDefault(item, 0.0);
    }

    public static boolean hasFoodRestore(Item item) {
        return FOOD_RESTORE.containsKey(item);
    }

    /* ══════════ 脱水食物（增加口渴） ══════════ */

    private static final Map<Item, Double> DEHYDRATION_FOODS = new HashMap<>();

    static {
        DEHYDRATION_FOODS.put(Items.ROTTEN_FLESH, -5.0);
        DEHYDRATION_FOODS.put(Items.SPIDER_EYE, -3.0);
        DEHYDRATION_FOODS.put(Items.PUFFERFISH, -4.0);
        DEHYDRATION_FOODS.put(Items.CHICKEN, -2.0);
        DEHYDRATION_FOODS.put(Items.SUGAR, -3.0);
    }

    public static double getDehydration(Item item) {
        return DEHYDRATION_FOODS.getOrDefault(item, 0.0);
    }

    public static boolean isDehydrating(Item item) {
        return DEHYDRATION_FOODS.containsKey(item);
    }

    /* ══════════ 需要阻止饮用的原版物品 ══════════ */

    /**
     * 判断该物品是否为“脏水”，必须先烧炼净化才能饮用。
     *
     * <p>旧实现用 {@code item == Items.POTION} 判定，而治疗 / 力量 / 抗火等药水
     * 共用同一个 {@code PotionItem} 类，导致<b>所有药水都被禁用</b>。
     * 现在仅拦截真正含水的水瓶。
     */
    public static boolean isBlockedDrink(Item item, net.minecraft.item.ItemStack stack) {
        if (item == Items.MILK_BUCKET) return true;   // 牛奶桶同样需要净化
        if (item == Items.POTION) {
            return PotionUtil.getPotion(stack) == Potions.WATER;
        }
        return false;
    }
}
