package com.squinchmods.redstonebackport.tick;

import net.minecraft.util.TimeUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Stores the fundamental state of the game's tick rate and freeze status. This is a faithful
 * replica of {@code net.minecraft.world.TickRateManager} introduced in 1.20.3.
 */
public class TickRateManager {
  public static final float MIN_TICKRATE = 1.0F;
  public static final float MAX_TICKRATE = 1000.0F;

  protected float tickrate = 20.0F;
  protected long nanosecondsPerTick = TimeUtil.NANOSECONDS_PER_SECOND / 20L;
  protected int frozenTicksToRun = 0;
  protected boolean runGameElements = true;
  protected boolean isFrozen = false;

  public void setTickRate(float f) {
    this.tickrate = Math.min(Math.max(f, MIN_TICKRATE), MAX_TICKRATE);
    this.nanosecondsPerTick = (long) ((double) TimeUtil.NANOSECONDS_PER_SECOND / this.tickrate);
  }

  public float tickrate() {
    return this.tickrate;
  }

  public float millisecondsPerTick() {
    return (float) this.nanosecondsPerTick / (float) TimeUtil.NANOSECONDS_PER_MILLISECOND;
  }

  public long nanosecondsPerTick() {
    return this.nanosecondsPerTick;
  }

  public boolean runsNormally() {
    return this.runGameElements;
  }

  public boolean isSteppingForward() {
    return this.frozenTicksToRun > 0;
  }

  public void setFrozenTicksToRun(int i) {
    this.frozenTicksToRun = i;
  }

  public int frozenTicksToRun() {
    return this.frozenTicksToRun;
  }

  public void setFrozen(boolean bl) {
    this.isFrozen = bl;
  }

  public boolean isFrozen() {
    return this.isFrozen;
  }

  public void tick() {
    this.runGameElements = !this.isFrozen || this.frozenTicksToRun > 0;
    if (this.frozenTicksToRun > 0) {
      this.frozenTicksToRun--;
    }
  }

  public boolean isEntityFrozen(Entity entity) {
    if (this.runsNormally() || entity instanceof Player) {
      return false;
    }
    for (Entity passenger : entity.getIndirectPassengers()) {
      if (passenger instanceof Player) {
        return false;
      }
    }
    return true;
  }
}
