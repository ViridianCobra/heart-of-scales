package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.stats.FlightStats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * Flight that always moves forward and banks toward its target a few degrees per tick, climbing or diving toward the
 * target's height no steeper than {@link #MAX_CLIMB_ANGLE}.
 * Vanilla's FlyingMoveControl snaps yaw by up to 90 degrees a tick, which looks wrong on a large body. The velocity is
 * set here directly and DragonEntity.travel moves by it as is: vanilla's air push and drag (98% kept vertically, 91%
 * horizontally) made the dragon overshoot its height and bounce before settling.
 * Gravity is not touched here; DragonEntity.setFlying owns that.
 */
public class DragonFlightMoveControl extends MoveControl {
    /** Steepest climb or dive in degrees. A steeper target is reached by circling up or down to it. */
    static final double MAX_CLIMB_ANGLE = 30.0;
    /** Share of the way to the wanted velocity covered each tick: the dragon's inertia. Low enough values overshoot. */
    static final double RESPONSIVENESS = 0.1;
    private static final double REACHED_DISTANCE_SQR = 1.0;

    private final DragonEntity dragon;

    public DragonFlightMoveControl(DragonEntity dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    @Override
    public void tick() {
        mob.setZza(0.0f);
        mob.setYya(0.0f);
        if (operation != Operation.MOVE_TO) {
            slowToHover();
            return;
        }
        double dx = wantedX - mob.getX();
        double dy = wantedY - mob.getY();
        double dz = wantedZ - mob.getZ();
        if (dx * dx + dy * dy + dz * dz < REACHED_DISTANCE_SQR) {
            operation = Operation.WAIT;
            slowToHover();
            return;
        }

        FlightStats flight = dragon.getStats().flight();
        float wantedYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float yaw = rotlerp(mob.getYRot(), wantedYaw, flight.aiMaxYawTurn());
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;

        Vec3 velocity = nextVelocity(mob.getDeltaMovement(), mob.position(), new Vec3(wantedX, wantedY, wantedZ), yaw,
                speedModifier * dragon.aiFlightSpeed());
        mob.setDeltaMovement(velocity);

        // Pitch follows the actual velocity rather than the target direction, which swings whenever the target changes
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float wantedPitch = (float) -(Mth.atan2(velocity.y, horizontalSpeed) * Mth.RAD_TO_DEG);
        mob.setXRot(rotlerp(mob.getXRot(), wantedPitch, flight.aiMaxPitchTurn()));
    }

    private void slowToHover() {
        mob.setDeltaMovement(mob.getDeltaMovement().scale(1.0 - RESPONSIVENESS));
    }

    /** One tick of easing toward {@link #wantedVelocity}. */
    static Vec3 nextVelocity(Vec3 velocity, Vec3 pos, Vec3 target, float yaw, double speed) {
        return velocity.lerp(wantedVelocity(pos, target, yaw, speed), RESPONSIVENESS);
    }

    /**
     * Full speed along the facing, tilted toward the target's height. The tilt flattens as the height is reached, so
     * the climb eases out instead of overshooting; turning toward the target is left to the yaw.
     */
    static Vec3 wantedVelocity(Vec3 pos, Vec3 target, float yaw, double speed) {
        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        double maxClimb = Math.toRadians(MAX_CLIMB_ANGLE);
        double climb = Mth.clamp(Math.atan2(target.y - pos.y, Math.sqrt(dx * dx + dz * dz)), -maxClimb, maxClimb);
        double yawRadians = Math.toRadians(yaw);
        double horizontal = speed * Math.cos(climb);
        return new Vec3(-Math.sin(yawRadians) * horizontal, speed * Math.sin(climb), Math.cos(yawRadians) * horizontal);
    }
}
