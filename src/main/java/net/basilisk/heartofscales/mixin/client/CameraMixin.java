package net.basilisk.heartofscales.mixin.client;

import net.basilisk.heartofscales.client.BreathCamera;
import net.basilisk.heartofscales.client.FlightCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla hard-codes the third-person camera distance as a literal 4.0 passed to getMaxZoom inside setup.
 * There is no Forge event for it on 1.20.1, so this scales that argument while the player is flying a dragon.
 * The breath camera is lifted just before getMaxZoom, so vanilla's wall check runs from the lifted point, then tilted
 * at the end of setup once the camera has been pulled back.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract float getYRot();

    @Inject(method = "setup", at = @At("HEAD"))
    private void heartOfScales$beginBreathFrame(BlockGetter level, Entity entity, boolean detached, boolean mirrored,
                                                float partialTick, CallbackInfo ci) {
        BreathCamera.beginFrame(entity, detached, mirrored, partialTick);
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"))
    private double heartOfScales$scaleFlightZoom(double startingDistance) {
        return startingDistance * FlightCamera.distanceMultiplier() + BreathCamera.extraDistance();
    }

    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"))
    private void heartOfScales$liftForBreath(BlockGetter level, Entity entity, boolean detached, boolean mirrored,
                                             float partialTick, CallbackInfo ci) {
        if (BreathCamera.isActive()) setPosition(BreathCamera.liftedPivot(level, entity, getPosition(), getYRot(), getXRot()));
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void heartOfScales$aimAtBreath(BlockGetter level, Entity entity, boolean detached, boolean mirrored,
                                           float partialTick, CallbackInfo ci) {
        if (!BreathCamera.isActive()) return;
        Vec2 rotation = BreathCamera.rotationTowardsAim(getPosition(), getYRot(), getXRot());
        setRotation(rotation.y, rotation.x);
    }
}
