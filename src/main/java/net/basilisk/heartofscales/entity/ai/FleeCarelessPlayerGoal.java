package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Untamed dragons run from players who sprint or carry a weapon nearby.
 * Not vanilla's AvoidEntityGoal: that one ignores creative players and peaceful difficulty.
 */
public class FleeCarelessPlayerGoal extends Goal {
    private final DragonEntity dragon;
    private final float range;
    private final double walkSpeed;
    private final double sprintSpeed;
    private Player threat;
    private Path path;

    public FleeCarelessPlayerGoal(DragonEntity dragon, float range, double walkSpeed, double sprintSpeed) {
        this.dragon = dragon;
        this.range = range;
        this.walkSpeed = walkSpeed;
        this.sprintSpeed = sprintSpeed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public static boolean isCareless(Player player) {
        return player.isSprinting()
                || isWeapon(player.getMainHandItem().getItem())
                || isWeapon(player.getOffhandItem().getItem());
    }

    private static boolean isWeapon(Item item) {
        return item instanceof SwordItem || item instanceof AxeItem
                || item instanceof ProjectileWeaponItem || item instanceof TridentItem;
    }

    @Override
    public boolean canUse() {
        if (dragon.isTame()) return false;
        threat = dragon.level().getNearestPlayer(dragon.getX(), dragon.getY(), dragon.getZ(), range,
                entity -> entity instanceof Player player && !player.isSpectator() && isCareless(player));
        if (threat == null) return false;
        Vec3 away = DefaultRandomPos.getPosAway(dragon, 16, 7, threat.position());
        if (away == null || threat.distanceToSqr(away) < threat.distanceToSqr(dragon)) return false;
        path = dragon.getNavigation().createPath(away.x, away.y, away.z, 0);
        return path != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !dragon.getNavigation().isDone();
    }

    @Override
    public void start() {
        dragon.getNavigation().moveTo(path, walkSpeed);
    }

    @Override
    public void stop() {
        threat = null;
    }

    @Override
    public void tick() {
        dragon.getNavigation().setSpeedModifier(dragon.distanceToSqr(threat) < 49.0 ? sprintSpeed : walkSpeed);
    }
}
