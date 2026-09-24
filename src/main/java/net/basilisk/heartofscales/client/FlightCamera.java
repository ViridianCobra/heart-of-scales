package net.basilisk.heartofscales.client;

import net.basilisk.heartofscales.config.HeartOfScalesClientConfig;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Third-person camera distance while flying or swimming a dragon, read by CameraMixin. Kept here so the mixin stays a one-liner. */
public final class FlightCamera {
    public static double distanceMultiplier() {
        if (!HeartOfScalesClientConfig.FLIGHT_CAMERA_ZOOM.get()) return 1.0;
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getVehicle() instanceof DragonEntity dragon && dragon.isInFluidMode()
                ? HeartOfScalesClientConfig.FLIGHT_CAMERA_DISTANCE_MULTIPLIER.get() : 1.0;
    }

    private FlightCamera() {}
}
