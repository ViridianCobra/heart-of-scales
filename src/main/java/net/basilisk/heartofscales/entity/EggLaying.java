package net.basilisk.heartofscales.entity;

import net.basilisk.heartofscales.block.NestBlock;
import net.basilisk.heartofscales.block.entity.DragonEggBlockEntity;
import net.basilisk.heartofscales.block.entity.NestBlockEntity;
import net.basilisk.heartofscales.genome.DragonGenome;
import net.basilisk.heartofscales.item.DragonEggItem;
import net.basilisk.heartofscales.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Where a freshly bred egg goes: an empty nest nearby, else the ground beside the parent, else an item drop. */
public final class EggLaying {
    private static final int NEST_RANGE = 3;
    private static final int GROUND_RANGE = 2;

    private EggLaying() {}

    public static void lay(ServerLevel level, Entity parent, DragonGenome genome) {
        BlockPos origin = parent.blockPosition();
        if (placeInNest(level, origin, genome) || placeOnGround(level, origin, genome)) return;
        parent.spawnAtLocation(DragonEggItem.withGenome(genome));
    }

    private static boolean placeInNest(ServerLevel level, BlockPos origin, DragonGenome genome) {
        BlockPos best = null;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-NEST_RANGE, -1, -NEST_RANGE), origin.offset(NEST_RANGE, 1, NEST_RANGE))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof NestBlock) || state.getValue(NestBlock.HAS_EGG)) continue;
            if (best == null || pos.distSqr(origin) < best.distSqr(origin)) best = pos.immutable();
        }
        if (best == null || !(level.getBlockEntity(best) instanceof NestBlockEntity nest)) return false;
        nest.setEgg(genome);
        level.setBlock(best, level.getBlockState(best).setValue(NestBlock.HAS_EGG, true), Block.UPDATE_ALL);
        return true;
    }

    private static boolean placeOnGround(ServerLevel level, BlockPos origin, DragonGenome genome) {
        BlockPos best = null;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-GROUND_RANGE, -1, -GROUND_RANGE), origin.offset(GROUND_RANGE, 1, GROUND_RANGE))) {
            if (!canHoldEgg(level, pos)) continue;
            // Prefer a spot beside the parent over the block it is standing in
            boolean underParent = pos.getX() == origin.getX() && pos.getZ() == origin.getZ();
            boolean bestUnderParent = best != null && best.getX() == origin.getX() && best.getZ() == origin.getZ();
            if (best == null || (bestUnderParent && !underParent)
                    || (bestUnderParent == underParent && pos.distSqr(origin) < best.distSqr(origin))) {
                best = pos.immutable();
            }
        }
        if (best == null) return false;
        BlockState egg = ModBlocks.DRAGON_EGG.get().defaultBlockState();
        level.setBlock(best, egg, Block.UPDATE_ALL);
        if (level.getBlockEntity(best) instanceof DragonEggBlockEntity blockEntity) {
            blockEntity.setGenome(genome);
            level.sendBlockUpdated(best, egg, egg, Block.UPDATE_CLIENTS);
        }
        return true;
    }

    private static boolean canHoldEgg(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(pos).isAir() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
