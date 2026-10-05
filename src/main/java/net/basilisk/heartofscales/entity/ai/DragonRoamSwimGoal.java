package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonCommand;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.stats.SwimStats;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * A swimming species in deep enough water swims between points ahead of it for a while, then rests and drifts.
 * Targets go straight to the move control (no pathfinder) so the swim is one continuous curve.
 */
public class DragonRoamSwimGoal extends Goal {
    public static final int MIN_DEPTH = 2;
    private static final float CONE_HALF_ANGLE = 60.0f;
    private static final int TARGET_ATTEMPTS = 8;
    private static final double NEXT_TARGET_DISTANCE_SQR = 4.0 * 4.0;
    private static final int STUCK_TICKS = 40;
    private static final int MAX_STUCK_RETARGETS = 2;
    private static final double STUCK_DISTANCE_SQR = 0.5 * 0.5;
    /** A following dragon only idles in water within this many blocks of its owner. */
    private static final double NEAR_OWNER = 8.0;
    /** Past this the idle swim ends and the follow goal swims or walks after the owner. */
    private static final double FOLLOW_DISTANCE = 10.0;

    private final DragonEntity dragon;
    @Nullable
    private Vec3 target;
    private int swimTicksLeft;
    private int restTicksLeft;
    private int stuckTicks;
    private int stuckRetargets;
    private Vec3 lastPos = Vec3.ZERO;

    public DragonRoamSwimGoal(DragonEntity dragon) {
        this.dragon = dragon;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (dragon.isVehicle() || leavesToFollow(dragon.position(), ownerPosition())) return false;
        if (dragon.isInSwimMode()) return true;
        if (restTicksLeft > 0) {
            restTicksLeft -= reducedTickDelay(1);
            return false;
        }
        return dragon.canSwim()
                && WaterColumn.at(dragon.level(), dragon.blockPosition()).map(c -> c.isAtLeast(MIN_DEPTH)).orElse(false)
                && !dragon.isInSittingPose()
                && !dragon.isInLove()
                && dragon.getCommand() != DragonCommand.SIT;
    }

    @Override
    public boolean canContinueToUse() {
        // A mounted dragon hands its swim to the rider rather than ending it on this goal's timer
        return dragon.isInSwimMode() && !dragon.isVehicle() && !leavesToFollow(dragon.position(), ownerPosition());
    }

    @Override
    public void start() {
        dragon.setSwimMode(true);
        swimTicksLeft = stats().roamMinTicks() + dragon.getRandom().nextInt(Math.max(1, stats().roamMaxTicks() - stats().roamMinTicks()));
        stuckTicks = 0;
        stuckRetargets = 0;
        lastPos = dragon.position();
        target = null;
    }

    @Override
    public void stop() {
        // Handing over to the follow goal keeps it swimming rather than dropping out of swim mode for a tick
        if (!leavesToFollow(dragon.position(), ownerPosition())) dragon.setSwimMode(false);
        target = null;
        restTicksLeft = stats().restMinTicks() + dragon.getRandom().nextInt(Math.max(1, stats().restMaxTicks() - stats().restMinTicks()));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!dragon.isInWater()) {
            dragon.setSwimMode(false);
            return;
        }
        if (isStuck()) {
            stuckTicks = 0;
            if (++stuckRetargets > MAX_STUCK_RETARGETS) {
                dragon.setSwimMode(false);
                return;
            }
            target = tryTargets(180.0f);
            if (target == null) {
                dragon.setSwimMode(false);
                return;
            }
        }
        if (--swimTicksLeft <= 0) {
            dragon.setSwimMode(false);
            return;
        }
        if (target == null || dragon.position().distanceToSqr(target) < NEXT_TARGET_DISTANCE_SQR) {
            target = pickTarget();
            if (target == null) {
                dragon.setSwimMode(false);
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

    @Nullable
    private Vec3 pickTarget() {
        Vec3 candidate = tryTargets(CONE_HALF_ANGLE);
        return candidate != null ? candidate : tryTargets(180.0f);
    }

    /** A point in water ahead, somewhere between one block below the surface and one above the floor. */
    @Nullable
    private Vec3 tryTargets(float halfAngle) {
        for (int i = 0; i < TARGET_ATTEMPTS; i++) {
            Vec3 ahead = pointAhead(halfAngle);
            Optional<WaterColumn> column = WaterColumn.at(dragon.level(), BlockPos.containing(ahead));
            if (column.isEmpty() || !column.get().isAtLeast(MIN_DEPTH)) continue;
            int floor = column.get().floorY();
            int surface = column.get().surfaceY();
            double y = Mth.nextDouble(dragon.getRandom(), floor + 1, surface);
            Vec3 candidate = new Vec3(ahead.x, y, ahead.z);
            if (isAllowed(candidate) && isNearOwner(candidate, ownerPosition()) && hasClearLine(candidate)) return candidate;
        }
        return null;
    }

    private SwimStats stats() {
        return dragon.getStats().swim();
    }

    private Vec3 pointAhead(float halfAngle) {
        float yaw = dragon.getYRot() + Mth.nextFloat(dragon.getRandom(), -halfAngle, halfAngle);
        double distance = Mth.nextDouble(dragon.getRandom(), stats().roamMinDistance(), stats().roamMaxDistance());
        double radians = Math.toRadians(yaw);
        return dragon.position().add(-Math.sin(radians) * distance, 0, Math.cos(radians) * distance);
    }

    @Nullable
    private Vec3 ownerPosition() {
        LivingEntity owner = dragon.getFollowedOwner();
        return owner != null ? owner.position() : null;
    }

    /** Anywhere will do with no owner to follow; otherwise within NEAR_OWNER of them. */
    static boolean isNearOwner(Vec3 spot, @Nullable Vec3 owner) {
        return owner == null || spot.distanceToSqr(owner) <= NEAR_OWNER * NEAR_OWNER;
    }

    /** True once a following dragon's owner is far enough off that it should stop idling and go after them. */
    static boolean leavesToFollow(Vec3 dragon, @Nullable Vec3 owner) {
        return owner != null && dragon.distanceToSqr(owner) > FOLLOW_DISTANCE * FOLLOW_DISTANCE;
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
