package com.squinchmods.redstonebackport;

import com.squinchmods.redstonebackport.client.CrafterScreen;
import com.squinchmods.redstonebackport.tick.ClientTickRateAccess;
import com.squinchmods.redstonebackport.tick.TickRateManager;
import com.squinchmods.redstonebackport.tick.TickingStatePayload;
import com.squinchmods.redstonebackport.tick.TickingStepPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class RedstoneBackportQuiltClient implements ClientModInitializer {
  private static final ResourceLocation TICK_STATE_ID = RedstoneBackport.id("tick_state");
  private static final ResourceLocation TICK_STEP_ID = RedstoneBackport.id("tick_step");

  @Override
  public void onInitializeClient(ModContainer mod) {
    MenuScreens.register(Platform.CRAFTER_MENU.get(), CrafterScreen::new);

    ClientPlayNetworking.registerGlobalReceiver(
        TICK_STATE_ID,
        (client, handler, buf, responseSender) -> {
          TickingStatePayload payload = TickingStatePayload.read(buf);
          client.execute(
              () -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level == null) return;
                TickRateManager manager =
                    ((ClientTickRateAccess) level).redstoneBackport$clientTickRateManager();
                manager.setTickRate(payload.tickRate());
                manager.setFrozen(payload.isFrozen());
              });
        });

    ClientPlayNetworking.registerGlobalReceiver(
        TICK_STEP_ID,
        (client, handler, buf, responseSender) -> {
          TickingStepPayload payload = TickingStepPayload.read(buf);
          client.execute(
              () -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level == null) return;
                TickRateManager manager =
                    ((ClientTickRateAccess) level).redstoneBackport$clientTickRateManager();
                manager.setFrozenTicksToRun(payload.frozenTicksToRun());
              });
        });
  }
}
