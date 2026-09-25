package net.basilisk.heartofscales.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.ModRegistries;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DragonRenderer extends GeoEntityRenderer<DragonEntity> {
    // Matches the halved hitbox vanilla gives baby mobs
    private static final float BABY_SCALE = 0.5f;
    // Height of the point the body pitches around while flying, in blocks (roughly the body's centre)
    private static final float PITCH_PIVOT_HEIGHT = 0.9f;

    public DragonRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonModel());
        this.shadowRadius = 0.8f;
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, DragonEntity dragon,
                                    BakedGeoModel model, boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        float scale = dragon.isBaby() ? BABY_SCALE : 1.0f;
        super.scaleModelForRender(widthScale * scale, heightScale * scale, poseStack, dragon, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    /** Pitch the whole body toward the flight or swim direction, turning about the body centre rather than the feet. */
    @Override
    protected void applyRotations(DragonEntity dragon, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(dragon, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        if (!dragon.isFlying() && !dragon.isInSwimMode()) return;
        float pitch = Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot()) + dragon.getTiltPitch(partialTick);
        float pivot = PITCH_PIVOT_HEIGHT * (dragon.isBaby() ? BABY_SCALE : 1.0f);
        poseStack.translate(0, pivot, 0);
        // GeckoLib's model space is flipped relative to vanilla's, so the phantom's sign is inverted here
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-dragon.getRoll(partialTick)));
        poseStack.translate(0, -pivot, 0);
    }

    // Placeholder: reuses the subspecies egg tint until the genome carries a base colour
    @Override
    public Color getRenderColor(DragonEntity dragon, float partialTick, int packedLight) {
        return Color.ofOpaque(ModRegistries.tint(dragon.level().registryAccess(), dragon.getSubspecies()));
    }
}
