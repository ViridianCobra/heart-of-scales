package net.basilisk.heartofscales.entity.breath;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.registry.ModEntities;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * One rock of a land dragon's breath. Flies like a snowball with less drop, hurts the first thing it hits that the breath
 * may hit, then crumbles. Removed once it has flown for its lifetime, and never saved.
 */
public class DragonRockEntity extends ThrowableItemProjectile {
    private static final float GRAVITY = 0.01f;
    private static final int CRUMBLE_PARTICLES = 8;

    private float damage;
    private int lifetime;
    // Who fired it, so a rider who dismounts mid-flight still gets the kill
    @Nullable
    private Entity attacker;

    public DragonRockEntity(EntityType<? extends DragonRockEntity> type, Level level) {
        super(type, level);
    }

    public DragonRockEntity(Level level, DragonEntity dragon, Entity attacker, Vec3 origin, float damage, int lifetime) {
        super(ModEntities.DRAGON_ROCK.get(), origin.x, origin.y, origin.z, level);
        setOwner(dragon);
        this.attacker = attacker;
        this.damage = damage;
        this.lifetime = lifetime;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected float getGravity() {
        return GRAVITY;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > lifetime) discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) return false;
        return !(getOwner() instanceof DragonEntity dragon) || BreathTargets.canHit(dragon, target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        if (getOwner() instanceof DragonEntity dragon && result.getEntity() instanceof LivingEntity target) {
            BreathDamage.hit(attacker != null ? attacker : dragon, this, target, damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        level().broadcastEntityEvent(this, EntityEvent.DEATH);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 0.8f,
                0.9f + random.nextFloat() * 0.2f);
        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != EntityEvent.DEATH) {
            super.handleEntityEvent(id);
            return;
        }
        ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, getItem());
        for (int i = 0; i < CRUMBLE_PARTICLES; i++) {
            level().addParticle(particle, getX(), getY(), getZ(),
                    (random.nextDouble() - 0.5) * 0.1, random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.1);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
