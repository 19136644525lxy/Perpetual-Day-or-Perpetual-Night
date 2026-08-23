/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.util.math.random;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSequence;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.PersistentState;
import org.slf4j.Logger;

public class RandomSequencesState
extends PersistentState {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final long seed;
    private final Map<Identifier, RandomSequence> sequences = new Object2ObjectOpenHashMap<Identifier, RandomSequence>();

    public RandomSequencesState(long seed) {
        this.seed = seed;
    }

    public Random getOrCreate(Identifier id) {
        final Random random = this.sequences.computeIfAbsent(id, idx -> new RandomSequence(this.seed, (Identifier)idx)).getSource();
        return new Random(){

            @Override
            public Random split() {
                RandomSequencesState.this.markDirty();
                return random.split();
            }

            @Override
            public RandomSplitter nextSplitter() {
                RandomSequencesState.this.markDirty();
                return random.nextSplitter();
            }

            @Override
            public void setSeed(long seed) {
                RandomSequencesState.this.markDirty();
                random.setSeed(seed);
            }

            @Override
            public int nextInt() {
                RandomSequencesState.this.markDirty();
                return random.nextInt();
            }

            @Override
            public int nextInt(int bound) {
                RandomSequencesState.this.markDirty();
                return random.nextInt(bound);
            }

            @Override
            public long nextLong() {
                RandomSequencesState.this.markDirty();
                return random.nextLong();
            }

            @Override
            public boolean nextBoolean() {
                RandomSequencesState.this.markDirty();
                return random.nextBoolean();
            }

            @Override
            public float nextFloat() {
                RandomSequencesState.this.markDirty();
                return random.nextFloat();
            }

            @Override
            public double nextDouble() {
                RandomSequencesState.this.markDirty();
                return random.nextDouble();
            }

            @Override
            public double nextGaussian() {
                RandomSequencesState.this.markDirty();
                return random.nextGaussian();
            }
        };
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        this.sequences.forEach((id, sequence) -> nbt.put(id.toString(), RandomSequence.CODEC.encodeStart(NbtOps.INSTANCE, (RandomSequence)sequence).result().orElseThrow()));
        return nbt;
    }

    public static RandomSequencesState fromNbt(long seed, NbtCompound nbt) {
        RandomSequencesState randomSequencesState = new RandomSequencesState(seed);
        Set<String> set = nbt.getKeys();
        for (String string : set) {
            try {
                RandomSequence randomSequence = (RandomSequence)RandomSequence.CODEC.decode(NbtOps.INSTANCE, nbt.get(string)).result().get().getFirst();
                randomSequencesState.sequences.put(new Identifier(string), randomSequence);
            } catch (Exception exception) {
                LOGGER.error("Failed to load random sequence {}", (Object)string, (Object)exception);
            }
        }
        return randomSequencesState;
    }
}

