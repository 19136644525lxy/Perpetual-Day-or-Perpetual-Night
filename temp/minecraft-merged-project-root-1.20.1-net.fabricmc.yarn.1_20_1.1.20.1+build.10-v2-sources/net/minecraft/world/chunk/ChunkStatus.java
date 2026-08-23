/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.world.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.jfr.Finishable;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.BelowZeroRetrogen;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

public class ChunkStatus {
    public static final int field_35470 = 8;
    private static final EnumSet<Heightmap.Type> PRE_CARVER_HEIGHTMAPS = EnumSet.of(Heightmap.Type.OCEAN_FLOOR_WG, Heightmap.Type.WORLD_SURFACE_WG);
    public static final EnumSet<Heightmap.Type> POST_CARVER_HEIGHTMAPS = EnumSet.of(Heightmap.Type.OCEAN_FLOOR, Heightmap.Type.WORLD_SURFACE, Heightmap.Type.MOTION_BLOCKING, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES);
    /**
     * A load task which only bumps the chunk status of the chunk.
     */
    private static final LoadTask STATUS_BUMP_LOAD_TASK = (targetStatus, world, structureTemplateManager, lightingProvider, fullChunkConverter, chunk) -> CompletableFuture.completedFuture(Either.left(chunk));
    public static final ChunkStatus EMPTY = ChunkStatus.register("empty", null, -1, PRE_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (ChunkStatus targetStatus, ServerWorld world, ChunkGenerator chunkGenerator, List<Chunk> chunks, Chunk chunk) -> {});
    public static final ChunkStatus STRUCTURE_STARTS = ChunkStatus.register("structure_starts", EMPTY, 0, false, PRE_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (targetStatus, executor, world, generator, structureTemplateManager, lightingProvider, fullChunkConverter, chunks, chunk) -> {
        if (world.getServer().getSaveProperties().getGeneratorOptions().shouldGenerateStructures()) {
            generator.setStructureStarts(world.getRegistryManager(), world.getChunkManager().getStructurePlacementCalculator(), world.getStructureAccessor(), chunk, structureTemplateManager);
        }
        world.cacheStructures(chunk);
        return CompletableFuture.completedFuture(Either.left(chunk));
    }, (targetStatus, world, structureTemplateManager, lightingProvider, fullChunkConverter, chunk) -> {
        world.cacheStructures(chunk);
        return CompletableFuture.completedFuture(Either.left(chunk));
    });
    public static final ChunkStatus STRUCTURE_REFERENCES = ChunkStatus.register("structure_references", STRUCTURE_STARTS, 8, PRE_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (ChunkStatus targetStatus, ServerWorld world, ChunkGenerator generator, List<Chunk> chunks, Chunk chunk) -> {
        ChunkRegion chunkRegion = new ChunkRegion(world, chunks, targetStatus, -1);
        generator.addStructureReferences(chunkRegion, world.getStructureAccessor().forRegion(chunkRegion), chunk);
    });
    public static final ChunkStatus BIOMES = ChunkStatus.register("biomes", STRUCTURE_REFERENCES, 8, PRE_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (ChunkStatus targetStatus, Executor executor, ServerWorld world, ChunkGenerator generator, StructureTemplateManager structureTemplateManager, ServerLightingProvider lightingProvider, Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> fullChunkConverter, List<Chunk> chunks, Chunk chunk) -> {
        ChunkRegion chunkRegion = new ChunkRegion(world, chunks, targetStatus, -1);
        return generator.populateBiomes(executor, world.getChunkManager().getNoiseConfig(), Blender.getBlender(chunkRegion), world.getStructureAccessor().forRegion(chunkRegion), chunk).thenApply(populatedChunk -> Either.left(populatedChunk));
    });
    public static final ChunkStatus NOISE = ChunkStatus.register("noise", BIOMES, 8, PRE_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (ChunkStatus targetStatus, Executor executor, ServerWorld world, ChunkGenerator generator, StructureTemplateManager structureTemplateManager, ServerLightingProvider lightingProvider, Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> fullChunkConverter, List<Chunk> chunks, Chunk chunk) -> {
        ChunkRegion chunkRegion = new ChunkRegion(world, chunks, targetStatus, 0);
        return generator.populateNoise(executor, Blender.getBlender(chunkRegion), world.getChunkManager().getNoiseConfig(), world.getStructureAccessor().forRegion(chunkRegion), chunk).thenApply(populatedChunk -> {
            ProtoChunk protoChunk;
            BelowZeroRetrogen belowZeroRetrogen;
            if (populatedChunk instanceof ProtoChunk && (belowZeroRetrogen = (protoChunk = (ProtoChunk)populatedChunk).getBelowZeroRetrogen()) != null) {
                BelowZeroRetrogen.replaceOldBedrock(protoChunk);
                if (belowZeroRetrogen.hasMissingBedrock()) {
                    belowZeroRetrogen.fillColumnsWithAirIfMissingBedrock(protoChunk);
                }
            }
            return Either.left(populatedChunk);
        });
    });
    public static final ChunkStatus SURFACE = ChunkStatus.register("surface", NOISE, 8, PRE_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (ChunkStatus targetStatus, ServerWorld world, ChunkGenerator generator, List<Chunk> chunks, Chunk chunk) -> {
        ChunkRegion chunkRegion = new ChunkRegion(world, chunks, targetStatus, 0);
        generator.buildSurface(chunkRegion, world.getStructureAccessor().forRegion(chunkRegion), world.getChunkManager().getNoiseConfig(), chunk);
    });
    public static final ChunkStatus CARVERS = ChunkStatus.register("carvers", SURFACE, 8, POST_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (ChunkStatus targetStatus, ServerWorld world, ChunkGenerator generator, List<Chunk> chunks, Chunk chunk) -> {
        ChunkRegion chunkRegion = new ChunkRegion(world, chunks, targetStatus, 0);
        if (chunk instanceof ProtoChunk) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            Blender.createCarvingMasks(chunkRegion, protoChunk);
        }
        generator.carve(chunkRegion, world.getSeed(), world.getChunkManager().getNoiseConfig(), world.getBiomeAccess(), world.getStructureAccessor().forRegion(chunkRegion), chunk, GenerationStep.Carver.AIR);
    });
    public static final ChunkStatus FEATURES = ChunkStatus.register("features", CARVERS, 8, POST_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (ChunkStatus targetStatus, ServerWorld world, ChunkGenerator generator, List<Chunk> chunks, Chunk chunk) -> {
        Heightmap.populateHeightmaps(chunk, EnumSet.of(Heightmap.Type.MOTION_BLOCKING, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Heightmap.Type.OCEAN_FLOOR, Heightmap.Type.WORLD_SURFACE));
        ChunkRegion chunkRegion = new ChunkRegion(world, chunks, targetStatus, 1);
        generator.generateFeatures(chunkRegion, chunk, world.getStructureAccessor().forRegion(chunkRegion));
        Blender.tickLeavesAndFluids(chunkRegion, chunk);
    });
    public static final ChunkStatus INITIALIZE_LIGHT = ChunkStatus.register("initialize_light", FEATURES, 0, false, POST_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (targetStatus, executor, world, generator, structureTemplateManager, lightingProvider, fullChunkConverter, chunks, chunk) -> ChunkStatus.getInitializeLightingFuture(lightingProvider, chunk), (status, world, structureTemplateManager, lightingProvider, fullChunkConverter, chunk) -> ChunkStatus.getInitializeLightingFuture(lightingProvider, chunk));
    public static final ChunkStatus LIGHT = ChunkStatus.register("light", INITIALIZE_LIGHT, 1, true, POST_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (targetStatus, executor, world, generator, structureTemplateManager, lightingProvider, fullChunkConverter, chunks, chunk) -> ChunkStatus.getLightingFuture(lightingProvider, chunk), (targetStatus, world, structureTemplateManager, lightingProvider, fullChunkConverter, chunk) -> ChunkStatus.getLightingFuture(lightingProvider, chunk));
    public static final ChunkStatus SPAWN = ChunkStatus.register("spawn", LIGHT, 0, POST_CARVER_HEIGHTMAPS, ChunkType.PROTOCHUNK, (ChunkStatus targetStatus, ServerWorld world, ChunkGenerator generator, List<Chunk> chunks, Chunk chunk) -> {
        if (!chunk.hasBelowZeroRetrogen()) {
            generator.populateEntities(new ChunkRegion(world, chunks, targetStatus, -1));
        }
    });
    public static final ChunkStatus FULL = ChunkStatus.register("full", SPAWN, 0, false, POST_CARVER_HEIGHTMAPS, ChunkType.LEVELCHUNK, (targetStatus, executor, world, generator, structureTemplateManager, lightingProvider, fullChunkConverter, chunks, chunk) -> (CompletableFuture)fullChunkConverter.apply(chunk), (targetStatus, world, structureTemplateManager, lightingProvider, fullChunkConverter, chunk) -> (CompletableFuture)fullChunkConverter.apply(chunk));
    private static final List<ChunkStatus> DISTANCE_TO_STATUS = ImmutableList.of(FULL, INITIALIZE_LIGHT, CARVERS, BIOMES, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, new ChunkStatus[0]);
    private static final IntList STATUS_TO_DISTANCE = Util.make(new IntArrayList(ChunkStatus.createOrderedList().size()), statusToDistance -> {
        int i = 0;
        for (int j = ChunkStatus.createOrderedList().size() - 1; j >= 0; --j) {
            while (i + 1 < DISTANCE_TO_STATUS.size() && j <= DISTANCE_TO_STATUS.get(i + 1).getIndex()) {
                ++i;
            }
            statusToDistance.add(0, i);
        }
    });
    private final int index;
    private final ChunkStatus previous;
    private final GenerationTask generationTask;
    private final LoadTask loadTask;
    private final int taskMargin;
    private final boolean shouldAlwaysUpgrade;
    private final ChunkType chunkType;
    private final EnumSet<Heightmap.Type> heightMapTypes;

    private static CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getInitializeLightingFuture(ServerLightingProvider lightingProvider, Chunk chunk) {
        chunk.refreshSurfaceY();
        ((ProtoChunk)chunk).setLightingProvider(lightingProvider);
        boolean bl = ChunkStatus.shouldExcludeBlockLight(chunk);
        return lightingProvider.initializeLight(chunk, bl).thenApply(Either::left);
    }

    private static CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getLightingFuture(ServerLightingProvider lightingProvider, Chunk chunk) {
        boolean bl = ChunkStatus.shouldExcludeBlockLight(chunk);
        return lightingProvider.light(chunk, bl).thenApply(Either::left);
    }

    private static ChunkStatus register(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<Heightmap.Type> heightMapTypes, ChunkType chunkType, SimpleGenerationTask task) {
        return ChunkStatus.register(id, previous, taskMargin, heightMapTypes, chunkType, (GenerationTask)task);
    }

    private static ChunkStatus register(String id, @Nullable ChunkStatus previous, int taskMargin, EnumSet<Heightmap.Type> heightMapTypes, ChunkType chunkType, GenerationTask task) {
        return ChunkStatus.register(id, previous, taskMargin, false, heightMapTypes, chunkType, task, STATUS_BUMP_LOAD_TASK);
    }

    private static ChunkStatus register(String id, @Nullable ChunkStatus previous, int taskMargin, boolean shouldAlwaysUpgrade, EnumSet<Heightmap.Type> heightMapTypes, ChunkType chunkType, GenerationTask generationTask, LoadTask loadTask) {
        return Registry.register(Registries.CHUNK_STATUS, id, new ChunkStatus(previous, taskMargin, shouldAlwaysUpgrade, heightMapTypes, chunkType, generationTask, loadTask));
    }

    public static List<ChunkStatus> createOrderedList() {
        ChunkStatus chunkStatus;
        ArrayList<ChunkStatus> list = Lists.newArrayList();
        for (chunkStatus = FULL; chunkStatus.getPrevious() != chunkStatus; chunkStatus = chunkStatus.getPrevious()) {
            list.add(chunkStatus);
        }
        list.add(chunkStatus);
        Collections.reverse(list);
        return list;
    }

    private static boolean shouldExcludeBlockLight(Chunk chunk) {
        return chunk.getStatus().isAtLeast(LIGHT) && chunk.isLightOn();
    }

    public static ChunkStatus byDistanceFromFull(int level) {
        if (level >= DISTANCE_TO_STATUS.size()) {
            return EMPTY;
        }
        if (level < 0) {
            return FULL;
        }
        return DISTANCE_TO_STATUS.get(level);
    }

    public static int getMaxDistanceFromFull() {
        return DISTANCE_TO_STATUS.size();
    }

    public static int getDistanceFromFull(ChunkStatus status) {
        return STATUS_TO_DISTANCE.getInt(status.getIndex());
    }

    ChunkStatus(@Nullable ChunkStatus previous, int taskMargin, boolean shouldAlwaysUpgrade, EnumSet<Heightmap.Type> heightMapTypes, ChunkType chunkType, GenerationTask generationTask, LoadTask loadTask) {
        this.previous = previous == null ? this : previous;
        this.generationTask = generationTask;
        this.loadTask = loadTask;
        this.taskMargin = taskMargin;
        this.shouldAlwaysUpgrade = shouldAlwaysUpgrade;
        this.chunkType = chunkType;
        this.heightMapTypes = heightMapTypes;
        this.index = previous == null ? 0 : previous.getIndex() + 1;
    }

    public int getIndex() {
        return this.index;
    }

    public ChunkStatus getPrevious() {
        return this.previous;
    }

    public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> runGenerationTask(Executor executor, ServerWorld world, ChunkGenerator generator, StructureTemplateManager structureTemplateManager, ServerLightingProvider lightingProvider, Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> fullChunkConverter, List<Chunk> chunks) {
        Chunk chunk2 = chunks.get(chunks.size() / 2);
        Finishable finishable = FlightProfiler.INSTANCE.startChunkGenerationProfiling(chunk2.getPos(), world.getRegistryKey(), this.toString());
        return this.generationTask.doWork(this, executor, world, generator, structureTemplateManager, lightingProvider, fullChunkConverter, chunks, chunk2).thenApply(chunk -> {
            chunk.ifLeft(c -> {
                ProtoChunk protoChunk;
                if (c instanceof ProtoChunk && !(protoChunk = (ProtoChunk)c).getStatus().isAtLeast(this)) {
                    protoChunk.setStatus(this);
                }
            });
            if (finishable != null) {
                finishable.finish();
            }
            return chunk;
        });
    }

    public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> runLoadTask(ServerWorld world, StructureTemplateManager structureTemplateManager, ServerLightingProvider lightingProvider, Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> fullChunkConverter, Chunk chunk) {
        return this.loadTask.doWork(this, world, structureTemplateManager, lightingProvider, fullChunkConverter, chunk);
    }

    public int getTaskMargin() {
        return this.taskMargin;
    }

    public boolean shouldAlwaysUpgrade() {
        return this.shouldAlwaysUpgrade;
    }

    public ChunkType getChunkType() {
        return this.chunkType;
    }

    public static ChunkStatus byId(String id) {
        return Registries.CHUNK_STATUS.get(Identifier.tryParse(id));
    }

    public EnumSet<Heightmap.Type> getHeightmapTypes() {
        return this.heightMapTypes;
    }

    public boolean isAtLeast(ChunkStatus chunkStatus) {
        return this.getIndex() >= chunkStatus.getIndex();
    }

    public String toString() {
        return Registries.CHUNK_STATUS.getId(this).toString();
    }

    public static enum ChunkType {
        PROTOCHUNK,
        LEVELCHUNK;

    }

    static interface GenerationTask {
        public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> doWork(ChunkStatus var1, Executor var2, ServerWorld var3, ChunkGenerator var4, StructureTemplateManager var5, ServerLightingProvider var6, Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> var7, List<Chunk> var8, Chunk var9);
    }

    static interface LoadTask {
        public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> doWork(ChunkStatus var1, ServerWorld var2, StructureTemplateManager var3, ServerLightingProvider var4, Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> var5, Chunk var6);
    }

    static interface SimpleGenerationTask
    extends GenerationTask {
        @Override
        default public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> doWork(ChunkStatus chunkStatus, Executor executor, ServerWorld serverWorld, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, ServerLightingProvider serverLightingProvider, Function<Chunk, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> function, List<Chunk> list, Chunk chunk) {
            this.doWork(chunkStatus, serverWorld, chunkGenerator, list, chunk);
            return CompletableFuture.completedFuture(Either.left(chunk));
        }

        public void doWork(ChunkStatus var1, ServerWorld var2, ChunkGenerator var3, List<Chunk> var4, Chunk var5);
    }
}

