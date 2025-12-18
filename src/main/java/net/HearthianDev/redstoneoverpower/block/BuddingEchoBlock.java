package net.HearthianDev.redstoneoverpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.SculkBehaviour;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.NonNull;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.*;

public class BuddingEchoBlock extends BuddingAmethystBlock implements SculkBehaviour {
    public BuddingEchoBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void tick(@NonNull BlockState state, @NonNull ServerLevel world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        super.tick(state, world, pos, random);
    }

    @Override
    protected void randomTick(@NonNull BlockState state, @NonNull ServerLevel world, @NonNull BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        Direction direction = UPDATE_SHAPE_ORDER[random.nextInt(UPDATE_SHAPE_ORDER.length)];
        BlockPos blockPos = pos.relative(direction);
        BlockState blockState = world.getBlockState(blockPos);
        Block block = null;
        if (BuddingEchoBlock.canClusterGrowAtState(blockState)) {
            block = SMALL_ECHO_BUD_BLOCK;
        } else if (blockState.is(SMALL_ECHO_BUD_BLOCK) && blockState.getValue(EchoClusterBlock.FACING) == direction) {
            block = MEDIUM_ECHO_BUD_BLOCK;
        } else if (blockState.is(MEDIUM_ECHO_BUD_BLOCK) && blockState.getValue(EchoClusterBlock.FACING) == direction) {
            block = LARGE_ECHO_BUD_BLOCK;
        } else if (blockState.is(LARGE_ECHO_BUD_BLOCK) && blockState.getValue(EchoClusterBlock.FACING) == direction) {
            block = ECHO_CLUSTER_BLOCK;
        }
        if (block != null) {
            BlockState blockState2 = block.defaultBlockState().setValue(EchoClusterBlock.FACING, direction)
                .setValue(EchoClusterBlock.WATERLOGGED, blockState.getFluidState().getType() == Fluids.WATER);
            world.setBlockAndUpdate(blockPos, blockState2);
        }
    }

    @Override
    public int attemptUseCharge(SculkSpreader.@NonNull ChargeCursor cursor, @NonNull LevelAccessor world, @NonNull BlockPos catalystPos, @NonNull RandomSource random, @NonNull SculkSpreader spreadManager, boolean shouldConvertToBlock) {
        return 0;
    }
}
