package com.squinchmods.redstonebackport.tick;

/**
 * Duck interface implemented via mixin on {@link net.minecraft.server.MinecraftServer}. Provides
 * access to the backported {@link ServerTickRateManager} and tick timing data that was added in
 * 1.20.3+.
 */
public interface TickRateManagerAccess {
  ServerTickRateManager redstoneBackport$tickRateManager();

  void onTickRateChanged();

  long redstoneBackport$getAverageTickTimeNanos();

  long[] redstoneBackport$getTickTimesNanos();
}
