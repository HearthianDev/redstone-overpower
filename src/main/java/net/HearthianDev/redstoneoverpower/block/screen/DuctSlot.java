package net.HearthianDev.redstoneoverpower.block.screen;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class DuctSlot extends Slot {
  private final DuctScreenHandler ductScreenHandler;

  public DuctSlot(Container inventory, int index, int x, int y, DuctScreenHandler ductScreenHandler) {
    super(inventory, index, x, y);
    this.ductScreenHandler = ductScreenHandler;
  }

  public void setChanged() {
    super.setChanged();
    this.ductScreenHandler.slotsChanged(this.container);
  }
}
