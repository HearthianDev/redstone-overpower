package net.HearthianDev.redstoneoverpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class SculkPulserBlock extends Block {
    public static final BooleanProperty POWERED;

    public SculkPulserBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, @NonNull BlockPos pos, @NonNull Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        boolean isPowered = world.hasNeighborSignal(pos);
        int power = world.getBestNeighborSignal(pos);

        if (isPowered != state.getValue(POWERED)) {
            if (isPowered) {
                world.blockEvent(pos, this, 0, 0);
                world.gameEvent(
                        VibrationSystem.getResonanceEventByFrequency(power),
                        pos,
                        GameEvent.Context.of(state)
                );
            }
            world.setBlock(pos, state.setValue(POWERED, isPowered), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    static {
        POWERED = BlockStateProperties.POWERED;
    }
}
