package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.stats.BreathStats;

/** One kind of breath. Both ticks only run while the dragon is breathing. */
public interface Breath {
    /** Damage, pushes and projectiles. {@code breathTicks} is 0 on the first tick of a breath. */
    void tickServer(DragonEntity dragon, BreathStats stats, BreathAim aim, int breathTicks);

    /** Particles, on every client that can see the dragon. */
    void tickClient(DragonEntity dragon, BreathStats stats, BreathAim aim);
}
