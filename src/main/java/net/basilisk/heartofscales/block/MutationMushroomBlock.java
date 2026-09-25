package net.basilisk.heartofscales.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A small glowing mushroom. Sits on any block with a solid top, so it works on cave stone as well as soil. */
public class MutationMushroomBlock extends BushBlock {
    public static final MapCodec<MutationMushroomBlock> CODEC = simpleCodec(MutationMushroomBlock::new);
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 6, 11);

    public MutationMushroomBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isSolidRender(level, pos);
    }
}
