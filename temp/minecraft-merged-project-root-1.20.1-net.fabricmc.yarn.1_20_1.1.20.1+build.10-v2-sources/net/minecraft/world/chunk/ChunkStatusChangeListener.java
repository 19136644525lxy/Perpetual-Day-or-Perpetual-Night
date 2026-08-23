/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.world.chunk;

import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.util.math.ChunkPos;

@FunctionalInterface
public interface ChunkStatusChangeListener {
    public void onChunkStatusChange(ChunkPos var1, ChunkLevelType var2);
}

