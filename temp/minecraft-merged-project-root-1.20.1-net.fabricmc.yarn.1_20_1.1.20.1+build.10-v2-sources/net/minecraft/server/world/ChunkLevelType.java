/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.server.world;

public enum ChunkLevelType {
    INACCESSIBLE,
    FULL,
    BLOCK_TICKING,
    ENTITY_TICKING;


    public boolean isAfter(ChunkLevelType levelType) {
        return this.ordinal() >= levelType.ordinal();
    }
}

