package net.basilisk.heartofscales.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * A tinted cloud puff that keeps the velocity it is launched with and drifts to a stop, which is what makes a breath read as
 * a stream. It starts small and grows as it travels, so the cloud is biggest out past the rider and the dragon's head where
 * the camera can see it. It uses vanilla's cloud sprites, so a new breath colour needs no art.
 */
public class BreathCloudParticle extends TextureSheetParticle {
    private static final float GROWTH = 4.0f;
    private static final float ALPHA = 0.85f;
    /** The last part of its life, as a fraction, over which a puff fades out. */
    private static final float FADE = 0.25f;

    protected BreathCloudParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                                  SpriteSet sprites, float red, float green, float blue) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.friction = 0.96f;
        this.xd = this.xd * 0.1 + xd;
        this.yd = this.yd * 0.1 + yd;
        this.zd = this.zd * 0.1 + zd;
        float shade = 1.0f - random.nextFloat() * 0.3f;
        setColor(red * shade, green * shade, blue * shade);
        setAlpha(ALPHA);
        this.quadSize = 0.25f + random.nextFloat() * 0.1f;
        // Launched at range * 0.07 with this friction, a puff covers about its breath's range in this many ticks
        this.lifetime = 18 + random.nextInt(7);
        this.hasPhysics = false;
        pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Grows quickly at first then levels off, so it clears the rider's silhouette soon after leaving the mouth. */
    @Override
    public float getQuadSize(float partialTick) {
        float life = Mth.clamp((age + partialTick) / lifetime, 0.0f, 1.0f);
        float eased = 1.0f - (1.0f - life) * (1.0f - life);
        return quadSize * (1.0f + (GROWTH - 1.0f) * eased);
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) age / lifetime;
        setAlpha(life < 1.0f - FADE ? ALPHA : ALPHA * Math.max(0.0f, 1.0f - life) / FADE);
    }

    /** Culling uses the particle's small box, so a grown puff would vanish as soon as its centre left the screen. */
    @Override
    public boolean shouldCull() {
        return false;
    }

    public record Provider(SpriteSet sprites, float red, float green, float blue) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new BreathCloudParticle(level, x, y, z, xd, yd, zd, sprites, red, green, blue);
        }
    }
}
