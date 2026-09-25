package net.basilisk.heartofscales.client;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.entity.FlightMode;
import net.basilisk.heartofscales.network.RiderInputPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Reads the rider's keys at the end of each client tick. Ascend and descend are applied to the local dragon straight away,
 * because the rider's client simulates the dragon's movement, and sent to the server only when they change.
 */
@EventBusSubscriber(modid = HeartOfScales.MOD_ID, value = Dist.CLIENT)
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
    private static boolean lastSprinting;
    /** Free cam is toggled by its key; it only means anything in the air, so landing and dismounting clear it. */
    private static boolean freeCamOn;
    /** Camera the player had before riding forced third person; null when nothing was forced. */
    @Nullable
    private static CameraType cameraBeforeRiding;

    @SubscribeEvent
    public static void onClientTickStart(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        suppressSwapHands(minecraft, minecraft.player);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof DragonEntity dragon) || dragon.getControllingPassenger() != player) {
            lastAscending = false;
            lastDescending = false;
            lastFreeCam = false;
            lastSprinting = false;
            freeCamOn = false;
            // A press made off the dragon must not flip free cam on the next mount
            while (ModKeyMappings.FREE_CAM.consumeClick()) {}
            restoreCamera(minecraft);
            return;
        }
        forceThirdPerson(minecraft);

        boolean ascending = ModKeyMappings.ASCEND.isDown();
        boolean descending = ModKeyMappings.DESCEND.isDown();
        boolean freeCamClicked = false;
        while (ModKeyMappings.FREE_CAM.consumeClick()) freeCamClicked = !freeCamClicked;
        if (!dragon.isInFluidMode()) {
            freeCamOn = false;
        } else if (freeCamClicked) {
            freeCamOn = !freeCamOn;
            player.displayClientMessage(Component.translatable("free_cam." + HeartOfScales.MOD_ID + (freeCamOn ? ".on" : ".off")), true);
        }
        boolean freeCam = freeCamOn;
        boolean toggleMode = ModKeyMappings.FLIGHT_MODE.consumeClick();
        dragon.setRiderAscending(ascending);
        dragon.setRiderDescending(descending);
        // Vanilla's own Sprint key, so the hold-or-toggle accessibility option applies to dragons too
        boolean sprinting = minecraft.options.keySprint.isDown();
        dragon.setRiderFreeCam(freeCam);
        dragon.setRiderSprinting(sprinting);
        if (ascending != lastAscending || descending != lastDescending || freeCam != lastFreeCam
                || sprinting != lastSprinting || toggleMode) {
            PacketDistributor.sendToServer(new RiderInputPacket(ascending, descending, freeCam, sprinting, toggleMode));
            lastSprinting = sprinting;
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
    public static void onRenderTick(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.isPaused() || minecraft.screen != null) return;
        if (!(player.getVehicle() instanceof DragonEntity dragon) || dragon.getControllingPassenger() != player) return;
        if (!dragon.isFlying() || dragon.getFlightMode() != FlightMode.GLIDE) return;
        // In free cam the keys and the stall assist turn the body itself, and the look belongs to the mouse alone
        if (dragon.isRiderFreeCam()) return;

        float ticks = event.getPartialTick().getGameTimeDeltaTicks();
        double pitch = Math.signum(player.input.forwardImpulse) * GLIDE_KEY_PITCH_RATE * ticks;
        if (dragon.isStallAssistActive() && player.getXRot() < STALL_ASSIST_PITCH) {
            pitch += Math.min(STALL_ASSIST_RATE * ticks, STALL_ASSIST_PITCH - player.getXRot());
        }
        if (pitch != 0) player.turn(0, pitch / TURN_SCALE);
    }

    /**
     * Vanilla handles Swap Hands between the Pre and Post client tick events, so draining its clicks at
     * Pre stops the swap. Only done while the two mappings share a key, so a rebind gives Swap Hands back.
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
