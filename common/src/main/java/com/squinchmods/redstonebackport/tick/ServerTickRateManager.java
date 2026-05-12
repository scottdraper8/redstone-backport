package com.squinchmods.redstonebackport.tick;

import com.squinchmods.redstonebackport.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TimeUtil;

/**
 * Server-specific tick rate manager handling sprinting, stepping, and client synchronization.
 * Faithful replica of {@code net.minecraft.server.ServerTickRateManager} from 1.20.3+.
 *
 * <p>Client sync uses custom S2C channels via {@link Platform.TickStateBroadcaster} rather than
 * vanilla 1.20.3+ packet types (which stock 1.20.1 clients would reject). Each loader provides its
 * own broadcaster implementation.
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
    broadcastState();
  }

  public boolean stepGameIfPaused(int i) {
    if (!this.isFrozen()) {
      return false;
    } else {
      this.frozenTicksToRun = i;
      broadcastStep();
      return true;
    }
  }

  public boolean stopStepping() {
    if (this.frozenTicksToRun > 0) {
      this.frozenTicksToRun = 0;
      broadcastStep();
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
    broadcastState();
  }

  /**
   * Sends the current tick state and step count to a player who just joined the server, analogous
   * to vanilla 1.20.3+ {@code ServerTickRateManager.updateJoiningPlayer}.
   */
  public void updateJoiningPlayer(ServerPlayer player) {
    Platform.TICK_STATE_BROADCASTER.sendState(player, this.tickrate, this.isFrozen);
    Platform.TICK_STATE_BROADCASTER.sendStep(player, this.frozenTicksToRun);
  }

  private void broadcastState() {
    Platform.TICK_STATE_BROADCASTER.broadcastState(this.server, this.tickrate, this.isFrozen);
  }

  private void broadcastStep() {
    Platform.TICK_STATE_BROADCASTER.broadcastStep(this.server, this.frozenTicksToRun);
  }
}
