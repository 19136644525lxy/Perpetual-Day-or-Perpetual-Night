package yifei.pdopn.recipe;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import yifei.pdopn.thirst.ThirstData;

/**
 * 净水烧炼配方的公共实现（熔炉 / 高炉 / 烟熏炉）。
 *
 * <p><b>为什么 getType() 必须返回原版配方类型：</b>
 * 炉子按「配方类型」建立索引后查询 ——
 * {@code AbstractFurnaceBlockEntity} 用 {@code RecipeType.SMELTING / BLASTING / SMOKING}
 * 创建匹配器，而 {@code RecipeManager#getAllOfType} 是
 * {@code recipes.getOrDefault(type, emptyMap())}，即按 {@code recipe.getType()} 分桶。
 * 因此若把配方注册成自定义 RecipeType，它会被放进一个永远不会被查询的桶里，
 * 表现为「配方存在但炉子不认」。本类改为报告设备对应的原版类型。
 *
 * <p><b>输入校验放在哪里：</b>真正的保护在 {@link PdopnRecipes.Serializer} ——
 * 它在反序列化阶段就拒绝任何非「水瓶 / 水桶」的输入。
 * 因此不需要自定义 RecipeType 也能阻止任意药水被误烧。
 */
public abstract class PurifyingRecipe extends AbstractCookingRecipe {

    protected PurifyingRecipe(RecipeType<?> type, Identifier id, String group,
                              CookingRecipeCategory category, Ingredient input,
                              ItemStack output, float experience, int cookTime) {
        super(type, id, group, category, input, output, experience, cookTime);
    }

    @Override
    public boolean matches(Inventory inventory, World world) {
        ItemStack stack = inventory.getStack(0);
        // 输入条件已带白名单，这里再显式校验一次
        return ThirstData.isAllowedPurifyingInput(stack) && this.input.test(stack);
    }

    /** 熔炉净化配方 */
    public static class Smelting extends PurifyingRecipe {
        /**
         * 本变体绑定的配方类型。
         * 单独暴露为常量，使「配方类型必须与设备一致」这一约束可以被单元测试断言，
         * 同时构造器与测试读取的是同一个值，不会各自漂移。
         */
        public static final RecipeType<?> BOUND_TYPE = RecipeType.SMELTING;

        public Smelting(Identifier id, String group, CookingRecipeCategory category,
                        Ingredient input, ItemStack output, float experience, int cookTime) {
            super(BOUND_TYPE, id, group, category, input, output, experience, cookTime);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return PdopnRecipes.PURIFYING_SERIALIZER;
        }
    }

    /** 高炉净化配方 */
    public static class Blasting extends PurifyingRecipe {
        /** @see Smelting#BOUND_TYPE */
        public static final RecipeType<?> BOUND_TYPE = RecipeType.BLASTING;

        public Blasting(Identifier id, String group, CookingRecipeCategory category,
                        Ingredient input, ItemStack output, float experience, int cookTime) {
            super(BOUND_TYPE, id, group, category, input, output, experience, cookTime);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return PdopnRecipes.PURIFYING_SERIALIZER;
        }
    }

    /** 烟熏炉净化配方 */
    public static class Smoking extends PurifyingRecipe {
        /** @see Smelting#BOUND_TYPE */
        public static final RecipeType<?> BOUND_TYPE = RecipeType.SMOKING;

        public Smoking(Identifier id, String group, CookingRecipeCategory category,
                       Ingredient input, ItemStack output, float experience, int cookTime) {
            super(BOUND_TYPE, id, group, category, input, output, experience, cookTime);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return PdopnRecipes.PURIFYING_SERIALIZER;
        }
    }
}
