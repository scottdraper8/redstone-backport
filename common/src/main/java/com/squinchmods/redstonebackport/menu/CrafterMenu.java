package com.squinchmods.redstonebackport.menu;

import com.squinchmods.redstonebackport.Platform;
import com.squinchmods.redstonebackport.blockentity.CrafterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CrafterMenu extends AbstractContainerMenu {
  public static final int GRID_SIZE = 9;
  private final CrafterBlockEntity blockEntity;
  private final ContainerData enabledData;

  public CrafterMenu(int containerId, Inventory playerInventory, CrafterBlockEntity blockEntity) {
    super(Platform.CRAFTER_MENU.get(), containerId);
    this.blockEntity = blockEntity;
    this.enabledData =
        new ContainerData() {
          @Override
          public int get(int index) {
            return CrafterMenu.this.blockEntity.isSlotEnabled(index) ? 1 : 0;
          }

          @Override
          public void set(int index, int value) {
            CrafterMenu.this.blockEntity.setSlotEnabled(index, value != 0);
          }

          @Override
          public int getCount() {
            return 9;
          }
        };
    this.addDataSlots(this.enabledData);

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        int index = col + row * 3;
        int x = 26 + col * 18;
        int y = 17 + row * 18;
        this.addSlot(new CrafterMenu.CrafterInputSlot(blockEntity, index, x, y));
      }
    }

    this.addSlot(new CrafterMenu.CrafterOutputSlot(blockEntity, 134, 35));
    int invStartY = 84;

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        this.addSlot(
            new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, invStartY + row * 18));
      }
    }

    int hotbarY = invStartY + 58;

    for (int col = 0; col < 9; col++) {
      this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
    }
  }

  public CrafterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
    this(
        containerId,
        playerInventory,
        (CrafterBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
  }

  @Override
  public boolean stillValid(Player player) {
    return this.blockEntity != null
        && !this.blockEntity.isRemoved()
        && player.distanceToSqr(
                this.blockEntity.getBlockPos().getX() + 0.5,
                this.blockEntity.getBlockPos().getY() + 0.5,
                this.blockEntity.getBlockPos().getZ() + 0.5)
            <= 64.0;
  }

  public CrafterBlockEntity getBlockEntity() {
    return this.blockEntity;
  }

  public boolean isSlotEnabledClient(int slot) {
    return slot >= 0 && slot < 9 && this.enabledData.get(slot) != 0;
  }

  @Override
  public void clicked(int slotId, int button, ClickType clickType, Player player) {
    if (slotId >= 0 && slotId < 9) {
      ItemStack slotStack = this.slots.get(slotId).getItem();
      if (clickType == ClickType.PICKUP && this.getCarried().isEmpty() && slotStack.isEmpty()) {
        if (!player.level().isClientSide) {
          this.blockEntity.toggleSlotEnabled(slotId);
          this.broadcastChanges();
        }
        return;
      }
    }
    super.clicked(slotId, button, clickType, player);
  }

  @Override
  public boolean clickMenuButton(Player player, int id) {
    if (id >= 0 && id < 9) {
      if (!player.level().isClientSide) {
        this.blockEntity.toggleSlotEnabled(id);
        this.broadcastChanges();
      }

      return true;
    } else {
      return super.clickMenuButton(player, id);
    }
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    ItemStack moved = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot != null && slot.hasItem()) {
      ItemStack stack = slot.getItem();
      moved = stack.copy();
      int containerSlots = 9;
      if (index < containerSlots) {
        if (!this.moveItemStackTo(stack, containerSlots + 1, this.slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else if (index == 9) {
        return ItemStack.EMPTY;
      } else if (!this.moveItemStackTo(stack, 0, containerSlots, false)) {
        return ItemStack.EMPTY;
      }

      if (stack.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }

      return moved;
    } else {
      return ItemStack.EMPTY;
    }
  }

  private static class CrafterContainer implements Container {
    private final CrafterBlockEntity be;

    private CrafterContainer(CrafterBlockEntity be) {
      this.be = be;
    }

    @Override
    public int getContainerSize() {
      return 9;
    }

    @Override
    public boolean isEmpty() {
      for (int i = 0; i < 9; i++) {
        if (!this.be.getItems().get(i).isEmpty()) {
          return false;
        }
      }

      return true;
    }

    @Override
    public ItemStack getItem(int slot) {
      return this.be.getItems().get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
      ItemStack stack = this.be.getItems().get(slot);
      if (stack.isEmpty()) {
        return ItemStack.EMPTY;
      } else {
        ItemStack split = stack.split(amount);
        this.be.setChanged();
        return split;
      }
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
      ItemStack stack = this.be.getItems().get(slot);
      this.be.getItems().set(slot, ItemStack.EMPTY);
      this.be.setChanged();
      return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
      this.be.getItems().set(slot, stack);
      this.be.setChanged();
    }

    @Override
    public void setChanged() {
      this.be.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
      return true;
    }

    @Override
    public void clearContent() {
      for (int i = 0; i < 9; i++) {
        this.be.getItems().set(i, ItemStack.EMPTY);
      }

      this.be.setChanged();
    }
  }

  private static class CrafterInputSlot extends Slot {
    private final CrafterBlockEntity be;
    private final int slotIndex;

    public CrafterInputSlot(CrafterBlockEntity be, int index, int x, int y) {
      super(new CrafterMenu.CrafterContainer(be), index, x, y);
      this.be = be;
      this.slotIndex = index;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
      return this.be.isSlotEnabled(this.slotIndex);
    }

    @Override
    public boolean isActive() {
      return true;
    }

    @Override
    public void setChanged() {
      super.setChanged();
      this.be.setChanged();
    }
  }

  private static class CrafterOutputSlot extends Slot {
    private final CrafterBlockEntity be;

    public CrafterOutputSlot(CrafterBlockEntity be, int x, int y) {
      super(new CrafterMenu.CrafterContainer(be), 0, x, y);
      this.be = be;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
      return false;
    }

    @Override
    public boolean mayPickup(Player player) {
      return false;
    }

    @Override
    public ItemStack getItem() {
      return this.be.getCraftingResult();
    }

    @Override
    public void set(ItemStack stack) {}

    @Override
    public ItemStack remove(int amount) {
      return ItemStack.EMPTY;
    }
  }
}
