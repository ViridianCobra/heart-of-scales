package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.stats.BreathStats;
import net.basilisk.heartofscales.species.stats.StaminaStats;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A dragon's breath: the meter, whether it is breathing this tick, and where the breath goes. The synced keys must be
 * declared on DragonEntity, so it hands them in. The meter follows the same rules as sprint stamina.
 */
public final class DragonBreath {
    private static final double MOUTH_FORWARD = 1.5;
    private static final double MOUTH_HEIGHT = 1.6;
    private static final double MAX_AIM_DEGREES = 60.0;
    private static final double BABY_SCALE = 0.5;
    /** The rider's eye sits behind the mouth, so the crosshair is followed this much past the breath's range. */
    private static final double CROSSHAIR_EXTRA_REACH = 4.0;
    /** A step longer than this in one tick is a teleport, not speed. */
    private static final double MAX_TRACKED_STEP = 4.0;

    private final DragonEntity dragon;
    private final EntityDataAccessor<Float> meterKey;
    private final EntityDataAccessor<Boolean> exhaustedKey;
    private final EntityDataAccessor<Boolean> breathingKey;
    private boolean riderBreathing;
    private int restTicks;
    private int breathTicks;
    @Nullable
    private Vec3 lastPosition;
    private Vec3 movement = Vec3.ZERO;

    public DragonBreath(DragonEntity dragon, EntityDataAccessor<Float> meterKey, EntityDataAccessor<Boolean> exhaustedKey,
                        EntityDataAccessor<Boolean> breathingKey) {
        this.dragon = dragon;
        this.meterKey = meterKey;
        this.exhaustedKey = exhaustedKey;
        this.breathingKey = breathingKey;
    }

    public void setRiderBreathing(boolean breathing) {
        this.riderBreathing = breathing;
    }

    public BreathType type() {
        return dragon.getSpecies().breathType();
    }

    /** Breath left, 0 to 1. */
    public float meterFraction() {
        return dragon.getEntityData().get(meterKey) / stats().meter().max();
    }

    public boolean isExhausted() {
        return dragon.getEntityData().get(exhaustedKey);
    }

    /** Synced, so every client knows when to draw the breath. */
    public boolean isBreathing() {
        return dragon.getEntityData().get(breathingKey);
    }

    /**
     * How far the dragon has been moving per tick, measured on the server. A ridden dragon is moved by its rider's client
     * between ticks, so the server never sees it move inside its own tick.
     */
    public Vec3 movement() {
        return movement;
    }

    /** Server side, every tick. */
    public void tick() {
        trackMovement();
        SynchedEntityData data = dragon.getEntityData();
        StaminaStats meter = stats().meter();
        float fill = data.get(meterKey);
        boolean breathing = riderBreathing && dragon.getControllingPassenger() instanceof Player && !isExhausted() && fill > 0.0f;
        if (breathing) {
            fill = Math.max(0.0f, fill - meter.drainPerTick());
            restTicks = 0;
            if (fill <= 0.0f) data.set(exhaustedKey, true);
        } else if (restTicks < meter.regenDelayTicks()) {
            restTicks++;
        } else {
            fill = Math.min(meter.max(), fill + meter.regen());
        }
        if (isExhausted() && fill >= meter.recoveredThreshold()) data.set(exhaustedKey, false);
        data.set(meterKey, fill);
        data.set(breathingKey, breathing);
        if (breathing) Breaths.of(type()).tickServer(dragon, stats(), aim(), breathTicks);
        breathTicks = breathing ? breathTicks + 1 : 0;
    }

    /** Client side, every tick. */
    public void tickClient() {
        if (isBreathing()) Breaths.of(type()).tickClient(dragon, stats(), aim());
    }

    /**
     * Where the breath goes: at whatever the rider's crosshair is on, or straight ahead with nobody riding. Only reads what
     * vanilla already syncs, so the server and every client agree.
     */
    public BreathAim aim() {
        if (dragon.getControllingPassenger() instanceof Player rider) return aimAt(crosshairTarget(rider));
        return new BreathAim(headPoint(0.0), headPoint(MOUTH_FORWARD), facing());
    }

    /** Aim at a point, kept within reach of the dragon's facing so the breath never goes back through its body. */
    public BreathAim aimAt(Vec3 target) {
        Vec3 origin = headPoint(0.0);
        Vec3 facing = facing();
        Vec3 direction = BreathCone.towards(origin, target, facing);
        return new BreathAim(origin, headPoint(MOUTH_FORWARD), BreathCone.clampDirection(direction, facing, MAX_AIM_DEGREES));
    }

    /** The first mob or block along the rider's look, or the far end of the breath's reach. */
    private Vec3 crosshairTarget(Player rider) {
        Vec3 eye = rider.getEyePosition();
        Vec3 end = eye.add(rider.getLookAngle().scale(stats().range() + CROSSHAIR_EXTRA_REACH));
        HitResult block = dragon.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB area = rider.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult mob = ProjectileUtil.getEntityHitResult(rider, eye, end, area,
                e -> !e.isSpectator() && e.isPickable() && e != dragon && !dragon.hasPassenger(e), eye.distanceToSqr(end));
        return mob != null ? mob.getLocation() : end;
    }

    /**
     * A point in the head at mouth height, {@code forward} blocks ahead of the body's centre. The body only pitches in the
     * air and water, so on the ground the head stays level.
     */
    private Vec3 headPoint(double forward) {
        double scale = dragon.isBaby() ? BABY_SCALE : 1.0;
        double pivot = DragonEntity.BODY_PIVOT_HEIGHT * scale;
        return new Vec3(0.0, MOUTH_HEIGHT * scale - pivot, forward * scale)
                .xRot(-pitch() * Mth.DEG_TO_RAD)
                .yRot(-dragon.getYRot() * Mth.DEG_TO_RAD)
                .add(dragon.getX(), dragon.getY() + pivot, dragon.getZ());
    }

    private Vec3 facing() {
        return Vec3.directionFromRotation(pitch(), dragon.getYRot());
    }

    private float pitch() {
        return dragon.isInFluidMode() ? dragon.getXRot() : 0.0f;
    }

    private void trackMovement() {
        Vec3 position = dragon.position();
        if (lastPosition != null) {
            Vec3 step = position.subtract(lastPosition);
            // Vehicle packets arrive zero or two to a tick, so the steps are averaged
            movement = step.lengthSqr() > MAX_TRACKED_STEP * MAX_TRACKED_STEP ? Vec3.ZERO : movement.add(step).scale(0.5);
        }
        lastPosition = position;
    }

    private BreathStats stats() {
        return dragon.getStats().breath();
    }
}
