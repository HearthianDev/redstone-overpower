package net.HearthianDev.redstoneoverpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.NOTE_BLOCK_SOUND_EVENT;

public class SculkNoteBlock extends Block {
    public static final BooleanProperty POWERED;
    public static final IntegerProperty NOTE;

    public SculkNoteBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(NOTE, 0).setValue(POWERED, false));
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, @NonNull BlockPos pos, @NonNull Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        boolean isPowered = world.hasNeighborSignal(pos);
        if (isPowered != state.getValue(POWERED)) {
            if (isPowered) {
                this.playNote(state, world, pos);
            }
            world.setBlock(pos, state.setValue(POWERED, isPowered), Block.UPDATE_ALL);
        }
    }

    private void playNote(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockState(pos.above()).isAir()) {
            world.blockEvent(pos, this, 0, 0);
            world.gameEvent(
                VibrationSystem.getResonanceEventByFrequency(state.getValue(NOTE) + 1),
                pos,
                GameEvent.Context.of(state)
            );
        }
    }

    @Override
    public @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hit) {
        ItemStack itemStack = player.getItemInHand(player.getUsedItemHand());
        if (itemStack.is(ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS) && hit.getDirection() == Direction.UP) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        state = state.cycle(NOTE);
        world.setBlock(pos, state, Block.UPDATE_ALL);
        this.playNote(state, world, pos);
        player.awardStat(Stats.TUNE_NOTEBLOCK);

        return InteractionResult.CONSUME;
    }

    @Override
    public void attack(@NonNull BlockState state, Level world, @NonNull BlockPos pos, @NonNull Player player) {
        if (world.isClientSide()) {
            return;
        }
        this.playNote(state, world, pos);
        player.awardStat(Stats.PLAY_NOTEBLOCK);
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        int i = state.getValue(NOTE);
        world.addParticle(
            ParticleTypes.NOTE,
            (double)pos.getX() + 0.5,
            (double)pos.getY() + 1.2,
            (double)pos.getZ() + 0.5,
            (double)i / 24.0,
            0.0,
            0.0
        );
        world.playSeededSound(
            null,
            (double)pos.getX() + 0.5,
            (double)pos.getY() + 0.5,
            (double)pos.getZ() + 0.5,
            NOTE_BLOCK_SOUND_EVENT,
            SoundSource.RECORDS,
            3.0f,
            NoteBlock.getPitchFromNote(i),
            world.getRandom().nextLong()
        );

        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, NOTE);
    }

    static {
        POWERED = BlockStateProperties.POWERED;
        NOTE = IntegerProperty.create("note", 0, 14);
    }
}
