/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class CuredZombieVillagerCriterion
extends AbstractCriterion<Conditions> {
    static final Identifier ID = new Identifier("cured_zombie_villager");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Conditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        LootContextPredicate lootContextPredicate2 = EntityPredicate.contextPredicateFromJson(jsonObject, "zombie", advancementEntityPredicateDeserializer);
        LootContextPredicate lootContextPredicate3 = EntityPredicate.contextPredicateFromJson(jsonObject, "villager", advancementEntityPredicateDeserializer);
        return new Conditions(lootContextPredicate, lootContextPredicate2, lootContextPredicate3);
    }

    public void trigger(ServerPlayerEntity player, ZombieEntity zombie, VillagerEntity villager) {
        LootContext lootContext = EntityPredicate.createAdvancementEntityLootContext(player, zombie);
        LootContext lootContext2 = EntityPredicate.createAdvancementEntityLootContext(player, villager);
        this.trigger(player, conditions -> conditions.matches(lootContext, lootContext2));
    }

    @Override
    public /* synthetic */ AbstractCriterionConditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return this.conditionsFromJson(obj, playerPredicate, predicateDeserializer);
    }

    public static class Conditions
    extends AbstractCriterionConditions {
        private final LootContextPredicate zombie;
        private final LootContextPredicate villager;

        public Conditions(LootContextPredicate player, LootContextPredicate zombie, LootContextPredicate villager) {
            super(ID, player);
            this.zombie = zombie;
            this.villager = villager;
        }

        public static Conditions any() {
            return new Conditions(LootContextPredicate.EMPTY, LootContextPredicate.EMPTY, LootContextPredicate.EMPTY);
        }

        public boolean matches(LootContext zombieContext, LootContext villagerContext) {
            if (!this.zombie.test(zombieContext)) {
                return false;
            }
            return this.villager.test(villagerContext);
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.add("zombie", this.zombie.toJson(predicateSerializer));
            jsonObject.add("villager", this.villager.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}

