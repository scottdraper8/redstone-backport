package com.squinchmods.redstonebackport.tick;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TimeUtil;

// Uses TickRateManagerAccess duck interface (applied via mixin on MinecraftServer) for methods
// that don't exist in vanilla 1.20.1.

/**
 * Server-specific tick rate manager handling sprinting, stepping, and client synchronization.
 * Faithful replica of {@code net.minecraft.server.ServerTickRateManager} from 1.20.3+.
 *
 * <p>Client-side synchronization packets (ClientboundTickingStatePacket,
 * ClientboundTickingStepPacket) are omitted because 1.20.1 clients do not recognize them. The
 * server-side behavioral parity (freeze, step, sprint, rate) is fully preserved.
 */
public class ServerTickRateManager extends TickRateManager {
  private long remainingSprintTicks = 0L;
  private long sprintTickStartTime = 0L;
  private long sprintTimeSpend = 0L;
  private long scheduledCurrentSprintTicks = 0L;
  private boolean previousIsFrozen = false;
  private final MinecraftServer server;

  public ServerTickRateManager(MinecraftServer server) {
    this.server = server;
  }

  public boolean isSprinting() {
    return this.scheduledCurrentSprintTicks > 0L;
  }

  @Override
  public void setFrozen(boolean bl) {
    super.setFrozen(bl);
  }

  public boolean stepGameIfPaused(int i) {
    if (!this.isFrozen()) {
      return false;
    } else {
      this.frozenTicksToRun = i;
      return true;
    }
  }

  public boolean stopStepping() {
    if (this.frozenTicksToRun > 0) {
      this.frozenTicksToRun = 0;
      return true;
    } else {
      return false;
    }
  }

  public boolean stopSprinting() {
    if (this.remainingSprintTicks > 0L) {
      this.finishTickSprint();
      return true;
    } else {
      return false;
    }
  }

  public boolean requestGameToSprint(int i) {
    boolean bl = this.remainingSprintTicks > 0L;
    this.sprintTimeSpend = 0L;
    this.scheduledCurrentSprintTicks = i;
    this.remainingSprintTicks = i;
    this.previousIsFrozen = this.isFrozen();
    this.setFrozen(false);
    return bl;
  }

  private void finishTickSprint() {
    long l = this.scheduledCurrentSprintTicks - this.remainingSprintTicks;
    double d = Math.max(1.0, (double) this.sprintTimeSpend) / TimeUtil.NANOSECONDS_PER_MILLISECOND;
    int i = (int) (1000L * l / d);
    String string = String.format("%.2f", l == 0L ? (double) this.millisecondsPerTick() : d / l);
    this.scheduledCurrentSprintTicks = 0L;
    this.sprintTimeSpend = 0L;
    this.server
        .createCommandSourceStack()
        .sendSuccess(() -> Component.translatable("commands.tick.sprint.report", i, string), true);
    this.remainingSprintTicks = 0L;
    this.setFrozen(this.previousIsFrozen);
    ((TickRateManagerAccess) this.server).onTickRateChanged();
  }

  /**
   * Called from the server loop each tick to determine whether sprint processing should occur.
   * Returns true when there are remaining sprint ticks and the game elements should run.
   */
  public boolean checkShouldSprintThisTick() {
    if (!this.runGameElements) {
      return false;
    } else if (this.remainingSprintTicks > 0L) {
      this.sprintTickStartTime = System.nanoTime();
      this.remainingSprintTicks--;
      return true;
    } else {
      this.finishTickSprint();
      return false;
    }
  }

  public void endTickWork() {
    this.sprintTimeSpend = this.sprintTimeSpend + (System.nanoTime() - this.sprintTickStartTime);
  }

  @Override
  public void setTickRate(float f) {
    super.setTickRate(f);
    ((TickRateManagerAccess) this.server).onTickRateChanged();
  }
}
