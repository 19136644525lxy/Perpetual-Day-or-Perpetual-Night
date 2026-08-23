/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.DamagePredicate;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class PlayerHurtEntityCriterion
extends AbstractCriterion<Conditions> {
    static final Identifier ID = new Identifier("player_hurt_entity");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Conditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        DamagePredicate damagePredicate = DamagePredicate.fromJson(jsonObject.get("damage"));
        LootContextPredicate lootContextPredicate2 = EntityPredicate.contextPredicateFromJson(jsonObject, "entity", advancementEntityPredicateDeserializer);
        return new Conditions(lootContextPredicate, damagePredicate, lootContextPredicate2);
    }

    public void trigger(ServerPlayerEntity player, Entity entity, DamageSource damage, float dealt, float taken, boolean blocked) {
        LootContext lootContext = EntityPredicate.createAdvancementEntityLootContext(player, entity);
        this.trigger(player, conditions -> conditions.matches(player, lootContext, damage, dealt, taken, blocked));
    }

    @Override
    public /* synthetic */ AbstractCriterionConditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return this.conditionsFromJson(obj, playerPredicate, predicateDeserializer);
    }

    public static class Conditions
    extends AbstractCriterionConditions {
        private final DamagePredicate damage;
        private final LootContextPredicate entity;

        public Conditions(LootContextPredicate player, DamagePredicate damage, LootContextPredicate entity) {
            super(ID, player);
            this.damage = damage;
            this.entity = entity;
        }

        public static Conditions create() {
            return new Conditions(LootContextPredicate.EMPTY, DamagePredicate.ANY, LootContextPredicate.EMPTY);
        }

        public static Conditions create(DamagePredicate damagePredicate) {
            return new Conditions(LootContextPredicate.EMPTY, damagePredicate, LootContextPredicate.EMPTY);
        }

        public static Conditions create(DamagePredicate.Builder damagePredicateBuilder) {
            return new Conditions(LootContextPredicate.EMPTY, damagePredicateBuilder.build(), LootContextPredicate.EMPTY);
        }

        public static Conditions create(EntityPredicate hurtEntityPredicate) {
            return new Conditions(LootContextPredicate.EMPTY, DamagePredicate.ANY, EntityPredicate.asLootContextPredicate(hurtEntityPredicate));
        }

        public static Conditions create(DamagePredicate damagePredicate, EntityPredicate hurtEntityPredicate) {
            return new Conditions(LootContextPredicate.EMPTY, damagePredicate, EntityPredicate.asLootContextPredicate(hurtEntityPredicate));
        }

        public static Conditions create(DamagePredicate.Builder damagePredicateBuilder, EntityPredicate hurtEntityPredicate) {
            return new Conditions(LootContextPredicate.EMPTY, damagePredicateBuilder.build(), EntityPredicate.asLootContextPredicate(hurtEntityPredicate));
        }

        public boolean matches(ServerPlayerEntity player, LootContext entityContext, DamageSource source, float dealt, float taken, boolean blocked) {
            if (!this.damage.test(player, source, dealt, taken, blocked)) {
                return false;
            }
            return this.entity.test(entityContext);
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.add("damage", this.damage.toJson());
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}

