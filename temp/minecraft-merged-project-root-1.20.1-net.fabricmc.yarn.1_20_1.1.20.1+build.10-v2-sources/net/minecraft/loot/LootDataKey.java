/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.loot;

import net.minecraft.loot.LootDataType;
import net.minecraft.util.Identifier;

public record LootDataKey<T>(LootDataType<T> type, Identifier id) {
}

