package net.basilisk.heartofscales.roster;

import net.basilisk.heartofscales.entity.DragonCommand;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** The roster's last-seen copy of one tamed dragon. Written by the dragon, read by beacons. */
public record RosterEntry(UUID dragon, UUID owner, @Nullable Component customName, String subspecies, long tamedAt,
                          @Nullable GlobalPos home, GlobalPos lastPos, DragonCommand command, RosterState state,
                          @Nullable Component deathMessage, long endedAt, @Nullable CompoundTag snapshot) {
    private static final String TAG_DRAGON = "Dragon";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_CUSTOM_NAME = "CustomName";
    private static final String TAG_SUBSPECIES = "Subspecies";
    private static final String TAG_TAMED_AT = "TamedAt";
    private static final String TAG_HOME = "Home";
    private static final String TAG_LAST_POS = "LastPos";
    private static final String TAG_DIMENSION = "Dimension";
    // The keys 1.20.1's NbtUtils.writeBlockPos used, so a roster saved on 1.20.1 still loads
    private static final String TAG_X = "X";
    private static final String TAG_Y = "Y";
    private static final String TAG_Z = "Z";
    private static final String TAG_COMMAND = "Command";
    private static final String TAG_STATE = "State";
    private static final String TAG_DEATH_MESSAGE = "DeathMessage";
    private static final String TAG_ENDED_AT = "EndedAt";
    private static final String TAG_SNAPSHOT = "Snapshot";

    public static RosterEntry alive(DragonEntity dragon) {
        return of(dragon, RosterState.ALIVE, null, RosterTime.UNKNOWN, null);
    }

    /** Keeps the dragon's full save data so it can be brought back later. */
    public static RosterEntry died(DragonEntity dragon, Component deathMessage, long now) {
        return of(dragon, RosterState.DIED, deathMessage, now, dragon.saveWithoutId(new CompoundTag()));
    }

    public static RosterEntry removed(DragonEntity dragon, long now) {
        return of(dragon, RosterState.REMOVED, null, now, null);
    }

    private static RosterEntry of(DragonEntity dragon, RosterState state, @Nullable Component deathMessage, long endedAt,
                                  @Nullable CompoundTag snapshot) {
        return new RosterEntry(dragon.getUUID(), dragon.getOwnerUUID(), dragon.getCustomName(), dragon.getSubspecies(),
                dragon.getTamedAt(), dragon.getHome(), GlobalPos.of(dragon.level().dimension(), dragon.blockPosition()),
                dragon.getCommand(), state, deathMessage, endedAt, snapshot);
    }

    /** The custom name, or the subspecies name for an unnamed dragon. */
    public Component displayName() {
        return customName != null ? customName : Component.translatable("subspecies." + subspecies.replace(':', '.'));
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TAG_DRAGON, dragon);
        tag.putUUID(TAG_OWNER, owner);
        if (customName != null) tag.putString(TAG_CUSTOM_NAME, Component.Serializer.toJson(customName, registries));
        tag.putString(TAG_SUBSPECIES, subspecies);
        tag.putLong(TAG_TAMED_AT, tamedAt);
        if (home != null) tag.put(TAG_HOME, writePos(home));
        tag.put(TAG_LAST_POS, writePos(lastPos));
        tag.putString(TAG_COMMAND, command.id());
        tag.putString(TAG_STATE, state.name());
        if (deathMessage != null) tag.putString(TAG_DEATH_MESSAGE, Component.Serializer.toJson(deathMessage, registries));
        tag.putLong(TAG_ENDED_AT, endedAt);
        if (snapshot != null) tag.put(TAG_SNAPSHOT, snapshot);
        return tag;
    }

    public static RosterEntry load(CompoundTag tag, HolderLookup.Provider registries) {
        return new RosterEntry(
                tag.getUUID(TAG_DRAGON),
                tag.getUUID(TAG_OWNER),
                tag.contains(TAG_CUSTOM_NAME, Tag.TAG_STRING) ? Component.Serializer.fromJson(tag.getString(TAG_CUSTOM_NAME), registries) : null,
                tag.getString(TAG_SUBSPECIES),
                tag.getLong(TAG_TAMED_AT),
                tag.contains(TAG_HOME, Tag.TAG_COMPOUND) ? readPos(tag.getCompound(TAG_HOME)) : null,
                readPos(tag.getCompound(TAG_LAST_POS)),
                DragonCommand.byId(tag.getString(TAG_COMMAND)),
                RosterState.byName(tag.getString(TAG_STATE)),
                tag.contains(TAG_DEATH_MESSAGE, Tag.TAG_STRING) ? Component.Serializer.fromJson(tag.getString(TAG_DEATH_MESSAGE), registries) : null,
                tag.getLong(TAG_ENDED_AT),
                tag.contains(TAG_SNAPSHOT, Tag.TAG_COMPOUND) ? tag.getCompound(TAG_SNAPSHOT) : null);
    }

    private static CompoundTag writePos(GlobalPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_X, pos.pos().getX());
        tag.putInt(TAG_Y, pos.pos().getY());
        tag.putInt(TAG_Z, pos.pos().getZ());
        tag.putString(TAG_DIMENSION, pos.dimension().location().toString());
        return tag;
    }

    private static GlobalPos readPos(CompoundTag tag) {
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(TAG_DIMENSION));
        ResourceKey<Level> key = dimension != null ? ResourceKey.create(Registries.DIMENSION, dimension) : Level.OVERWORLD;
        return GlobalPos.of(key, new BlockPos(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z)));
    }
}
