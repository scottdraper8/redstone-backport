package com.squinchmods.redstonebackport.mixin.client;

import com.squinchmods.redstonebackport.tick.ClientTickRateAccess;
import com.squinchmods.redstonebackport.tick.TickRateManager;
import java.util.function.BooleanSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements ClientTickRateAccess {
  @Unique private final TickRateManager redstoneBackport$tickRateManager = new TickRateManager();

  @Override
  public TickRateManager redstoneBackport$clientTickRateManager() {
    return this.redstoneBackport$tickRateManager;
  }

  /**
   * Advances the client tick rate manager each tick, mirroring the server's HEAD inject in
   * tickServer.
   */
  @Inject(method = "tick", at = @At("HEAD"))
  private void redstoneBackport$tickClientManager(BooleanSupplier supplier, CallbackInfo ci) {
    this.redstoneBackport$tickRateManager.tick();
  }

  /**
   * Gates client-side time ticking (day/night cycle) when the tick manager indicates the world is
   * frozen.
   */
  @Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
  private void redstoneBackport$gateClientTimeTick(CallbackInfo ci) {
    if (!this.redstoneBackport$tickRateManager.runsNormally()) {
      ci.cancel();
    }
  }
}
