package com.squinchmods.redstonebackport;

import com.squinchmods.redstonebackport.blockentity.CrafterBlockEntity;
import com.squinchmods.redstonebackport.menu.CrafterMenu;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class Platform {
  public static Supplier<BlockEntityType<CrafterBlockEntity>> CRAFTER_BLOCK_ENTITY =
      () -> {
        throw new IllegalStateException("CRAFTER_BLOCK_ENTITY not initialized");
      };

  public static Supplier<MenuType<CrafterMenu>> CRAFTER_MENU =
      () -> {
        throw new IllegalStateException("CRAFTER_MENU not initialized");
      };

  public interface ScreenOpener {
    void openScreen(Player player, MenuProvider provider, BlockPos pos);
  }

  public static ScreenOpener SCREEN_OPENER =
      (player, provider, pos) -> {
        throw new IllegalStateException("SCREEN_OPENER not initialized");
      };

  public static void openScreen(Player player, MenuProvider provider, BlockPos pos) {
    SCREEN_OPENER.openScreen(player, provider, pos);
  }

  public interface ItemTransfer {
    ItemStack insertToNeighbor(
        net.minecraft.world.level.Level level,
        BlockPos pos,
        net.minecraft.world.level.block.state.BlockState state,
        ItemStack stack);
  }

  public static ItemTransfer ITEM_TRANSFER =
      (level, pos, state, stack) -> {
        throw new IllegalStateException("ITEM_TRANSFER not initialized");
      };

  public static ItemStack insertToNeighbor(
      net.minecraft.world.level.Level level,
      BlockPos pos,
      net.minecraft.world.level.block.state.BlockState state,
      ItemStack stack) {
    return ITEM_TRANSFER.insertToNeighbor(level, pos, state, stack);
  }
}
