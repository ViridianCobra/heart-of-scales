package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonCommand;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** A water-bound dragon on land walks to the nearest water it can find. Lower priority than sit, flee, breed and follow. */
public class ReturnToWaterGoal extends Goal {
    private static final int SAMPLES = 32;
    private static final int HORIZONTAL_RANGE = 16;
    private static final int VERTICAL_RANGE = 4;
    private static final int RETRY_TICKS = 40;

    private final DragonEntity dragon;
    @Nullable
    private BlockPos water;
    private int retryTicksLeft;

    public ReturnToWaterGoal(DragonEntity dragon) {
        this.dragon = dragon;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (retryTicksLeft > 0) {
            retryTicksLeft -= reducedTickDelay(1);
            return false;
        }
        if (!dragon.canSwim() || !dragon.onGround() || dragon.isInWater()
                || dragon.isInSittingPose() || dragon.getCommand() == DragonCommand.SIT) {
            return false;
        }
        water = findWater();
        if (water == null) retryTicksLeft = RETRY_TICKS;
        return water != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !dragon.isInWater() && !dragon.getGroundNavigation().isDone();
    }

    @Override
    public void start() {
        dragon.getGroundNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.0);
    }

    @Override
    public void stop() {
        dragon.getGroundNavigation().stop();
        water = null;
        retryTicksLeft = RETRY_TICKS;
    }

    /** Nearest sampled water block with water or air directly above it, or null. */
    @Nullable
    private BlockPos findWater() {
        Level level = dragon.level();
        BlockPos origin = dragon.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < SAMPLES; i++) {
            BlockPos candidate = origin.offset(
                    dragon.getRandom().nextInt(HORIZONTAL_RANGE * 2 + 1) - HORIZONTAL_RANGE,
                    dragon.getRandom().nextInt(VERTICAL_RANGE * 2 + 1) - VERTICAL_RANGE,
                    dragon.getRandom().nextInt(HORIZONTAL_RANGE * 2 + 1) - HORIZONTAL_RANGE);
            if (!level.getFluidState(candidate).is(FluidTags.WATER)) continue;
            BlockState above = level.getBlockState(candidate.above());
            if (!above.isAir() && !above.getFluidState().is(FluidTags.WATER)) continue;
            double distance = candidate.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }
}
