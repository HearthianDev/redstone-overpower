package net.HearthianDev.redstoneoverpower.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.*;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.HearthianDev.redstoneoverpower.block.entity.DuctBlockEntity;
import net.HearthianDev.redstoneoverpower.block.enums.PipeType;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.*;

public class DuctBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<DuctBlock> CODEC = simpleCodec(DuctBlock::new);
    public static final BooleanProperty ENABLED;
    public static final EnumProperty<PipeType> NORTH;
    public static final EnumProperty<PipeType> EAST;
    public static final EnumProperty<PipeType> SOUTH;
    public static final EnumProperty<PipeType> WEST;
    public static final EnumProperty<PipeType> UP;
    public static final EnumProperty<PipeType> DOWN;
    public static final EnumProperty<Direction> FACING;
    public static final BooleanProperty WATERLOGGED;
    public static final Map<Direction, EnumProperty<PipeType>> FACING_PROPERTIES;
    private static final Direction[] FACINGS;

    protected final VoxelShape[] facingsToShape;

    public DuctBlock(Properties settings) {
        super(settings);
        this.facingsToShape = this.generateFacingsToShapeMap();
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(ENABLED, true)
            .setValue(WATERLOGGED, false)
            .setValue(NORTH, PipeType.NONE)
            .setValue(EAST, PipeType.NONE)
            .setValue(SOUTH, PipeType.NONE)
            .setValue(WEST, PipeType.NONE)
            .setValue(UP, PipeType.NONE)
            .setValue(DOWN, PipeType.NONE)
            .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    private VoxelShape[] generateFacingsToShapeMap() {
        float radius = 0.25f;

        float f = 0.5f - radius;
        float g = 0.5f + radius;
        VoxelShape voxelShape = Block.box(f * 16.0f, f * 16.0f, f * 16.0f, g * 16.0f, g * 16.0f, g * 16.0f);
        VoxelShape[] voxelShapes = new VoxelShape[FACINGS.length];
        for (int i = 0; i < FACINGS.length; ++i) {
            Direction direction = FACINGS[i];
            voxelShapes[i] = Shapes.box(
                0.5 + Math.min((-radius), (double)direction.getStepX() * 0.5),
                0.5 + Math.min((-radius), (double)direction.getStepY() * 0.5),
                0.5 + Math.min((-radius), (double)direction.getStepZ() * 0.5),
                0.5 + Math.max(radius, (double)direction.getStepX() * 0.5),
                0.5 + Math.max(radius, (double)direction.getStepY() * 0.5),
                0.5 + Math.max(radius, (double)direction.getStepZ() * 0.5)
            );
        }
        VoxelShape[] voxelShapes2 = new VoxelShape[64];
        for (int j = 0; j < 64; ++j) {
            VoxelShape voxelShape2 = voxelShape;
            for (int k = 0; k < FACINGS.length; ++k) {
                if ((j & 1 << k) == 0) continue;
                voxelShape2 = Shapes.or(voxelShape2, voxelShapes[k]);
            }
            voxelShapes2[j] = voxelShape2;
        }

        return voxelShapes2;
    }

    @Override
    public @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter world, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return this.facingsToShape[this.getConnectionMask(state)];
    }

    protected int getConnectionMask(BlockState state) {
        int i = 0;
        for (int j = 0; j < FACINGS.length; ++j) {
            if (state.getValue(FACING_PROPERTIES.get(FACINGS[j])) == PipeType.NONE) continue;
            i |= 1 << j;
        }

        return i;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new DuctBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, @NonNull BlockState state, @NonNull BlockEntityType<T> type) {
        return world.isClientSide() ? null : DuctBlock.createTickerHelper(type, DUCT_BLOCK_ENTITY, DuctBlockEntity::serverTick);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction dir = ctx.getClickedFace().getOpposite();
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());

        return this.defaultBlockState()
            .setValue(DOWN, getSideMode(world, pos.below(), Direction.DOWN, dir))
            .setValue(UP, getSideMode(world, pos.above(), Direction.UP, dir))
            .setValue(NORTH, getSideMode(world, pos.north(), Direction.NORTH, dir))
            .setValue(EAST, getSideMode(world, pos.east(), Direction.EAST, dir))
            .setValue(SOUTH, getSideMode(world, pos.south(), Direction.SOUTH, dir))
            .setValue(WEST, getSideMode(world, pos.west(), Direction.WEST, dir))
            .setValue(FACING, dir)
            .setValue(ENABLED, true)
            .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

/*
    Determines if the side is in, out or none.
    If the adjacent block is a container, and the player has triggered a click with it, should be out
    If the adjacent block is a opposite or a hopper oriented to the block, should be in
    In any other case, should be none
 */
    private PipeType getSideMode(Level world, BlockPos neighborPos, Direction side,  Direction facing) {
        if (world.getBlockEntity(neighborPos) instanceof DuctBlockEntity ductBlockEntity) {
            if (facing.equals(side)) {
                return PipeType.OUT;
            }
            if (ductBlockEntity.getBlockState().getValue(FACING).equals(side.getOpposite())) {
                return PipeType.IN;
            }

            return PipeType.NONE;
        }

        if (world.getBlockEntity(neighborPos) instanceof HopperBlockEntity hopperBlockEntity) {
            if (hopperBlockEntity.getBlockState().getValue(HopperBlock.FACING).equals(side.getOpposite())) {
                return side.getAxis() == Direction.Axis.Y ? PipeType.IN : PipeType.IN_HOPPER;
            }
        }

        return DuctBlockEntity.getInventoryAt(world, neighborPos) != null ? PipeType.OUT : PipeType.NONE;
    }

    @Override
    protected @NonNull BlockState updateShape(BlockState state, @NonNull LevelReader world, @NonNull ScheduledTickAccess tickView, @NonNull BlockPos pos, @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if (!state.canSurvive(world, pos)) {
            ((LevelAccessor) world).scheduleTick(pos, this, 1);

            return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        }

        if (state.getValue(WATERLOGGED)) {
            ((LevelAccessor) world).scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        // If duct/hopper facing here, IN
        if (neighborState.is(DUCT_BLOCK)) {
            if (neighborState.getValue(FACING).equals(direction.getOpposite())) {
                if (state.getValue(FACING).equals(direction)) {
                    return state.setValue(FACING_PROPERTIES.get(direction), PipeType.OUT);
                }

                return state.setValue(FACING_PROPERTIES.get(direction), PipeType.IN);
            }

            PipeType opposite;

            if ((opposite = PipeType.getOpposite(neighborState.getValue(FACING_PROPERTIES.get(direction.getOpposite())))) != null) {
                return state.setValue(FACING_PROPERTIES.get(direction), opposite);
            }

            return state.setValue(FACING_PROPERTIES.get(direction), PipeType.NONE);
        }
        if (neighborState.is(Blocks.HOPPER) && neighborState.getValue(HopperBlock.FACING).equals(direction.getOpposite())) {
            return state.setValue(FACING_PROPERTIES.get(direction), direction.getAxis() == Direction.Axis.Y ? PipeType.IN : PipeType.IN_HOPPER);
        }

        return state.setValue(FACING_PROPERTIES.get(direction), DuctBlockEntity.getInventoryAt((Level) world, neighborPos) != null
                ? PipeType.OUT
                : PipeType.NONE
        );
    }

    @Override
    public @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, Level world, @NonNull BlockPos pos, @NonNull Player player, @NonNull BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.getItemInHand(player.getUsedItemHand()).getItem().asItem().equals(this.asItem()) && world.getBlockEntity(pos.relative(hit.getDirection())) instanceof DuctBlockEntity) {
            world.setBlockAndUpdate(pos, state.setValue(FACING_PROPERTIES.get(hit.getDirection()), PipeType.IN));

            return InteractionResult.CONSUME;
        }

        if (world.getBlockEntity(pos) instanceof DuctBlockEntity ductBlockEntity) {
            player.openMenu(ductBlockEntity);
        }

        return InteractionResult.CONSUME;
    }

    //This method will drop all items onto the ground when the block is broken
    @Override
    public void affectNeighborsAfterRemoval(@NonNull BlockState state, @NonNull ServerLevel world, @NonNull BlockPos pos, boolean moved) {
        Containers.updateNeighboursAfterDestroy(state, world, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(@NonNull BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(@NonNull BlockState state, Level world, @NonNull BlockPos pos, @NonNull Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos));
    }

    @Override
    public void onPlace(BlockState state, @NonNull Level world, @NonNull BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.is(state.getBlock())) {
            return;
        }
        this.updateEnabled(world, pos, state);
    }

    @Override
    protected void neighborChanged(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    private void updateEnabled(Level world, BlockPos pos, BlockState state) {
        boolean isPowered = !world.hasNeighborSignal(pos);

        if (isPowered != state.getValue(ENABLED)) {
            world.setBlock(pos, state.setValue(ENABLED, isPowered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public @NonNull FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED)) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    protected boolean propagatesSkylightDown(@NonNull BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ENABLED, NORTH, EAST, SOUTH, WEST, UP, DOWN, FACING, WATERLOGGED);
    }

    static {
        ENABLED = BlockStateProperties.ENABLED;
        NORTH = EnumProperty.create("north", PipeType.class);
        EAST = EnumProperty.create("east", PipeType.class);
        SOUTH = EnumProperty.create("south", PipeType.class);
        WEST = EnumProperty.create("west", PipeType.class);
        UP = EnumProperty.create("up", PipeType.class);
        DOWN = EnumProperty.create("down", PipeType.class);
        FACING = BlockStateProperties.FACING;
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        FACING_PROPERTIES = ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), directions -> {
            directions.put(Direction.NORTH, NORTH);
            directions.put(Direction.EAST, EAST);
            directions.put(Direction.SOUTH, SOUTH);
            directions.put(Direction.WEST, WEST);
            directions.put(Direction.UP, UP);
            directions.put(Direction.DOWN, DOWN);
        }));
        FACINGS = Direction.values();
    }
}
