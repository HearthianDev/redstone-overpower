package net.HearthianDev.redstoneoverpower.client.gui.screen.ingame;

import net.HearthianDev.redstoneoverpower.RedstoneOverpower;
import net.HearthianDev.redstoneoverpower.block.screen.DuctScreenHandler;
import net.HearthianDev.redstoneoverpower.block.screen.DuctSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.NonNull;

public class DuctScreen extends AbstractContainerScreen<DuctScreenHandler> {
  private static final Component ENABLE_FILTER_TEXT = Component.translatable("gui.enable_filter");
  private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "textures/gui/container/duct.png");
  private static final Identifier FILTER_SLOT_TEXTURE = Identifier.fromNamespaceAndPath(RedstoneOverpower.MOD_ID, "textures/gui/sprites/container/duct/filter_slot.png");

  private final Player player;

  public DuctScreen(DuctScreenHandler handler, Inventory playerInventory, Component title) {
    super(handler, playerInventory, title);
    this.imageHeight = 133;
    this.inventoryLabelY = this.imageHeight - 94;
    this.player = playerInventory.player;
  }

  @Override
  public void render(@NonNull GuiGraphics context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    this.renderTooltip(context, mouseX, mouseY);
    if (this.hoveredSlot instanceof DuctSlot && this.menu.getCarried().isEmpty() && !this.hoveredSlot.hasItem() && !this.menu.isSlotDisabled(this.hoveredSlot.index)) {
      context.setTooltipForNextFrame(this.font, ENABLE_FILTER_TEXT, mouseX, mouseY);
    }
  }

  @Override
  protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
    int i = (this.width - this.imageWidth) / 2;
    int j = (this.height - this.imageHeight) / 2;
    context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
  }

  protected void slotClicked(@NonNull Slot slot, int slotId, int button, @NonNull ClickType actionType) {
    if (slot instanceof DuctSlot && !slot.hasItem() && !this.player.isSpectator()) {
      if (actionType == ClickType.PICKUP) {
        if (this.menu.getCarried().isEmpty() && !this.menu.getSlot(slotId).hasItem()) {
          this.toggleSlot(this.menu.isSlotDisabled(slotId));
        }
      }
    }

    super.slotClicked(slot, slotId, button, actionType);
  }

  private void toggleSlot(boolean enabled) {
    float f = enabled ? 1.0F : 0.75F;
    this.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, f);
  }

  public void renderSlot(@NonNull GuiGraphics context, @NonNull Slot slot, int mouseX, int mouseY) {
    if (slot instanceof DuctSlot ductSlot) {
      if (this.menu.isSlotDisabled(slot.index)) {
        this.drawDisabledSlot(context, ductSlot);

        return;
      }
    }

    super.renderSlot(context, slot, mouseX, mouseY);
  }

  private void drawDisabledSlot(GuiGraphics context, DuctSlot slot) {
    context.renderItem(slot.getItem(), slot.x, slot.y);
    context.renderItemDecorations(this.font, slot.getItem(), slot.x, slot.y);
    context.blit(RenderPipelines.GUI_TEXTURED, FILTER_SLOT_TEXTURE, slot.x - 1, slot.y - 1, 18, 18, 18, 18, 18, 18);
  }
}
