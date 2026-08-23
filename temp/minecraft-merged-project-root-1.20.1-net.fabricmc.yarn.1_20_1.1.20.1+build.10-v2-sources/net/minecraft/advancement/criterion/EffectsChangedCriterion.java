/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.entity.Entity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.EntityEffectPredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class EffectsChangedCriterion
extends AbstractCriterion<Conditions> {
    static final Identifier ID = new Identifier("effects_changed");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Conditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        EntityEffectPredicate entityEffectPredicate = EntityEffectPredicate.fromJson(jsonObject.get("effects"));
        LootContextPredicate lootContextPredicate2 = EntityPredicate.contextPredicateFromJson(jsonObject, "source", advancementEntityPredicateDeserializer);
        return new Conditions(lootContextPredicate, entityEffectPredicate, lootContextPredicate2);
    }

    public void trigger(ServerPlayerEntity player, @Nullable Entity source) {
        LootContext lootContext = source != null ? EntityPredicate.createAdvancementEntityLootContext(player, source) : null;
        this.trigger(player, (T conditions) -> conditions.matches(player, lootContext));
    }

    @Override
    public /* synthetic */ AbstractCriterionConditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return this.conditionsFromJson(obj, playerPredicate, predicateDeserializer);
    }

    public static class Conditions
    extends AbstractCriterionConditions {
        private final EntityEffectPredicate effects;
        private final LootContextPredicate source;

        public Conditions(LootContextPredicate player, EntityEffectPredicate effects, LootContextPredicate source) {
            super(ID, player);
            this.effects = effects;
            this.source = source;
        }

        public static Conditions create(EntityEffectPredicate effects) {
            return new Conditions(LootContextPredicate.EMPTY, effects, LootContextPredicate.EMPTY);
        }

        public static Conditions create(EntityPredicate source) {
            return new Conditions(LootContextPredicate.EMPTY, EntityEffectPredicate.EMPTY, EntityPredicate.asLootContextPredicate(source));
        }

        public boolean matches(ServerPlayerEntity player, @Nullable LootContext context) {
            if (!this.effects.test(player)) {
                return false;
            }
            return this.source == LootContextPredicate.EMPTY || context != null && this.source.test(context);
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.add("effects", this.effects.toJson());
            jsonObject.add("source", this.source.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}

