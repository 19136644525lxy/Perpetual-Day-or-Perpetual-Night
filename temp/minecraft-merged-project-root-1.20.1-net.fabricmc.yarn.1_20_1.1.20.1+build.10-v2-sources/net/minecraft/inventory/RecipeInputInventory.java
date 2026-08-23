/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.inventory;

import java.util.List;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeInputProvider;

public interface RecipeInputInventory
extends Inventory,
RecipeInputProvider {
    public int getWidth();

    public int getHeight();

    public List<ItemStack> getInputStacks();
}

