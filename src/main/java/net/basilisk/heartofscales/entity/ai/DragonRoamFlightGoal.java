package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonCommand;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * An idle flying dragon takes off, sweeps between points ahead of it for a while, then finds ground and lands.
 * Targets go straight to the move control (no pathfinder) so the flight is one continuous curve.
 */
public class DragonRoamFlightGoal extends Goal {
    private static final int TAKEOFF_CHANCE = 600;
    private static final int MIN_FLIGHT_TICKS = 300;
    private static final int MAX_FLIGHT_TICKS = 600;
    private static final double MIN_TARGET_DISTANCE = 8.0;
    private static final double MAX_TARGET_DISTANCE = 16.0;
    private static final float CONE_HALF_ANGLE = 60.0f;
    private static final int TARGET_ATTEMPTS = 8;
    /** Pick the next target once this close to the current one, so the dragon never slows to arrive. */
    private static final double NEXT_TARGET_DISTANCE_SQR = 3.0 * 3.0;
    private static final int MIN_HEIGHT_ABOVE_GROUND = 3;
    private static final int MAX_HEIGHT_ABOVE_GROUND = 12;
    private static final int GROUND_SEARCH_DEPTH = 24;
    private static final int STUCK_TICKS = 40;
    /** Stuck this many times in one flight -> give up and land. */
    private static final int MAX_STUCK_RETARGETS = 2;
    private static final double STUCK_DISTANCE_SQR = 0.5 * 0.5;

    private final DragonEntity dragon;
    @Nullable
    private Vec3 target;
    private int flightTicksLeft;
    private boolean landing;
    private int stuckTicks;
    private int stuckRetargets;
    private Vec3 lastPos = Vec3.ZERO;

    public DragonRoamFlightGoal(DragonEntity dragon) {
        this.dragon = dragon;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // A dragon already in the air (reloaded mid-flight, or left flying by an interrupted goal) is picked up again
        if (dragon.isVehicle()) return false;
        if (dragon.isFlying()) return true;
        return dragon.canFly()
                && dragon.onGround()
                && !dragon.isInSittingPose()
                && !dragon.isInLove()
                && dragon.getCommand() != DragonCommand.SIT
                && dragon.getRandom().nextInt(reducedTickDelay(TAKEOFF_CHANCE)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return dragon.isFlying();
    }

    @Override
    public void start() {
        dragon.setFlying(true);
        flightTicksLeft = MIN_FLIGHT_TICKS + dragon.getRandom().nextInt(MAX_FLIGHT_TICKS - MIN_FLIGHT_TICKS);
        landing = false;
        stuckTicks = 0;
        stuckRetargets = 0;
        lastPos = dragon.position();
        target = null;
    }

    @Override
    public void stop() {
        dragon.setFlying(false);
        target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (landing && dragon.onGround()) {
            dragon.setFlying(false);
            return;
        }
        if (isStuck()) {
            // Blocked by something: turn away and try elsewhere before giving up
            stuckTicks = 0;
            if (landing || ++stuckRetargets > MAX_STUCK_RETARGETS) {
                dragon.setFlying(false);
                return;
            }
            target = tryTargets(180.0f);
            if (target == null) {
                dragon.setFlying(false);
                return;
            }
        }

        if (!landing && --flightTicksLeft <= 0) {
            landing = true;
            target = null;
        }

        // While landing the ground target is kept until touchdown; a stuck check covers the last stretch
        if (target == null || (!landing && dragon.position().distanceToSqr(target) < NEXT_TARGET_DISTANCE_SQR)) {
            target = landing ? pickLandingTarget() : pickRoamTarget();
            if (target == null) {
                if (landing) {
                    dragon.setFlying(false);
                } else {
                    landing = true;
                }
                return;
            }
        }
        dragon.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.0);
    }

    private boolean isStuck() {
        Vec3 pos = dragon.position();
        if (pos.distanceToSqr(lastPos) < STUCK_DISTANCE_SQR) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPos = pos;
        }
        return stuckTicks > STUCK_TICKS;
    }

    /** A clear point ahead, a few blocks above the ground, inside the home box when the dragon has one. */
    @Nullable
    private Vec3 pickRoamTarget() {
        Vec3 candidate = tryTargets(CONE_HALF_ANGLE);
        return candidate != null ? candidate : tryTargets(180.0f);
    }

    @Nullable
    private Vec3 tryTargets(float halfAngle) {
        for (int i = 0; i < TARGET_ATTEMPTS; i++) {
            Vec3 ahead = pointAhead(halfAngle, MIN_TARGET_DISTANCE, MAX_TARGET_DISTANCE);
            int ground = groundBelow(BlockPos.containing(ahead));
            if (ground == Integer.MIN_VALUE) continue;
            double y = ground + 1 + MIN_HEIGHT_ABOVE_GROUND
                    + dragon.getRandom().nextInt(MAX_HEIGHT_ABOVE_GROUND - MIN_HEIGHT_ABOVE_GROUND + 1);
            Vec3 candidate = new Vec3(ahead.x, y, ahead.z);
            if (isAllowed(candidate) && hasClearLine(candidate)) return candidate;
        }
        return null;
    }

    /** A spot on the ground a short way ahead; failing that, keep descending. */
    @Nullable
    private Vec3 pickLandingTarget() {
        for (int i = 0; i < TARGET_ATTEMPTS; i++) {
            Vec3 ahead = pointAhead(CONE_HALF_ANGLE, 6.0, 10.0);
            int ground = groundBelow(BlockPos.containing(ahead));
            if (ground == Integer.MIN_VALUE) continue;
            Vec3 candidate = new Vec3(ahead.x, ground + 1, ahead.z);
            if (hasClearLine(candidate)) return candidate;
        }
        Vec3 ahead = pointAhead(CONE_HALF_ANGLE, 6.0, 10.0);
        return new Vec3(ahead.x, dragon.getY() - 4, ahead.z);
    }

    private Vec3 pointAhead(float halfAngle, double minDistance, double maxDistance) {
        float yaw = dragon.getYRot() + Mth.nextFloat(dragon.getRandom(), -halfAngle, halfAngle);
        double distance = Mth.nextDouble(dragon.getRandom(), minDistance, maxDistance);
        double radians = Math.toRadians(yaw);
        return dragon.position().add(-Math.sin(radians) * distance, 0, Math.cos(radians) * distance);
    }

    /** Y of the highest solid block at or below the column's start, or MIN_VALUE if none within reach. */
    private int groundBelow(BlockPos start) {
        Level level = dragon.level();
        BlockPos.MutableBlockPos pos = start.mutable();
        for (int i = 0; i < GROUND_SEARCH_DEPTH; i++) {
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return pos.getY();
            pos.move(0, -1, 0);
        }
        return Integer.MIN_VALUE;
    }

    private boolean isAllowed(Vec3 pos) {
        return !dragon.hasRestriction() || dragon.isWithinRestriction(BlockPos.containing(pos));
    }

    private boolean hasClearLine(Vec3 to) {
        HitResult hit = dragon.level().clip(new ClipContext(dragon.getEyePosition(), to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
        return hit.getType() == HitResult.Type.MISS;
    }
}
