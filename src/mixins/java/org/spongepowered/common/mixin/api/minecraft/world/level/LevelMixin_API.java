/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.api.minecraft.world.level;

import net.kyori.adventure.sound.Sound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StaticTagHelper;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelData;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.sound.music.MusicDisc;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.registry.RegistryHolder;
import org.spongepowered.api.registry.RegistryType;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.tag.Tag;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.HeightTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.WorldLike;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.biome.Biome;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.api.world.volume.archetype.ArchetypeVolume;
import org.spongepowered.api.world.volume.biome.BiomeVolume;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumeApplicators;
import org.spongepowered.api.world.volume.stream.VolumeCollectors;
import org.spongepowered.api.world.volume.stream.VolumePositionTranslators;
import org.spongepowered.api.world.volume.stream.VolumeStream;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.accessor.server.level.ChunkMapAccessor;
import org.spongepowered.common.accessor.world.entity.EntityAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.world.level.LevelBridge;
import org.spongepowered.common.effect.particle.SpongeParticleHelper;
import org.spongepowered.common.effect.record.SpongeMusicDisc;
import org.spongepowered.common.entity.living.human.HumanEntity;
import org.spongepowered.common.registry.InitialRegistryData;
import org.spongepowered.common.registry.RegistryHolderLogic;
import org.spongepowered.common.registry.SpongeRegistryHolder;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.storage.SpongeChunkLayout;
import org.spongepowered.common.world.volume.VolumeStreamUtils;
import org.spongepowered.common.world.volume.buffer.archetype.SpongeArchetypeVolume;
import org.spongepowered.common.world.volume.buffer.entity.ObjectArrayMutableEntityBuffer;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(net.minecraft.world.level.Level.class)
public abstract class LevelMixin_API<W extends World<W, L>, L extends Location<W, L>> implements World<W, L>, SpongeRegistryHolder, AutoCloseable {

    // @formatter:off
    @Shadow public @Final Random random;
    @Shadow @Final public List<net.minecraft.world.level.block.entity.BlockEntity> blockEntityList;

    @Shadow @Nullable public abstract MinecraftServer shadow$getServer();
    @Shadow public abstract BlockState shadow$getBlockState(BlockPos p_180495_1_);
    @Shadow public abstract void shadow$playSound(@javax.annotation.Nullable net.minecraft.world.entity.player.Player p_184148_1_, double p_184148_2_, double p_184148_4_, double p_184148_6_, SoundEvent p_184148_8_, SoundSource p_184148_9_, float p_184148_10_, float p_184148_11_);
    @Shadow public abstract LevelData shadow$getLevelData();
    @Shadow public abstract void shadow$setBlockEntity(BlockPos pos, @javax.annotation.Nullable net.minecraft.world.level.block.entity.BlockEntity tileEntityIn);
    @Shadow public abstract void shadow$removeBlockEntity(BlockPos pos);
    @Shadow public abstract ResourceKey<net.minecraft.world.level.Level> shadow$dimension();
    @Shadow public abstract LevelChunk shadow$getChunkAt(BlockPos param0);
    @Shadow public abstract List<net.minecraft.world.entity.Entity> shadow$getEntities(
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.Entity param0,
            net.minecraft.world.phys.AABB param1,
            @org.jetbrains.annotations.Nullable Predicate<? super net.minecraft.world.entity.Entity> param2);
    @Shadow public abstract <T extends net.minecraft.world.entity.Entity> List<T> shadow$getEntitiesOfClass(Class<? extends T> param0,
            net.minecraft.world.phys.AABB param1,
            @org.jetbrains.annotations.Nullable Predicate<? super T> param2);
    // @formatter:on

    private Context api$context;
    private RegistryHolderLogic api$registryHolder;

    // World

    @Override
    public Optional<? extends Player> closestPlayer(final int x, final int y, final int z, final double distance, final Predicate<? super Player> predicate) {
        return Optional.ofNullable((Player) ((net.minecraft.world.level.Level) (Object) this).getNearestPlayer(x, y, z, distance, (Predicate) Objects.requireNonNull(predicate, "predicate")));
    }

    @Override
    public WorldChunk chunk(final int cx, final int cy, final int cz) {
        final ChunkAccess chunk = ((Level) (Object) this).getChunk(cx, cz, ChunkStatus.EMPTY, true);
        if (chunk instanceof WorldChunk) {
            return (WorldChunk) chunk;
        }
        if (chunk instanceof ImposterProtoChunk) {
            return (WorldChunk) ((ImposterProtoChunk) chunk).getWrapped();
        }
        throw new IllegalStateException("Chunk is a Proto-Chunk"); // TODO this may return a ProtoChunk
    }

    @Override
    public Optional<WorldChunk> loadChunk(final int cx, final int cy, final int cz, final boolean shouldGenerate) {
        if (!SpongeChunkLayout.INSTANCE.isValidChunk(cx, cy, cz)) {
            return Optional.empty();
        }
        final ChunkSource chunkProvider = ((LevelAccessor) this).getChunkSource();

        // If we aren't generating, return the chunk
        final ChunkStatus status = shouldGenerate ? ChunkStatus.EMPTY : ChunkStatus.FULL;
        final @Nullable ChunkAccess chunkAccess = chunkProvider.getChunk(cx, cz, status, true);
        if (chunkAccess == null) {
            return Optional.empty();
        }

        if (chunkAccess instanceof ImposterProtoChunk) {
            return Optional.of((WorldChunk) ((ImposterProtoChunk) chunkAccess).getWrapped());
        }

        return Optional.of((WorldChunk) chunkAccess);
    }

    @Override
    public Iterable<WorldChunk> loadedChunks() {
        final ChunkSource chunkProvider = ((LevelAccessor) this).getChunkSource();
        if (chunkProvider instanceof ServerChunkCache) {
            final ChunkMapAccessor chunkManager = (ChunkMapAccessor) ((ServerChunkCache) chunkProvider).chunkMap;
            final List<WorldChunk> chunks = new ArrayList<>();
            chunkManager.invoker$getChunks().forEach(holder -> {
                final WorldChunk chunk = (WorldChunk) holder.getTickingChunk();
                if (chunk != null) {
                    chunks.add(chunk);
                }
            });
            return chunks;
        }
        return Collections.emptyList();
    }

    @Override
    public <T> org.spongepowered.api.registry.Registry<T> registry(final RegistryType<T> type) {
        return this.api$registryHolder().registry(Objects.requireNonNull(type, "type"));
    }

    @Override
    public <T> Optional<org.spongepowered.api.registry.Registry<T>> findRegistry(final RegistryType<T> type) {
        return this.api$registryHolder().findRegistry(Objects.requireNonNull(type, "type"));
    }

    @Override
    public Stream<org.spongepowered.api.registry.Registry<?>> streamRegistries(final org.spongepowered.api.ResourceKey root) {
        return this.api$registryHolder().streamRegistries(Objects.requireNonNull(root, "root"));
    }

    @Override
    public void setRootMinecraftRegistry(final net.minecraft.core.Registry<net.minecraft.core.Registry<?>> registry) {
        this.api$registryHolder().setRootMinecraftRegistry(registry);
    }

    @Override
    public <T> org.spongepowered.api.registry.Registry<T> createRegistry(final RegistryType<T> type, @Nullable final InitialRegistryData<T> defaultValues, final boolean isDynamic,
        @Nullable final BiConsumer<ResourceKey<T>, T> callback) {
        return this.api$registryHolder().createRegistry(type, defaultValues, isDynamic, callback);
    }

    @Override
    public <T> void wrapTagHelperAsRegistry(final RegistryType<Tag<T>> type, final StaticTagHelper<T> helper) {
        this.api$registryHolder().wrapTagHelperAsRegistry(type, helper);
    }

    private RegistryHolderLogic api$registryHolder() {
        if (this.api$registryHolder == null) {
            this.api$registryHolder = new RegistryHolderLogic(((LevelAccessor) this).registryAccess());
        }

        return this.api$registryHolder;
    }

    // BlockVolume

    @Override
    public int highestYAt(final int x, final int z) {
        return this.height(HeightTypes.WORLD_SURFACE.get(), x, z);
    }

    // Volume

    @Override
    public Vector3i min() {
        return Constants.World.BLOCK_MIN;
    }

    @Override
    public Vector3i max() {
        return Constants.World.BLOCK_MAX;
    }

    @Override
    public Vector3i size() {
        return Constants.World.BLOCK_SIZE;
    }

    // ContextSource
    
    @Override
    public Context context() {
        if (this.api$context == null) {
            this.api$context = new Context(Context.WORLD_KEY, this.shadow$dimension().location().toString());
        }
        return this.api$context;
    }

    // Viewer

    @Override
    public void spawnParticles(final ParticleEffect particleEffect, final Vector3d position, final int radius) {
        Objects.requireNonNull(particleEffect, "particleEffect");
        Objects.requireNonNull(position, "position");
        if (radius <= 0) {
            throw new IllegalArgumentException("The radius has to be greater then zero!");
        }

        SpongeParticleHelper.sendPackets(particleEffect, position, radius, this.shadow$dimension(), this.shadow$getServer().getPlayerList());
    }

    @Override
    public void playMusicDisc(final Vector3i position, final MusicDisc musicDisc) {
        this.api$playRecord(Objects.requireNonNull(position, "position"), Objects.requireNonNull(musicDisc, "musicDisc"));
    }

    @Override
    public void stopMusicDisc(final Vector3i position) {
        this.api$playRecord(Objects.requireNonNull(position, "position"), null);
    }

    @Override
    public void sendBlockChange(final int x, final int y, final int z, final org.spongepowered.api.block.BlockState state) {
        Objects.requireNonNull(state, "state");

        final ClientboundBlockUpdatePacket packet = new ClientboundBlockUpdatePacket(new BlockPos(x, y, z), (BlockState) state);

        ((net.minecraft.world.level.Level) (Object) this).players()
                .stream()
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .forEach(p -> p.connection.send(packet));
    }

    @Override
    public void resetBlockChange(final int x, final int y, final int z) {
        final ClientboundBlockUpdatePacket packet = new ClientboundBlockUpdatePacket((LevelReader) this, new BlockPos(x, y, z));

        ((net.minecraft.world.level.Level) (Object) this).players().stream()
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .forEach(p -> p.connection.send(packet));
    }

    // ArchetypeVolumeCreator

    // Audience

    @Override
    public void playSound(final Sound sound, final double x, final double y, final double z) {
        // Check if the event is registered (ie has an integer ID)
        final ResourceLocation soundKey = SpongeAdventure.asVanilla(sound.name());
        final Optional<SoundEvent> event = Registry.SOUND_EVENT.getOptional(soundKey);
        final SoundSource soundCategory = SpongeAdventure.asVanilla(sound.source());
        if (event.isPresent()) {
            this.shadow$playSound(null,x, y, z, event.get(), soundCategory, sound.volume(), sound.pitch());
        } else {
            // Otherwise send it as a custom sound
            final float volume = sound.volume();
            final double radius = volume > 1.0f ? (16.0f * volume) : 16.0d;
            final ClientboundCustomSoundPacket packet = new ClientboundCustomSoundPacket(soundKey, soundCategory, new net.minecraft.world.phys.Vec3(x, y, z), volume, sound.pitch());
            this.shadow$getServer().getPlayerList().broadcast(null, x, y, z, radius, this.shadow$dimension(), packet);
        }
    }

    @Override
    public Collection<? extends BlockEntity> blockEntities() {
        return (Collection) Collections.unmodifiableCollection(this.blockEntityList);
    }

    @Override
    public void addBlockEntity(final int x, final int y, final int z, final BlockEntity blockEntity) {
        this.shadow$setBlockEntity(new BlockPos(x, y, z), (net.minecraft.world.level.block.entity.BlockEntity) Objects.requireNonNull(blockEntity, "blockEntity"));
    }

    // MutableEntityVolume

    @Override
    public <E extends org.spongepowered.api.entity.Entity> E createEntity(final EntityType<E> type, final Vector3d position) throws IllegalArgumentException, IllegalStateException {
        return ((LevelBridge) this).bridge$createEntity(Objects.requireNonNull(type, "type"), Objects.requireNonNull(position, "position"), false);
    }

    @Override
    public <E extends org.spongepowered.api.entity.Entity> E createEntityNaturally(final EntityType<E> type, final Vector3d position) throws IllegalArgumentException, IllegalStateException {
        return ((LevelBridge) this).bridge$createEntity(Objects.requireNonNull(type, "type"), Objects.requireNonNull(position, "position"), true);
    }

    @Override
    public Optional<Entity> createEntity(final DataContainer container) {
        return ((LevelBridge) this).bridge$createEntity(container, null, null);
    }

    @Override
    public Optional<Entity> createEntity(final DataContainer container, final Vector3d position) {
        return Optional.ofNullable(((LevelBridge) this).bridge$createEntity(container, position, null));
    }

    @Override
    public ArchetypeVolume createArchetypeVolume(final Vector3i min, final Vector3i max, final Vector3i origin) {
        final Vector3i rawVolMin = Objects.requireNonNull(min, "min").min(Objects.requireNonNull(max, "max"));
        final Vector3i volMax = max.max(min);
        final Vector3i size = volMax.sub(rawVolMin).add(1, 1, 1);
        final Vector3i relativeMin = rawVolMin.sub(Objects.requireNonNull(origin, "origin"));
        final SpongeArchetypeVolume volume = new SpongeArchetypeVolume(relativeMin, size, this);

        this.blockStateStream(min, max, StreamOptions.lazily())
            .apply(VolumeCollectors.of(
                volume,
                VolumePositionTranslators.offset(origin),
                VolumeApplicators.applyBlocks()
            ));

        this.blockEntityStream(min, max, StreamOptions.lazily())
            .map((world, blockEntity, x, y, z) -> blockEntity.get().createArchetype())
            .apply(VolumeCollectors.of(
                volume,
                VolumePositionTranslators.offset(origin),
                VolumeApplicators.applyBlockEntityArchetypes()
            ));

        this.biomeStream(min, max, StreamOptions.lazily())
            .apply(VolumeCollectors.of(
                volume,
                VolumePositionTranslators.offset(origin),
                VolumeApplicators.applyBiomes()
            ));

        this.entityStream(min, max, StreamOptions.lazily())
            .filter((world, entity, x, y, z) -> ((EntityAccessor) entity.get()).invoker$getEncodeId() != null || entity.get().type() == HumanEntity.TYPE)
            .map((world, entity, x, y, z) -> entity.get().createArchetype())
            .apply(VolumeCollectors.of(
                volume,
                VolumePositionTranslators.offset(origin),
                VolumeApplicators.applyEntityArchetypes()
            ));
        return volume;
    }

    private void api$playRecord(final Vector3i position, @javax.annotation.Nullable final MusicDisc recordType) {
        this.shadow$getServer().getPlayerList().broadcastAll(SpongeMusicDisc.createPacket(position, recordType), this.shadow$dimension());
    }

    // EntityVolume

    @Override
    public Optional<Entity> entity(final UUID uuid) {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of Level that isn't part of Sponge API");
    }

    @Override
    public Collection<? extends Player> players() {
        throw new UnsupportedOperationException("Unfortunately, you've found an extended class of Level that isn't part of Sponge API");
    }

    @SuppressWarnings("unchecked")
    @Override
    public VolumeStream<W, Entity> entityStream(final Vector3i min, final Vector3i max, final StreamOptions options) {
        VolumeStreamUtils.validateStreamArgs(Objects.requireNonNull(min, "min"), Objects.requireNonNull(max, "max"),
            Objects.requireNonNull(options, "options"));

        final boolean shouldCarbonCopy = options.carbonCopy();
        final Vector3i size = max.sub(min).add(1, 1 ,1);
        final @MonotonicNonNull ObjectArrayMutableEntityBuffer backingVolume;
        if (shouldCarbonCopy) {
            backingVolume = new ObjectArrayMutableEntityBuffer(min, size);
        } else {
            backingVolume = null;
        }
        return VolumeStreamUtils.<W, Entity, net.minecraft.world.entity.Entity, ChunkAccess, UUID>generateStream(
            min,
            max,
            options,
            // Ref
            (W) this,
            // IdentityFunction
            VolumeStreamUtils.getOrCloneEntityWithVolume(shouldCarbonCopy, backingVolume, (Level) (Object) this),
            // ChunkAccessor
            VolumeStreamUtils.getChunkAccessorByStatus((LevelReader) (Object) this, options.loadingStyle().generateArea()),
            // Entity -> UniqueID
            (key, entity) -> entity.getUUID(),
            // Entity Accessor
            (chunk) -> chunk instanceof LevelChunk
                ? VolumeStreamUtils.getEntitiesFromChunk(min, max, (LevelChunk) chunk)
                : Stream.empty()
            ,
            // Filtered Position Entity Accessor
            (entityUuid, world) -> {
                final net.minecraft.world.entity.@Nullable Entity entity = shouldCarbonCopy
                    ? (net.minecraft.world.entity.Entity) backingVolume.entity(entityUuid).orElse(null)
                    : (net.minecraft.world.entity.Entity) ((WorldLike) world).entity(entityUuid).orElse(null);
                if (entity == null) {
                    return null;
                }
                return new Tuple<>(entity.blockPosition(), entity);
            }
        );
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean setBiome(final int x, final int y, final int z, final Biome biome) {
        if (!((Level) (Object) this).hasChunk(x << 4, z << 4)) {
            return false;
        }
        final LevelChunk levelChunk = this.shadow$getChunkAt(new BlockPos(x, y, z));
        // technically we don't like to forward to the api, but this
        // is implemented by LevelChunkMixin_API
        return ((BiomeVolume.Modifiable) levelChunk).setBiome(x, y, z, biome);
    }

    @Override
    public Collection<Entity> spawnEntities(final Iterable<? extends Entity> entities) {
        final List<org.spongepowered.api.entity.Entity> entityList = new ArrayList<>();
        for (final org.spongepowered.api.entity.Entity entity : entities) {
            if (this.spawnEntity(entity)) {
                entityList.add(entity);
            }
        }
        return entityList;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T extends Entity> Collection<? extends T> entities(final Class<? extends T> entityClass, final AABB box, @Nullable final Predicate<? super T> filter) {
        return (List) this.shadow$getEntitiesOfClass((Class) entityClass, VecHelper.toMinecraftAABB(box), (Predicate) filter);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Collection<? extends Entity> entities(final AABB box, final Predicate<? super Entity> filter) {
        return (List) this.shadow$getEntities(null, VecHelper.toMinecraftAABB(box), (Predicate) filter);
    }

}
