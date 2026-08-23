/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.EntityEquipmentPredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class TickCriterion
extends AbstractCriterion<Conditions> {
    final Identifier id;

    public TickCriterion(Identifier id) {
        this.id = id;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    public Conditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        return new Conditions(this.id, lootContextPredicate);
    }

    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, conditions -> true);
    }

    @Override
    public /* synthetic */ AbstractCriterionConditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return this.conditionsFromJson(obj, playerPredicate, predicateDeserializer);
    }

    public static class Conditions
    extends AbstractCriterionConditions {
        public Conditions(Identifier identifier, LootContextPredicate lootContextPredicate) {
            super(identifier, lootContextPredicate);
        }

        public static Conditions createLocation(LocationPredicate location) {
            return new Conditions(Criteria.LOCATION.id, EntityPredicate.asLootContextPredicate(EntityPredicate.Builder.create().location(location).build()));
        }

        public static Conditions createLocation(EntityPredicate entity) {
            return new Conditions(Criteria.LOCATION.id, EntityPredicate.asLootContextPredicate(entity));
        }

        public static Conditions createSleptInBed() {
            return new Conditions(Criteria.SLEPT_IN_BED.id, LootContextPredicate.EMPTY);
        }

        public static Conditions createHeroOfTheVillage() {
            return new Conditions(Criteria.HERO_OF_THE_VILLAGE.id, LootContextPredicate.EMPTY);
        }

        public static Conditions createAvoidVibration() {
            return new Conditions(Criteria.AVOID_VIBRATION.id, LootContextPredicate.EMPTY);
        }

        public static Conditions createTick() {
            return new Conditions(Criteria.TICK.id, LootContextPredicate.EMPTY);
        }

        public static Conditions createLocation(Block block, Item item) {
            return Conditions.createLocation(EntityPredicate.Builder.create().equipment(EntityEquipmentPredicate.Builder.create().feet(ItemPredicate.Builder.create().items(item).build()).build()).steppingOn(LocationPredicate.Builder.create().block(BlockPredicate.Builder.create().blocks(block).build()).build()).build());
        }
    }
}

