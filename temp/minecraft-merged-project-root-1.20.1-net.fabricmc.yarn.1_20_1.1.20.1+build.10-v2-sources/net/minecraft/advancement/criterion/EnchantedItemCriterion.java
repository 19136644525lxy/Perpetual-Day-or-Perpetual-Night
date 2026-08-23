/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class EnchantedItemCriterion
extends AbstractCriterion<Conditions> {
    static final Identifier ID = new Identifier("enchanted_item");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Conditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        NumberRange.IntRange intRange = NumberRange.IntRange.fromJson(jsonObject.get("levels"));
        return new Conditions(lootContextPredicate, itemPredicate, intRange);
    }

    public void trigger(ServerPlayerEntity player, ItemStack stack, int levels) {
        this.trigger(player, conditions -> conditions.matches(stack, levels));
    }

    @Override
    public /* synthetic */ AbstractCriterionConditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return this.conditionsFromJson(obj, playerPredicate, predicateDeserializer);
    }

    public static class Conditions
    extends AbstractCriterionConditions {
        private final ItemPredicate item;
        private final NumberRange.IntRange levels;

        public Conditions(LootContextPredicate player, ItemPredicate item, NumberRange.IntRange levels) {
            super(ID, player);
            this.item = item;
            this.levels = levels;
        }

        public static Conditions any() {
            return new Conditions(LootContextPredicate.EMPTY, ItemPredicate.ANY, NumberRange.IntRange.ANY);
        }

        public boolean matches(ItemStack stack, int levels) {
            if (!this.item.test(stack)) {
                return false;
            }
            return this.levels.test(levels);
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.add("item", this.item.toJson());
            jsonObject.add("levels", this.levels.toJson());
            return jsonObject;
        }
    }
}

