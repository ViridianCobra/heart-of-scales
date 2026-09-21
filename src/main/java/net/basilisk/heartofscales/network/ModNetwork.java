package net.basilisk.heartofscales.network;

import net.basilisk.heartofscales.HeartOfScales;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(HeartOfScales.MOD_ID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private static int nextId = 0;

    public static void register() {
        CHANNEL.messageBuilder(RiderInputPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RiderInputPacket::encode)
                .decoder(RiderInputPacket::decode)
                .consumerMainThread(RiderInputPacket::handle)
                .add();
    }

    private ModNetwork() {}
}
