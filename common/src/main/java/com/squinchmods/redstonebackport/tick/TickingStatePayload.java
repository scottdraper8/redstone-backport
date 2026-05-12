package com.squinchmods.redstonebackport.tick;

import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C payload analogous to vanilla 1.20.3+ {@code ClientboundTickingStatePacket}. Carries the
 * current tick rate and frozen flag from server to client.
 */
public record TickingStatePayload(float tickRate, boolean isFrozen) {
  public void write(FriendlyByteBuf buf) {
    buf.writeFloat(this.tickRate);
    buf.writeBoolean(this.isFrozen);
  }

  public static TickingStatePayload read(FriendlyByteBuf buf) {
    return new TickingStatePayload(buf.readFloat(), buf.readBoolean());
  }
}
