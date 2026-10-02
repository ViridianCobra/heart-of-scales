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
 * Vanilla's cloud puff, tinted. It keeps the velocity it is launched with and drifts to a stop, which is what makes the air
 * breath read as a stream. It uses vanilla's cloud sprites, so a new breath colour needs no art.
 */
public class BreathCloudParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected BreathCloudParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                                  SpriteSet sprites, float red, float green, float blue) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.friction = 0.96f;
        this.xd = this.xd * 0.1 + xd;
        this.yd = this.yd * 0.1 + yd;
        this.zd = this.zd * 0.1 + zd;
        float shade = 1.0f - random.nextFloat() * 0.3f;
        setColor(red * shade, green * shade, blue * shade);
        this.quadSize *= 1.875f;
        this.lifetime = (int) Math.max((int) (8.0 / (random.nextFloat() * 0.8 + 0.3)) * 2.5f, 1.0f);
        this.hasPhysics = false;
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public float getQuadSize(float partialTick) {
        return quadSize * Mth.clamp((age + partialTick) / lifetime * 32.0f, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) setSpriteFromAge(sprites);
    }

    public record Provider(SpriteSet sprites, float red, float green, float blue) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new BreathCloudParticle(level, x, y, z, xd, yd, zd, sprites, red, green, blue);
        }
    }
}
