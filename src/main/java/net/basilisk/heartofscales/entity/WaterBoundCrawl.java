package net.basilisk.heartofscales.entity;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Slows a water-bound dragon while it is out of water. A transient attribute modifier, so nothing is saved
 * and nothing needs cleaning up on reload. Delete this class and its one call in DragonEntity.tick to make
 * water dragons amphibious.
 */
public final class WaterBoundCrawl {
    private static final UUID MODIFIER_ID = UUID.fromString("7a1e5c3e-2b6f-4d0a-9c8e-1f2a3b4c5d6e");
    /** Multiply-total by -0.6 leaves 40% of normal speed. */
    private static final AttributeModifier CRAWL = new AttributeModifier(MODIFIER_ID, "Water-bound crawl", -0.6,
            AttributeModifier.Operation.MULTIPLY_TOTAL);

    private WaterBoundCrawl() {
    }

    public static void tick(DragonEntity dragon) {
        AttributeInstance speed = dragon.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean shouldCrawl = dragon.canSwim() && !dragon.isInWater() && !dragon.isInSwimMode();
        boolean crawling = speed.hasModifier(CRAWL);
        if (shouldCrawl && !crawling) speed.addTransientModifier(CRAWL);
        if (!shouldCrawl && crawling) speed.removeModifier(MODIFIER_ID);
    }
}
