/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.loot.condition;

import net.minecraft.loot.condition.AlternativeLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.condition.LootConditionTypes;

public class AllOfLootCondition
extends AlternativeLootCondition {
    AllOfLootCondition(LootCondition[] terms) {
        super(terms, LootConditionTypes.matchingAll(terms));
    }

    @Override
    public LootConditionType getType() {
        return LootConditionTypes.ALL_OF;
    }

    public static Builder builder(LootCondition.Builder ... terms) {
        return new Builder(terms);
    }

    public static class Builder
    extends AlternativeLootCondition.Builder {
        public Builder(LootCondition.Builder ... builders) {
            super(builders);
        }

        @Override
        public Builder and(LootCondition.Builder builder) {
            this.add(builder);
            return this;
        }

        @Override
        protected LootCondition build(LootCondition[] terms) {
            return new AllOfLootCondition(terms);
        }
    }

    public static class Serializer
    extends AlternativeLootCondition.Serializer<AllOfLootCondition> {
        @Override
        protected AllOfLootCondition fromTerms(LootCondition[] lootConditions) {
            return new AllOfLootCondition(lootConditions);
        }

        @Override
        protected /* synthetic */ AlternativeLootCondition fromTerms(LootCondition[] terms) {
            return this.fromTerms(terms);
        }
    }
}

