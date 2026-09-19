package net.basilisk.heartofscales.client;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.network.ModNetwork;
import net.basilisk.heartofscales.network.RiderInputPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Reads the rider's keys each client tick. Ascend is applied to the local dragon straight away, because the
 * rider's client simulates the dragon's movement, and sent to the server only when it changes.
 */
@Mod.EventBusSubscriber(modid = HeartOfScales.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RiderInputHandler {
    private static boolean lastAscending;
    /** Camera the player had before riding forced third person; null when nothing was forced. */
    @Nullable
    private static CameraType cameraBeforeRiding;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof DragonEntity dragon) || dragon.getControllingPassenger() != player) {
            lastAscending = false;
            restoreCamera(minecraft);
            return;
        }
        forceThirdPerson(minecraft);

        boolean ascending = ModKeyMappings.ASCEND.isDown();
        boolean toggleMode = ModKeyMappings.FLIGHT_MODE.consumeClick();
        dragon.setRiderAscending(ascending);
        if (ascending != lastAscending || toggleMode) {
            ModNetwork.CHANNEL.sendToServer(new RiderInputPacket(ascending, toggleMode));
            lastAscending = ascending;
        }
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
