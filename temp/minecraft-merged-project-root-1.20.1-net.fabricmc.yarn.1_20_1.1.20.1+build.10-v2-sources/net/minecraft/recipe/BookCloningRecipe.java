/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.recipe;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class BookCloningRecipe
extends SpecialCraftingRecipe {
    public BookCloningRecipe(Identifier identifier, CraftingRecipeCategory craftingRecipeCategory) {
        super(identifier, craftingRecipeCategory);
    }

    @Override
    public boolean matches(RecipeInputInventory recipeInputInventory, World world) {
        int i = 0;
        ItemStack itemStack = ItemStack.EMPTY;
        for (int j = 0; j < recipeInputInventory.size(); ++j) {
            ItemStack itemStack2 = recipeInputInventory.getStack(j);
            if (itemStack2.isEmpty()) continue;
            if (itemStack2.isOf(Items.WRITTEN_BOOK)) {
                if (!itemStack.isEmpty()) {
                    return false;
                }
                itemStack = itemStack2;
                continue;
            }
            if (itemStack2.isOf(Items.WRITABLE_BOOK)) {
                ++i;
                continue;
            }
            return false;
        }
        return !itemStack.isEmpty() && itemStack.hasNbt() && i > 0;
    }

    @Override
    public ItemStack craft(RecipeInputInventory recipeInputInventory, DynamicRegistryManager dynamicRegistryManager) {
        int i = 0;
        ItemStack itemStack = ItemStack.EMPTY;
        for (int j = 0; j < recipeInputInventory.size(); ++j) {
            ItemStack itemStack2 = recipeInputInventory.getStack(j);
            if (itemStack2.isEmpty()) continue;
            if (itemStack2.isOf(Items.WRITTEN_BOOK)) {
                if (!itemStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                itemStack = itemStack2;
                continue;
            }
            if (itemStack2.isOf(Items.WRITABLE_BOOK)) {
                ++i;
                continue;
            }
            return ItemStack.EMPTY;
        }
        if (itemStack.isEmpty() || !itemStack.hasNbt() || i < 1 || WrittenBookItem.getGeneration(itemStack) >= 2) {
            return ItemStack.EMPTY;
        }
        ItemStack itemStack3 = new ItemStack(Items.WRITTEN_BOOK, i);
        NbtCompound nbtCompound = itemStack.getNbt().copy();
        nbtCompound.putInt("generation", WrittenBookItem.getGeneration(itemStack) + 1);
        itemStack3.setNbt(nbtCompound);
        return itemStack3;
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory recipeInputInventory) {
        DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(recipeInputInventory.size(), ItemStack.EMPTY);
        for (int i = 0; i < defaultedList.size(); ++i) {
            ItemStack itemStack = recipeInputInventory.getStack(i);
            if (itemStack.getItem().hasRecipeRemainder()) {
                defaultedList.set(i, new ItemStack(itemStack.getItem().getRecipeRemainder()));
                continue;
            }
            if (!(itemStack.getItem() instanceof WrittenBookItem)) continue;
            defaultedList.set(i, itemStack.copyWithCount(1));
            break;
        }
        return defaultedList;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.BOOK_CLONING;
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }
}

