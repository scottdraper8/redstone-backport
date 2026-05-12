package com.squinchmods.redstonebackport.mixin;

import com.squinchmods.redstonebackport.tick.TickRateManager;
import com.squinchmods.redstonebackport.tick.TickRateManagerAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLevel.class)
@SuppressWarnings("NullAway")
public abstract class ServerLevelMixin {
  @Shadow @Final private MinecraftServer server;

  @Shadow
  protected abstract void tickTime();

  @Shadow
  @SuppressWarnings("all")
  private void advanceWeatherCycle() {}

  @Shadow
  @SuppressWarnings("all")
  private void runBlockEvents() {}

  @Unique private TickRateManager redstoneBackport$getTickRateManager() {
    return ((TickRateManagerAccess) this.server).redstoneBackport$tickRateManager();
  }

  @Redirect(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;tick()V"))
  private void redstoneBackport$gateWorldBorderTick(WorldBorder worldBorder) {
    if (redstoneBackport$getTickRateManager().runsNormally()) {
      worldBorder.tick();
    }
  }

  @Redirect(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/server/level/ServerLevel;advanceWeatherCycle()V"))
  private void redstoneBackport$gateWeatherTick(ServerLevel instance) {
    if (redstoneBackport$getTickRateManager().runsNormally()) {
      advanceWeatherCycle();
    }
  }

  @Redirect(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickTime()V"))
  private void redstoneBackport$gateTimeTick(ServerLevel instance) {
    if (redstoneBackport$getTickRateManager().runsNormally()) {
      tickTime();
    }
  }

  /**
   * When the world is frozen, returns {@code true} to skip scheduled block and fluid ticks (the
   * vanilla code already skips them in debug worlds).
   */
  @Redirect(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isDebug()Z"))
  private boolean redstoneBackport$gateScheduledTicks(ServerLevel instance) {
    if (!redstoneBackport$getTickRateManager().runsNormally()) {
      return true;
    }
    return instance.isDebug();
  }

  @Redirect(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raids;tick()V"))
  private void redstoneBackport$gateRaidTick(Raids raids) {
    if (redstoneBackport$getTickRateManager().runsNormally()) {
      raids.tick();
    }
  }

  @Redirect(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"))
  private void redstoneBackport$gateBlockEvents(ServerLevel instance) {
    if (redstoneBackport$getTickRateManager().runsNormally()) {
      runBlockEvents();
    }
  }

  @Redirect(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/level/dimension/end/EndDragonFight;tick()V"))
  private void redstoneBackport$gateDragonFightTick(EndDragonFight dragonFight) {
    if (redstoneBackport$getTickRateManager().runsNormally()) {
      dragonFight.tick();
    }
  }
}
