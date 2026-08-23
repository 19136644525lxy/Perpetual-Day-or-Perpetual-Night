/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.loot.entry;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootDataKey;
import net.minecraft.loot.LootDataType;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.loot.entry.LootPoolEntryTypes;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class LootTableEntry
extends LeafEntry {
    final Identifier id;

    LootTableEntry(Identifier id, int weight, int quality, LootCondition[] conditions, LootFunction[] functions) {
        super(weight, quality, conditions, functions);
        this.id = id;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntryTypes.LOOT_TABLE;
    }

    @Override
    public void generateLoot(Consumer<ItemStack> lootConsumer, LootContext context) {
        LootTable lootTable = context.getDataLookup().getLootTable(this.id);
        lootTable.generateUnprocessedLoot(context, lootConsumer);
    }

    @Override
    public void validate(LootTableReporter reporter) {
        LootDataKey<LootTable> lootDataKey = new LootDataKey<LootTable>(LootDataType.LOOT_TABLES, this.id);
        if (reporter.isInStack(lootDataKey)) {
            reporter.report("Table " + this.id + " is recursively called");
            return;
        }
        super.validate(reporter);
        reporter.getDataLookup().getElementOptional(lootDataKey).ifPresentOrElse(table -> table.validate(reporter.makeChild("->{" + this.id + "}", lootDataKey)), () -> reporter.report("Unknown loot table called " + this.id));
    }

    public static LeafEntry.Builder<?> builder(Identifier id) {
        return LootTableEntry.builder((int weight, int quality, LootCondition[] conditions, LootFunction[] functions) -> new LootTableEntry(id, weight, quality, conditions, functions));
    }

    public static class Serializer
    extends LeafEntry.Serializer<LootTableEntry> {
        @Override
        public void addEntryFields(JsonObject jsonObject, LootTableEntry lootTableEntry, JsonSerializationContext jsonSerializationContext) {
            super.addEntryFields(jsonObject, lootTableEntry, jsonSerializationContext);
            jsonObject.addProperty("name", lootTableEntry.id.toString());
        }

        @Override
        protected LootTableEntry fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, int i, int j, LootCondition[] lootConditions, LootFunction[] lootFunctions) {
            Identifier identifier = new Identifier(JsonHelper.getString(jsonObject, "name"));
            return new LootTableEntry(identifier, i, j, lootConditions, lootFunctions);
        }

        @Override
        protected /* synthetic */ LeafEntry fromJson(JsonObject entryJson, JsonDeserializationContext context, int weight, int quality, LootCondition[] conditions, LootFunction[] functions) {
            return this.fromJson(entryJson, context, weight, quality, conditions, functions);
        }
    }
}

