package net.basilisk.heartofscales.block;

import net.basilisk.heartofscales.block.entity.NestBlockEntity;
import net.basilisk.heartofscales.genome.DragonGenome;
import net.basilisk.heartofscales.item.DragonEggItem;
import net.basilisk.heartofscales.registry.ModBlockEntities;
import net.basilisk.heartofscales.registry.ModItems;
import net.basilisk.heartofscales.species.HatchCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class NestBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final BooleanProperty HAS_EGG = BooleanProperty.create("has_egg");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape NEST = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape NEST_WITH_EGG = Shapes.or(NEST, Block.box(4, 3, 4, 12, 15, 12));

    public NestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HAS_EGG, false).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_EGG, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean inWater = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState().setValue(WATERLOGGED, inWater);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HAS_EGG) ? NEST_WITH_EGG : NEST;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.NEST.get()) return null;
        return (tickLevel, pos, tickState, blockEntity) -> ((NestBlockEntity) blockEntity).serverTick();
    }

    // 1.21 splits Block#use into useItemOn (holding something) and useWithoutItem (empty hand)
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(HAS_EGG) || !held.is(ModItems.DRAGON_EGG.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof NestBlockEntity nest)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // Set on both sides so the client's predicted state renders with the right tint on the first frame
        nest.setEgg(DragonEggItem.genomeOf(held));
        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level.setBlock(pos, state.setValue(HAS_EGG, true), Block.UPDATE_ALL);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(HAS_EGG) || !(level.getBlockEntity(pos) instanceof NestBlockEntity nest)) {
            return InteractionResult.PASS;
        }
        // Also reached while holding a non-egg item; only a truly empty hand touches the egg
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                player.displayClientMessage(tryStartHatching(nest, level, pos, player), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            DragonGenome egg = nest.getEgg() != null ? nest.getEgg() : DragonGenome.defaultGenome();
            nest.setEgg(null);
            level.setBlock(pos, state.setValue(HAS_EGG, false), Block.UPDATE_ALL);
            ItemStack stack = DragonEggItem.withGenome(egg);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Component tryStartHatching(NestBlockEntity nest, Level level, BlockPos pos, Player player) {
        if (nest.isHatching()) return Component.translatable("hatch.heart_of_scales.already");
        Optional<HatchCondition> condition = nest.hatchCondition();
        if (condition.isEmpty()) return Component.translatable("hatch.heart_of_scales.unknown");
        if (!condition.get().test(level, pos)) return Component.translatable(condition.get().translationKey());
        nest.startHatching(player);
        return Component.translatable("hatch.heart_of_scales.started");
    }
}
