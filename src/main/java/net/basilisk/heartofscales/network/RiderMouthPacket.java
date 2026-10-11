package net.basilisk.heartofscales.network;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Rider -> server, every tick the breath key is held. Animations only run on clients, so the server cannot see where the
 * head is; this carries where the rider's client drew the mouth, relative to the dragon.
 */
public record RiderMouthPacket(Vec3 offset) {
    public static void encode(RiderMouthPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat((float) packet.offset.x);
        buf.writeFloat((float) packet.offset.y);
        buf.writeFloat((float) packet.offset.z);
    }

    public static RiderMouthPacket decode(FriendlyByteBuf buf) {
        return new RiderMouthPacket(new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()));
    }

    public static void handle(RiderMouthPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer sender = context.get().getSender();
            if (sender == null) return;
            if (!(sender.getVehicle() instanceof DragonEntity dragon) || dragon.getControllingPassenger() != sender) return;
            dragon.getBreath().setRiderMouth(packet.offset);
        });
        context.get().setPacketHandled(true);
    }
}
