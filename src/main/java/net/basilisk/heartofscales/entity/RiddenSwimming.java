package net.basilisk.heartofscales.entity;

import net.basilisk.heartofscales.entity.ai.DragonRoamSwimGoal;
import net.basilisk.heartofscales.entity.ai.WaterColumn;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The swim-only parts of riding. Steering itself is shared with free flight in DragonEntity.travelFreeSteered.
 * Each method here is one rule that may change later: see the "future changes" note in the ridden swimming spec.
 */
public final class RiddenSwimming {
    private RiddenSwimming() {
    }

    /** Swim mode follows depth: on in water deep enough to swim, off in the shallows or on land. */
    public static void tickMode(DragonEntity dragon) {
        if (!dragon.canSwim()) return;
        boolean deep = dragon.isInWater() && WaterColumn.at(dragon.level(), dragon.blockPosition())
                .map(c -> c.isAtLeast(DragonRoamSwimGoal.MIN_DEPTH)).orElse(false);
        dragon.setSwimMode(deep);
    }

    /** No breaching: an upward push that would lift the dragon's eyes out of the water is dropped. */
    public static Vec3 capAtSurface(DragonEntity dragon, Vec3 wanted) {
        if (wanted.y <= 0) return wanted;
        BlockPos eyesAfter = BlockPos.containing(dragon.getX(), dragon.getEyeY() + wanted.y, dragon.getZ());
        if (dragon.level().getFluidState(eyesAfter).is(FluidTags.WATER)) return wanted;
        return new Vec3(wanted.x, 0, wanted.z);
    }

    /** Everyone aboard breathes while the dragon swims. */
    public static void topUpPassengerAir(DragonEntity dragon) {
        for (Entity passenger : dragon.getPassengers()) {
            if (passenger instanceof LivingEntity living) living.setAirSupply(living.getMaxAirSupply());
        }
    }
}
