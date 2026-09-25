package net.basilisk.heartofscales.client;

import net.basilisk.heartofscales.HeartOfScales;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;

/** Pulls the third-person camera back while the player is flying or swimming a dragon. */
@EventBusSubscriber(modid = HeartOfScales.MOD_ID, value = Dist.CLIENT)
public final class FlightCameraHandler {
    @SubscribeEvent
    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        event.setDistance((float) (event.getDistance() * FlightCamera.distanceMultiplier()));
    }

    private FlightCameraHandler() {}
}
