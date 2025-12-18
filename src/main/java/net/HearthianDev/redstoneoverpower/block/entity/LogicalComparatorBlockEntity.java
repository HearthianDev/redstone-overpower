package net.HearthianDev.redstoneoverpower.block.entity;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.LOGICAL_COMPARATOR_BLOCK_ENTITY;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

public class LogicalComparatorBlockEntity extends BlockEntity {
    private int outputSignal;

    public LogicalComparatorBlockEntity(BlockPos pos, BlockState state) {
        super(LOGICAL_COMPARATOR_BLOCK_ENTITY, pos, state);
    }

    protected void saveAdditional(@NonNull ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("OutputSignal", this.outputSignal);
    }

    protected void loadAdditional(@NonNull ValueInput view) {
        super.loadAdditional(view);
        this.outputSignal = view.getIntOr("OutputSignal", 0);
    }

    public int getOutputSignal() {
        return this.outputSignal;
    }

    public void setOutputSignal(int outputSignal) {
        this.outputSignal = outputSignal;
    }
}
