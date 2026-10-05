package net.basilisk.heartofscales.entity.ai;

import net.basilisk.heartofscales.entity.DragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The only times a following dragon teleports: stuck for a while (see DragonFollowOwnerGoal), or about to fall outside
 * the area the server runs and freeze. It reappears a little way off, behind the owner where it can, so they see it
 * arrive rather than appear, and only where its whole body fits with nothing solid or liquid inside it.
 */
public final class OwnerCatchUp {
    /** Entities tick out to the simulation distance (in chunks, measured as a square) and freeze one chunk past it. */
    private static final int EDGE_MARGIN_CHUNKS = 1;
    private static final double MIN_DISTANCE = 16.0;
    private static final double MAX_DISTANCE = 24.0;
    /** Degrees either side of straight behind the owner tried first. */
    private static final double BEHIND_SPREAD = 60.0;
    private static final int ATTEMPTS = 12;
    /** Standing room is looked for this far above and below the owner's feet, so it works on hills and in caves. */
    private static final int SCAN_UP = 4;
    private static final int SCAN_DOWN = 8;
    /** A flier with no ground to stand on may reappear in the air this far above the owner. */
    private static final double AIR_MIN_HEIGHT = 4.0;
    private static final double AIR_MAX_HEIGHT = 8.0;

    private OwnerCatchUp() {}

    /** Within a chunk of the last ticking chunk, which leaves two chunks before it would freeze. */
    public static boolean nearSimulationEdge(int dragonChunkX, int dragonChunkZ, int ownerChunkX, int ownerChunkZ, int simulationDistance) {
        int chunks = Math.max(Math.abs(dragonChunkX - ownerChunkX), Math.abs(dragonChunkZ - ownerChunkZ));
        return chunks >= simulationDistance - EDGE_MARGIN_CHUNKS;
    }

    /** Horizontal offset from the owner, {@code angleOffset} degrees round from straight behind them. */
    static Vec3 offsetBehind(float ownerYaw, double angleOffset, double distance) {
        double radians = Math.toRadians(ownerYaw + 180.0 + angleOffset);
        return new Vec3(-Math.sin(radians) * distance, 0.0, Math.cos(radians) * distance);
    }

    /** Moves the dragon to a safe spot a little way from its owner. Returns false, leaving it where it is, if none is found. */
    public static boolean teleportNear(DragonEntity dragon, LivingEntity owner) {
        RandomSource random = dragon.getRandom();
        for (int i = 0; i < ATTEMPTS * 2; i++) {
            Vec3 spot = groundSpot(dragon, owner, owner.position().add(randomOffset(owner, random, i < ATTEMPTS ? BEHIND_SPREAD : 180.0)));
            if (spot != null) {
                moveTo(dragon, owner, spot, false);
                return true;
            }
        }
        if (!dragon.canFly()) return false;
        for (int i = 0; i < ATTEMPTS; i++) {
            Vec3 column = owner.position().add(randomOffset(owner, random, 180.0));
            Vec3 spot = new Vec3(column.x, owner.getY() + Mth.nextDouble(random, AIR_MIN_HEIGHT, AIR_MAX_HEIGHT), column.z);
            if (dragon.level().isLoaded(BlockPos.containing(spot)) && fitsAt(dragon, spot)) {
                moveTo(dragon, owner, spot, true);
                return true;
            }
        }
        return false;
    }

    private static Vec3 randomOffset(LivingEntity owner, RandomSource random, double spread) {
        return offsetBehind(owner.getYRot(), Mth.nextDouble(random, -spread, spread), Mth.nextDouble(random, MIN_DISTANCE, MAX_DISTANCE));
    }

    /** Walkable ground in this column near the owner's height, not on leaves, with room for the whole body; or null. */
    @Nullable
    private static Vec3 groundSpot(DragonEntity dragon, LivingEntity owner, Vec3 column) {
        Level level = dragon.level();
        BlockPos.MutableBlockPos pos = BlockPos.containing(column.x, owner.getY() + SCAN_UP, column.z).mutable();
        if (!level.isLoaded(pos)) return null;
        for (int i = 0; i <= SCAN_UP + SCAN_DOWN; i++, pos.move(Direction.DOWN)) {
            // Walkable also rules out standing in or beside fire, lava, cactus and the like
            if (WalkNodeEvaluator.getPathTypeStatic(dragon, pos) != PathType.WALKABLE) continue;
            if (level.getBlockState(pos.below()).getBlock() instanceof LeavesBlock) continue;
            Vec3 spot = Vec3.atBottomCenterOf(pos);
            if (fitsAt(dragon, spot)) return spot;
        }
        return null;
    }

    /** Nothing solid and no liquid anywhere in the dragon's full-size box there. */
    private static boolean fitsAt(DragonEntity dragon, Vec3 spot) {
        AABB box = dragon.getDimensions(dragon.getPose()).makeBoundingBox(spot);
        return dragon.level().noCollision(dragon, box) && !dragon.level().containsAnyLiquid(box);
    }

    private static void moveTo(DragonEntity dragon, LivingEntity owner, Vec3 spot, boolean inAir) {
        float yaw = (float) (Mth.atan2(owner.getZ() - spot.z, owner.getX() - spot.x) * Mth.RAD_TO_DEG) - 90.0f;
        dragon.setSwimMode(false);
        dragon.setFlying(inAir);
        dragon.moveTo(spot.x, spot.y, spot.z, yaw, 0.0f);
        dragon.setYHeadRot(yaw);
        dragon.setYBodyRot(yaw);
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.resetFallDistance();
        dragon.getNavigation().stop();
    }
}
