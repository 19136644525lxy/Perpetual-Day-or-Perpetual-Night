/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.loot.condition;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.JsonSerializer;

public abstract class AlternativeLootCondition
implements LootCondition {
    final LootCondition[] terms;
    private final Predicate<LootContext> predicate;

    protected AlternativeLootCondition(LootCondition[] terms, Predicate<LootContext> predicate) {
        this.terms = terms;
        this.predicate = predicate;
    }

    @Override
    public final boolean test(LootContext lootContext) {
        return this.predicate.test(lootContext);
    }

    @Override
    public void validate(LootTableReporter reporter) {
        LootCondition.super.validate(reporter);
        for (int i = 0; i < this.terms.length; ++i) {
            this.terms[i].validate(reporter.makeChild(".term[" + i + "]"));
        }
    }

    @Override
    public /* synthetic */ boolean test(Object context) {
        return this.test((LootContext)context);
    }

    public static abstract class Serializer<T extends AlternativeLootCondition>
    implements JsonSerializer<T> {
        @Override
        public void toJson(JsonObject jsonObject, AlternativeLootCondition alternativeLootCondition, JsonSerializationContext jsonSerializationContext) {
            jsonObject.add("terms", jsonSerializationContext.serialize(alternativeLootCondition.terms));
        }

        @Override
        public T fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LootCondition[] lootConditions = JsonHelper.deserialize(jsonObject, "terms", jsonDeserializationContext, LootCondition[].class);
            return this.fromTerms(lootConditions);
        }

        protected abstract T fromTerms(LootCondition[] var1);

        @Override
        public /* synthetic */ Object fromJson(JsonObject json, JsonDeserializationContext context) {
            return this.fromJson(json, context);
        }
    }

    public static abstract class Builder
    implements LootCondition.Builder {
        private final List<LootCondition> terms = new ArrayList<LootCondition>();

        public Builder(LootCondition.Builder ... terms) {
            for (LootCondition.Builder builder : terms) {
                this.terms.add(builder.build());
            }
        }

        public void add(LootCondition.Builder builder) {
            this.terms.add(builder.build());
        }

        @Override
        public LootCondition build() {
            LootCondition[] lootConditions = (LootCondition[])this.terms.toArray(LootCondition[]::new);
            return this.build(lootConditions);
        }

        protected abstract LootCondition build(LootCondition[] var1);
    }
}

