package com.squinchmods.redstonebackport.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.squinchmods.redstonebackport.menu.CrafterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {
  private static final ResourceLocation BG =
      new ResourceLocation("redstone_backport", "textures/gui/container/crafter.png");
  private static final ResourceLocation DISABLED_SLOT =
      new ResourceLocation("redstone_backport", "textures/gui/container/crafter_disabled_slot.png");

  public CrafterScreen(CrafterMenu menu, Inventory playerInventory, Component title) {
    super(menu, playerInventory, title);
    this.imageWidth = 176;
    this.imageHeight = 166;
  }

  @Override
  protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    gfx.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    int left = this.leftPos;
    int top = this.topPos;

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        int slot = col + row * 3;
        if (!this.menu.isSlotEnabledClient(slot)) {
          int x = left + 26 + col * 18;
          int y = top + 17 + row * 18;
          gfx.blit(DISABLED_SLOT, x - 1, y - 1, 0, 0, 18, 18, 18, 18);
        }
      }
    }
  }

  @Override
  public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
    this.renderBackground(gfx);
    super.render(gfx, mouseX, mouseY, partialTick);
    this.renderTooltip(gfx, mouseX, mouseY);
  }

  @Override
  protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {}
}
