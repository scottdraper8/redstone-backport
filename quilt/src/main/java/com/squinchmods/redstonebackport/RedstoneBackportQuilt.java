package com.squinchmods.redstonebackport;

import com.squinchmods.redstonebackport.block.CrafterBlock;
import com.squinchmods.redstonebackport.blockentity.CrafterBlockEntity;
import com.squinchmods.redstonebackport.menu.CrafterMenu;
import com.squinchmods.redstonebackport.tick.TickCommand;
import com.squinchmods.redstonebackport.tick.TickingStatePayload;
import com.squinchmods.redstonebackport.tick.TickingStepPayload;
import io.netty.buffer.Unpooled;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootingEnchantFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

public class RedstoneBackportQuilt implements ModInitializer {
  private static final ResourceLocation TICK_STATE_ID = RedstoneBackport.id("tick_state");
  private static final ResourceLocation TICK_STEP_ID = RedstoneBackport.id("tick_step");

  private void modifyLootTables() {
    LootTableEvents.MODIFY.register(
        (resourceManager, lootManager, id, tableBuilder, source) -> {
          if (source.isBuiltin()
              && id.equals(new ResourceLocation("minecraft", "entities/witch"))) {
            LootPool.Builder poolBuilder =
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(Items.REDSTONE))
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 8.0F)))
                    .apply(LootingEnchantFunction.lootingMultiplier(ConstantValue.exactly(1.0F)));

            tableBuilder.withPool(poolBuilder);
          }
        });
  }

  @Override
  public void onInitialize(ModContainer mod) {
    modifyLootTables();

    Block block =
        Registry.register(
            BuiltInRegistries.BLOCK,
            RedstoneBackport.id("crafter"),
            new CrafterBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    Item item =
        Registry.register(
            BuiltInRegistries.ITEM,
            RedstoneBackport.id("crafter"),
            new BlockItem(block, new Item.Properties()));

    BlockEntityType<CrafterBlockEntity> beType =
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            RedstoneBackport.id("crafter"),
            BlockEntityType.Builder.of(CrafterBlockEntity::new, block).build(null));

    MenuType<CrafterMenu> menuType =
        Registry.register(
            BuiltInRegistries.MENU,
            RedstoneBackport.id("crafter"),
            new ExtendedScreenHandlerType<>(CrafterMenu::new));

    Platform.CRAFTER_BLOCK_ENTITY = () -> beType;
    Platform.CRAFTER_MENU = () -> menuType;
    Platform.SCREEN_OPENER =
        (player, provider, pos) -> {
          if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                new ExtendedScreenHandlerFactory() {
                  @Override
                  public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
                    buf.writeBlockPos(pos);
                  }

                  @Override
                  public Component getDisplayName() {
                    return provider.getDisplayName();
                  }

                  @Override
                  @Nullable public AbstractContainerMenu createMenu(
                      int syncId, Inventory playerInventory, Player player) {
                    return provider.createMenu(syncId, playerInventory, player);
                  }
                });
          }
        };
    Platform.ITEM_TRANSFER =
        (level, pos, state, stack) -> {
          Direction facing = state.getValue(CrafterBlock.FACING);
          BlockPos outPos = pos.relative(facing);
          BlockEntity be = level.getBlockEntity(outPos);
          if (be instanceof Container container) {
            return insertIntoContainer(container, facing.getOpposite(), stack);
          }
          return stack;
        };
    Platform.TICK_STATE_BROADCASTER =
        new Platform.TickStateBroadcaster() {
          @Override
          public void broadcastState(
              net.minecraft.server.MinecraftServer server, float tickRate, boolean frozen) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
              sendState(player, tickRate, frozen);
            }
          }

          @Override
          public void broadcastStep(
              net.minecraft.server.MinecraftServer server, int frozenTicksToRun) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
              sendStep(player, frozenTicksToRun);
            }
          }

          @Override
          public void sendState(ServerPlayer player, float tickRate, boolean frozen) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            new TickingStatePayload(tickRate, frozen).write(buf);
            ServerPlayNetworking.send(player, TICK_STATE_ID, buf);
          }

          @Override
          public void sendStep(ServerPlayer player, int frozenTicksToRun) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            new TickingStepPayload(frozenTicksToRun).write(buf);
            ServerPlayNetworking.send(player, TICK_STEP_ID, buf);
          }
        };

    ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS)
        .register(
            content -> {
              content.accept(item);
            });

    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> TickCommand.register(dispatcher));
  }

  private static ItemStack insertIntoContainer(
      Container container, @Nullable Direction side, ItemStack stack) {
    ItemStack remaining = stack.copy();
    int[] slots =
        container instanceof WorldlyContainer worldly && side != null
            ? worldly.getSlotsForFace(side)
            : allSlots(container);

    for (int slot : slots) {
      if (!canPlace(container, slot, remaining, side)) {
        continue;
      }

      ItemStack inSlot = container.getItem(slot);
      int maxStackSize = Math.min(container.getMaxStackSize(), remaining.getMaxStackSize());
      if (inSlot.isEmpty()) {
        ItemStack placed = remaining.copy();
        placed.setCount(Math.min(remaining.getCount(), maxStackSize));
        container.setItem(slot, placed);
        remaining.shrink(placed.getCount());
      } else if (ItemStack.isSameItemSameTags(inSlot, remaining)) {
        int space = Math.min(maxStackSize, inSlot.getMaxStackSize()) - inSlot.getCount();
        if (space <= 0) {
          continue;
        }

        int toAdd = Math.min(remaining.getCount(), space);
        inSlot.grow(toAdd);
        remaining.shrink(toAdd);
        container.setChanged();
      }

      if (remaining.isEmpty()) {
        return ItemStack.EMPTY;
      }
    }

    return remaining;
  }

  private static boolean canPlace(
      Container container, int slot, ItemStack stack, @Nullable Direction side) {
    if (container instanceof WorldlyContainer worldly && side != null) {
      return worldly.canPlaceItemThroughFace(slot, stack, side);
    }
    return container.canPlaceItem(slot, stack);
  }

  private static int[] allSlots(Container container) {
    int[] slots = new int[container.getContainerSize()];
    for (int i = 0; i < slots.length; i++) {
      slots[i] = i;
    }
    return slots;
  }
}
