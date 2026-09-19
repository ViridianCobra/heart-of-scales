package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.world.entity.ai.control.LookControl;

/** Vanilla's look control zeroes the entity pitch every tick; in flight the move control owns the pitch. */
public class DragonLookControl extends LookControl {
    private final DragonEntity dragon;

    public DragonLookControl(DragonEntity dragon) {
        super(dragon);
        this.dragon = dragon;
    }

    @Override
    protected boolean resetXRotOnTick() {
        return !dragon.isFlying();
    }
}
