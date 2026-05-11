package com.squinchmods.redstonebackport.block;

import com.squinchmods.redstonebackport.Platform;
import com.squinchmods.redstonebackport.blockentity.CrafterBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public class CrafterBlock extends BaseEntityBlock {
  public static final DirectionProperty FACING = BlockStateProperties.FACING;
  public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
  public static final BooleanProperty CRAFTING = BooleanProperty.create("crafting");

  public CrafterBlock(Properties properties) {
    super(properties);
    this.registerDefaultState(
        (BlockState)
            ((BlockState)
                    ((BlockState)
                            ((BlockState) this.stateDefinition.any())
                                .setValue(FACING, Direction.NORTH))
                        .setValue(TRIGGERED, false))
                .setValue(CRAFTING, false));
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    Direction facing = context.getNearestLookingDirection().getOpposite();
    return this.defaultBlockState().setValue(FACING, facing);
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, TRIGGERED, CRAFTING);
  }

  @Override
  public void neighborChanged(
      BlockState state,
      Level level,
      BlockPos pos,
      Block block,
      BlockPos fromPos,
      boolean isMoving) {
    if (!level.isClientSide) {
      boolean powered = level.hasNeighborSignal(pos);
      boolean triggered = state.getValue(TRIGGERED);
      if (powered && !triggered) {
        level.setBlock(pos, state.setValue(TRIGGERED, true), 3);
        level.scheduleTick(pos, this, 1);
      } else if (!powered && triggered) {
        level.setBlock(pos, state.setValue(TRIGGERED, false), 3);
      }
    }
  }

  @Override
  public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (state.getValue(CRAFTING)) {
      level.setBlock(pos, state.setValue(CRAFTING, false), 3);
      return;
    }
    if (level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter) {
      crafter.tryCraftAndEject();
    }
  }

  @Override
  public InteractionResult use(
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit) {
    if (level.isClientSide) {
      return InteractionResult.SUCCESS;
    } else {
      MenuProvider provider = this.getMenuProvider(state, level, pos);
      if (provider != null && player instanceof ServerPlayer serverPlayer) {
        Platform.openScreen(serverPlayer, provider, pos);
      }

      return InteractionResult.CONSUME;
    }
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  @Nullable public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new CrafterBlockEntity(pos, state);
  }

  @Override
  @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return null;
  }

  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
  }

  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    return state.rotate(mirror.getRotation(state.getValue(FACING)));
  }

  @Override
  public VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.block();
  }
}
