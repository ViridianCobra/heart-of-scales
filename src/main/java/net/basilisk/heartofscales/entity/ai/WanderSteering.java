package net.basilisk.heartofscales.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * The meandering part of an idle flight. The turn is two slow waves of random length added together, so the path
 * drifts through arcs, S-bends and the odd near-loop, and changes smoothly enough that the lean stays smooth. Height
 * swells up and down the same way. Past the wander radius a leash turns the dragon back toward its centre.
 */
public final class WanderSteering {
    private static final int MIN_TURN_PERIOD = 160;
    private static final int MAX_TURN_PERIOD = 400;
    private static final int MIN_SWELL_PERIOD = 300;
    private static final int MAX_SWELL_PERIOD = 600;
    private static final double SWELL_HEIGHT = 4.0;
    /** Share of the turn the main wave carries; with the minor wave it never passes the cap. */
    private static final double MAIN_WAVE_SHARE = 0.6;

    private final double maxTurn;
    private final double mainPeriod;
    private final double mainPhase;
    private final double minorPeriod;
    private final double minorPhase;
    private final double swellPeriod;
    private final double swellPhase;

    private WanderSteering(double maxTurn, double mainPeriod, double mainPhase, double minorPeriod, double minorPhase,
                           double swellPeriod, double swellPhase) {
        this.maxTurn = maxTurn;
        this.mainPeriod = mainPeriod;
        this.mainPhase = mainPhase;
        this.minorPeriod = minorPeriod;
        this.minorPhase = minorPhase;
        this.swellPeriod = swellPeriod;
        this.swellPhase = swellPhase;
    }

    /** A new wander, turning at most {@code maxTurn} degrees per tick. */
    public static WanderSteering random(RandomSource random, double maxTurn) {
        return new WanderSteering(maxTurn,
                Mth.nextInt(random, MIN_TURN_PERIOD, MAX_TURN_PERIOD), random.nextDouble() * Math.PI * 2,
                Mth.nextInt(random, MIN_TURN_PERIOD, MAX_TURN_PERIOD), random.nextDouble() * Math.PI * 2,
                Mth.nextInt(random, MIN_SWELL_PERIOD, MAX_SWELL_PERIOD), random.nextDouble() * Math.PI * 2);
    }

    /** Degrees per tick to turn at this tick of the flight; positive is clockwise seen from above. */
    public double turn(int tick) {
        return maxTurn * (MAIN_WAVE_SHARE * wave(tick, mainPeriod, mainPhase)
                + (1 - MAIN_WAVE_SHARE) * wave(tick, minorPeriod, minorPhase));
    }

    /** Blocks above or below the cruise height at this tick of the flight. */
    public double swell(int tick) {
        return SWELL_HEIGHT * wave(tick, swellPeriod, swellPhase);
    }

    private static double wave(int tick, double period, double phase) {
        return Math.sin(tick * Math.PI * 2 / period + phase);
    }

    /**
     * The turn for this tick: the wander inside the radius, blended into turning for the centre further out until, at
     * twice the radius, it only heads home, turning as hard as {@code maxLeashTurn} allows.
     */
    public static double steer(double wanderTurn, float yaw, Vec3 pos, Vec3 centre, double radius, double maxLeashTurn) {
        double dx = centre.x - pos.x;
        double dz = centre.z - pos.z;
        double pull = Mth.clamp((Math.sqrt(dx * dx + dz * dz) - radius) / radius, 0.0, 1.0);
        if (pull <= 0) return wanderTurn;
        float homeYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        double homeTurn = Mth.clamp(Mth.wrapDegrees(homeYaw - yaw), -maxLeashTurn, maxLeashTurn);
        return Mth.lerp(pull, wanderTurn, homeTurn);
    }
}
