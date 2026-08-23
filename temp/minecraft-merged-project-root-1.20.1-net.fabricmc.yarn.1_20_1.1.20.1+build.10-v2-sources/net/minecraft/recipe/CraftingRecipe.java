/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.recipe;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.CraftingRecipeCategory;

public interface CraftingRecipe
extends Recipe<RecipeInputInventory> {
    @Override
    default public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    public CraftingRecipeCategory getCategory();
}

