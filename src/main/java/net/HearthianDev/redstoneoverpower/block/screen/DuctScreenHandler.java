package net.HearthianDev.redstoneoverpower.block.screen;

import net.HearthianDev.redstoneoverpower.block.entity.DuctBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import static net.HearthianDev.redstoneoverpower.utils.Initialiser.DUCT_SCREEN_HANDLER;

public class DuctScreenHandler extends AbstractContainerMenu {
  public static final int SLOT_COUNT = 1;
  private final Container inventory;
  private final ContainerData propertyDelegate;

  public DuctScreenHandler(int syncId, Inventory playerInventory) {
    this(syncId, playerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(SLOT_COUNT));
  }

  public DuctScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
    super(DUCT_SCREEN_HANDLER, syncId);
    this.propertyDelegate = propertyDelegate;
    this.inventory = inventory;
    checkContainerSize(inventory, SLOT_COUNT);
    inventory.startOpen(playerInventory.player);
    this.addDataSlots(propertyDelegate);
    this.addSlots(playerInventory);
  }

  private void addSlots(Inventory playerInventory) {
    int j;

    // Duct inventory
    this.addSlot(new DuctSlot(inventory, 0, 44 + 2 * 18, 20, this));
    // Player inventory
    for (j = 0; j < 3; ++j) {
      for (int k = 0; k < 9; ++k) {
        this.addSlot(new Slot(playerInventory, k + j * 9 + 9, 8 + k * 18, j * 18 + 51));
      }
    }
    // Player hotbar
    for (j = 0; j < 9; ++j) {
      this.addSlot(new Slot(playerInventory, j, 8 + j * 18, 109));
    }
  }

  @Override
  public void clicked(int slotIndex, int button, @NonNull ClickType actionType, @NonNull Player player) {
    if (slotIndex == 0) {
      if (actionType == ClickType.PICKUP && getCarried().isEmpty() && !getSlot(slotIndex).hasItem()) {
        this.toggleSlot(slotIndex);
      }
    }

    super.clicked(slotIndex, button, actionType, player);
  }

  public void toggleSlot(int slot) {
    DuctSlot ductSlot = (DuctSlot)this.getSlot(slot);
    this.propertyDelegate.set(ductSlot.index, this.isSlotDisabled(slot) ? DuctBlockEntity.FILTER_DISABLED : DuctBlockEntity.FILTER_ENABLED);
    this.broadcastChanges();
  }

  public boolean isSlotDisabled(int slot) {
    if (slot == 0) {
      return this.propertyDelegate.get(slot) == DuctBlockEntity.FILTER_ENABLED;
    } else {
      return false;
    }
  }

  @Override
  public boolean stillValid(@NonNull Player player) {
    return this.inventory.stillValid(player);
  }

  @Override
  public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slot) {
    ItemStack itemStack = ItemStack.EMPTY;
    Slot slot2 = this.slots.get(slot);
    if (slot2.hasItem()) {
      ItemStack itemStack2 = slot2.getItem();
      itemStack = itemStack2.copy();
      if (slot < this.inventory.getContainerSize()
        ? !this.moveItemStackTo(itemStack2, this.inventory.getContainerSize(), this.slots.size(), true)
        : !this.moveItemStackTo(itemStack2, 0, this.inventory.getContainerSize(), false)
      ) {
        return ItemStack.EMPTY;
      }
      if (itemStack2.isEmpty()) {
        slot2.setByPlayer(ItemStack.EMPTY);
      } else {
        slot2.setChanged();
      }
    }
    return itemStack;
  }

  @Override
  public void removed(@NonNull Player player) {
    super.removed(player);
    this.inventory.stopOpen(player);
  }
}
