package net.basilisk.heartofscales.entity.ai;

import net.minecraft.world.phys.Vec3;

/**
 * Steering for laps around a centre. Rather than flying to fixed points, the dragon aims at a spot a little ahead of
 * where it is around the centre, so it can never overshoot a point and loop back for it. Angles are in radians,
 * measured from east (+X) toward south (+Z), which is clockwise seen from above.
 */
public final class CirclePath {
    private CirclePath() {}

    public static double angleAround(Vec3 centre, Vec3 pos) {
        return Math.atan2(pos.z - centre.z, pos.x - centre.x);
    }

    /** The point on the circle {@code leadAngle} further round than {@code pos}, whatever its distance from the centre. */
    public static Vec3 aimPoint(Vec3 centre, Vec3 pos, double radius, double leadAngle, boolean clockwise, double y) {
        double angle = angleAround(centre, pos) + (clockwise ? leadAngle : -leadAngle);
        return new Vec3(centre.x + radius * Math.cos(angle), y, centre.z + radius * Math.sin(angle));
    }

    /** How far round a move between two angles went in the flying direction; negative if it went backwards. */
    public static double progress(double fromAngle, double toAngle, boolean clockwise) {
        double delta = Math.IEEEremainder(toAngle - fromAngle, 2 * Math.PI);
        return clockwise ? delta : -delta;
    }
}
