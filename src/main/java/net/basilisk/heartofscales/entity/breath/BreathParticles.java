package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.stats.BreathStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The particle stream every breath draws on the client, spread to match the breath's width. */
public final class BreathParticles {
    /** Launch speed per block of range. Clouds slow down as they go, so this is set by eye. */
    private static final double SPEED_PER_BLOCK = 0.07;

    private BreathParticles() {}

    /** {@code count} particles from the mouth along the aim, picked at random from {@code particles}; bubbles underwater. */
    public static void stream(DragonEntity dragon, BreathStats stats, BreathAim aim, int count, ParticleOptions... particles) {
        Level level = dragon.level();
        RandomSource random = dragon.getRandom();
        Vec3 mouth = aim.mouth();
        boolean underwater = level.getFluidState(BlockPos.containing(mouth)).is(FluidTags.WATER);
        double spread = stats.width() / 4.0 / stats.range();
        double speed = stats.range() * SPEED_PER_BLOCK;
        // Carried along with the dragon, so a stream breathed in flight is not left behind
        Vec3 carried = dragon.position().subtract(dragon.xo, dragon.yo, dragon.zo);
        for (int i = 0; i < count; i++) {
            Vec3 velocity = BreathCone.scatter(aim.direction(), spread, random).scale(speed).add(carried);
            ParticleOptions particle = underwater ? ParticleTypes.BUBBLE : particles[random.nextInt(particles.length)];
            level.addParticle(particle, mouth.x, mouth.y, mouth.z, velocity.x, velocity.y, velocity.z);
        }
    }
}
