package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonCommand;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Walks after the owner, running to keep up as they pull ahead. Unlike vanilla's FollowOwnerGoal it never teleports:
 * a dragon left behind (its area stopped running) waits until the owner comes back into range. Flying species take off
 * to catch up instead once the owner is far or high above; see DragonRoamFlightGoal.
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

    private final DragonEntity dragon;
    @Nullable
    private LivingEntity owner;
    private int repathTicks;
    private float oldWaterCost;

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
    public void start() {
        repathTicks = 0;
        oldWaterCost = dragon.getPathfindingMalus(BlockPathTypes.WATER);
        dragon.setPathfindingMalus(BlockPathTypes.WATER, 0.0f);
    }

    @Override
    public void stop() {
        owner = null;
        dragon.getNavigation().stop();
        dragon.setPathfindingMalus(BlockPathTypes.WATER, oldWaterCost);
    }

    @Override
    public void tick() {
        dragon.getLookControl().setLookAt(owner, 10.0f, dragon.getMaxHeadXRot());
        if (--repathTicks > 0) return;
        repathTicks = adjustedTickDelay(REPATH_TICKS);
        // An owner beyond follow range gets a path to the nearest reachable point, redone from there next time
        dragon.getNavigation().moveTo(owner, runFactor(dragon.distanceTo(owner)));
    }

    /** Speed multiplier for an owner this many blocks away: a walk up close, building to a run as they pull ahead. */
    static double runFactor(double distance) {
        double share = Mth.clamp((distance - RUN_START_DISTANCE) / (FULL_RUN_DISTANCE - RUN_START_DISTANCE), 0.0, 1.0);
        return 1.0 + share * (MAX_RUN_FACTOR - 1.0);
    }
}
