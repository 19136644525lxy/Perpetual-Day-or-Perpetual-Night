/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class ThrownItemPickedUpByEntityCriterion
extends AbstractCriterion<Conditions> {
    private final Identifier id;

    public ThrownItemPickedUpByEntityCriterion(Identifier id) {
        this.id = id;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    protected Conditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        ItemPredicate itemPredicate = ItemPredicate.fromJson(jsonObject.get("item"));
        LootContextPredicate lootContextPredicate2 = EntityPredicate.contextPredicateFromJson(jsonObject, "entity", advancementEntityPredicateDeserializer);
        return new Conditions(this.id, lootContextPredicate, itemPredicate, lootContextPredicate2);
    }

    public void trigger(ServerPlayerEntity player, ItemStack stack, @Nullable Entity entity) {
        LootContext lootContext = EntityPredicate.createAdvancementEntityLootContext(player, entity);
        this.trigger(player, conditions -> conditions.test(player, stack, lootContext));
    }

    @Override
    protected /* synthetic */ AbstractCriterionConditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return this.conditionsFromJson(obj, playerPredicate, predicateDeserializer);
    }

    public static class Conditions
    extends AbstractCriterionConditions {
        private final ItemPredicate item;
        private final LootContextPredicate entity;

        public Conditions(Identifier id, LootContextPredicate player, ItemPredicate item, LootContextPredicate entity) {
            super(id, player);
            this.item = item;
            this.entity = entity;
        }

        public static Conditions createThrownItemPickedUpByEntity(LootContextPredicate player, ItemPredicate item, LootContextPredicate entity) {
            return new Conditions(Criteria.THROWN_ITEM_PICKED_UP_BY_ENTITY.getId(), player, item, entity);
        }

        public static Conditions createThrownItemPickedUpByPlayer(LootContextPredicate player, ItemPredicate item, LootContextPredicate entity) {
            return new Conditions(Criteria.THROWN_ITEM_PICKED_UP_BY_PLAYER.getId(), player, item, entity);
        }

        public boolean test(ServerPlayerEntity player, ItemStack stack, LootContext entityContext) {
            if (!this.item.test(stack)) {
                return false;
            }
            return this.entity.test(entityContext);
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.add("item", this.item.toJson());
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}

