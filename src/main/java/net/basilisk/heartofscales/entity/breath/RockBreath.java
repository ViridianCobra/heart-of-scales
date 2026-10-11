package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.registry.ModParticles;
import net.basilisk.heartofscales.species.stats.BreathStats;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Land breath: a dust stream with rocks in it. One rock every rateTicks does the damage, scattered so most land within
 * {@code width} blocks at full range; the dust is only for show.
 */
public final class RockBreath implements Breath {
    /** Blocks per tick on top of the dragon's own speed. Slow enough to see the rock leave. */
    private static final float ROCK_SPEED = 1.2f;
    private static final int DUST_PER_TICK = 5;
    /** How wide the dust spreads at full range. Separate from the species' width, which sets how far rocks scatter. */
    private static final double DUST_WIDTH = 5.0;

    @Override
    public void tickServer(DragonEntity dragon, BreathStats stats, BreathAim aim, int breathTicks) {
        if (breathTicks % stats.rateTicks() != 0) return;
        Level level = dragon.level();
        Vec3 direction = BreathCone.scatter(aim.direction(), stats.width() / 4.0 / stats.range(), dragon.getRandom());
        int lifetime = Mth.ceil(aim.reach(stats.range()) / ROCK_SPEED);
        DragonRockEntity rock = new DragonRockEntity(level, dragon, BreathDamage.attacker(dragon), aim.origin(), stats.damage(), lifetime);
        rock.shoot(direction.x, direction.y, direction.z, ROCK_SPEED, 0.0f);
        rock.setDeltaMovement(rock.getDeltaMovement().add(dragon.getBreath().movement()));
        level.addFreshEntity(rock);
        Vec3 mouth = aim.mouth();
        level.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.DISPENSER_LAUNCH, dragon.getSoundSource(), 1.0f,
                0.8f + dragon.getRandom().nextFloat() * 0.2f);
    }

    @Override
    public void tickClient(DragonEntity dragon, BreathStats stats, BreathAim aim) {
        BreathParticles.stream(dragon, aim, stats.range(), DUST_WIDTH, DUST_PER_TICK, ModParticles.DUST_BREATH.get());
    }
}
