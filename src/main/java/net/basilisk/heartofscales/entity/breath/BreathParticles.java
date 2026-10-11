package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The particle stream every breath draws on the client, spread to fill the breath's cone. */
public final class BreathParticles {
    /** Launch speed per block of range. Clouds slow down as they go, so this is set by eye. */
    private static final double SPEED_PER_BLOCK = 0.07;
    /** How far past the mouth puffs start, as a fraction of the mouth's distance from the breath's origin. */
    private static final double START_AHEAD = 0.5;

    private BreathParticles() {}

    /**
     * {@code count} particles from just past the mouth along the aim, picked at random from {@code particles}; bubbles
     * underwater. Most land inside a cone {@code width} blocks across at {@code range}, the same shape BreathCone hits.
     */
    public static void stream(DragonEntity dragon, BreathAim aim, double range, double width, int count,
                              ParticleOptions... particles) {
        Level level = dragon.level();
        RandomSource random = dragon.getRandom();
        Vec3 direction = aim.direction();
        Vec3 start = aim.mouth().add(direction.scale(aim.origin().distanceTo(aim.mouth()) * START_AHEAD));
        boolean underwater = level.getFluidState(BlockPos.containing(start)).is(FluidTags.WATER);
        // Two standard deviations reach the cone's edge at full range
        double spread = Math.max(0.0, width / 2.0 - BreathCone.START_RADIUS) / (2.0 * range);
        double speed = range * SPEED_PER_BLOCK;
        // Carried along with the dragon, so a stream breathed in flight is not left behind
        Vec3 carried = dragon.position().subtract(dragon.xo, dragon.yo, dragon.zo);
        for (int i = 0; i < count; i++) {
            Vec3 from = start.add(BreathCone.disc(direction, BreathCone.START_RADIUS, random));
            Vec3 velocity = BreathCone.scatter(direction, spread, random).scale(speed).add(carried);
            ParticleOptions particle = underwater ? ParticleTypes.BUBBLE : particles[random.nextInt(particles.length)];
            level.addParticle(particle, from.x, from.y, from.z, velocity.x, velocity.y, velocity.z);
        }
    }
}
