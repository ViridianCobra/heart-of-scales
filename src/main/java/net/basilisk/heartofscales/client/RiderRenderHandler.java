package net.basilisk.heartofscales.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tilts a rider's model with the flying or swimming dragon's pitch and roll, rotating in the dragon's own frame. */
@Mod.EventBusSubscriber(modid = HeartOfScales.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RiderRenderHandler {
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity().getVehicle() instanceof DragonEntity dragon) || !dragon.isInFluidMode()) return;
        float partialTick = event.getPartialTick();
        float yaw = Mth.rotLerp(partialTick, dragon.yRotO, dragon.getYRot());
        float pitch = Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot()) + dragon.getTiltPitch(partialTick);
        float roll = dragon.getRoll(partialTick);

        PoseStack poseStack = event.getPoseStack();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-roll));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 180.0f));
    }

    private RiderRenderHandler() {}
}
