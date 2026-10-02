package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.HeartOfScales;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** The dragon-breath damage type, and the one way breath hurts something. */
public final class BreathDamage {
    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(HeartOfScales.MOD_ID, "dragon-breath"));

    private BreathDamage() {}

    /** Who breath hits are credited to: the rider, so kills count as theirs, or the dragon when nobody rides. */
    public static Entity attacker(DragonEntity dragon) {
        LivingEntity rider = dragon.getControllingPassenger();
        return rider != null ? rider : dragon;
    }

    /**
     * Hurts {@code target}. {@code direct} is what touched it: the dragon for a stream, the rock for rock breath. Returns
     * whether the hit landed; no-PvP servers, spawn protection and invulnerable mobs refuse it.
     */
    public static boolean hit(Entity attacker, Entity direct, LivingEntity target, float damage) {
        DamageSource source = new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TYPE), direct, attacker);
        // Breath sets its own hit rate, so vanilla's half second of immunity after a hit must not swallow it
        target.invulnerableTime = 0;
        return target.hurt(source, damage);
    }
}
