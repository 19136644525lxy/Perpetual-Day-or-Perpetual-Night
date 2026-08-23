package yifei.pdopn.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import yifei.pdopn.PerpetualDayOrPerpetualNight;

/**
 * 注册自定义物品（净水瓶、净水桶）和熔炉配方。
 */
public final class PdopnItems {

    /** 净水瓶 — 烧炼原版水瓶获得 */
    public static final Item PURE_WATER_BOTTLE = new PureWaterBottleItem(
        new Item.Settings().maxCount(16)
    );

    /** 净水桶 — 烧炼原版水桶获得 */
    public static final Item PURE_WATER_BUCKET = new PureWaterBucketItem(
        new Item.Settings().maxCount(1)
    );

    private PdopnItems() {}

    /** 由主类 onInitialize 调用 */
    public static void register() {
        // 注册物品
        Registry.register(Registries.ITEM,
            new Identifier(PerpetualDayOrPerpetualNight.MOD_ID, "pure_water_bottle"),
            PURE_WATER_BOTTLE);
        Registry.register(Registries.ITEM,
            new Identifier(PerpetualDayOrPerpetualNight.MOD_ID, "pure_water_bucket"),
            PURE_WATER_BUCKET);

        // 添加到物品组
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK)
            .register(entries -> {
                entries.add(PURE_WATER_BOTTLE);
                entries.add(PURE_WATER_BUCKET);
            });

        // 熔炉配方通过 JSON 文件注册：
        // resources/data/pdopn/recipes/pure_water_bottle_from_potion.json
        // resources/data/pdopn/recipes/pure_water_bucket.json
    }
}
