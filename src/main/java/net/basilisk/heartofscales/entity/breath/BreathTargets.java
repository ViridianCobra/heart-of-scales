package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.OwnableEntity;

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
}
