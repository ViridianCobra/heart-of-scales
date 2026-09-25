package net.basilisk.heartofscales.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * Swimming that always moves forward and turns toward its target a few degrees per tick, like DragonFlightMoveControl.
 * Speed is not set here: DragonEntity.travelSwimming pushes along the direction this control gives, so only the
 * ratio of forward to vertical input matters. Gravity is owned by DragonEntity.setSwimMode.
 */
public class DragonSwimMoveControl extends MoveControl {
    private static final float MAX_YAW_TURN = 5.0f;
    private static final float MAX_PITCH_TURN = 10.0f;
    private static final double CLIMB_FULL_STRENGTH_DISTANCE = 4.0;
    private static final double REACHED_DISTANCE_SQR = 1.0;

    public DragonSwimMoveControl(Mob mob) {
        super(mob);
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

        float wantedYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float yaw = rotlerp(mob.getYRot(), wantedYaw, MAX_YAW_TURN);
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;

        Vec3 velocity = mob.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float wantedPitch = (float) -(Mth.atan2(velocity.y, horizontalSpeed) * Mth.RAD_TO_DEG);
        mob.setXRot(rotlerp(mob.getXRot(), wantedPitch, MAX_PITCH_TURN));

        mob.setSpeed(1.0f);
        mob.setYya((float) Mth.clamp(dy / CLIMB_FULL_STRENGTH_DISTANCE, -1.0, 1.0));
    }
}
