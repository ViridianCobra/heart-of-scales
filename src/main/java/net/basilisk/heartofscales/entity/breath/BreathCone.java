package net.basilisk.heartofscales.entity.breath;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Breath geometry. Pure maths with no world access; every direction is unit length. */
public final class BreathCone {
    /** Radius where the cone starts, so something touching the dragon's nose is still hit. */
    public static final double START_RADIUS = 0.5;
    /** A target closer than this to where the breath starts is inside the dragon's own head and says nothing about direction. */
    private static final double MIN_AIM_DISTANCE = 1.0;

    private BreathCone() {}

    /**
     * Whether a target centred at {@code point}, {@code targetRadius} blocks wide either side of its centre, overlaps a cone
     * that starts at {@code start}, runs {@code range} blocks along {@code direction} and is {@code width} blocks across at
     * its far end.
     */
    public static boolean contains(Vec3 start, Vec3 direction, double range, double width, Vec3 point, double targetRadius) {
        Vec3 offset = point.subtract(start);
        double along = offset.dot(direction);
        if (along < -targetRadius || along > range + targetRadius) return false;
        double sideways = Math.sqrt(Math.max(0.0, offset.lengthSqr() - along * along));
        double radius = START_RADIUS + (width / 2.0 - START_RADIUS) * (Math.max(0.0, Math.min(along, range)) / range);
        return sideways <= radius + targetRadius;
    }

    /** The direction from {@code origin} to {@code target}, or {@code fallback} when the target is too close to give one. */
    public static Vec3 towards(Vec3 origin, Vec3 target, Vec3 fallback) {
        Vec3 offset = target.subtract(origin);
        return offset.lengthSqr() < MIN_AIM_DISTANCE * MIN_AIM_DISTANCE ? fallback : offset.normalize();
    }

    /** {@code look}, swung towards {@code facing} until it is at most {@code maxDegrees} away from it. */
    public static Vec3 clampDirection(Vec3 look, Vec3 facing, double maxDegrees) {
        double cos = Math.max(-1.0, Math.min(1.0, look.dot(facing)));
        double max = Math.toRadians(maxDegrees);
        if (Math.acos(cos) <= max) return look;
        Vec3 side = look.subtract(facing.scale(cos));
        // Straight behind has no side to swing round towards
        if (side.lengthSqr() < 1.0E-8) return facing;
        return facing.scale(Math.cos(max)).add(side.normalize().scale(Math.sin(max)));
    }

    /** {@code direction} nudged at random, {@code spread} blocks sideways per block travelled as one standard deviation. */
    public static Vec3 scatter(Vec3 direction, double spread, RandomSource random) {
        return direction.add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread)
                .normalize();
    }
}
