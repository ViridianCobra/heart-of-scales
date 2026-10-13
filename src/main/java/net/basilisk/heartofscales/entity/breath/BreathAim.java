package net.basilisk.heartofscales.entity.breath;

import net.minecraft.world.phys.Vec3;

/**
 * Where a breath comes from and the unit direction it travels. Hits are measured from {@code origin}, inside the head, so a
 * mob pressed against the nose is caught and a door the dragon is pressed against still blocks; particles leave the
 * {@code mouth}.
 */
public record BreathAim(Vec3 origin, Vec3 mouth, Vec3 direction) {
    /** How far hits reach from the origin for a breath that reaches {@code range} blocks past the mouth. */
    public double reach(double range) {
        return range + origin.distanceTo(mouth);
    }
}
