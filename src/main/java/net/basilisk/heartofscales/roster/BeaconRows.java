package net.basilisk.heartofscales.roster;

import com.mojang.authlib.GameProfile;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Builds a beacon's list for one viewer from the roster, preferring what loaded dragons are doing right now. */
public final class BeaconRows {
    // Forge refuses screen-open data over 32600 bytes, and a beacon that cannot open cannot have its dead rows cleared
    private static final int PAYLOAD_BUDGET = 30000;

    /** Sorted rows, cut short if they would not fit in the screen-open packet. The viewer's own rows sort first, so they are kept. */
    public static List<BeaconRow> build(ServerPlayer viewer, GlobalPos beacon) {
        MinecraftServer server = viewer.server;
        List<BeaconRow> rows = new ArrayList<>();
        for (RosterEntry entry : DragonRoster.get(server).entriesAt(beacon)) {
            rows.add(row(server, viewer, entry));
        }
        rows.sort(Comparator.comparing(BeaconRow::sortKey));
        return RowBudget.prefixWithin(rows, BeaconRow::encodedSize, PAYLOAD_BUDGET);
    }

    private static BeaconRow row(MinecraftServer server, ServerPlayer viewer, RosterEntry entry) {
        boolean yours = entry.owner().equals(viewer.getUUID());
        Component owner = ownerName(server, entry.owner(), yours);
        DragonEntity live = entry.state() == RosterState.ALIVE ? findLoaded(server, entry.dragon()) : null;
        if (live == null) {
            RowStatus status = RowStatus.of(entry.state(), false, false);
            return new BeaconRow(entry.dragon(), entry.displayName(), owner, yours, entry.tamedAt(), status,
                    entry.lastPos(), entry.command(), entry.deathMessage(), entry.endedAt());
        }
        RowStatus status = RowStatus.of(RosterState.ALIVE, true, live.isInsideHomeArea(live.blockPosition()));
        GlobalPos pos = GlobalPos.of(live.level().dimension(), live.blockPosition());
        return new BeaconRow(entry.dragon(), live.getName(), owner, yours, entry.tamedAt(), status, pos,
                live.getCommand(), null, RosterTime.UNKNOWN);
    }

    @Nullable
    private static DragonEntity findLoaded(MinecraftServer server, UUID dragon) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(dragon) instanceof DragonEntity found && found.isAlive()) return found;
        }
        return null;
    }

    private static Component ownerName(MinecraftServer server, UUID owner, boolean yours) {
        if (yours) return Component.translatable(BeaconLang.YOU);
        GameProfileCache profiles = server.getProfileCache();
        Optional<GameProfile> profile = profiles != null ? profiles.get(owner) : Optional.empty();
        return profile.<Component>map(found -> Component.literal(found.getName()))
                .orElseGet(() -> Component.translatable(BeaconLang.UNKNOWN_PLAYER));
    }

    private BeaconRows() {}
}
