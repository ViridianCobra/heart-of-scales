package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.stats.FlightStats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * Flight that always moves forward and banks toward its target a few degrees per tick.
 * Vanilla's FlyingMoveControl snaps yaw by up to 90 degrees a tick, which looks wrong on a large body.
 * Gravity is not touched here; DragonEntity.setFlying owns that.
 */
public class DragonFlightMoveControl extends MoveControl {
    /** Vertical input reaches full strength when the target is this many blocks above or below. */
    private static final double CLIMB_FULL_STRENGTH_DISTANCE = 4.0;
    private static final double REACHED_DISTANCE_SQR = 1.0;

    private final DragonEntity dragon;

    public DragonFlightMoveControl(DragonEntity dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    @Override
    public void tick() {
        if (operation != Operation.MOVE_TO) {
            mob.setZza(0.0f);
            mob.setYya(0.0f);
            return;
        }
        double dx = wantedX - mob.getX();
        double dy = wantedY - mob.getY();
        double dz = wantedZ - mob.getZ();
        if (dx * dx + dy * dy + dz * dz < REACHED_DISTANCE_SQR) {
            operation = Operation.WAIT;
            mob.setZza(0.0f);
            mob.setYya(0.0f);
            return;
        }

        FlightStats flight = dragon.getStats().flight();
        float wantedYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float yaw = rotlerp(mob.getYRot(), wantedYaw, flight.aiMaxYawTurn());
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;

        // Pitch follows the actual velocity rather than the target direction, which swings whenever the target changes
        Vec3 velocity = mob.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float wantedPitch = (float) -(Mth.atan2(velocity.y, horizontalSpeed) * Mth.RAD_TO_DEG);
        mob.setXRot(rotlerp(mob.getXRot(), wantedPitch, flight.aiMaxPitchTurn()));

        float speed = (float) (speedModifier * mob.getAttributeValue(Attributes.FLYING_SPEED));
        mob.setSpeed(speed);
        mob.setYya((float) (speed * Mth.clamp(dy / CLIMB_FULL_STRENGTH_DISTANCE, -1.0, 1.0)));
    }
}
