package net.basilisk.heartofscales.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "2";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(RiderInputPacket.TYPE, RiderInputPacket.STREAM_CODEC, RiderInputPacket::handle);
    }

    private ModNetwork() {}
}
