package net.HearthianDev.redstoneoverpower.block;

import com.mojang.serialization.MapCodec;
import net.HearthianDev.redstoneoverpower.block.entity.LogicalComparatorBlockEntity;
import net.HearthianDev.redstoneoverpower.block.enums.LogicalComparatorMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;

public class LogicalComparatorBlock extends DiodeBlock implements EntityBlock {
    public static final MapCodec<LogicalComparatorBlock> CODEC = simpleCodec(LogicalComparatorBlock::new);

    public static final EnumProperty<LogicalComparatorMode> MODE;

    public LogicalComparatorBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, false)
            .setValue(MODE, LogicalComparatorMode.AND)
        );
    }

    @Override
    protected @NonNull MapCodec<? extends DiodeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new LogicalComparatorBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE, POWERED);
    }

    @Override
    protected int getDelay(@NonNull BlockState state) {
        return 2;
    }

    @Override
    public @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            state = state.cycle(MODE);
            world.setBlock(pos, state, 3);
            this.update(world, pos, state);

            return InteractionResult.SUCCESS;
        }
    }

    protected boolean shouldTurnOn(Level world, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        Direction rightDir = direction.getClockWise();
        Direction leftDir = direction.getCounterClockWise();
        int rightInput = world.getControlInputSignal(pos.relative(rightDir), rightDir, this.sideInputDiodesOnly());
        int leftInput = world.getControlInputSignal(pos.relative(leftDir), leftDir, this.sideInputDiodesOnly());
        boolean right = rightInput > 0;
        boolean left = leftInput > 0;

        return switch(state.getValue(MODE)) {
            case AND -> right && left;
            case OR -> right || left;
            case XOR -> right != left;
            case NAND -> !(right && left);
            case NOR -> !(right || left);
            case XNOR -> right == left;
        };
    }

    private void update(Level world, BlockPos pos, BlockState state) {
        int i = this.shouldTurnOn(world, pos, state) ? 15 : 0;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        int j = 0;
        if (blockEntity instanceof LogicalComparatorBlockEntity logicalComparatorBlockEntity) {
            j = logicalComparatorBlockEntity.getOutputSignal();
            logicalComparatorBlockEntity.setOutputSignal(i);
        }

        if (j != i) {
            boolean bl = this.shouldTurnOn(world, pos, state);
            boolean bl2 = state.getValue(POWERED);
            if (bl2 && !bl) {
                world.setBlock(pos, state.setValue(POWERED, false), 2);
            } else if (!bl2 && bl) {
                world.setBlock(pos, state.setValue(POWERED, true), 2);
            }

            this.updateNeighborsInFront(world, pos, state);
        }
    }

    static {
        MODE = EnumProperty.create("mode", LogicalComparatorMode.class);
    }
}
