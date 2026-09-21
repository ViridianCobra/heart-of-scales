package net.basilisk.heartofscales.client;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.entity.FlightMode;
import net.basilisk.heartofscales.network.ModNetwork;
import net.basilisk.heartofscales.network.RiderInputPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Reads the rider's keys at the end of each client tick. Ascend and descend are applied to the local dragon straight away,
 * because the rider's client simulates the dragon's movement, and sent to the server only when they change.
 */
@Mod.EventBusSubscriber(modid = HeartOfScales.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RiderInputHandler {
    /** Glide W and S pitch, degrees per tick. Kept below the body's chase rate so it keeps up with a held key. */
    private static final float GLIDE_KEY_PITCH_RATE = 2.5f;
    // Stall assist: a rider who has not set up a dive has their look eased down to this pitch. The look is moved
    // rather than the body, because the body chases the look and would climb straight back into the stall.
    private static final float STALL_ASSIST_PITCH = 30.0f;
    private static final float STALL_ASSIST_RATE = 4.0f;
    /** Player.turn scales its input by this, as it does for the mouse. */
    private static final double TURN_SCALE = 0.15;

    private static boolean lastAscending;
    private static boolean lastDescending;
    private static boolean lastFreeCam;
    /** Camera the player had before riding forced third person; null when nothing was forced. */
    @Nullable
    private static CameraType cameraBeforeRiding;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (event.phase == TickEvent.Phase.START) {
            suppressSwapHands(minecraft, player);
            return;
        }
        if (player == null || !(player.getVehicle() instanceof DragonEntity dragon) || dragon.getControllingPassenger() != player) {
            lastAscending = false;
            lastDescending = false;
            lastFreeCam = false;
            restoreCamera(minecraft);
            return;
        }
        forceThirdPerson(minecraft);

        boolean ascending = ModKeyMappings.ASCEND.isDown();
        boolean descending = ModKeyMappings.DESCEND.isDown();
        boolean freeCam = ModKeyMappings.FREE_CAM.isDown();
        boolean toggleMode = ModKeyMappings.FLIGHT_MODE.consumeClick();
        dragon.setRiderAscending(ascending);
        dragon.setRiderDescending(descending);
        dragon.setRiderFreeCam(freeCam);
        if (ascending != lastAscending || descending != lastDescending || freeCam != lastFreeCam || toggleMode) {
            ModNetwork.CHANNEL.sendToServer(new RiderInputPacket(ascending, descending, freeCam, toggleMode));
            lastAscending = ascending;
            lastDescending = descending;
            lastFreeCam = freeCam;
        }
    }

    /**
     * Glide W and S pitch by turning the rider's look, the same target the mouse moves, so the two add up and the
     * body's chase never undoes the keys. A and D side-slip instead and are handled by the dragon. Done every frame like the mouse: vanilla does not interpolate the local
     * player's view pitch between ticks, so a per-tick change makes the camera step.
     */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.isPaused() || minecraft.screen != null) return;
        if (!(player.getVehicle() instanceof DragonEntity dragon) || dragon.getControllingPassenger() != player) return;
        if (!dragon.isFlying() || dragon.getFlightMode() != FlightMode.GLIDE) return;
        // In free cam the keys and the stall assist turn the body itself, and the look belongs to the mouse alone
        if (dragon.isRiderFreeCam()) return;

        float ticks = minecraft.getDeltaFrameTime();
        double pitch = Math.signum(player.input.forwardImpulse) * GLIDE_KEY_PITCH_RATE * ticks;
        if (dragon.isStallAssistActive() && player.getXRot() < STALL_ASSIST_PITCH) {
            pitch += Math.min(STALL_ASSIST_RATE * ticks, STALL_ASSIST_PITCH - player.getXRot());
        }
        if (pitch != 0) player.turn(0, pitch / TURN_SCALE);
    }

    /**
     * Vanilla handles Swap Hands between the START and END phases of the client tick, so draining its clicks at
     * START stops the swap. Only done while the two mappings share a key, so a rebind gives Swap Hands back.
     */
    private static void suppressSwapHands(Minecraft minecraft, @Nullable LocalPlayer player) {
        if (player == null || !(player.getVehicle() instanceof DragonEntity dragon) || dragon.getControllingPassenger() != player) return;
        KeyMapping swapHands = minecraft.options.keySwapOffhand;
        if (!ModKeyMappings.FLIGHT_MODE.getKey().equals(swapHands.getKey())) return;
        while (swapHands.consumeClick()) {}
    }

    /** Dragons are ridden in third person; the rider's previous camera comes back on dismount. */
    private static void forceThirdPerson(Minecraft minecraft) {
        if (cameraBeforeRiding == null && minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
            cameraBeforeRiding = CameraType.FIRST_PERSON;
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    private static void restoreCamera(Minecraft minecraft) {
        if (cameraBeforeRiding == null) return;
        if (minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK) minecraft.options.setCameraType(cameraBeforeRiding);
        cameraBeforeRiding = null;
    }

    private RiderInputHandler() {}
}
