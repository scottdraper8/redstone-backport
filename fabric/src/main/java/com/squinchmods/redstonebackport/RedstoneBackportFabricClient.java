package com.squinchmods.redstonebackport;

import com.squinchmods.redstonebackport.client.CrafterScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class RedstoneBackportFabricClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    MenuScreens.register(Platform.CRAFTER_MENU.get(), CrafterScreen::new);
  }
}
