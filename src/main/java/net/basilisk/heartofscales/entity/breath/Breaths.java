package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** The behaviour behind each BreathType. Kept out of the enum so the enum loads without vanilla's registries. */
public final class Breaths {
    public static final Breath ROCK = new RockBreath();
    public static final Breath AIR = new StreamBreath(
            (direction, strength) -> new Vec3(direction.x * 0.3, 0.6, direction.z * 0.3).scale(strength),
            SoundEvents.ENDER_DRAGON_FLAP, ParticleTypes.POOF, ParticleTypes.CLOUD);
    public static final Breath WATER = new StreamBreath(
            (direction, strength) -> new Vec3(direction.x * 0.5, -1.0, direction.z * 0.5).scale(strength),
            SoundEvents.BUCKET_EMPTY, ModParticles.WATER_BREATH.get(), ModParticles.WATER_BREATH.get(), ParticleTypes.FISHING);

    private Breaths() {}

    public static Breath of(BreathType type) {
        return switch (type) {
            case ROCK -> ROCK;
            case AIR -> AIR;
            case WATER -> WATER;
        };
    }
}
