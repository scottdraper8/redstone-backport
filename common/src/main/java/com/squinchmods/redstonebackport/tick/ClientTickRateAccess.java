package com.squinchmods.redstonebackport.tick;

/**
 * Duck interface applied via mixin to {@code net.minecraft.client.multiplayer.ClientLevel}.
 * Provides access to the client-side tick rate manager that mirrors the server's tick state via
 * custom S2C packets.
 */
public interface ClientTickRateAccess {
  TickRateManager redstoneBackport$clientTickRateManager();
}
