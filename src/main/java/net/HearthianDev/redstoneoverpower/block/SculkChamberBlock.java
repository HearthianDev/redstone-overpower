package net.HearthianDev.redstoneoverpower.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.redstone.Orientation;
import net.HearthianDev.redstoneoverpower.block.entity.SculkChamberBlockEntity;
import net.HearthianDev.redstoneoverpower.block.enums.SculkChamberMode;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.SCULK_CHAMBER_BLOCK_ENTITY;

public class SculkChamberBlock extends BaseEntityBlock {
    public static final MapCodec<SculkChamberBlock> CODEC = simpleCodec(SculkChamberBlock::new);
    public static final EnumProperty<SculkChamberMode> MODE;

    private static final float[] RESONATION_NOTE_PITCHES = Util.make(new float[16], frequency -> {
        int[] is = new int[]{0, 0, 2, 4, 6, 7, 9, 10, 12, 14, 15, 18, 19, 21, 22, 24};
        for (int i = 0; i < 16; ++i) {
            frequency[i] = NoteBlock.getPitchFromNote(is[i]);
        }
    });

    public SculkChamberBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MODE, SculkChamberMode.LISTEN)
        );
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new SculkChamberBlockEntity(pos, state);
    }

    @Override
    public void tick(@NonNull BlockState state, @NonNull ServerLevel world, @NonNull BlockPos pos, @NonNull RandomSource random) {
        if (getMode(state) != SculkChamberMode.CHARGED) {
            if (getMode(state) == SculkChamberMode.COOLDOWN) {
                world.setBlock(pos, state.setValue(MODE, world.hasNeighborSignal(pos) ? SculkChamberMode.ISOLATED : SculkChamberMode.LISTEN), Block.UPDATE_CLIENTS);
                world.playSound(null, pos, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0f, world.getRandom().nextFloat() * 0.2f + 0.8f);
            }
            return;
        }
        SculkChamberBlock.setCooldown(world, pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, @NonNull BlockState state, @NonNull BlockEntityType<T> type) {
        if (!world.isClientSide()) {
            return SculkChamberBlock.createTickerHelper(
                type,
                SCULK_CHAMBER_BLOCK_ENTITY,
                (worldx, _, _, blockEntity) -> VibrationSystem.Ticker.tick(worldx, blockEntity.getVibrationData(), blockEntity.getVibrationUser())
            );
        }
        return null;
    }

    @Override
    protected void neighborChanged(@NonNull BlockState state, Level world, @NonNull BlockPos pos, @NonNull Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        if (world.isClientSide()) {
            return;
        }
        boolean isPowered = world.hasNeighborSignal(pos);

        if (!isPowered && getMode(state) == SculkChamberMode.ISOLATED) {
            world.setBlock(pos, state.setValue(MODE, SculkChamberMode.LISTEN), Block.UPDATE_ALL);
        } else if (isPowered && getMode(state) == SculkChamberMode.LISTEN) {
            world.setBlock(pos, state.setValue(MODE, SculkChamberMode.ISOLATED), Block.UPDATE_ALL);
        }
        if ((state.getValue(MODE) == SculkChamberMode.CHARGED) && world.hasNeighborSignal(pos)) {
            SculkChamberBlock.setCooldown(world, pos, state);

            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!(blockEntity instanceof SculkChamberBlockEntity sculkChamberBlockEntity)) {
                return;
            }
            SculkChamberBlock.updateNeighbors(world, pos, state);
            world.gameEvent(
                    VibrationSystem.getResonanceEventByFrequency(sculkChamberBlockEntity.getLastVibrationFrequency()),
                    pos,
                    GameEvent.Context.of(state)
            );
        }
    }

    public static void setCooldown(Level world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state.setValue(MODE, SculkChamberMode.COOLDOWN), Block.UPDATE_ALL);
        world.scheduleTick(pos, state.getBlock(), getCooldownTime());
        SculkChamberBlock.updateNeighbors(world, pos, state);
    }

    private static void updateNeighbors(Level world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        world.updateNeighborsAt(pos, block);
        world.updateNeighborsAt(pos.below(), block);
    }

    public static SculkChamberMode getMode(BlockState state) {
        return state.getValue(MODE);
    }

    public static boolean canStoreSound(BlockState state) {
        return SculkChamberBlock.getMode(state) == SculkChamberMode.LISTEN || SculkChamberBlock.getMode(state) == SculkChamberMode.ISOLATED;
    }

    public static int getCooldownTime() {
        return 40;
    }

    public void setCharged(@Nullable Entity sourceEntity, Level world, BlockPos pos, BlockState state, int frequency) {
        world.setBlock(pos, state.setValue(MODE, SculkChamberMode.CHARGED), Block.UPDATE_ALL);
        SculkChamberBlock.updateNeighbors(world, pos, state);
        SculkChamberBlock.tryResonate(sourceEntity, world, pos, frequency);
        world.gameEvent(sourceEntity, GameEvent.SCULK_SENSOR_TENDRILS_CLICKING, pos);
        world.playSound(
            null,
            (double)pos.getX() + 0.5,
            (double)pos.getY() + 0.5,
            (double)pos.getZ() + 0.5,
            SoundEvents.SCULK_CLICKING,
            SoundSource.BLOCKS,
            1.0f,
            world.getRandom().nextFloat() * 0.2f + 0.8f
        );
    }

    public static void tryResonate(@Nullable Entity sourceEntity, Level world, BlockPos pos, int frequency) {
        for (Direction direction : Direction.values()) {
            BlockPos blockPos = pos.relative(direction);
            BlockState blockState = world.getBlockState(blockPos);
            if (!blockState.is(BlockTags.VIBRATION_RESONATORS)) continue;
            world.gameEvent(VibrationSystem.getResonanceEventByFrequency(frequency), blockPos, GameEvent.Context.of(sourceEntity, blockState));
            world.playSound(null, blockPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, RESONATION_NOTE_PITCHES[frequency]);
        }
    }

    static {
        MODE = EnumProperty.create("mode", SculkChamberMode.class);
    }
}
