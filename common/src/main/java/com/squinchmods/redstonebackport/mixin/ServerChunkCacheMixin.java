package com.squinchmods.redstonebackport.mixin;

import com.squinchmods.redstonebackport.tick.TickRateManagerAccess;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkCache.class)
@SuppressWarnings("NullAway")
public abstract class ServerChunkCacheMixin {
  @Shadow @Final ServerLevel level;

  /**
   * Skips chunk ticking (natural mob spawning, random block ticks) when the world is frozen. The
   * vanilla {@code tickChunks()} method is private so we inject at its head.
   */
  @Inject(method = "tickChunks", at = @At("HEAD"), cancellable = true)
  private void redstoneBackport$gateChunkTicking(CallbackInfo ci) {
    if (!((TickRateManagerAccess) this.level.getServer())
        .redstoneBackport$tickRateManager()
        .runsNormally()) {
      ci.cancel();
    }
  }
}
