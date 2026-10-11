package net.basilisk.heartofscales.species.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The ridden jump of a species with "jumps". Height is in blocks at a full jump bar; the launch speed that reaches it
 * comes from simulating vanilla gravity, so the jump flies under the game's own physics.
 */
public record JumpStats(double height, double forwardPush) {
    public static final JumpStats DEFAULT = new JumpStats(5.0, 0.4);
    // LivingEntity.travel: each tick a falling entity moves, then loses GRAVITY and keeps DRAG of its vertical speed
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;

    public static final Codec<JumpStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("height", DEFAULT.height()).forGetter(JumpStats::height),
            Codec.DOUBLE.optionalFieldOf("forward_push", DEFAULT.forwardPush()).forGetter(JumpStats::forwardPush)
    ).apply(instance, JumpStats::new));

    /** Upward speed for a vanilla jump bar power of 0 to 100. */
    public double launchSpeed(int power) {
        return launchSpeedFor(height) * chargeScale(power);
    }

    /** Share of the full jump a jump bar power gives, worked out as AbstractHorse.onPlayerJump does. */
    public static double chargeScale(int power) {
        return power >= 90 ? 1.0 : 0.4 + 0.4 * Math.max(power, 0) / 90.0;
    }

    public static double peakHeight(double speed) {
        double height = 0;
        while (speed > 0) {
            height += speed;
            speed = (speed - GRAVITY) * DRAG;
        }
        return height;
    }

    /** The upward speed that peaks at this height. No jump for a height that is not a positive number. */
    public static double launchSpeedFor(double height) {
        if (!(height > 0) || !Double.isFinite(height)) return 0;
        double low = 0;
        double high = 1;
        while (peakHeight(high) < height) high *= 2;
        for (int i = 0; i < 50; i++) {
            double mid = (low + high) / 2;
            if (peakHeight(mid) < height) low = mid;
            else high = mid;
        }
        return high;
    }
}
