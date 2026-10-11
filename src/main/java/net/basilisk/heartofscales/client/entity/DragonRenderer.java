package net.basilisk.heartofscales.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.ModRegistries;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DragonRenderer extends GeoEntityRenderer<DragonEntity> {
    public DragonRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonModel());
        this.shadowRadius = 0.8f;
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, DragonEntity dragon,
                                    BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        float scale = dragon.getBodyScale();
        super.scaleModelForRender(widthScale * scale, heightScale * scale, poseStack, dragon, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    /** Pitch the whole body toward the flight or swim direction, turning about the body centre rather than the feet. */
    @Override
    protected void applyRotations(DragonEntity dragon, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        super.applyRotations(dragon, poseStack, ageInTicks, rotationYaw, partialTick);
        if (!dragon.isFlying() && !dragon.isInSwimMode()) return;
        float pitch = Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot()) + dragon.getTiltPitch(partialTick);
        // scaleModelForRender has already scaled the pose, so the pivot is in unscaled model blocks
        float pivot = (float) dragon.getBody().pivotHeight();
        poseStack.translate(0, pivot, 0);
        // GeckoLib's model space is flipped relative to vanilla's, so the phantom's sign is inverted here
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-dragon.getRoll(partialTick)));
        poseStack.translate(0, -pivot, 0);
    }

    /** A long neck and tail reach well past the hitbox, so they stay drawn while the body is just off screen. */
    @Override
    public boolean shouldRender(DragonEntity dragon, Frustum frustum, double camX, double camY, double camZ) {
        if (super.shouldRender(dragon, frustum, camX, camY, camZ)) return true;
        float reach = dragon.getBody().reach() * dragon.getBodyScale();
        return reach > 0 && dragon.shouldRender(camX, camY, camZ)
                && frustum.isVisible(dragon.getBoundingBoxForCulling().inflate(reach));
    }

    // Placeholder: reuses the subspecies egg tint until the genome carries a base colour
    @Override
    public Color getRenderColor(DragonEntity dragon, float partialTick, int packedLight) {
        if (!dragon.getBody().tinted()) return super.getRenderColor(dragon, partialTick, packedLight);
        return Color.ofOpaque(ModRegistries.tint(dragon.level().registryAccess(), dragon.getSubspecies()));
    }
}
