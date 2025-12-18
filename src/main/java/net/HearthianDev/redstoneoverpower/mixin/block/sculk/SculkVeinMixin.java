package net.HearthianDev.redstoneoverpower.mixin.block.sculk;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.SculkVeinBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.BUDDING_ECHO_BLOCK;
import static net.HearthianDev.redstoneoverpower.utils.Initialiser.SCULK_NOTE_BLOCK;

@Mixin(SculkVeinBlock.class)
public class SculkVeinMixin {

    @Unique
    private LevelAccessor world;
    @Unique
    private BlockPos pos;

    @Inject(
        method = "attemptPlaceSculk",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void injected(SculkSpreader spreadManager, LevelAccessor world, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) BlockPos arg1) {
        this.world = world;
        this.pos = arg1;
    }

    @Redirect(method = "attemptPlaceSculk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    public BlockState convertToBlockMixin(Block instance) {
        if (world.getBlockState(pos).is(Blocks.BUDDING_AMETHYST)) {
            return BUDDING_ECHO_BLOCK.defaultBlockState();
        } else if (world.getBlockState(pos).is(Blocks.NOTE_BLOCK)) {
            return SCULK_NOTE_BLOCK.defaultBlockState();
        } else {
            return instance.defaultBlockState();
        }
    }
}
