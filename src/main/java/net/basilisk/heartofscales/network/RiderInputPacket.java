package net.basilisk.heartofscales.network;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Rider -> server. Vanilla already syncs the rider's look and WASD; this carries the inputs it does not:
 * whether ascend, descend and sprint are held, whether free cam is on, and a request to toggle the flight mode.
 */
public record RiderInputPacket(boolean ascending, boolean descending, boolean freeCam, boolean sprinting, boolean toggleMode) {
    public static void encode(RiderInputPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.ascending);
        buf.writeBoolean(packet.descending);
        buf.writeBoolean(packet.freeCam);
        buf.writeBoolean(packet.sprinting);
        buf.writeBoolean(packet.toggleMode);
    }

    public static RiderInputPacket decode(FriendlyByteBuf buf) {
        return new RiderInputPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(RiderInputPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer sender = context.get().getSender();
            if (sender == null) return;
            // Only the player actually driving the dragon may steer it
            if (!(sender.getVehicle() instanceof DragonEntity dragon) || dragon.getControllingPassenger() != sender) return;
            dragon.setRiderAscending(packet.ascending);
            dragon.setRiderDescending(packet.descending);
            dragon.setRiderFreeCam(packet.freeCam);
            dragon.setRiderSprinting(packet.sprinting);
            if (packet.toggleMode) dragon.toggleFlightMode(sender);
        });
        context.get().setPacketHandled(true);
    }
}
