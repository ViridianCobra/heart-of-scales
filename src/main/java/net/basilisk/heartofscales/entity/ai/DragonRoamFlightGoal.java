package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonCommand;
import net.basilisk.heartofscales.entity.DragonEntity;
import net.basilisk.heartofscales.species.stats.FlightStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A flying dragon takes off, flies a few laps around its centre at one steady height, then lands near the centre.
 * See {@link #circleCentre(BlockPos, DragonCommand, BlockPos, Vec3)} for what it circles. A following dragon also takes
 * off to catch its owner up (see {@link #shouldChaseOwner}): with the owner as the centre, being far off the circle
 * just means flying at them, faster the further behind it is. Targets go straight to the move control (no pathfinder)
 * so the flight is one continuous curve.
 */
public class DragonRoamFlightGoal extends Goal {
    /** How far round the circle ahead of itself the dragon aims. */
    private static final double LEAD_ANGLE = Math.toRadians(30.0);
    /** How much the radius swells and shrinks around a lap, as a share of it, so laps are not perfect circles. */
    private static final double WOBBLE = 0.1;
    /** Laps only count once the dragon is this far out, as a share of the radius; near the centre its angle swings wildly. */
    private static final double COUNTING_RADIUS_SHARE = 0.5;
    private static final int HEIGHT_SAMPLES = 12;
    /** The circle height is re-checked this often, for a centre that moves or ground that has loaded since. */
    private static final int ALTITUDE_CHECK_TICKS = 20;
    /** A following dragon only takes off on a whim with its owner this close. */
    private static final double IDLE_OWNER_RANGE = 16.0;
    /** A following dragon flies after an owner further away than this, or higher above it than CHASE_HEIGHT. */
    private static final double CHASE_DISTANCE = 24.0;
    private static final double CHASE_HEIGHT = 6.0;
    /** Ticks after landing before it will take off to chase again, so an owner on a tower does not make it hop. */
    private static final int CHASE_COOLDOWN_TICKS = 100;
    /** An owner this far above the ground under them is flying: the dragon keeps circling them until they come down. */
    private static final double OWNER_AIRBORNE_HEIGHT = 4.0;
    /** Laps are flown at least this far above the owner. */
    private static final double OWNER_CLEARANCE = 4.0;
    /** Past the circle by this much the dragon starts to speed up, reaching full catch-up speed CATCH_UP_RAMP further out. */
    private static final double CATCH_UP_MARGIN = 8.0;
    private static final double CATCH_UP_RAMP = 16.0;
    private static final double MAX_CATCH_UP_SPEED = 2.0;
    private static final double LANDING_RADIUS = 6.0;
    private static final float CONE_HALF_ANGLE = 60.0f;
    private static final int TARGET_ATTEMPTS = 8;
    private static final int GROUND_SEARCH_DEPTH = 24;
    private static final int STUCK_TICKS = 40;
    /** Stuck this many times in one flight -> give up and land. */
    private static final int MAX_STUCK_RETARGETS = 2;
    private static final double STUCK_DISTANCE_SQR = 0.5 * 0.5;

    private final DragonEntity dragon;
    private boolean landing;
    @Nullable
    private Vec3 landingTarget;
    private boolean clockwise;
    private double radius;
    private double wobblePhase;
    private int heightAboveGround;
    private double altitude;
    private int altitudeCheckTicks;
    private double lastAngle;
    /** Radians still to fly round the centre before landing. */
    private double angleLeft;
    private int stuckTicks;
    private int stuckRetargets;
    private Vec3 lastPos = Vec3.ZERO;
    private int landedAtTick = -CHASE_COOLDOWN_TICKS;

    public DragonRoamFlightGoal(DragonEntity dragon) {
        this.dragon = dragon;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // A dragon already in the air (reloaded mid-flight, or left flying by an interrupted goal) is picked up again
        if (dragon.isVehicle()) return false;
        if (dragon.isFlying()) return true;
        if (!dragon.canFly() || !dragon.onGround() || dragon.isInSittingPose() || dragon.isInLove() || dragon.isLeashed()
                || dragon.getCommand() == DragonCommand.SIT || circleCentre() == null) {
            return false;
        }
        LivingEntity owner = followedOwner();
        if (owner != null) {
            if (dragon.tickCount - landedAtTick > CHASE_COOLDOWN_TICKS && shouldChaseOwner(dragon.position(), owner.position())) {
                return true;
            }
            if (dragon.distanceToSqr(owner) > IDLE_OWNER_RANGE * IDLE_OWNER_RANGE) return false;
        }
        return dragon.getRandom().nextInt(reducedTickDelay(stats().roamTakeoffChance())) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return dragon.isFlying();
    }

    @Override
    public void start() {
        dragon.setFlying(true);
        landing = false;
        landingTarget = null;
        stuckTicks = 0;
        stuckRetargets = 0;
        lastPos = dragon.position();
        Vec3 centre = circleCentre();
        if (centre == null) {
            // Nothing to circle, as when a following dragon is reloaded mid-flight with its owner away: just come down
            landing = true;
            return;
        }
        RandomSource random = dragon.getRandom();
        FlightStats flight = stats();
        clockwise = random.nextBoolean();
        radius = followedOwner() != null
                ? Mth.nextDouble(random, flight.ownerCircleMinRadius(), flight.ownerCircleMaxRadius())
                : Mth.nextDouble(random, flight.circleMinRadius(), flight.circleMaxRadius());
        wobblePhase = random.nextDouble() * Math.PI * 2;
        heightAboveGround = Mth.nextInt(random, flight.circleMinHeight(), flight.circleMaxHeight());
        altitude = cruiseAltitude(centre);
        altitudeCheckTicks = ALTITUDE_CHECK_TICKS;
        lastAngle = CirclePath.angleAround(centre, dragon.position());
        angleLeft = Mth.nextInt(random, flight.circleMinLaps(), flight.circleMaxLaps()) * Math.PI * 2;
    }

    @Override
    public void stop() {
        dragon.setFlying(false);
        landingTarget = null;
        landedAtTick = dragon.tickCount;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (landing && dragon.onGround()) {
            dragon.setFlying(false);
            return;
        }
        if (isStuck()) {
            stuckTicks = 0;
            if (landing || ++stuckRetargets > MAX_STUCK_RETARGETS) {
                dragon.setFlying(false);
                return;
            }
            // Blocked by something: turn round and circle the other way
            clockwise = !clockwise;
        }

        Vec3 centre = circleCentre();
        if (!landing && centre == null) landing = true;
        if (!landing) {
            if (--altitudeCheckTicks <= 0) {
                altitudeCheckTicks = ALTITUDE_CHECK_TICKS;
                altitude = cruiseAltitude(centre);
            }
            // Laps still count while the owner is flying, so it comes down soon after they do
            if (lapsFinished(centre) && !isOwnerAirborne()) landing = true;
        }
        Vec3 target;
        double speed = 1.0;
        if (landing) {
            // Kept until touchdown; the stuck check covers the last stretch
            if (landingTarget == null) landingTarget = pickLandingTarget(centre);
            target = landingTarget;
        } else {
            double wobbledRadius = radius * (1 + WOBBLE * Math.sin(2 * lastAngle + wobblePhase));
            target = CirclePath.aimPoint(centre, dragon.position(), wobbledRadius, LEAD_ANGLE, clockwise, altitude);
            double dx = dragon.getX() - centre.x;
            double dz = dragon.getZ() - centre.z;
            speed = catchUpSpeed(Math.sqrt(dx * dx + dz * dz), radius);
        }
        dragon.getMoveControl().setWantedPosition(target.x, target.y, target.z, speed);
    }

    /** What the dragon flies laps around, or null when it has nothing to circle. */
    @Nullable
    private Vec3 circleCentre() {
        // Only spots in this dimension are passed on
        GlobalPos wildHome = dragon.getWildHome();
        BlockPos wildHomeHere = wildHome != null && wildHome.dimension() == dragon.level().dimension() ? wildHome.pos() : null;
        BlockPos beaconHere = dragon.isHomeInThisDimension() ? dragon.getHome().pos() : null;
        LivingEntity owner = followedOwner();
        return circleCentre(wildHomeHere, dragon.getCommand(), beaconHere, owner != null ? owner.position() : null);
    }

    /**
     * A wandering dragon circles its beacon, flying out past the box it walks in; a following one circles its owner,
     * given only when it is following them; a wild one circles where it first appeared.
     */
    @Nullable
    static Vec3 circleCentre(@Nullable BlockPos wildHome, DragonCommand command, @Nullable BlockPos beacon, @Nullable Vec3 owner) {
        if (command == DragonCommand.WANDER) return beacon != null ? Vec3.atBottomCenterOf(beacon) : null;
        if (owner != null) return owner;
        return wildHome != null ? Vec3.atBottomCenterOf(wildHome) : null;
    }

    /** Too far, or too high above, to keep up with on foot. */
    static boolean shouldChaseOwner(Vec3 dragon, Vec3 owner) {
        return owner.y - dragon.y > CHASE_HEIGHT || dragon.distanceToSqr(owner) > CHASE_DISTANCE * CHASE_DISTANCE;
    }

    /** Speed multiplier for a dragon this far from the centre: cruise near the circle, up to double when far behind. */
    static double catchUpSpeed(double distanceFromCentre, double radius) {
        double share = Mth.clamp((distanceFromCentre - radius - CATCH_UP_MARGIN) / CATCH_UP_RAMP, 0.0, 1.0);
        return 1.0 + share * (MAX_CATCH_UP_SPEED - 1.0);
    }

    /** The owner a tamed dragon is told to follow, when they are here to follow. */
    @Nullable
    private LivingEntity followedOwner() {
        if (!dragon.isTame() || dragon.getCommand() != DragonCommand.FOLLOW) return null;
        LivingEntity owner = dragon.getOwner();
        return owner != null && !owner.isSpectator() ? owner : null;
    }

    private boolean isOwnerAirborne() {
        LivingEntity owner = followedOwner();
        if (owner == null) return false;
        int ground = dragon.level().getHeight(Heightmap.Types.MOTION_BLOCKING, owner.getBlockX(), owner.getBlockZ());
        return owner.getY() - ground > OWNER_AIRBORNE_HEIGHT;
    }

    /** One steady height for the laps: clear of the ground and trees under the circle, and above a flying owner. */
    private double cruiseAltitude(Vec3 centre) {
        double clearOfGround = highestGroundAround(centre, radius * (1 + WOBBLE)) + heightAboveGround;
        LivingEntity owner = followedOwner();
        return owner != null ? Math.max(clearOfGround, owner.getY() + OWNER_CLEARANCE) : clearOfGround;
    }

    /** Adds this tick's progress round the centre and says whether the laps are done. */
    private boolean lapsFinished(Vec3 centre) {
        Vec3 pos = dragon.position();
        double angle = CirclePath.angleAround(centre, pos);
        double dx = pos.x - centre.x;
        double dz = pos.z - centre.z;
        if (dx * dx + dz * dz > Mth.square(radius * COUNTING_RADIUS_SHARE)) {
            angleLeft -= CirclePath.progress(lastAngle, angle, clockwise);
        }
        lastAngle = angle;
        return angleLeft <= 0;
    }

    private boolean isStuck() {
        Vec3 pos = dragon.position();
        if (pos.distanceToSqr(lastPos) < STUCK_DISTANCE_SQR) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPos = pos;
        }
        return stuckTicks > STUCK_TICKS;
    }

    private FlightStats stats() {
        return dragon.getStats().flight();
    }

    /**
     * The highest block top under the circle and under the dragon itself, trees included, so one level lap clears it
     * all. Unloaded columns are skipped.
     */
    private int highestGroundAround(Vec3 centre, double ringRadius) {
        Level level = dragon.level();
        int highest = level.getHeight(Heightmap.Types.MOTION_BLOCKING, dragon.getBlockX(), dragon.getBlockZ());
        for (int i = 0; i < HEIGHT_SAMPLES; i++) {
            double angle = i * Math.PI * 2 / HEIGHT_SAMPLES;
            BlockPos column = BlockPos.containing(centre.x + ringRadius * Math.cos(angle), 0, centre.z + ringRadius * Math.sin(angle));
            if (level.isLoaded(column)) {
                highest = Math.max(highest, level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()));
            }
        }
        return highest;
    }

    /** Dry ground near the centre the dragon can see; failing that, a spot a short way ahead; failing that, keep descending. */
    private Vec3 pickLandingTarget(@Nullable Vec3 centre) {
        if (centre != null) {
            RandomSource random = dragon.getRandom();
            for (int i = 0; i < TARGET_ATTEMPTS; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = random.nextDouble() * LANDING_RADIUS;
                Vec3 ground = dryGroundAt(centre.x + Math.cos(angle) * distance, centre.z + Math.sin(angle) * distance);
                if (ground != null && isAllowed(ground) && hasClearLine(ground)) return ground;
            }
        }
        for (int i = 0; i < TARGET_ATTEMPTS; i++) {
            Vec3 ahead = pointAhead(CONE_HALF_ANGLE, 6.0, 10.0);
            int ground = groundBelow(BlockPos.containing(ahead));
            if (ground == Integer.MIN_VALUE) continue;
            Vec3 candidate = new Vec3(ahead.x, ground + 1, ahead.z);
            if (hasClearLine(candidate)) return candidate;
        }
        Vec3 ahead = pointAhead(CONE_HALF_ANGLE, 6.0, 10.0);
        return new Vec3(ahead.x, dragon.getY() - 4, ahead.z);
    }

    /** The top of the ground at x, z, or null if it is water or not loaded. Leaves do not count, so it never lands in a tree. */
    @Nullable
    private Vec3 dryGroundAt(double x, double z) {
        Level level = dragon.level();
        BlockPos column = BlockPos.containing(x, 0, z);
        if (!level.isLoaded(column)) return null;
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        if (!level.getFluidState(new BlockPos(column.getX(), top - 1, column.getZ())).isEmpty()) return null;
        return new Vec3(column.getX() + 0.5, top, column.getZ() + 0.5);
    }

    private Vec3 pointAhead(float halfAngle, double minDistance, double maxDistance) {
        float yaw = dragon.getYRot() + Mth.nextFloat(dragon.getRandom(), -halfAngle, halfAngle);
        double distance = Mth.nextDouble(dragon.getRandom(), minDistance, maxDistance);
        double radians = Math.toRadians(yaw);
        return dragon.position().add(-Math.sin(radians) * distance, 0, Math.cos(radians) * distance);
    }

    /** Y of the highest solid block at or below the column's start, or MIN_VALUE if none within reach. */
    private int groundBelow(BlockPos start) {
        Level level = dragon.level();
        BlockPos.MutableBlockPos pos = start.mutable();
        for (int i = 0; i < GROUND_SEARCH_DEPTH; i++) {
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return pos.getY();
            pos.move(0, -1, 0);
        }
        return Integer.MIN_VALUE;
    }

    /** Inside the beacon box when the dragon is wandering, so it lands where it is allowed to walk. */
    private boolean isAllowed(Vec3 pos) {
        return !dragon.hasRestriction() || dragon.isWithinRestriction(BlockPos.containing(pos));
    }

    private boolean hasClearLine(Vec3 to) {
        HitResult hit = dragon.level().clip(new ClipContext(dragon.getEyePosition(), to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
        return hit.getType() == HitResult.Type.MISS;
    }
}
