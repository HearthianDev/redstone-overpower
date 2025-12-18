package net.HearthianDev.redstoneoverpower.mixin.block.piston;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonStructureResolver.class)
public abstract class PistonHandlerMixin {
  @Shadow @Final
  private Level level;
  @Shadow @Final
  private Direction pushDirection;

  @Shadow
  private boolean addBlockLine(BlockPos blockPos, Direction direction) {
    return false;
  }
  @Shadow
  private static boolean canStickToEachOther(BlockState state, BlockState adjacentState) {
    return false;
  }

  @Inject(method = "isSticky", at = @At(value = "HEAD"), cancellable = true)
  private static void isBlockStickyMixin(BlockState state, CallbackInfoReturnable<Boolean> cir) {
    if(state.is(BlockTags.CHAINS)) {
      cir.setReturnValue(true);
      cir.cancel();
    }
  }

  @Unique
  private boolean canConnectChain(BlockState state, Direction dir) {
    return switch (dir) {
      case UP, DOWN -> state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
      case NORTH, SOUTH -> state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Z;
      case WEST, EAST -> state.getValue(BlockStateProperties.AXIS) == Direction.Axis.X;
    };
  }

  @Unique
  private boolean isAdjacentBlockStuck(BlockState state, BlockState adjacentState, Direction dir) {
    if (state.is(BlockTags.CHAINS) && adjacentState.is(BlockTags.CHAINS) && dir.getAxis() == pushDirection.getAxis()) {
      return canConnectChain(state, pushDirection.getOpposite()) && canConnectChain(adjacentState, pushDirection);
    }
    if (state.is(BlockTags.CHAINS) && !canConnectChain(state, dir)) {
      return false;
    }
    if (adjacentState.is(BlockTags.CHAINS)
      && !state.is(Blocks.SLIME_BLOCK)
      && !state.is(Blocks.HONEY_BLOCK)
      && !canConnectChain(adjacentState, dir.getOpposite())
    ) {
      return false;
    }

    return canStickToEachOther(state, adjacentState);
  }

   /**
   * @author Juarrin
   * @reason different isAdjacentBlockStuck invocation
   */
  @Overwrite
  private boolean addBranchingBlocks(BlockPos pos) {
    BlockState blockState = this.level.getBlockState(pos);
    for (Direction direction : Direction.values()) {
      BlockPos blockPos;
      if (direction.getAxis() == this.pushDirection.getAxis()
        || !isAdjacentBlockStuck(blockState, this.level.getBlockState(blockPos = pos.relative(direction)), direction)
        || this.addBlockLine(blockPos, direction)
      ) continue;

      return false;
    }

    return true;
  }

  @Redirect(
    method = "addBlockLine",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;canStickToEachOther(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z"
    )
  )
  private boolean isAdjacentBlockStuckRedirect(BlockState state, BlockState adjacentState, BlockPos pos, Direction dir) {
    return isAdjacentBlockStuck(state, adjacentState, this.pushDirection);
  }
}