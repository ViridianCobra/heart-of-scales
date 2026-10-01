package net.basilisk.heartofscales.roster;

import io.netty.buffer.Unpooled;
import net.basilisk.heartofscales.entity.DragonCommand;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
    public int encodedSize() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            write(buf);
            return buf.readableBytes();
        } finally {
            buf.release();
        }
    }

    public static void writeList(FriendlyByteBuf buf, List<BeaconRow> rows) {
        buf.writeCollection(rows, (out, row) -> row.write(out));
    }

    public static List<BeaconRow> readList(FriendlyByteBuf buf) {
        return buf.readList(BeaconRow::read);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUUID(dragon);
        buf.writeComponent(name);
        buf.writeComponent(owner);
        buf.writeBoolean(yours);
        buf.writeLong(tamedAt);
        buf.writeEnum(status);
        buf.writeGlobalPos(pos);
        buf.writeEnum(command);
        buf.writeNullable(deathMessage, FriendlyByteBuf::writeComponent);
        buf.writeLong(endedAt);
    }

    private static BeaconRow read(FriendlyByteBuf buf) {
        return new BeaconRow(buf.readUUID(), buf.readComponent(), buf.readComponent(), buf.readBoolean(), buf.readLong(),
                buf.readEnum(RowStatus.class), buf.readGlobalPos(), buf.readEnum(DragonCommand.class),
                buf.readNullable(FriendlyByteBuf::readComponent), buf.readLong());
    }
}
