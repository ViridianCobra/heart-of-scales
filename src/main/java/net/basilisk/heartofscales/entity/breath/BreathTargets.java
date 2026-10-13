package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.stats.BreathStats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Who a dragon's breath may hit: never the dragon's own side, nor creative or spectator players. */
public final class BreathTargets {
    private BreathTargets() {}

    public static boolean canHit(DragonEntity dragon, Entity target) {
        if (target == dragon || dragon.hasPassenger(target)) return false;
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) return false;
        UUID owner = dragon.getOwnerUUID();
        if (owner == null) return true;
        if (owner.equals(target.getUUID())) return false;
        return !(target instanceof OwnableEntity pet && owner.equals(pet.getOwnerUUID()));
    }

    /** Everything the breath may hit that is inside its cone with a clear line to it. */
    public static List<LivingEntity> inCone(Level level, DragonEntity dragon, BreathStats stats, BreathAim aim) {
        Vec3 origin = aim.origin();
        double reach = aim.reach(stats.range());
        AABB area = new AABB(origin, origin.add(aim.direction().scale(reach))).inflate(stats.width() / 2.0 + 1.0);
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> canHit(dragon, e))) {
            Vec3 centre = target.getBoundingBox().getCenter();
            if (!BreathCone.contains(origin, aim.direction(), reach, stats.width(), centre, target.getBbWidth() / 2.0)) continue;
            if (canReach(level, dragon, origin, centre)) targets.add(target);
        }
        return targets;
    }

    private static boolean canReach(Level level, DragonEntity dragon, Vec3 from, Vec3 to) {
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon);
        return level.clip(context).getType() == HitResult.Type.MISS;
    }
}
