package net.basilisk.heartofscales.network;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Rider -> server. Vanilla already syncs the rider's look and WASD; this carries the inputs it does not:
 * whether ascend, descend and sprint are held, whether free cam is on, and a request to toggle the flight mode.
 */
public record RiderInputPacket(boolean ascending, boolean descending, boolean freeCam, boolean sprinting, boolean toggleMode)
        implements CustomPacketPayload {
    public static final Type<RiderInputPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HeartOfScales.MOD_ID, "rider_input"));

    public static final StreamCodec<FriendlyByteBuf, RiderInputPacket> STREAM_CODEC =
            StreamCodec.of(RiderInputPacket::encode, RiderInputPacket::decode);

    private static void encode(FriendlyByteBuf buf, RiderInputPacket packet) {
        buf.writeBoolean(packet.ascending);
        buf.writeBoolean(packet.descending);
        buf.writeBoolean(packet.freeCam);
        buf.writeBoolean(packet.sprinting);
        buf.writeBoolean(packet.toggleMode);
    }

    private static RiderInputPacket decode(FriendlyByteBuf buf) {
        return new RiderInputPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RiderInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
            // Only the player actually driving the dragon may steer it
            if (!(sender.getVehicle() instanceof DragonEntity dragon) || dragon.getControllingPassenger() != sender) return;
            dragon.setRiderAscending(packet.ascending);
            dragon.setRiderDescending(packet.descending);
            dragon.setRiderFreeCam(packet.freeCam);
            dragon.setRiderSprinting(packet.sprinting);
            if (packet.toggleMode) dragon.toggleFlightMode(sender);
        });
    }
}
