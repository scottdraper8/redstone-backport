package com.squinchmods.redstonebackport;

import com.squinchmods.redstonebackport.block.CrafterBlock;
import com.squinchmods.redstonebackport.blockentity.CrafterBlockEntity;
import com.squinchmods.redstonebackport.client.CrafterScreen;
import com.squinchmods.redstonebackport.menu.CrafterMenu;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(RedstoneBackport.MOD_ID)
public class RedstoneBackportForge {
  public static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(ForgeRegistries.BLOCKS, RedstoneBackport.MOD_ID);
  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, RedstoneBackport.MOD_ID);
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
      DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RedstoneBackport.MOD_ID);
  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(ForgeRegistries.MENU_TYPES, RedstoneBackport.MOD_ID);

  public static final RegistryObject<Block> CRAFTER_BLOCK =
      BLOCKS.register(
          "crafter",
          () ->
              new CrafterBlock(
                  Properties.of()
                      .mapColor(MapColor.METAL)
                      .strength(3.5F)
                      .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
  public static final RegistryObject<Item> CRAFTER_BLOCK_ITEM =
      ITEMS.register(
          "crafter",
          () -> new BlockItem(CRAFTER_BLOCK.get(), new net.minecraft.world.item.Item.Properties()));
  public static final RegistryObject<BlockEntityType<CrafterBlockEntity>> CRAFTER_BLOCK_ENTITY =
      BLOCK_ENTITY_TYPES.register(
          "crafter",
          () ->
              BlockEntityType.Builder.of(CrafterBlockEntity::new, CRAFTER_BLOCK.get()).build(null));
  public static final RegistryObject<MenuType<CrafterMenu>> CRAFTER_MENU =
      MENUS.register("crafter", () -> IForgeMenuType.create(CrafterMenu::new));

  static {
    Platform.CRAFTER_BLOCK_ENTITY = CRAFTER_BLOCK_ENTITY;
    Platform.CRAFTER_MENU = CRAFTER_MENU;
    Platform.SCREEN_OPENER =
        (player, provider, pos) -> {
          if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, provider, pos);
          }
        };
    Platform.ITEM_TRANSFER =
        (level, pos, state, stack) -> {
          Direction facing = state.getValue(CrafterBlock.FACING);
          BlockPos outPos = pos.relative(facing);
          BlockEntity be = level.getBlockEntity(outPos);
          if (be == null) return stack;

          return be.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite())
              .map(
                  handler -> {
                    ItemStack remaining = stack;
                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                      remaining = handler.insertItem(slot, remaining, false);
                      if (remaining.isEmpty()) break;
                    }
                    return remaining;
                  })
              .orElse(stack);
        };
  }

  public RedstoneBackportForge(FMLJavaModLoadingContext context) {
    IEventBus modEventBus = context.getModEventBus();

    BLOCKS.register(modEventBus);
    ITEMS.register(modEventBus);
    BLOCK_ENTITY_TYPES.register(modEventBus);
    MENUS.register(modEventBus);

    MinecraftForge.EVENT_BUS.register(this);
    modEventBus.addListener(this::addCreative);
  }

  private void addCreative(BuildCreativeModeTabContentsEvent event) {
    if (Objects.equals(event.getTabKey(), CreativeModeTabs.REDSTONE_BLOCKS)) {
      event.accept(CRAFTER_BLOCK_ITEM);
    }
  }

  @SubscribeEvent
  public void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
    if (event.getObject() instanceof CrafterBlockEntity crafter) {
      event.addCapability(
          RedstoneBackport.id("inventory"),
          new ICapabilityProvider() {
            private final LazyOptional<IItemHandler> handler =
                LazyOptional.of(() -> new CrafterForgeItemHandler(crafter));

            @Override
            @Nonnull
            public <T> LazyOptional<T> getCapability(
                @Nonnull Capability<T> cap, @Nullable Direction side) {
              if (cap == ForgeCapabilities.ITEM_HANDLER) {
                return handler.cast();
              }
              return LazyOptional.empty();
            }
          });
    }
  }

  private static class CrafterForgeItemHandler implements IItemHandler {
    private final CrafterBlockEntity be;

    public CrafterForgeItemHandler(CrafterBlockEntity be) {
      this.be = be;
    }

    @Override
    public int getSlots() {
      return be.getContainerSize();
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot) {
      return be.getItem(slot);
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
      if (stack.isEmpty()) return ItemStack.EMPTY;

      int target = be.findNextInsertSlot(stack);
      if (target < 0) return stack;

      if (!simulate) {
        ItemStack existing = be.getItem(target);
        if (existing.isEmpty()) {
          ItemStack placed = stack.copy();
          placed.setCount(1);
          be.setItem(target, placed);
        } else {
          existing.grow(1);
          be.setChanged();
        }
        be.moveNextInsertSlot(target);
      }

      ItemStack remaining = stack.copy();
      remaining.shrink(1);
      return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
      return 64;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
      return be.canPlaceItemThroughFace(slot, stack, null);
    }
  }

  @EventBusSubscriber(modid = RedstoneBackport.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
  public static class ClientModEvents {
    @SubscribeEvent
    @SuppressWarnings("FutureReturnValueIgnored")
    public static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(
          () -> MenuScreens.register(RedstoneBackportForge.CRAFTER_MENU.get(), CrafterScreen::new));
    }
  }
}
