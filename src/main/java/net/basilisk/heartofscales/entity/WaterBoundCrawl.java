package net.basilisk.heartofscales.entity;

import net.basilisk.heartofscales.HeartOfScales;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Slows a water-bound dragon while it is out of water. A transient attribute modifier, so nothing is saved
 * and nothing needs cleaning up on reload. Delete this class and its one call in DragonEntity.tick to make
 * water dragons amphibious.
 */
public final class WaterBoundCrawl {
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(HeartOfScales.MOD_ID, "water_bound_crawl");
    /** Multiply-total by -0.6 leaves 40% of normal speed. */
    private static final AttributeModifier CRAWL = new AttributeModifier(MODIFIER_ID, -0.6,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private WaterBoundCrawl() {
    }

    public static void tick(DragonEntity dragon) {
        AttributeInstance speed = dragon.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean shouldCrawl = dragon.canSwim() && !dragon.isInWater() && !dragon.isInSwimMode();
        boolean crawling = speed.hasModifier(MODIFIER_ID);
        if (shouldCrawl && !crawling) speed.addTransientModifier(CRAWL);
        if (!shouldCrawl && crawling) speed.removeModifier(MODIFIER_ID);
    }
}
