package com.squinchmods.redstonebackport.mixin;

import com.squinchmods.redstonebackport.tick.ServerTickRateManager;
import com.squinchmods.redstonebackport.tick.TickRateManagerAccess;
import java.util.function.BooleanSupplier;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
@SuppressWarnings("NullAway")
public abstract class MinecraftServerMixin implements TickRateManagerAccess {
  @Shadow @Final public long[] tickTimes;

  @Shadow private int tickCount;

  @Shadow private long nextTickTime;

  @Shadow private long lastOverloadWarning;

  @Unique private ServerTickRateManager redstoneBackport$tickRateManager;

  @Unique private boolean redstoneBackport$isSprinting = false;

  @Inject(method = "<init>", at = @At("RETURN"))
  private void redstoneBackport$initTickRateManager(CallbackInfo ci) {
    this.redstoneBackport$tickRateManager =
        new ServerTickRateManager((MinecraftServer) (Object) this);
  }

  @Override
  public ServerTickRateManager redstoneBackport$tickRateManager() {
    return this.redstoneBackport$tickRateManager;
  }

  @Override
  public void onTickRateChanged() {
    // 1.20.1 uses hardcoded autosave intervals; no dynamic recalculation needed
  }

  @Override
  public long redstoneBackport$getAverageTickTimeNanos() {
    int count = Math.min(100, Math.max(this.tickCount, 1));
    long total = 0L;
    for (int i = 0; i < count; i++) {
      total += this.tickTimes[i];
    }
    return total / count;
  }

  @Override
  public long[] redstoneBackport$getTickTimesNanos() {
    return this.tickTimes;
  }

  /**
   * Runs the TickRateManager tick at the start of each server tick, updating the runGameElements
   * flag before world ticking occurs.
   */
  @Inject(method = "tickServer", at = @At("HEAD"))
  private void redstoneBackport$tickRateManagerTick(BooleanSupplier supplier, CallbackInfo ci) {
    this.redstoneBackport$tickRateManager.tick();
  }

  /**
   * At the start of each loop iteration (before timing calculation), checks whether the tick rate
   * manager is in sprint mode. If sprinting, resets timing so the server runs back-to-back ticks
   * with no sleep.
   */
  @Inject(
      method = "runServer",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J", ordinal = 1))
  private void redstoneBackport$checkSprint(CallbackInfo ci) {
    ServerTickRateManager manager = this.redstoneBackport$tickRateManager;
    if (manager.isSprinting() && manager.checkShouldSprintThisTick()) {
      this.redstoneBackport$isSprinting = true;
      this.nextTickTime = Util.getMillis();
      this.lastOverloadWarning = this.nextTickTime;
    } else {
      this.redstoneBackport$isSprinting = false;
    }
  }

  /**
   * Replaces the hardcoded 50ms tick delay with a dynamic value derived from the tick rate manager.
   * During sprinting, returns 0 to eliminate inter-tick delay.
   */
  @ModifyConstant(method = "runServer", constant = @Constant(longValue = 50L))
  private long redstoneBackport$modifyTickDelay(long original) {
    if (this.redstoneBackport$isSprinting) {
      return 0L;
    }
    return (long) (1000.0 / this.redstoneBackport$tickRateManager.tickrate());
  }

  /**
   * During sprinting, passes {@code () -> false} to tickServer instead of {@code this::haveTime} to
   * prevent extra task processing between sprint ticks.
   */
  @Redirect(
      method = "runServer",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/server/MinecraftServer;tickServer(Ljava/util/function/BooleanSupplier;)V"))
  private void redstoneBackport$redirectTickServer(
      MinecraftServer instance, BooleanSupplier supplier) {
    if (this.redstoneBackport$isSprinting) {
      instance.tickServer(() -> false);
    } else {
      instance.tickServer(supplier);
    }
  }

  /**
   * After {@code waitUntilNextTick()} completes during a sprint tick, records the elapsed work time
   * for sprint performance reporting.
   */
  @Inject(
      method = "runServer",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/server/MinecraftServer;waitUntilNextTick()V",
              shift = At.Shift.AFTER))
  private void redstoneBackport$endSprintWork(CallbackInfo ci) {
    if (this.redstoneBackport$isSprinting) {
      this.redstoneBackport$tickRateManager.endTickWork();
    }
  }
}
