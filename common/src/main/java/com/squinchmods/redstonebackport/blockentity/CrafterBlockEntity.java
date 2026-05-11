package com.squinchmods.redstonebackport.blockentity;

import com.squinchmods.redstonebackport.Platform;
import com.squinchmods.redstonebackport.block.CrafterBlock;
import com.squinchmods.redstonebackport.menu.CrafterMenu;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CrafterBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
  private static final int SLOT_COUNT = 9;
  private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
  private final boolean[] enabled = new boolean[9];
  private int nextInsertSlot = 0;

  public CrafterBlockEntity(BlockPos pos, BlockState state) {
    super(Platform.CRAFTER_BLOCK_ENTITY.get(), pos, state);

    for (int i = 0; i < this.enabled.length; i++) {
      this.enabled[i] = true;
    }
  }

  private void setChangedAndSync() {
    this.setChanged();
    if (this.level != null && !this.level.isClientSide) {
      BlockState state = this.getBlockState();
      this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
    }
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("container.redstone_backport.crafter");
  }

  @Override
  @Nullable public AbstractContainerMenu createMenu(
      int containerId, Inventory playerInventory, Player player) {
    return new CrafterMenu(containerId, playerInventory, this);
  }

  public NonNullList<ItemStack> getItems() {
    return this.items;
  }

  public boolean isSlotEnabled(int slot) {
    return slot >= 0 && slot < 9 && this.enabled[slot];
  }

  public void toggleSlotEnabled(int slot) {
    if (slot >= 0 && slot < this.enabled.length) {
      this.enabled[slot] = !this.enabled[slot];
      if (!this.enabled[slot]) {
        ItemStack stack = this.items.get(slot);
        if (!stack.isEmpty()) {
          this.dropToFront(stack.copy());
          this.items.set(slot, ItemStack.EMPTY);
        }
      }

      this.setChangedAndSync();
    }
  }

  public void setSlotEnabled(int slot, boolean value) {
    if (slot >= 0 && slot < this.enabled.length) {
      this.enabled[slot] = value;
      if (!this.enabled[slot]) {
        ItemStack stack = this.items.get(slot);
        if (!stack.isEmpty()) {
          this.dropToFront(stack.copy());
          this.items.set(slot, ItemStack.EMPTY);
        }
      }

      this.setChangedAndSync();
    }
  }

  @Override
  public CompoundTag getUpdateTag() {
    CompoundTag tag = super.getUpdateTag();
    this.saveAdditional(tag);
    return tag;
  }

  @Override
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  public ItemStack getCraftingResult() {
    if (this.level == null) {
      return ItemStack.EMPTY;
    } else {
      CraftingContainer craftingContainer = this.buildCraftingContainer();
      Optional<CraftingRecipe> optional =
          this.level
              .getRecipeManager()
              .getRecipeFor(RecipeType.CRAFTING, craftingContainer, this.level);
      if (optional.isEmpty()) {
        return ItemStack.EMPTY;
      } else {
        CraftingRecipe recipe = optional.get();
        return recipe.assemble(craftingContainer, this.level.registryAccess());
      }
    }
  }

  public void tryCraftAndEject() {
    if (this.level != null && !this.level.isClientSide) {
      if (this.level instanceof ServerLevel serverLevel) {
        CraftingContainer var12 = this.buildCraftingContainer();
        Optional<CraftingRecipe> optional =
            serverLevel.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, var12, serverLevel);
        if (optional.isEmpty()) {
          serverLevel.playSound(
              null, this.worldPosition, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.8F, 1.0F);
        } else {
          CraftingRecipe recipe = optional.get();
          ItemStack result = recipe.assemble(var12, serverLevel.registryAccess());
          if (result.isEmpty()) {
            serverLevel.playSound(
                null,
                this.worldPosition,
                SoundEvents.DISPENSER_FAIL,
                SoundSource.BLOCKS,
                0.8F,
                1.0F);
          } else {
            NonNullList<ItemStack> remaining = recipe.getRemainingItems(var12);

            for (int i = 0; i < 9; i++) {
              if (this.enabled[i]) {
                ItemStack in = (ItemStack) this.items.get(i);
                if (!in.isEmpty()) {
                  in.shrink(1);
                  if (in.isEmpty()) {
                    this.items.set(i, ItemStack.EMPTY);
                  }
                }

                ItemStack rem = (ItemStack) remaining.get(i);
                if (!rem.isEmpty()) {
                  ItemStack current = (ItemStack) this.items.get(i);
                  if (current.isEmpty()) {
                    this.items.set(i, rem);
                  } else {
                    ItemStack leftover =
                        Platform.insertToNeighbor(
                            this.level, this.worldPosition, this.getBlockState(), rem);
                    if (!leftover.isEmpty()) {
                      this.dropToFront(leftover);
                    }
                  }
                }
              }
            }

            ItemStack leftover =
                Platform.insertToNeighbor(
                    this.level, this.worldPosition, this.getBlockState(), result.copy());
            if (!leftover.isEmpty()) {
              this.dropToFront(leftover);
            }

            serverLevel.playSound(
                null,
                this.worldPosition,
                SoundEvents.DISPENSER_DISPENSE,
                SoundSource.BLOCKS,
                0.8F,
                1.0F);
            BlockState state = this.getBlockState();
            if (state.getBlock() instanceof CrafterBlock) {
              serverLevel.setBlock(
                  this.worldPosition, state.setValue(CrafterBlock.CRAFTING, true), 3);
              serverLevel.scheduleTick(this.worldPosition, state.getBlock(), 6);
            }

            this.setChanged();
          }
        }
      }
    }
  }

  private CraftingContainer buildCraftingContainer() {
    NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);

    for (int i = 0; i < 9; i++) {
      grid.set(i, this.enabled[i] ? this.items.get(i).copy() : ItemStack.EMPTY);
    }

    return new CrafterBlockEntity.CrafterCraftingContainer(grid);
  }

  private void dropToFront(ItemStack stack) {
    if (this.level != null) {
      Direction facing = this.getBlockState().getValue(CrafterBlock.FACING);
      Vec3 center =
          Vec3.atCenterOf(this.worldPosition)
              .add(facing.getStepX() * 0.6, facing.getStepY() * 0.6, facing.getStepZ() * 0.6);
      ItemEntity ent = new ItemEntity(this.level, center.x, center.y, center.z, stack);
      ent.setDefaultPickUpDelay();
      this.level.addFreshEntity(ent);
    }
  }

  @Override
  protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    tag.putInt("NextInsertSlot", this.nextInsertSlot);

    for (int i = 0; i < 9; i++) {
      CompoundTag itemTag = new CompoundTag();
      this.items.get(i).save(itemTag);
      tag.put("Item" + i, itemTag);
      tag.putBoolean("Enabled" + i, this.enabled[i]);
    }
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    this.nextInsertSlot = tag.contains("NextInsertSlot") ? tag.getInt("NextInsertSlot") : 0;

    for (int i = 0; i < 9; i++) {
      CompoundTag itemTag = tag.getCompound("Item" + i);
      this.items.set(i, ItemStack.of(itemTag));
      String key = "Enabled" + i;
      this.enabled[i] = tag.contains(key) ? tag.getBoolean(key) : true;
    }
  }

  // Container implementation
  @Override
  public int getContainerSize() {
    return SLOT_COUNT;
  }

  @Override
  public boolean isEmpty() {
    for (ItemStack stack : this.items) {
      if (!stack.isEmpty()) return false;
    }
    return true;
  }

  @Override
  public ItemStack getItem(int slot) {
    return this.items.get(slot);
  }

  @Override
  public ItemStack removeItem(int slot, int amount) {
    ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
    if (!result.isEmpty()) {
      this.setChanged();
    }
    return result;
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    return ContainerHelper.takeItem(this.items, slot);
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    this.items.set(slot, stack);
    if (stack.getCount() > this.getMaxStackSize()) {
      stack.setCount(this.getMaxStackSize());
    }
    this.setChanged();
  }

  @Override
  public boolean stillValid(Player player) {
    return Container.stillValidBlockEntity(this, player);
  }

  @Override
  public void clearContent() {
    this.items.clear();
    this.setChanged();
  }

  // WorldlyContainer implementation
  @Override
  public int[] getSlotsForFace(Direction side) {
    return new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8};
  }

  @Override
  public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
    return this.isSlotEnabled(slot);
  }

  @Override
  public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
    return false; // Crafter doesn't allow extraction
  }

  // Custom insertion logic for automation (to be used by adapters)
  public int findNextInsertSlot(ItemStack stack) {
    for (int step = 0; step < SLOT_COUNT; step++) {
      int idx = (this.nextInsertSlot + step) % SLOT_COUNT;
      if (this.isSlotEnabled(idx) && this.items.get(idx).isEmpty()) {
        return idx;
      }
    }

    for (int step = 0; step < SLOT_COUNT; step++) {
      int idx = (this.nextInsertSlot + step) % SLOT_COUNT;
      if (this.isSlotEnabled(idx)) {
        ItemStack existing = this.items.get(idx);
        if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)) {
          if (existing.getCount() < existing.getMaxStackSize()) {
            return idx;
          }
        }
      }
    }

    return -1;
  }

  public void moveNextInsertSlot(int lastSlot) {
    this.nextInsertSlot = (lastSlot + 1) % SLOT_COUNT;
    this.setChangedAndSync();
  }

  private static class CrafterCraftingContainer implements CraftingContainer {
    private final NonNullList<ItemStack> items;

    private CrafterCraftingContainer(NonNullList<ItemStack> items) {
      this.items = items;
    }

    @Override
    public int getWidth() {
      return 3;
    }

    @Override
    public int getHeight() {
      return 3;
    }

    @Override
    public List<ItemStack> getItems() {
      return this.items;
    }

    @Override
    public void fillStackedContents(StackedContents stackedContents) {
      for (int i = 0; i < this.items.size(); i++) {
        stackedContents.accountStack(this.items.get(i));
      }
    }

    @Override
    public int getContainerSize() {
      return this.items.size();
    }

    @Override
    public boolean isEmpty() {
      for (int i = 0; i < this.items.size(); i++) {
        if (!this.items.get(i).isEmpty()) {
          return false;
        }
      }

      return true;
    }

    @Override
    public ItemStack getItem(int slot) {
      return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
      ItemStack stack = this.items.get(slot);
      return stack.isEmpty() ? ItemStack.EMPTY : stack.split(amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
      ItemStack stack = this.items.get(slot);
      this.items.set(slot, ItemStack.EMPTY);
      return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
      this.items.set(slot, stack);
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
      return true;
    }

    @Override
    public void clearContent() {
      for (int i = 0; i < this.items.size(); i++) {
        this.items.set(i, ItemStack.EMPTY);
      }
    }
  }
}
