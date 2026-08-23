/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.loot;

import java.util.Optional;
import net.minecraft.loot.LootDataKey;
import net.minecraft.loot.LootDataType;
import net.minecraft.loot.LootTable;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface LootDataLookup {
    @Nullable
    public <T> T getElement(LootDataKey<T> var1);

    @Nullable
    default public <T> T getElement(LootDataType<T> type, Identifier id) {
        return this.getElement(new LootDataKey<T>(type, id));
    }

    default public <T> Optional<T> getElementOptional(LootDataKey<T> key) {
        return Optional.ofNullable(this.getElement(key));
    }

    default public <T> Optional<T> getElementOptional(LootDataType<T> type, Identifier id) {
        return this.getElementOptional(new LootDataKey<T>(type, id));
    }

    default public LootTable getLootTable(Identifier id) {
        return this.getElementOptional(LootDataType.LOOT_TABLES, id).orElse(LootTable.EMPTY);
    }
}

