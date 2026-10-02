package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonCommand;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Walks after the owner, running to keep up as they pull ahead. Unlike vanilla's FollowOwnerGoal it only teleports when
 * it has been stuck for a while (see {@link Headway}); DragonEntity also moves one about to fall out of the area the
 * server runs. Flying species take off to catch up instead once the owner is far or high above; see DragonRoamFlightGoal.
 */
public class DragonFollowOwnerGoal extends Goal {
    private static final double START_DISTANCE = 10.0;
    private static final double STOP_DISTANCE = 2.0;
    /** The dragon starts to run once the owner is further than this... */
    private static final double RUN_START_DISTANCE = 16.0;
    /** ...and is at its full run from here. */
    private static final double FULL_RUN_DISTANCE = 32.0;
    private static final double MAX_RUN_FACTOR = 2.0;
    private static final int REPATH_TICKS = 10;
    /** Ticks in a row without heading toward the owner before it counts as stuck. */
    private static final int STUCK_TICKS = 200;
    /** Blocks per tick of movement toward the owner that counts as heading for them; a slow walk is about 0.14. */
    private static final double MIN_HEADWAY = 0.05;
    /** After climbing out at a bank it walks for this long before it may swim again, so a bump on the shore cannot flip it back and forth. */
    private static final int CLIMB_OUT_TICKS = 40;

    private final DragonEntity dragon;
    @Nullable
    private LivingEntity owner;
    private int repathTicks;
    private float oldWaterCost;
    private Headway headway = new Headway();
    private Vec3 lastPos = Vec3.ZERO;
    private int climbOutTicks;

    public DragonFollowOwnerGoal(DragonEntity dragon) {
        this.dragon = dragon;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = dragon.getOwner();
        if (owner == null || owner.isSpectator() || !canFollow()) return false;
        if (dragon.distanceToSqr(owner) < START_DISTANCE * START_DISTANCE) return false;
        this.owner = owner;
        return true;
    }

    // Unlike vanilla this keeps going when a path cannot be found, and simply tries again at the next repath
    @Override
    public boolean canContinueToUse() {
        return owner != null && owner.isAlive() && !owner.isSpectator() && owner.level() == dragon.level() && canFollow()
                && dragon.distanceToSqr(owner) > STOP_DISTANCE * STOP_DISTANCE;
    }

    private boolean canFollow() {
        return dragon.getCommand() == DragonCommand.FOLLOW && !dragon.isOrderedToSit() && !dragon.isPassenger() && !dragon.isLeashed();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        repathTicks = 0;
        headway = new Headway();
        lastPos = dragon.position();
        oldWaterCost = dragon.getPathfindingMalus(BlockPathTypes.WATER);
        dragon.setPathfindingMalus(BlockPathTypes.WATER, 0.0f);
    }

    @Override
    public void stop() {
        owner = null;
        leaveSwimMode();
        dragon.getNavigation().stop();
        dragon.setPathfindingMalus(BlockPathTypes.WATER, oldWaterCost);
    }

    @Override
    public void tick() {
        dragon.getLookControl().setLookAt(owner, 10.0f, dragon.getMaxHeadXRot());
        Vec3 pos = dragon.position();
        if (headway.tick(pos.subtract(lastPos), owner.position().subtract(pos))) {
            // Tried again in another STUCK_TICKS if there was nowhere safe to go
            OwnerCatchUp.teleportNear(dragon, owner);
            headway = new Headway();
            repathTicks = 0;
        }
        lastPos = dragon.position();
        if (swimAfterOwner()) return;
        if (--repathTicks > 0) return;
        repathTicks = adjustedTickDelay(REPATH_TICKS);
        // An owner beyond follow range gets a path to the nearest reachable point, redone from there next time
        dragon.getNavigation().moveTo(owner, runFactor(dragon.distanceTo(owner)));
    }

    /**
     * In deep water a swimming species swims after the owner rather than paddling along the surface. Returns true while
     * it does; reaching the bank with the owner on land, it climbs out and walks the rest.
     */
    private boolean swimAfterOwner() {
        if (climbOutTicks > 0) climbOutTicks--;
        Optional<WaterColumn> column = dragon.canSwim() && dragon.isInWater() && climbOutTicks == 0
                ? WaterColumn.at(dragon.level(), dragon.blockPosition()) : Optional.empty();
        if (column.isEmpty() || !column.get().isAtLeast(DragonRoamSwimGoal.MIN_DEPTH)) {
            leaveSwimMode();
            return false;
        }
        if (dragon.isInSwimMode() && dragon.horizontalCollision && !owner.isInWater()) {
            climbOutTicks = CLIMB_OUT_TICKS;
            leaveSwimMode();
            return false;
        }
        dragon.setSwimMode(true);
        double y = swimTargetY(owner.getY(), owner.isInWater(), column.get().floorY(), column.get().surfaceY());
        dragon.getMoveControl().setWantedPosition(owner.getX(), y, owner.getZ(), 1.0);
        return true;
    }

    private void leaveSwimMode() {
        if (!dragon.isInSwimMode()) return;
        dragon.setSwimMode(false);
        repathTicks = 0;
    }

    /** At the surface toward an owner on land, so it can climb out at the bank; to an owner in the water, at their depth but off the bottom. */
    static double swimTargetY(double ownerY, boolean ownerInWater, int floorY, int surfaceY) {
        return ownerInWater ? Mth.clamp(ownerY, floorY + 1, surfaceY) : surfaceY;
    }

    /** Speed multiplier for an owner this many blocks away: a walk up close, building to a run as they pull ahead. */
    static double runFactor(double distance) {
        double share = Mth.clamp((distance - RUN_START_DISTANCE) / (FULL_RUN_DISTANCE - RUN_START_DISTANCE), 0.0, 1.0);
        return 1.0 + share * (MAX_RUN_FACTOR - 1.0);
    }

    /**
     * Counts ticks in a row the dragon has not moved toward its owner. Measured from its own movement rather than the
     * distance, so a dragon trailing an owner who keeps the same lead is not stuck, while one standing at a river or
     * pacing along a wall is.
     */
    static final class Headway {
        private int ticksWithout;

        /**
         * Feeds one tick's horizontal movement; true once STUCK_TICKS in a row have gone without real headway. Only
         * counts while the owner is further than the distance following starts at: a big dragon often stops a little
         * short of a standing owner, and that is not stuck.
         */
        boolean tick(Vec3 moved, Vec3 toOwner) {
            Vec3 direction = new Vec3(toOwner.x, 0.0, toOwner.z);
            if (direction.lengthSqr() <= START_DISTANCE * START_DISTANCE) {
                ticksWithout = 0;
                return false;
            }
            double towardOwner = new Vec3(moved.x, 0.0, moved.z).dot(direction.normalize());
            ticksWithout = towardOwner >= MIN_HEADWAY ? 0 : ticksWithout + 1;
            return ticksWithout >= STUCK_TICKS;
        }
    }
}
