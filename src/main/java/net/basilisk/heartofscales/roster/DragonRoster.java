package net.basilisk.heartofscales.roster;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Every tamed dragon on the server by UUID. Kept on the overworld so there is one roster whatever dimension a dragon is in. */
public class DragonRoster extends SavedData {
    private static final String NAME = "heart_of_scales_dragon_roster";
    private static final String TAG_ENTRIES = "Entries";

    private final Map<UUID, RosterEntry> entries = new HashMap<>();

    public static DragonRoster get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DragonRoster::load, DragonRoster::new, NAME);
    }

    public void update(DragonEntity dragon) {
        put(RosterEntry.alive(dragon));
    }

    public void markDied(DragonEntity dragon, Component deathMessage) {
        put(RosterEntry.died(dragon, deathMessage, System.currentTimeMillis()));
    }

    public void markRemoved(DragonEntity dragon) {
        put(RosterEntry.removed(dragon, System.currentTimeMillis()));
    }

    public List<RosterEntry> entriesAt(GlobalPos beacon) {
        return entries.values().stream().filter(entry -> beacon.equals(entry.home())).toList();
    }

    /** Forgets a dead or removed dragon. Only its owner can, and a living dragon is never forgotten. */
    public void dismiss(UUID dragon, UUID owner) {
        RosterEntry entry = entries.get(dragon);
        if (entry == null || !entry.owner().equals(owner) || entry.state() == RosterState.ALIVE) return;
        entries.remove(dragon);
        setDirty();
    }

    private void put(RosterEntry entry) {
        if (!entry.equals(entries.put(entry.dragon(), entry))) setDirty();
    }

    public static DragonRoster load(CompoundTag tag) {
        DragonRoster roster = new DragonRoster();
        for (Tag saved : tag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND)) {
            RosterEntry entry = RosterEntry.load((CompoundTag) saved);
            roster.entries.put(entry.dragon(), entry);
        }
        return roster;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (RosterEntry entry : entries.values()) {
            list.add(entry.save());
        }
        tag.put(TAG_ENTRIES, list);
        return tag;
    }
}
