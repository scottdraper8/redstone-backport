package com.squinchmods.redstonebackport.tick;

import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C payload analogous to vanilla 1.20.3+ {@code ClientboundTickingStepPacket}. Carries the
 * remaining frozen-step tick count from server to client.
 */
public record TickingStepPayload(int frozenTicksToRun) {
  public void write(FriendlyByteBuf buf) {
    buf.writeVarInt(this.frozenTicksToRun);
  }

  public static TickingStepPayload read(FriendlyByteBuf buf) {
    return new TickingStepPayload(buf.readVarInt());
  }
}
