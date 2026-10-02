package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.stats.BreathStats;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A continuous breath. Every rateTicks it damages and shoves everything in its cone that it has a clear line to.
 * Air and water are two instances that differ only in their push, sound and particles.
 */
public final class StreamBreath implements Breath {
    /** The velocity added to a target, from the unit aim direction and the species' push strength. */
    @FunctionalInterface
    public interface Push {
        Vec3 of(Vec3 direction, double strength);
    }

    private static final int PARTICLES_PER_TICK = 6;

    private final Push push;
    private final SoundEvent sound;
    private final ParticleOptions[] particles;

    public StreamBreath(Push push, SoundEvent sound, ParticleOptions... particles) {
        this.push = push;
        this.sound = sound;
        this.particles = particles;
    }

    @Override
    public void tickServer(DragonEntity dragon, BreathStats stats, BreathAim aim, int breathTicks) {
        if (breathTicks % stats.rateTicks() != 0) return;
        Level level = dragon.level();
        Vec3 origin = aim.origin();
        double reach = aim.reach(stats.range());
        AABB area = new AABB(origin, origin.add(aim.direction().scale(reach))).inflate(stats.width() / 2.0 + 1.0);
        Entity attacker = BreathDamage.attacker(dragon);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> BreathTargets.canHit(dragon, e))) {
            Vec3 centre = target.getBoundingBox().getCenter();
            if (!BreathCone.contains(origin, aim.direction(), reach, stats.width(), centre, target.getBbWidth() / 2.0)) continue;
            if (!canReach(level, dragon, origin, centre)) continue;
            Vec3 before = target.getDeltaMovement();
            if (!BreathDamage.hit(attacker, dragon, target, stats.damage())) continue;
            // The push replaces the knockback the hit just gave, and heavy mobs shrug it off the same way
            double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            target.setDeltaMovement(before.add(push.of(aim.direction(), stats.push()).scale(Math.max(0.0, 1.0 - resistance))));
            // Without this a player's client never hears about the server changing their velocity
            target.hurtMarked = true;
        }
        Vec3 mouth = aim.mouth();
        level.playSound(null, mouth.x, mouth.y, mouth.z, sound, dragon.getSoundSource(), 1.0f, 1.0f);
    }

    @Override
    public void tickClient(DragonEntity dragon, BreathStats stats, BreathAim aim) {
        BreathParticles.stream(dragon, stats, aim, PARTICLES_PER_TICK, particles);
    }

    private static boolean canReach(Level level, DragonEntity dragon, Vec3 from, Vec3 to) {
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon);
        return level.clip(context).getType() == HitResult.Type.MISS;
    }
}
