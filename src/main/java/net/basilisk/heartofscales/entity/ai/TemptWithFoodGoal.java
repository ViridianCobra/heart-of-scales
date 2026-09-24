package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * An untamed dragon comes to a nearby player holding its food. On land it walks; in deep water it swims and
 * surfaces beside the player, so it can be fed from a bank or a boat.
 */
public class TemptWithFoodGoal extends Goal {
    private static final double NOTICE_RANGE = 10.0;
    private static final double GIVE_UP_RANGE = 12.0;
    private static final double STOP_DISTANCE = 2.5;
    /** How far from the player, toward the dragon, to look for water when the player is on land. */
    private static final int SURFACE_SEARCH_STEPS = 6;

    private final DragonEntity dragon;
    private final TargetingConditions holdingFood;
    @Nullable
    private Player player;

    public TemptWithFoodGoal(DragonEntity dragon) {
        this.dragon = dragon;
        this.holdingFood = TargetingConditions.forNonCombat().range(NOTICE_RANGE).ignoreLineOfSight()
                .selector(e -> e instanceof Player p && (dragon.isFood(p.getMainHandItem()) || dragon.isFood(p.getOffhandItem())));
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (dragon.isTame() || dragon.isVehicle()) return false;
        player = dragon.level().getNearestPlayer(holdingFood, dragon);
        return player != null;
    }

    @Override
    public boolean canContinueToUse() {
        return player != null && !dragon.isTame() && holdingFood.test(dragon, player)
                && dragon.distanceTo(player) <= GIVE_UP_RANGE;
    }

    @Override
    public void stop() {
        player = null;
        dragon.getGroundNavigation().stop();
        dragon.setSwimMode(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        dragon.getLookControl().setLookAt(player, 30.0f, 30.0f);
        boolean inDeepWater = dragon.canSwim()
                && WaterColumn.at(dragon.level(), dragon.blockPosition()).map(c -> c.isAtLeast(DragonRoamSwimGoal.MIN_DEPTH)).orElse(false);
        if (inDeepWater) {
            dragon.setSwimMode(true);
            swimToPlayer();
            return;
        }
        dragon.setSwimMode(false);
        if (dragon.distanceTo(player) < STOP_DISTANCE) {
            dragon.getGroundNavigation().stop();
        } else {
            dragon.getGroundNavigation().moveTo(player, 1.0);
        }
    }

    private void swimToPlayer() {
        Vec3 target = surfaceNearPlayer();
        if (target == null || dragon.position().distanceTo(target) < STOP_DISTANCE) {
            dragon.getMoveControl().setWantedPosition(dragon.getX(), dragon.getY(), dragon.getZ(), 0.0);
            return;
        }
        dragon.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.0);
    }

    /** The water surface at the player's column, or failing that the first water column stepping from the player toward the dragon. */
    @Nullable
    private Vec3 surfaceNearPlayer() {
        Vec3 from = player.position();
        Vec3 step = dragon.position().subtract(from);
        step = new Vec3(step.x, 0, step.z);
        if (step.lengthSqr() < 1.0e-4) return null;
        step = step.normalize();
        for (int i = 0; i <= SURFACE_SEARCH_STEPS; i++) {
            Vec3 probe = from.add(step.scale(i));
            Optional<WaterColumn> column = WaterColumn.at(dragon.level(), BlockPos.containing(probe));
            if (column.isPresent()) return new Vec3(probe.x, column.get().surfaceY() + 0.5, probe.z);
        }
        return null;
    }
}
