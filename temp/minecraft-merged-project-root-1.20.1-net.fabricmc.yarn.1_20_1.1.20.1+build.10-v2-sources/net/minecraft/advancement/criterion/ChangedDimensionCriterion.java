/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ChangedDimensionCriterion
extends AbstractCriterion<Conditions> {
    static final Identifier ID = new Identifier("changed_dimension");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Conditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        RegistryKey<World> registryKey = jsonObject.has("from") ? RegistryKey.of(RegistryKeys.WORLD, new Identifier(JsonHelper.getString(jsonObject, "from"))) : null;
        RegistryKey<World> registryKey2 = jsonObject.has("to") ? RegistryKey.of(RegistryKeys.WORLD, new Identifier(JsonHelper.getString(jsonObject, "to"))) : null;
        return new Conditions(lootContextPredicate, registryKey, registryKey2);
    }

    public void trigger(ServerPlayerEntity player, RegistryKey<World> from, RegistryKey<World> to) {
        this.trigger(player, conditions -> conditions.matches(from, to));
    }

    @Override
    public /* synthetic */ AbstractCriterionConditions conditionsFromJson(JsonObject obj, LootContextPredicate playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return this.conditionsFromJson(obj, playerPredicate, predicateDeserializer);
    }

    public static class Conditions
    extends AbstractCriterionConditions {
        @Nullable
        private final RegistryKey<World> from;
        @Nullable
        private final RegistryKey<World> to;

        public Conditions(LootContextPredicate player, @Nullable RegistryKey<World> from, @Nullable RegistryKey<World> to) {
            super(ID, player);
            this.from = from;
            this.to = to;
        }

        public static Conditions create() {
            return new Conditions(LootContextPredicate.EMPTY, null, null);
        }

        public static Conditions create(RegistryKey<World> from, RegistryKey<World> to) {
            return new Conditions(LootContextPredicate.EMPTY, from, to);
        }

        public static Conditions to(RegistryKey<World> to) {
            return new Conditions(LootContextPredicate.EMPTY, null, to);
        }

        public static Conditions from(RegistryKey<World> from) {
            return new Conditions(LootContextPredicate.EMPTY, from, null);
        }

        public boolean matches(RegistryKey<World> from, RegistryKey<World> to) {
            if (this.from != null && this.from != from) {
                return false;
            }
            return this.to == null || this.to == to;
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            if (this.from != null) {
                jsonObject.addProperty("from", this.from.getValue().toString());
            }
            if (this.to != null) {
                jsonObject.addProperty("to", this.to.getValue().toString());
            }
            return jsonObject;
        }
    }
}

