package net.basilisk.heartofscales.client;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.config.HeartOfScalesClientConfig;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.entity.breath.DragonBreath;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * While the local rider breathes, the third-person camera rises over the rider and the dragon's head and tilts down so the
 * middle of the screen stays on the point the breath is aimed at. CameraMixin applies it; the blend eases in and out per tick.
 */
@Mod.EventBusSubscriber(modid = HeartOfScales.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BreathCamera {
    private static final double CEILING_MARGIN = 0.3;
    /** How much of the way the tilt catches up to a new aim distance per tick, so it does not jump on and off a mob. */
    private static final double AIM_CATCH_UP = 0.35;

    private static float blendO;
    private static float blend;
    /** This frame's eased blend; 0 leaves the camera as vanilla has it. */
    private static float frameBlend;
    /** How far along the rider's look the camera aims, smoothed; negative until the first frame of a lift. */
    private static double aimDistance = -1.0;
    private static Vec3 aimPoint = Vec3.ZERO;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        DragonEntity dragon = riddenDragon(minecraft.player);
        if (dragon == null) {
            blendO = 0.0f;
            blend = 0.0f;
            return;
        }
        blendO = blend;
        if (shouldLift(minecraft, dragon)) {
            blend = Math.min(1.0f, blend + 1.0f / HeartOfScalesClientConfig.BREATH_CAMERA_EASE_IN.get());
        } else {
            blend = Math.max(0.0f, blend - 1.0f / HeartOfScalesClientConfig.BREATH_CAMERA_EASE_OUT.get());
        }
    }

    /** Called at the start of each camera setup. */
    public static void beginFrame(Entity entity, boolean detached, boolean mirrored, float partialTick) {
        float eased = Mth.lerp(partialTick, blendO, blend);
        boolean applies = detached && !mirrored && eased > 0.0f && riddenDragon(entity) != null;
        frameBlend = applies ? eased * eased * (3.0f - 2.0f * eased) : 0.0f;
        if (frameBlend == 0.0f) aimDistance = -1.0;
    }

    public static boolean isActive() {
        return frameBlend > 0.0f;
    }

    /** Extra third-person distance this frame. */
    public static double extraDistance() {
        return HeartOfScalesClientConfig.BREATH_CAMERA_EXTRA_DISTANCE.get() * frameBlend;
    }

    /**
     * The rider's eye raised by this frame's lift, stopping short of any ceiling. Also settles where the camera aims, on the
     * rider's look from {@code eye}, which is where the breath goes.
     */
    public static Vec3 liftedPivot(BlockGetter level, Entity entity, Vec3 eye, float yRot, float xRot) {
        DragonEntity dragon = riddenDragon(entity);
        if (dragon == null) return eye;
        Vec3 look = Vec3.directionFromRotation(xRot, yRot);
        double target = eye.distanceTo(dragon.getBreath().crosshairTarget((LocalPlayer) entity, eye, look));
        if (aimDistance < 0.0) {
            aimDistance = target;
        } else {
            // Frame-rate independent: the same catch-up per tick however many frames that tick is drawn over
            double catchUp = 1.0 - Math.pow(1.0 - AIM_CATCH_UP, Minecraft.getInstance().getDeltaFrameTime());
            aimDistance += (target - aimDistance) * catchUp;
        }
        aimPoint = eye.add(look.scale(aimDistance));

        double lift = HeartOfScalesClientConfig.BREATH_CAMERA_LIFT.get() * frameBlend;
        Vec3 top = eye.add(0.0, lift + CEILING_MARGIN, 0.0);
        HitResult ceiling = level.clip(new ClipContext(eye, top, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
        if (ceiling.getType() != HitResult.Type.MISS) lift = Math.max(0.0, ceiling.getLocation().y - eye.y - CEILING_MARGIN);
        return eye.add(0.0, lift, 0.0);
    }

    /** The x and y rotation that looks from {@code from} at the aim point, with the y kept within a turn of {@code yRot}. */
    public static Vec2 rotationTowardsAim(Vec3 from, float yRot, float xRot) {
        Vec3 offset = aimPoint.subtract(from);
        double horizontal = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        // Straight down or up has no heading to turn to
        if (horizontal < 1.0E-4) return new Vec2(xRot, yRot);
        float pitch = (float) (-Mth.atan2(offset.y, horizontal) * Mth.RAD_TO_DEG);
        float yaw = (float) (Mth.atan2(offset.z, offset.x) * Mth.RAD_TO_DEG) - 90.0f;
        // Vanilla's yaw is not wrapped, so the turn is added to it rather than replacing it
        return new Vec2(pitch, yRot + Mth.wrapDegrees(yaw - yRot));
    }

    private static boolean shouldLift(Minecraft minecraft, DragonEntity dragon) {
        if (!HeartOfScalesClientConfig.BREATH_CAMERA.get()) return false;
        if (minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK) return false;
        // Rising out of the water would flick the fog off and on
        if (dragon.isInSwimMode()) return false;
        DragonBreath breath = dragon.getBreath();
        // The key is checked as well as the synced flag, so the camera starts rising without waiting on the server
        return breath.isBreathing() || RiderInputHandler.isBreathHeld(breath);
    }

    @Nullable
    private static DragonEntity riddenDragon(@Nullable Entity entity) {
        if (!(entity instanceof LocalPlayer player)) return null;
        return player.getVehicle() instanceof DragonEntity dragon && dragon.getControllingPassenger() == player ? dragon : null;
    }

    private BreathCamera() {}
}
