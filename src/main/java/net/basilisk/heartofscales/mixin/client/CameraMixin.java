package net.basilisk.heartofscales.mixin.client;

import net.basilisk.heartofscales.client.FlightCamera;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Vanilla hard-codes the third-person camera distance as a literal 4.0 passed to getMaxZoom inside setup.
 * There is no Forge event for it on 1.20.1, so this scales that argument while the player is flying a dragon.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"))
    private double heartOfScales$scaleFlightZoom(double startingDistance) {
        return startingDistance * FlightCamera.distanceMultiplier();
    }
}
