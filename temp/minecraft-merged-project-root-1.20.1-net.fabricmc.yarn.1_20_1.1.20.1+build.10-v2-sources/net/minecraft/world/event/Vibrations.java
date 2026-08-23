/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.world.event;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.GameEventTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockStateRaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.GameEventListener;
import net.minecraft.world.event.listener.Vibration;
import net.minecraft.world.event.listener.VibrationSelector;
import org.jetbrains.annotations.Nullable;

public interface Vibrations {
    public static final GameEvent[] RESONATIONS = new GameEvent[]{GameEvent.RESONATE_1, GameEvent.RESONATE_2, GameEvent.RESONATE_3, GameEvent.RESONATE_4, GameEvent.RESONATE_5, GameEvent.RESONATE_6, GameEvent.RESONATE_7, GameEvent.RESONATE_8, GameEvent.RESONATE_9, GameEvent.RESONATE_10, GameEvent.RESONATE_11, GameEvent.RESONATE_12, GameEvent.RESONATE_13, GameEvent.RESONATE_14, GameEvent.RESONATE_15};
    public static final ToIntFunction<GameEvent> FREQUENCIES = Util.make(new Object2IntOpenHashMap(), frequencies -> {
        frequencies.defaultReturnValue(0);
        frequencies.put(GameEvent.STEP, 1);
        frequencies.put(GameEvent.SWIM, 1);
        frequencies.put(GameEvent.FLAP, 1);
        frequencies.put(GameEvent.PROJECTILE_LAND, 2);
        frequencies.put(GameEvent.HIT_GROUND, 2);
        frequencies.put(GameEvent.SPLASH, 2);
        frequencies.put(GameEvent.ITEM_INTERACT_FINISH, 3);
        frequencies.put(GameEvent.PROJECTILE_SHOOT, 3);
        frequencies.put(GameEvent.INSTRUMENT_PLAY, 3);
        frequencies.put(GameEvent.ENTITY_ROAR, 4);
        frequencies.put(GameEvent.ENTITY_SHAKE, 4);
        frequencies.put(GameEvent.ELYTRA_GLIDE, 4);
        frequencies.put(GameEvent.ENTITY_DISMOUNT, 5);
        frequencies.put(GameEvent.EQUIP, 5);
        frequencies.put(GameEvent.ENTITY_INTERACT, 6);
        frequencies.put(GameEvent.SHEAR, 6);
        frequencies.put(GameEvent.ENTITY_MOUNT, 6);
        frequencies.put(GameEvent.ENTITY_DAMAGE, 7);
        frequencies.put(GameEvent.DRINK, 8);
        frequencies.put(GameEvent.EAT, 8);
        frequencies.put(GameEvent.CONTAINER_CLOSE, 9);
        frequencies.put(GameEvent.BLOCK_CLOSE, 9);
        frequencies.put(GameEvent.BLOCK_DEACTIVATE, 9);
        frequencies.put(GameEvent.BLOCK_DETACH, 9);
        frequencies.put(GameEvent.CONTAINER_OPEN, 10);
        frequencies.put(GameEvent.BLOCK_OPEN, 10);
        frequencies.put(GameEvent.BLOCK_ACTIVATE, 10);
        frequencies.put(GameEvent.BLOCK_ATTACH, 10);
        frequencies.put(GameEvent.PRIME_FUSE, 10);
        frequencies.put(GameEvent.NOTE_BLOCK_PLAY, 10);
        frequencies.put(GameEvent.BLOCK_CHANGE, 11);
        frequencies.put(GameEvent.BLOCK_DESTROY, 12);
        frequencies.put(GameEvent.FLUID_PICKUP, 12);
        frequencies.put(GameEvent.BLOCK_PLACE, 13);
        frequencies.put(GameEvent.FLUID_PLACE, 13);
        frequencies.put(GameEvent.ENTITY_PLACE, 14);
        frequencies.put(GameEvent.LIGHTNING_STRIKE, 14);
        frequencies.put(GameEvent.TELEPORT, 14);
        frequencies.put(GameEvent.ENTITY_DIE, 15);
        frequencies.put(GameEvent.EXPLODE, 15);
        for (int i = 1; i <= 15; ++i) {
            frequencies.put(Vibrations.getResonation(i), i);
        }
    });

    public ListenerData getVibrationListenerData();

    public Callback getVibrationCallback();

    public static int getFrequency(GameEvent event) {
        return FREQUENCIES.applyAsInt(event);
    }

    public static GameEvent getResonation(int frequency) {
        return RESONATIONS[frequency - 1];
    }

    public static int getSignalStrength(float distance, int range) {
        double d = 15.0 / (double)range;
        return Math.max(1, 15 - MathHelper.floor(d * (double)distance));
    }

    public static interface Callback {
        public int getRange();

        public PositionSource getPositionSource();

        public boolean accepts(ServerWorld var1, BlockPos var2, GameEvent var3, GameEvent.Emitter var4);

        public void accept(ServerWorld var1, BlockPos var2, GameEvent var3, @Nullable Entity var4, @Nullable Entity var5, float var6);

        default public TagKey<GameEvent> getTag() {
            return GameEventTags.VIBRATIONS;
        }

        default public boolean triggersAvoidCriterion() {
            return false;
        }

        default public boolean requiresTickingChunksAround() {
            return false;
        }

        default public int getDelay(float distance) {
            return MathHelper.floor(distance);
        }

        default public boolean canAccept(GameEvent gameEvent, GameEvent.Emitter emitter) {
            if (!gameEvent.isIn(this.getTag())) {
                return false;
            }
            Entity entity = emitter.sourceEntity();
            if (entity != null) {
                if (entity.isSpectator()) {
                    return false;
                }
                if (entity.bypassesSteppingEffects() && gameEvent.isIn(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
                    if (this.triggersAvoidCriterion() && entity instanceof ServerPlayerEntity) {
                        ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)entity;
                        Criteria.AVOID_VIBRATION.trigger(serverPlayerEntity);
                    }
                    return false;
                }
                if (entity.occludeVibrationSignals()) {
                    return false;
                }
            }
            if (emitter.affectedState() != null) {
                return !emitter.affectedState().isIn(BlockTags.DAMPENS_VIBRATIONS);
            }
            return true;
        }

        default public void onListen() {
        }
    }

    public static interface Ticker {
        public static void tick(World world, ListenerData listenerData, Callback callback) {
            if (!(world instanceof ServerWorld)) {
                return;
            }
            ServerWorld serverWorld = (ServerWorld)world;
            if (listenerData.vibration == null) {
                Ticker.tryListen(serverWorld, listenerData, callback);
            }
            if (listenerData.vibration == null) {
                return;
            }
            boolean bl = listenerData.getDelay() > 0;
            Ticker.spawnVibrationParticle(serverWorld, listenerData, callback);
            listenerData.tickDelay();
            if (listenerData.getDelay() <= 0) {
                bl = Ticker.accept(serverWorld, listenerData, callback, listenerData.vibration);
            }
            if (bl) {
                callback.onListen();
            }
        }

        private static void tryListen(ServerWorld world, ListenerData listenerData, Callback callback) {
            listenerData.getSelector().getVibrationToTick(world.getTime()).ifPresent(vibration -> {
                listenerData.setVibration((Vibration)vibration);
                Vec3d vec3d = vibration.pos();
                listenerData.setDelay(callback.getDelay(vibration.distance()));
                world.spawnParticles(new VibrationParticleEffect(callback.getPositionSource(), listenerData.getDelay()), vec3d.x, vec3d.y, vec3d.z, 1, 0.0, 0.0, 0.0, 0.0);
                callback.onListen();
                listenerData.getSelector().clear();
            });
        }

        private static void spawnVibrationParticle(ServerWorld world, ListenerData listenerData, Callback callback) {
            double g;
            double f;
            int j;
            double d;
            double e;
            boolean bl;
            if (!listenerData.shouldSpawnParticle()) {
                return;
            }
            if (listenerData.vibration == null) {
                listenerData.setSpawnParticle(false);
                return;
            }
            Vec3d vec3d = listenerData.vibration.pos();
            PositionSource positionSource = callback.getPositionSource();
            Vec3d vec3d2 = positionSource.getPos(world).orElse(vec3d);
            int i = listenerData.getDelay();
            boolean bl2 = bl = world.spawnParticles(new VibrationParticleEffect(positionSource, i), e = MathHelper.lerp(d = 1.0 - (double)i / (double)(j = callback.getDelay(listenerData.vibration.distance())), vec3d.x, vec3d2.x), f = MathHelper.lerp(d, vec3d.y, vec3d2.y), g = MathHelper.lerp(d, vec3d.z, vec3d2.z), 1, 0.0, 0.0, 0.0, 0.0) > 0;
            if (bl) {
                listenerData.setSpawnParticle(false);
            }
        }

        private static boolean accept(ServerWorld world, ListenerData listenerData, Callback callback, Vibration vibration) {
            BlockPos blockPos = BlockPos.ofFloored(vibration.pos());
            BlockPos blockPos2 = callback.getPositionSource().getPos(world).map(BlockPos::ofFloored).orElse(blockPos);
            if (callback.requiresTickingChunksAround() && !Ticker.areChunksTickingAround(world, blockPos2)) {
                return false;
            }
            callback.accept(world, blockPos, vibration.gameEvent(), vibration.getEntity(world).orElse(null), vibration.getOwner(world).orElse(null), VibrationListener.getTravelDelay(blockPos, blockPos2));
            listenerData.setVibration(null);
            return true;
        }

        private static boolean areChunksTickingAround(World world, BlockPos pos) {
            ChunkPos chunkPos = new ChunkPos(pos);
            for (int i = chunkPos.x - 1; i < chunkPos.x + 1; ++i) {
                for (int j = chunkPos.z - 1; j < chunkPos.z + 1; ++j) {
                    WorldChunk chunk = world.getChunkManager().getWorldChunk(i, j);
                    if (chunk != null && world.shouldTickBlocksInChunk(chunk.getPos().toLong())) continue;
                    return false;
                }
            }
            return true;
        }
    }

    public static class VibrationListener
    implements GameEventListener {
        private final Vibrations receiver;

        public VibrationListener(Vibrations receiver) {
            this.receiver = receiver;
        }

        @Override
        public PositionSource getPositionSource() {
            return this.receiver.getVibrationCallback().getPositionSource();
        }

        @Override
        public int getRange() {
            return this.receiver.getVibrationCallback().getRange();
        }

        @Override
        public boolean listen(ServerWorld world, GameEvent event, GameEvent.Emitter emitter, Vec3d emitterPos) {
            ListenerData listenerData = this.receiver.getVibrationListenerData();
            Callback callback = this.receiver.getVibrationCallback();
            if (listenerData.getVibration() != null) {
                return false;
            }
            if (!callback.canAccept(event, emitter)) {
                return false;
            }
            Optional<Vec3d> optional = callback.getPositionSource().getPos(world);
            if (optional.isEmpty()) {
                return false;
            }
            Vec3d vec3d = optional.get();
            if (!callback.accepts(world, BlockPos.ofFloored(emitterPos), event, emitter)) {
                return false;
            }
            if (VibrationListener.isOccluded(world, emitterPos, vec3d)) {
                return false;
            }
            this.listen(world, listenerData, event, emitter, emitterPos, vec3d);
            return true;
        }

        public void forceListen(ServerWorld world, GameEvent event, GameEvent.Emitter emitter, Vec3d emitterPos) {
            this.receiver.getVibrationCallback().getPositionSource().getPos(world).ifPresent(pos -> this.listen(world, this.receiver.getVibrationListenerData(), event, emitter, emitterPos, (Vec3d)pos));
        }

        private void listen(ServerWorld world, ListenerData listenerData, GameEvent event, GameEvent.Emitter emitter, Vec3d emitterPos, Vec3d listenerPos) {
            listenerData.vibrationSelector.tryAccept(new Vibration(event, (float)emitterPos.distanceTo(listenerPos), emitterPos, emitter.sourceEntity()), world.getTime());
        }

        public static float getTravelDelay(BlockPos emitterPos, BlockPos listenerPos) {
            return (float)Math.sqrt(emitterPos.getSquaredDistance(listenerPos));
        }

        private static boolean isOccluded(World world, Vec3d emitterPos, Vec3d listenerPos) {
            Vec3d vec3d = new Vec3d((double)MathHelper.floor(emitterPos.x) + 0.5, (double)MathHelper.floor(emitterPos.y) + 0.5, (double)MathHelper.floor(emitterPos.z) + 0.5);
            Vec3d vec3d2 = new Vec3d((double)MathHelper.floor(listenerPos.x) + 0.5, (double)MathHelper.floor(listenerPos.y) + 0.5, (double)MathHelper.floor(listenerPos.z) + 0.5);
            for (Direction direction : Direction.values()) {
                Vec3d vec3d3 = vec3d.offset(direction, 1.0E-5f);
                if (world.raycast(new BlockStateRaycastContext(vec3d3, vec3d2, state -> state.isIn(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType() == HitResult.Type.BLOCK) continue;
                return false;
            }
            return true;
        }
    }

    public static final class ListenerData {
        public static Codec<ListenerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(Vibration.CODEC.optionalFieldOf("event").forGetter(listenerData -> Optional.ofNullable(listenerData.vibration)), ((MapCodec)VibrationSelector.CODEC.fieldOf("selector")).forGetter(ListenerData::getSelector), ((MapCodec)Codecs.NONNEGATIVE_INT.fieldOf("event_delay")).orElse(0).forGetter(ListenerData::getDelay)).apply((Applicative<ListenerData, ?>)instance, (vibration, selector, delay) -> new ListenerData(vibration.orElse(null), (VibrationSelector)selector, (int)delay, true)));
        public static final String LISTENER_NBT_KEY = "listener";
        @Nullable
        Vibration vibration;
        private int delay;
        final VibrationSelector vibrationSelector;
        private boolean spawnParticle;

        private ListenerData(@Nullable Vibration vibration, VibrationSelector vibrationSelector, int delay, boolean spawnParticle) {
            this.vibration = vibration;
            this.delay = delay;
            this.vibrationSelector = vibrationSelector;
            this.spawnParticle = spawnParticle;
        }

        public ListenerData() {
            this(null, new VibrationSelector(), 0, false);
        }

        public VibrationSelector getSelector() {
            return this.vibrationSelector;
        }

        @Nullable
        public Vibration getVibration() {
            return this.vibration;
        }

        public void setVibration(@Nullable Vibration vibration) {
            this.vibration = vibration;
        }

        public int getDelay() {
            return this.delay;
        }

        public void setDelay(int delay) {
            this.delay = delay;
        }

        public void tickDelay() {
            this.delay = Math.max(0, this.delay - 1);
        }

        public boolean shouldSpawnParticle() {
            return this.spawnParticle;
        }

        public void setSpawnParticle(boolean spawnParticle) {
            this.spawnParticle = spawnParticle;
        }
    }
}

