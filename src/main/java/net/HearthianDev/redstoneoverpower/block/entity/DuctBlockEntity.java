package net.HearthianDev.redstoneoverpower.block.entity;

import net.HearthianDev.redstoneoverpower.block.DuctBlock;
import net.HearthianDev.redstoneoverpower.block.enums.PipeType;
import net.HearthianDev.redstoneoverpower.block.screen.DuctScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.IntStream;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.DUCT_BLOCK_ENTITY;

public class DuctBlockEntity extends RandomizableContainerBlockEntity {
  public static final int TRANSFER_COOLDOWN = 8;
  public static final int INVENTORY_SIZE = 1;
  public static final int FILTER_ENABLED = 1;
  public static final int FILTER_DISABLED = 0;
  public static final ArrayList<Direction> TRANSFER_PRIORITY = new ArrayList<>(Arrays.asList(
    Direction.DOWN,
    Direction.NORTH,
    Direction.EAST,
    Direction.SOUTH,
    Direction.WEST,
    Direction.UP
  ));

  private NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
  private int transferCooldown = -1;
  private long lastTickTime;
  protected final ContainerData propertyDelegate;
  protected int slotState;

  public DuctBlockEntity(BlockPos pos, BlockState state) {
    super(DUCT_BLOCK_ENTITY, pos, state);

    this.slotState = FILTER_DISABLED;
    this.propertyDelegate = new ContainerData() {
      public int get(int index) {
        return index == 0 ? slotState : FILTER_DISABLED;
      }

      public void set(int index, int value) {
        if (index == 0) {
          setSlotState(value);
        }
      }

      public int getCount() {
        return INVENTORY_SIZE;
      }
    };
  }

  public void setSlotState (int value) {
    this.slotState = value;
  }

  @Override
  protected @NonNull AbstractContainerMenu createMenu(int syncId, @NonNull Inventory playerInventory) {
    return new DuctScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
  }

  @Override
  public void loadAdditional(@NonNull ValueInput view) {
    super.loadAdditional(view);
    this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    if (!this.tryLoadLootTable(view)) {
      ContainerHelper.loadAllItems(view, this.inventory);
    }
    this.transferCooldown = view.getIntOr("TransferCooldown", 0);
    this.setSlotState(view.getIntOr("slot_state", 0));
  }

  @Override
  protected void saveAdditional(@NonNull ValueOutput view) {
    super.saveAdditional(view);
    if (!this.trySaveLootTable(view)) {
      ContainerHelper.saveAllItems(view, this.inventory);
    }
    view.putInt("TransferCooldown", this.transferCooldown);
    view.putInt("slot_state", this.slotState);
  }

  @Override
  public int getContainerSize() {
    return this.inventory.size();
  }

  @Override
  public @NonNull ItemStack removeItem(int slot, int amount) {
    this.unpackLootTable(null);
    return ContainerHelper.removeItem(this.getItems(), slot, amount);
  }

  @Override
  public void setItem(int slot, @NonNull ItemStack stack) {
    this.unpackLootTable(null);
    this.getItems().set(slot, stack);
    if (stack.getCount() > this.getMaxStackSize()) {
      stack.setCount(this.getMaxStackSize());
    }
  }

  @Override
  protected @NonNull Component getDefaultName() {
    return Component.translatable("container.redstoneoverpower.duct");
  }

  public static void serverTick(Level world, BlockPos pos, BlockState state, DuctBlockEntity blockEntity) {
    --blockEntity.transferCooldown;
    blockEntity.lastTickTime = world.getGameTime();
    if (!blockEntity.needsCooldown()) {
      blockEntity.setTransferCooldown(0);
      DuctBlockEntity.insertMain(world, pos, state, blockEntity);
    }
  }

  @Nullable
  public static Container getInventoryAt(Level world, BlockPos pos) {
    return DuctBlockEntity.getInventoryAt(world, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
  }

  @Nullable
  private static Container getInventoryAt(Level world, double x, double y, double z) {
    List<Entity> list;
    BlockEntity blockEntity;
    Container inventory = null;
    BlockPos blockPos = BlockPos.containing(x, y, z);
    BlockState blockState = world.getBlockState(blockPos);
    Block block = blockState.getBlock();
    if (block instanceof WorldlyContainerHolder) {
      inventory = ((WorldlyContainerHolder) block).getContainer(blockState, world, blockPos);
    } else if (blockState.hasBlockEntity() && (blockEntity = world.getBlockEntity(blockPos)) instanceof Container && (inventory = (Container) blockEntity) instanceof ChestBlockEntity && block instanceof ChestBlock) {
      inventory = ChestBlock.getContainer((ChestBlock)block, blockState, world, blockPos, true);
    }
    if (inventory == null && !(list = world.getEntities((Entity) null, new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5), EntitySelector.CONTAINER_ENTITY_SELECTOR)).isEmpty()) {
      inventory = (Container) list.get(world.random.nextInt(list.size()));
    }
    return inventory;
  }

  @Override
  protected @NonNull NonNullList<ItemStack> getItems() {
    return this.inventory;
  }

  @Override
  protected void setItems(@NonNull NonNullList<ItemStack> list) {
    this.inventory = list;
  }

  private static void insertMain(Level world, BlockPos pos, BlockState state, DuctBlockEntity blockEntity) {
    if (world.isClientSide()) {
      return;
    }

    boolean canMoveItem = (blockEntity.slotState == FILTER_ENABLED && blockEntity.inventory.getFirst().getCount() > 1) || blockEntity.slotState == FILTER_DISABLED;

    if (!blockEntity.needsCooldown() && state.getValue(DuctBlock.ENABLED) && canMoveItem) {
      boolean bl = false;
      if (!blockEntity.isEmpty()) {
        bl = DuctBlockEntity.insert(world, pos, state, blockEntity);
      }
      if (bl) {
        blockEntity.setTransferCooldown(TRANSFER_COOLDOWN);
        DuctBlockEntity.setChanged(world, pos, state);

      }
    }
  }

  private static boolean insert(Level world, BlockPos pos, BlockState state, Container inventory) {
    for (Direction direction : TRANSFER_PRIORITY) {
      if (state.getValue(DuctBlock.FACING_PROPERTIES.get(direction)) != PipeType.OUT) {
          continue;
      }

      Container inventory2 = DuctBlockEntity.getInventoryAt(world, pos.relative(direction));

      if (inventory2 != null && !DuctBlockEntity.isInventoryFull(inventory2, direction)) {
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
          if (inventory.getItem(i).isEmpty()) continue;
          ItemStack itemStack = inventory.getItem(i).copy();
          ItemStack itemStack2 = DuctBlockEntity.transfer(inventory, inventory2, inventory.removeItem(i, 1), direction);
          if (itemStack2.isEmpty()) {
            inventory2.setChanged();

            return true;
          }
          inventory.setItem(i, itemStack);
        }
      }
    }

    return false;
  }

  private static IntStream getAvailableSlots(Container inventory, Direction side) {
    if (inventory instanceof WorldlyContainer) {
      return IntStream.of(((WorldlyContainer)inventory).getSlotsForFace(side));
    }

    return IntStream.range(0, inventory.getContainerSize());
  }

  private static boolean isInventoryFull(Container inventory, Direction direction) {
    return DuctBlockEntity.getAvailableSlots(inventory, direction).allMatch(slot -> {
      ItemStack itemStack = inventory.getItem(slot);

      return itemStack.getCount() >= itemStack.getMaxStackSize();
    });
  }

  private static boolean canInsert(Container inventory, ItemStack stack, int slot, @Nullable Direction side) {
    if (!inventory.canPlaceItem(slot, stack)) {
      return false;
    }

    return !(inventory instanceof WorldlyContainer) || ((WorldlyContainer)inventory).canPlaceItemThroughFace(slot, stack, side);
  }

  /*
   * Enabled aggressive block sorting
   * Lifted jumps to return sites
   */
  public static ItemStack transfer(@Nullable Container from, Container to, ItemStack stack, @Nullable Direction side) {
    if (to instanceof WorldlyContainer sidedTo) {
      if (side != null) {
        int[] is = sidedTo.getSlotsForFace(side.getOpposite());
        int i = 0;
        while (i < is.length) {
          if (stack.isEmpty()) return stack;
          stack = DuctBlockEntity.transfer(from, to, stack, is[i], side);
          ++i;
        }

        return stack;
      }
    }

    int j = to.getContainerSize();
    int i = 0;
    while (i < j) {
      if (stack.isEmpty()) return stack;
      stack = DuctBlockEntity.transfer(from, to, stack, i, side);
      ++i;
    }

    return stack;
  }

  private static ItemStack transfer(@Nullable Container from, Container to, ItemStack stack, int slot, @Nullable Direction side) {
    ItemStack itemStack = to.getItem(slot);

    if (DuctBlockEntity.canInsert(to, stack, slot, side)) {
      int j;
      boolean bl = false;
      boolean bl2 = to.isEmpty();

      if (itemStack.isEmpty()) {
        to.setItem(slot, stack);
        stack = ItemStack.EMPTY;
        bl = true;
      } else if (DuctBlockEntity.canMergeItems(itemStack, stack)) {
        int i = stack.getMaxStackSize() - itemStack.getCount();
        j = Math.min(stack.getCount(), i);
        stack.shrink(j);
        itemStack.grow(j);
        bl = j > 0;
      }

      if (bl) {
        DuctBlockEntity hopperBlockEntity;
        if (bl2 && to instanceof DuctBlockEntity && !(hopperBlockEntity = (DuctBlockEntity)to).isDisabled()) {
          j = 0;
          if (from instanceof DuctBlockEntity hopperBlockEntity2) {
            if (hopperBlockEntity.lastTickTime >= hopperBlockEntity2.lastTickTime) {
              j = 1;
            }
          }
          hopperBlockEntity.setTransferCooldown(8 - j);
        }
        to.setChanged();
      }
    }

    return stack;
  }

  private static boolean canMergeItems(ItemStack first, ItemStack second) {
    return first.getCount() <= first.getMaxStackSize() && ItemStack.isSameItemSameComponents(first, second);
  }

  private void setTransferCooldown(int transferCooldown) {
    this.transferCooldown = transferCooldown;
  }

  private boolean needsCooldown() {
    return this.transferCooldown > 0;
  }

  private boolean isDisabled() {
    return this.transferCooldown > TRANSFER_COOLDOWN;
  }
}
