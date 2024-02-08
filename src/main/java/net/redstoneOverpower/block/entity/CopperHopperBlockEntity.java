package net.redstoneOverpower.block.entity;

import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.redstoneOverpower.block.CopperHopperBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.redstoneOverpower.utils.Initialiser.COPPER_HOPPER_BLOCK_ENTITY;

public class CopperHopperBlockEntity extends LootableContainerBlockEntity implements Hopper {
    public static final int TRANSFER_COOLDOWN = 8;
    public static final int INVENTORY_SIZE = 5;
    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private int transferCooldown = -1;
    private long lastTickTime;

    public CopperHopperBlockEntity(BlockPos pos, BlockState state) {
      super(COPPER_HOPPER_BLOCK_ENTITY, pos, state);
    }

  @Override
  public void readNbt(NbtCompound nbt) {
    super.readNbt(nbt);
    this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
    if (!this.readLootTable(nbt)) {
      Inventories.readNbt(nbt, this.inventory);
    }
    this.transferCooldown = nbt.getInt("TransferCooldown");
  }

  @Override
  protected void writeNbt(NbtCompound nbt) {
    super.writeNbt(nbt);
    if (!this.writeLootTable(nbt)) {
      Inventories.writeNbt(nbt, this.inventory);
    }
    nbt.putInt("TransferCooldown", this.transferCooldown);
  }

  @Override
  public int size() {
    return this.inventory.size();
  }

  @Override
  public ItemStack removeStack(int slot, int amount) {
    this.generateLoot(null);
    return Inventories.splitStack(this.method_11282(), slot, amount);
  }

  @Override
  public void setStack(int slot, ItemStack stack) {
    this.generateLoot(null);
    this.method_11282().set(slot, stack);
    if (stack.getCount() > this.getMaxCountPerStack()) {
      stack.setCount(this.getMaxCountPerStack());
    }
  }

  @Override
  protected Text getContainerName() {
    return Text.translatable("container.hopper");
  }

  public static void serverTick(World world, BlockPos pos, BlockState state, CopperHopperBlockEntity blockEntity) {
    --blockEntity.transferCooldown;
    blockEntity.lastTickTime = world.getTime();
    if (!blockEntity.needsCooldown()) {
      blockEntity.setTransferCooldown(0);
      CopperHopperBlockEntity.insertAndExtract(world, pos, state, blockEntity, () -> CopperHopperBlockEntity.extract(world, blockEntity));
    }
  }

  private static boolean insertAndExtract(World world, BlockPos pos, BlockState state, CopperHopperBlockEntity blockEntity, BooleanSupplier booleanSupplier) {
    if (world.isClient) {
      return false;
    }
    if (!blockEntity.needsCooldown() && state.get(CopperHopperBlock.ENABLED)) {
      boolean bl = false;
      if (!blockEntity.isEmpty()) {
        bl = CopperHopperBlockEntity.insert(world, pos, state, blockEntity);
      }
      if (!blockEntity.isFull()) {
        bl |= booleanSupplier.getAsBoolean();
      }
      if (bl) {
        blockEntity.setTransferCooldown(TRANSFER_COOLDOWN);
        CopperHopperBlockEntity.markDirty(world, pos, state);
        return true;
      }
    }
    return false;
  }

  private boolean isFull() {
    for (ItemStack itemStack : this.inventory) {
      if (!itemStack.isEmpty() && itemStack.getCount() == itemStack.getMaxCount()) continue;
      return false;
    }
    return true;
  }

  private static boolean insert(World world, BlockPos pos, BlockState state, Inventory inventory) {
//      System.out.println("insert");
    Inventory inventory2 = CopperHopperBlockEntity.getOutputInventory(world, pos, state);
    if (inventory2 == null) {
      return false;
    }
    Direction direction = state.get(CopperHopperBlock.FACING).getOpposite();
    if (CopperHopperBlockEntity.isInventoryFull(inventory2, direction)) {
      return false;
    }
    for (int i = 0; i < inventory.size(); ++i) {
      System.out.println(inventory.getStack(i).isEmpty());
      if (inventory.getStack(i).isEmpty()) continue;
      System.out.println("pasa");
      ItemStack itemStack = inventory.getStack(i).copy();
      ItemStack itemStack2 = CopperHopperBlockEntity.transfer(inventory, inventory2, inventory.removeStack(i, 1), direction);
      if (itemStack2.isEmpty()) {
        inventory2.markDirty();
        return true;
      }
      inventory.setStack(i, itemStack);
    }
    return false;
  }

  private static IntStream getAvailableSlots(Inventory inventory, Direction side) {
    if (inventory instanceof SidedInventory) {
      return IntStream.of(((SidedInventory)inventory).getAvailableSlots(side));
    }
    return IntStream.range(0, inventory.size());
  }

  private static boolean isInventoryFull(Inventory inventory, Direction direction) {
    return CopperHopperBlockEntity.getAvailableSlots(inventory, direction).allMatch(slot -> {
      ItemStack itemStack = inventory.getStack(slot);
      return itemStack.getCount() >= itemStack.getMaxCount();
    });
  }

  private static boolean isInventoryEmpty(Inventory inv, Direction facing) {
    return CopperHopperBlockEntity.getAvailableSlots(inv, facing).allMatch(slot -> inv.getStack(slot).isEmpty());
  }

  public static boolean extract(World world, Hopper hopper) {
//      System.out.println("extract main");
    Inventory inventory = CopperHopperBlockEntity.getInputInventory(world, hopper);
    if (inventory != null) {
      Direction direction = Direction.DOWN;
      if (CopperHopperBlockEntity.isInventoryEmpty(inventory, direction)) {
        return false;
      }
      return CopperHopperBlockEntity.getAvailableSlots(inventory, direction).anyMatch(slot -> CopperHopperBlockEntity.extract(hopper, inventory, slot, direction));
    }
    for (ItemEntity itemEntity : CopperHopperBlockEntity.getInputItemEntities(world, hopper)) {
      if (!CopperHopperBlockEntity.extract(hopper, itemEntity)) continue;
      return true;
    }
    return false;
  }

  private static boolean extract(Hopper hopper, Inventory inventory, int slot, Direction side) {
    System.out.println("extract 1");
    ItemStack itemStack = inventory.getStack(slot);
    if (!itemStack.isEmpty() && CopperHopperBlockEntity.canExtract(hopper, inventory, itemStack, slot, side)) {
      ItemStack itemStack2 = itemStack.copy();
      ItemStack itemStack3 = CopperHopperBlockEntity.transfer(inventory, hopper, inventory.removeStack(slot, 1), null);
      if (itemStack3.isEmpty()) {
        inventory.markDirty();
        return true;
      }
      inventory.setStack(slot, itemStack2);
    }
    return false;
  }

  public static boolean extract(Inventory inventory, ItemEntity itemEntity) {
    System.out.println("extract 2");
    boolean bl = false;
    ItemStack itemStack = itemEntity.getStack().copy();
    ItemStack itemStack2 = CopperHopperBlockEntity.transfer(null, inventory, itemStack, null);
    if (itemStack2.isEmpty()) {
      bl = true;
      itemEntity.setStack(ItemStack.EMPTY);
      itemEntity.discard();
    } else {
      itemEntity.setStack(itemStack2);
    }
    return bl;
  }

  /*
   * Enabled aggressive block sorting
   * Lifted jumps to return sites
   */
  public static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, @Nullable Direction side) {
    System.out.println("transfer main");
    if (to instanceof SidedInventory sidedInventory) {
      System.out.println("transfer main sided");
      if (side != null) {
        int[] is = sidedInventory.getAvailableSlots(side);
        int i = 0;
        while (i < is.length) {
          if (stack.isEmpty()) return stack;
          stack = CopperHopperBlockEntity.transfer(from, to, stack, is[i], side);
          ++i;
        }
        return stack;
      }
    }
    int j = to.size();
    int i = 0;
    while (i < j) {
      if (stack.isEmpty()) return stack;
      stack = CopperHopperBlockEntity.transfer(from, to, stack, i, side);
      ++i;
    }
    return stack;
  }

  private static boolean canInsert(Inventory inventory, ItemStack stack, int slot, @Nullable Direction side) {
    SidedInventory sidedInventory;
    if (slot < INVENTORY_SIZE - 1 && !inventory.getStack(slot + 1).isEmpty()) {
      return false;
    }
    if (!inventory.isValid(slot, stack)) {
      return false;
    }
    return !(inventory instanceof SidedInventory) || (sidedInventory = (SidedInventory)inventory).canInsert(slot, stack, side);
  }

  private static boolean canExtract(Inventory hopperInventory, Inventory fromInventory, ItemStack stack, int slot, Direction facing) {
    SidedInventory sidedInventory;
    if (!fromInventory.canTransferTo(hopperInventory, slot, stack)) {
      return false;
    }
    return !(fromInventory instanceof SidedInventory) || (sidedInventory = (SidedInventory)fromInventory).canExtract(slot, stack, facing);
  }

  // Must make sure that is the last slot available, if next slot is not available CANNOT insert there
  private static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, int slot, @Nullable Direction side) {
    System.out.println("transfer 1");
    ItemStack itemStack = to.getStack(slot);

    if (CopperHopperBlockEntity.canInsert(to, stack, slot, side)) {
      int j;
      boolean bl = false;
      boolean bl2 = to.isEmpty();
      if (itemStack.isEmpty()) {
        to.setStack(slot, stack);
        stack = ItemStack.EMPTY;
        bl = true;
      } else if (CopperHopperBlockEntity.canMergeItems(itemStack, stack)) {
        int i = stack.getMaxCount() - itemStack.getCount();
        j = Math.min(stack.getCount(), i);
        stack.decrement(j);
        itemStack.increment(j);
        boolean bl3 = bl = j > 0;
      }
      if (bl) {
        CopperHopperBlockEntity hopperBlockEntity;
        if (bl2 && to instanceof CopperHopperBlockEntity && !(hopperBlockEntity = (CopperHopperBlockEntity)to).isDisabled()) {
          j = 0;
          if (from instanceof CopperHopperBlockEntity hopperBlockEntity2) {
            if (hopperBlockEntity.lastTickTime >= hopperBlockEntity2.lastTickTime) {
              j = 1;
            }
          }
          hopperBlockEntity.setTransferCooldown(TRANSFER_COOLDOWN - j);
        }
        to.markDirty();
      }
    }
    return stack;
  }

  @Nullable
  private static Inventory getOutputInventory(World world, BlockPos pos, BlockState state) {
    Direction direction = state.get(CopperHopperBlock.FACING);
    return CopperHopperBlockEntity.getInventoryAt(world, pos.offset(direction));
  }

  @Nullable
  private static Inventory getInputInventory(World world, Hopper hopper) {
    return CopperHopperBlockEntity.getInventoryAt(world, hopper.getHopperX(), hopper.getHopperY() + 1.0, hopper.getHopperZ());
  }

  public static List<ItemEntity> getInputItemEntities(World world, Hopper hopper) {
    return hopper.getInputAreaShape().getBoundingBoxes().stream().flatMap(box -> world.getEntitiesByClass(ItemEntity.class, box.offset(hopper.getHopperX() - 0.5, hopper.getHopperY() - 0.5, hopper.getHopperZ() - 0.5), EntityPredicates.VALID_ENTITY).stream()).collect(Collectors.toList());
  }

  @Nullable
  public static Inventory getInventoryAt(World world, BlockPos pos) {
    return CopperHopperBlockEntity.getInventoryAt(world, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
  }

  @Nullable
  private static Inventory getInventoryAt(World world, double x, double y, double z) {
    List<Entity> list;
    BlockEntity blockEntity;
    Inventory inventory = null;
    BlockPos blockPos = BlockPos.ofFloored(x, y, z);
    BlockState blockState = world.getBlockState(blockPos);
    Block block = blockState.getBlock();
    if (block instanceof InventoryProvider) {
      inventory = ((InventoryProvider) block).getInventory(blockState, world, blockPos);
    } else if (blockState.hasBlockEntity() && (blockEntity = world.getBlockEntity(blockPos)) instanceof Inventory && (inventory = (Inventory) blockEntity) instanceof ChestBlockEntity && block instanceof ChestBlock) {
      inventory = ChestBlock.getInventory((ChestBlock)block, blockState, world, blockPos, true);
    }
    if (inventory == null && !(list = world.getOtherEntities(null, new Box(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5), EntityPredicates.VALID_INVENTORIES)).isEmpty()) {
      inventory = (Inventory) list.get(world.random.nextInt(list.size()));
    }
    return inventory;
  }

  private static boolean canMergeItems(ItemStack first, ItemStack second) {
    return first.getCount() <= first.getMaxCount() && ItemStack.canCombine(first, second);
  }

  @Override
  public double getHopperX() {
    return (double)this.pos.getX() + 0.5;
  }

  @Override
  public double getHopperY() {
    return (double)this.pos.getY() + 0.5;
  }

  @Override
  public double getHopperZ() {
    return (double)this.pos.getZ() + 0.5;
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

  @Override
  protected DefaultedList<ItemStack> method_11282() {
    return this.inventory;
  }

  @Override
  protected void setInvStackList(DefaultedList<ItemStack> list) {
    this.inventory = list;
  }

  public static void onEntityCollided(World world, BlockPos pos, BlockState state, Entity entity, CopperHopperBlockEntity blockEntity) {
    ItemEntity itemEntity;
    if (entity instanceof ItemEntity && !(itemEntity = (ItemEntity)entity).getStack().isEmpty() && VoxelShapes.matchesAnywhere(VoxelShapes.cuboid(entity.getBoundingBox().offset(-pos.getX(), -pos.getY(), -pos.getZ())), blockEntity.getInputAreaShape(), BooleanBiFunction.AND)) {
      CopperHopperBlockEntity.insertAndExtract(world, pos, state, blockEntity, () -> CopperHopperBlockEntity.extract(blockEntity, itemEntity));
    }
  }

  @Override
  protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
    return new HopperScreenHandler(syncId, playerInventory, this);
  }
}
