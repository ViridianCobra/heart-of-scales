package net.basilisk.heartofscales.block.entity;

import net.basilisk.heartofscales.block.NestBlock;
import net.basilisk.heartofscales.config.HeartOfScalesConfig;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.genome.DragonGenome;
import net.basilisk.heartofscales.nbt.GenomeNbt;
import net.basilisk.heartofscales.registry.ModBlockEntities;
import net.basilisk.heartofscales.registry.ModEntities;
import net.basilisk.heartofscales.species.DragonSpecies;
import net.basilisk.heartofscales.species.HatchCondition;
import net.basilisk.heartofscales.species.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class NestBlockEntity extends BlockEntity {
    private static final String TAG_EGG = "Egg";
    private static final String TAG_HATCHING = "Hatching";
    private static final String TAG_HATCH_TICKS = "HatchTicks";
    private static final String TAG_HATCHER = "Hatcher";
    private static final int CONDITION_CHECK_INTERVAL = 20;
    private static final int PARTICLE_INTERVAL = 40;

    @Nullable
    private DragonGenome egg;
    private boolean hatching;
    private int hatchTicks;
    @Nullable
    private UUID hatcher;

    private boolean conditionMet;
    private int conditionCheckCooldown;

    public NestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEST.get(), pos, state);
    }

    @Nullable
    public DragonGenome getEgg() {
        return egg;
    }

    /** Replacing or removing the egg also abandons any hatching in progress. */
    public void setEgg(@Nullable DragonGenome egg) {
        this.egg = egg;
        stopHatching();
    }

    public boolean isHatching() {
        return hatching;
    }

    /** The hatch condition of the egg's subspecies, if there is an egg and its subspecies is known. */
    public Optional<HatchCondition> hatchCondition() {
        if (egg == null || level == null) return Optional.empty();
        return ModRegistries.species(level.registryAccess(), egg).map(DragonSpecies::hatchCondition);
    }

    public void startHatching(Player player) {
        hatching = true;
        hatchTicks = 0;
        hatcher = player.getUUID();
        conditionCheckCooldown = 0;
        setChanged();
    }

    private void stopHatching() {
        hatching = false;
        hatchTicks = 0;
        hatcher = null;
        setChanged();
    }

    public void serverTick() {
        if (!hatching || !(level instanceof ServerLevel serverLevel)) return;
        if (egg == null) {
            stopHatching();
            return;
        }

        if (--conditionCheckCooldown <= 0) {
            conditionCheckCooldown = CONDITION_CHECK_INTERVAL;
            conditionMet = hatchCondition().map(condition -> condition.test(serverLevel, worldPosition)).orElse(false);
        }
        if (conditionMet) {
            hatchTicks++;
            setChanged();
        }
        if (serverLevel.getGameTime() % PARTICLE_INTERVAL == 0) {
            ParticleOptions particle = conditionMet ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SMOKE;
            spawnParticles(serverLevel, particle, 4);
        }
        if (hatchTicks >= HeartOfScalesConfig.HATCH_TIME_TICKS.get()) {
            hatch(serverLevel);
        }
    }

    private void hatch(ServerLevel serverLevel) {
        DragonEntity baby = ModEntities.DRAGON.get().create(serverLevel);
        if (baby == null) return;
        baby.setGenome(egg);
        baby.setHealth(baby.getMaxHealth());
        baby.setBaby(true);
        baby.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5,
                serverLevel.random.nextFloat() * 360.0f, 0.0f);
        if (hatcher != null) {
            baby.setTame(true);
            baby.setOwnerUUID(hatcher);
        }
        serverLevel.addFreshEntity(baby);

        serverLevel.playSound(null, worldPosition, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1.0f, 0.8f);
        spawnParticles(serverLevel, ParticleTypes.HAPPY_VILLAGER, 12);

        setEgg(null);
        serverLevel.setBlock(worldPosition, getBlockState().setValue(NestBlock.HAS_EGG, false), Block.UPDATE_ALL);
    }

    private void spawnParticles(ServerLevel serverLevel, ParticleOptions particle, int count) {
        serverLevel.sendParticles(particle, worldPosition.getX() + 0.5, worldPosition.getY() + 0.7, worldPosition.getZ() + 0.5,
                count, 0.25, 0.3, 0.25, 0.0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (egg != null) {
            tag.put(TAG_EGG, GenomeNbt.save(egg, new CompoundTag()));
        }
        if (hatching) {
            tag.putBoolean(TAG_HATCHING, true);
            tag.putInt(TAG_HATCH_TICKS, hatchTicks);
            if (hatcher != null) {
                tag.putUUID(TAG_HATCHER, hatcher);
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        egg = tag.contains(TAG_EGG) ? GenomeNbt.load(tag.getCompound(TAG_EGG)) : null;
        hatching = tag.getBoolean(TAG_HATCHING);
        hatchTicks = tag.getInt(TAG_HATCH_TICKS);
        hatcher = tag.hasUUID(TAG_HATCHER) ? tag.getUUID(TAG_HATCHER) : null;
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // An empty nest saves an empty tag, which the packet sends as null and the default handler
    // then ignores, so the client would keep showing the old egg
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        load(tag != null ? tag : new CompoundTag());
    }
}
