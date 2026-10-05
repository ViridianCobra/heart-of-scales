package net.basilisk.heartofscales.roster;

import io.netty.buffer.Unpooled;
import net.basilisk.heartofscales.entity.DragonCommand;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** One line of a beacon's list, built on the server for one viewer and sent to their client. */
public record BeaconRow(UUID dragon, Component name, Component owner, boolean yours, long tamedAt, RowStatus status,
                        GlobalPos pos, DragonCommand command, @Nullable Component deathMessage, long endedAt) {
    public RosterSortKey sortKey() {
        return new RosterSortKey(yours, tamedAt, dragon);
    }

    /** Only the owner can clear away a dragon that has died or been removed. */
    public boolean dismissable() {
        return yours && status.hasEnded();
    }

    /** Bytes this row takes in the screen-open packet. */
    public int encodedSize(RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            write(buf);
            return buf.readableBytes();
        } finally {
            buf.release();
        }
    }

    public static void writeList(RegistryFriendlyByteBuf buf, List<BeaconRow> rows) {
        buf.writeVarInt(rows.size());
        for (BeaconRow row : rows) row.write(buf);
    }

    public static List<BeaconRow> readList(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BeaconRow> rows = new ArrayList<>(size);
        for (int i = 0; i < size; i++) rows.add(read(buf));
        return rows;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(dragon);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, name);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, owner);
        buf.writeBoolean(yours);
        buf.writeLong(tamedAt);
        buf.writeEnum(status);
        buf.writeGlobalPos(pos);
        buf.writeEnum(command);
        ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC.encode(buf, Optional.ofNullable(deathMessage));
        buf.writeLong(endedAt);
    }

    private static BeaconRow read(RegistryFriendlyByteBuf buf) {
        return new BeaconRow(buf.readUUID(), ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf),
                ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readLong(),
                buf.readEnum(RowStatus.class), buf.readGlobalPos(), buf.readEnum(DragonCommand.class),
                ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC.decode(buf).orElse(null), buf.readLong());
    }
}
