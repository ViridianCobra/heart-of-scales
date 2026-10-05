package net.basilisk.heartofscales.block;

import net.basilisk.heartofscales.menu.DragonBeaconMenu;
import net.basilisk.heartofscales.registry.ModItems;
import net.basilisk.heartofscales.roster.BeaconLang;
import net.basilisk.heartofscales.roster.BeaconRow;
import net.basilisk.heartofscales.roster.BeaconRows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Two blocks tall, built like a door: a lower and an upper half that live and die together.
 * Each half draws its own part of the model; the lower half is the position dragons are tied to.
 */
public class DragonBeaconBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public DragonBeaconBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    /** Position of the lower half, whichever half was given. */
    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        boolean roomAbove = pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context);
        return roomAbove ? defaultBlockState() : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        // Blocks get the click before held items, so step aside and let the staff assign homes
        if (player.isHolding(ModItems.DRAGON_STAFF.get())) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Opens the list of dragons that live here. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            BlockPos lower = lowerPos(state, pos);
            List<BeaconRow> rows = BeaconRows.build(serverPlayer, GlobalPos.of(level.dimension(), lower));
            serverPlayer.openMenu(
                    new SimpleMenuProvider((windowId, inventory, p) ->
                            new DragonBeaconMenu(windowId, ContainerLevelAccess.create(level, lower), rows),
                            Component.translatable(BeaconLang.TITLE)),
                    buf -> BeaconRow.writeList(buf, rows));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbourPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean towardsOtherHalf = direction == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        if (towardsOtherHalf && !(neighbourState.is(this) && neighbourState.getValue(HALF) != half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Breaking the upper half in creative would otherwise make the lwer half drop an itemo as it follows
        if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, below, Block.getId(belowState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
