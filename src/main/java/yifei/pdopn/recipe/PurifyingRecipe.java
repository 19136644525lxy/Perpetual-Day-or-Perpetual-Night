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
 * <p>与直接使用原版 {@code minecraft:smelting} 的区别：
 * <ol>
 *   <li>输入在反序列化阶段被强制校验，<b>只允许水瓶或水桶</b>，否则拒绝加载；
 *   <li>运行时匹配额外校验物品类型，杜绝任意药水被误烧成净水瓶。
 * </ol>
 *
 * <p>存在的原因：1.20.1 的 {@link Ingredient} 只支持 {@code item}/{@code tag} 两种
 * JSON 写法，其 {@code test()} 最终只比较物品类型（{@code isOf}），<b>不比较 NBT</b>。
 * 因此纯数据包无法区分「水瓶」与「治疗药水」——旧实现会让任意药水都被烧成净水瓶，
 * 造成贵重药水被误烧。本类通过显式白名单填补这一缺口。
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
        public Smelting(Identifier id, String group, CookingRecipeCategory category,
                        Ingredient input, ItemStack output, float experience, int cookTime) {
            super(PdopnRecipes.PURIFYING_SMELTING, id, group, category, input, output, experience, cookTime);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return PdopnRecipes.PURIFYING_SERIALIZER;
        }
    }

    /** 高炉净化配方 */
    public static class Blasting extends PurifyingRecipe {
        public Blasting(Identifier id, String group, CookingRecipeCategory category,
                        Ingredient input, ItemStack output, float experience, int cookTime) {
            super(PdopnRecipes.PURIFYING_BLASTING, id, group, category, input, output, experience, cookTime);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return PdopnRecipes.PURIFYING_SERIALIZER;
        }
    }

    /** 烟熏炉净化配方 */
    public static class Smoking extends PurifyingRecipe {
        public Smoking(Identifier id, String group, CookingRecipeCategory category,
                       Ingredient input, ItemStack output, float experience, int cookTime) {
            super(PdopnRecipes.PURIFYING_SMOKING, id, group, category, input, output, experience, cookTime);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return PdopnRecipes.PURIFYING_SERIALIZER;
        }
    }
}
