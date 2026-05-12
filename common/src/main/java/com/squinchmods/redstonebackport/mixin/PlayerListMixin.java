package com.squinchmods.redstonebackport.mixin;

import com.squinchmods.redstonebackport.tick.TickRateManagerAccess;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sends the current tick state to players when they join, ensuring late joiners see the correct
 * freeze/rate/step state immediately.
 */
@Mixin(PlayerList.class)
@SuppressWarnings("NullAway")
public abstract class PlayerListMixin {
  @Inject(method = "placeNewPlayer", at = @At("TAIL"))
  private void redstoneBackport$syncTickStateOnJoin(
      Connection connection, ServerPlayer player, CallbackInfo ci) {
    ((TickRateManagerAccess) player.getServer())
        .redstoneBackport$tickRateManager()
        .updateJoiningPlayer(player);
  }
}
