package com.squinchmods.redstonebackport;

import com.squinchmods.redstonebackport.client.CrafterScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class RedstoneBackportQuiltClient implements ClientModInitializer {
  @Override
  public void onInitializeClient(ModContainer mod) {
    MenuScreens.register(Platform.CRAFTER_MENU.get(), CrafterScreen::new);
  }
}
