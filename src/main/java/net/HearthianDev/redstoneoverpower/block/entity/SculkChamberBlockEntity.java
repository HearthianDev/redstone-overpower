package net.HearthianDev.redstoneoverpower.block.entity;

import net.HearthianDev.redstoneoverpower.block.SculkChamberBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.*;

public class SculkChamberBlockEntity extends BlockEntity implements GameEventListener.Provider<VibrationSystem.Listener>, VibrationSystem {
    private VibrationSystem.Data listenerData;
    private final VibrationSystem.Listener listener;
    private final VibrationSystem.User callback;
    private int lastVibrationFrequency;

    protected SculkChamberBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        this.callback = this.createCallback();
        this.listenerData = new VibrationSystem.Data();
        this.listener = new VibrationSystem.Listener(this);
    }

    public SculkChamberBlockEntity(BlockPos pos, BlockState state) {
        this(SCULK_CHAMBER_BLOCK_ENTITY, pos, state);
    }

    public VibrationSystem.User createCallback() {
        return new SculkChamberBlockEntity.VibrationCallback(this.getBlockPos());
    }

    public void loadAdditional(@NonNull ValueInput view) {
        super.loadAdditional(view);
        this.lastVibrationFrequency = view.getIntOr("last_vibration_frequency", 0);
        this.listenerData = view.read("listener", Data.CODEC).orElseGet(VibrationSystem.Data::new);
    }

    protected void saveAdditional(@NonNull ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("last_vibration_frequency", this.lastVibrationFrequency);
        view.store("listener", Data.CODEC, this.listenerData);
    }

    @Override
    public @NonNull Data getVibrationData() {
        return this.listenerData;
    }

    @Override
    public @NonNull User getVibrationUser() {
        return this.callback;
    }

    @Override
    public Listener getListener() {
        return this.listener;
    }

    public int getLastVibrationFrequency() {
        return this.lastVibrationFrequency;
    }

    public void setLastVibrationFrequency(int lastVibrationFrequency) {
        this.lastVibrationFrequency = lastVibrationFrequency;
    }

    protected class VibrationCallback implements VibrationSystem.User {
        public static final int RANGE = 16;
        protected final BlockPos pos;
        private final PositionSource positionSource;

        public VibrationCallback(BlockPos pos) {
            this.pos = pos;
            this.positionSource = new BlockPositionSource(pos);
        }

        public int getListenerRadius() {
            return RANGE;
        }

        public @NonNull PositionSource getPositionSource() {
            return this.positionSource;
        }

        // Decides if the block can accept a sound
        @Override
        public boolean canReceiveVibration(@NonNull ServerLevel world, @NonNull BlockPos pos, @NonNull Holder<GameEvent> event, GameEvent.@NonNull Context emitter) {
            BlockState blockState = SculkChamberBlockEntity.this.getBlockState();

            if (blockState.getBlock() instanceof SculkChamberBlock) {
                return (!pos.equals(this.pos)
                    || event != GameEvent.BLOCK_DESTROY
                    && event != GameEvent.BLOCK_PLACE)
                    && SculkChamberBlock.canStoreSound(blockState);
            }

            return !pos.equals(this.pos) || event != GameEvent.BLOCK_DESTROY && event != GameEvent.BLOCK_PLACE;
        }

        @Override
        public void onReceiveVibration(@NonNull ServerLevel world, @NonNull BlockPos pos, @NonNull Holder<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity entity, float distance) {
            BlockState blockState = SculkChamberBlockEntity.this.getBlockState();

            if (SculkChamberBlock.canStoreSound(blockState)) {
                SculkChamberBlockEntity.this.setLastVibrationFrequency(VibrationSystem.getGameEventFrequency(event));
                if (blockState.getBlock() instanceof SculkChamberBlock sculkChamberBlock) {
                    sculkChamberBlock.setCharged(sourceEntity, world, this.pos, blockState, SculkChamberBlockEntity.this.getLastVibrationFrequency());
                }
            }
        }

        public boolean canTriggerAvoidVibration() {
            return true;
        }


        public void onDataChanged() {
            SculkChamberBlockEntity.this.setChanged();
        }

        public boolean requiresAdjacentChunksToBeTicking() {
            return true;
        }
    }
}
