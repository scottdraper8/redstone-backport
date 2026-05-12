package com.squinchmods.redstonebackport.tick;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TimeUtil;

/**
 * Faithful backport of {@code net.minecraft.server.commands.TickCommand} from 1.20.3+. All
 * subcommands (query, rate, step, sprint, freeze, unfreeze) are implemented with the same argument
 * structure and feedback messages as the original.
 */
public final class TickCommand {
  private static final float MAX_TICKRATE = TickRateManager.MAX_TICKRATE;
  private static final String DEFAULT_TICKRATE = String.valueOf(20);

  private TickCommand() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("tick")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("query").executes(ctx -> tickQuery(ctx.getSource())))
            .then(
                Commands.literal("rate")
                    .then(
                        Commands.argument("rate", FloatArgumentType.floatArg(1.0F, MAX_TICKRATE))
                            .suggests(
                                (ctx, builder) ->
                                    SharedSuggestionProvider.suggest(
                                        new String[] {DEFAULT_TICKRATE}, builder))
                            .executes(
                                ctx ->
                                    setTickingRate(
                                        ctx.getSource(), FloatArgumentType.getFloat(ctx, "rate")))))
            .then(
                Commands.literal("step")
                    .executes(ctx -> step(ctx.getSource(), 1))
                    .then(Commands.literal("stop").executes(ctx -> stopStepping(ctx.getSource())))
                    .then(
                        Commands.argument("time", TimeArgument.time(1))
                            .suggests(
                                (ctx, builder) ->
                                    SharedSuggestionProvider.suggest(
                                        new String[] {"1t", "1s"}, builder))
                            .executes(
                                ctx ->
                                    step(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "time")))))
            .then(
                Commands.literal("sprint")
                    .then(Commands.literal("stop").executes(ctx -> stopSprinting(ctx.getSource())))
                    .then(
                        Commands.argument("time", TimeArgument.time(1))
                            .suggests(
                                (ctx, builder) ->
                                    SharedSuggestionProvider.suggest(
                                        new String[] {"60s", "1d", "3d"}, builder))
                            .executes(
                                ctx ->
                                    sprint(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "time")))))
            .then(Commands.literal("unfreeze").executes(ctx -> setFreeze(ctx.getSource(), false)))
            .then(Commands.literal("freeze").executes(ctx -> setFreeze(ctx.getSource(), true))));
  }

  private static ServerTickRateManager getManager(CommandSourceStack source) {
    return ((TickRateManagerAccess) source.getServer()).redstoneBackport$tickRateManager();
  }

  private static String nanosToMillisString(long nanos) {
    return String.format("%.1f", (float) nanos / (float) TimeUtil.NANOSECONDS_PER_MILLISECOND);
  }

  private static int setTickingRate(CommandSourceStack source, float rate) {
    ServerTickRateManager manager = getManager(source);
    manager.setTickRate(rate);
    String string = String.format("%.1f", rate);
    source.sendSuccess(() -> Component.translatable("commands.tick.rate.success", string), true);
    return (int) rate;
  }

  private static int tickQuery(CommandSourceStack source) {
    ServerTickRateManager manager = getManager(source);
    TickRateManagerAccess access = (TickRateManagerAccess) source.getServer();
    String avgMspt = nanosToMillisString(access.redstoneBackport$getAverageTickTimeNanos());
    float rate = manager.tickrate();
    String rateStr = String.format("%.1f", rate);

    if (manager.isSprinting()) {
      source.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), false);
      source.sendSuccess(
          () -> Component.translatable("commands.tick.query.rate.sprinting", rateStr, avgMspt),
          false);
    } else {
      if (manager.isFrozen()) {
        source.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), false);
      } else if (manager.nanosecondsPerTick() < access.redstoneBackport$getAverageTickTimeNanos()) {
        source.sendSuccess(() -> Component.translatable("commands.tick.status.lagging"), false);
      } else {
        source.sendSuccess(() -> Component.translatable("commands.tick.status.running"), false);
      }

      String targetMspt = nanosToMillisString(manager.nanosecondsPerTick());
      source.sendSuccess(
          () ->
              Component.translatable(
                  "commands.tick.query.rate.running", rateStr, avgMspt, targetMspt),
          false);
    }

    long[] tickTimes =
        Arrays.copyOf(
            access.redstoneBackport$getTickTimesNanos(),
            access.redstoneBackport$getTickTimesNanos().length);
    Arrays.sort(tickTimes);
    String p50 = nanosToMillisString(tickTimes[tickTimes.length / 2]);
    String p95 = nanosToMillisString(tickTimes[(int) (tickTimes.length * 0.95)]);
    String p99 = nanosToMillisString(tickTimes[(int) (tickTimes.length * 0.99)]);
    source.sendSuccess(
        () ->
            Component.translatable(
                "commands.tick.query.percentiles", p50, p95, p99, tickTimes.length),
        false);

    return (int) rate;
  }

  private static int sprint(CommandSourceStack source, int ticks) {
    boolean wasSprinting = getManager(source).requestGameToSprint(ticks);
    if (wasSprinting) {
      source.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
    }
    source.sendSuccess(() -> Component.translatable("commands.tick.status.sprinting"), true);
    return 1;
  }

  private static int setFreeze(CommandSourceStack source, boolean freeze) {
    ServerTickRateManager manager = getManager(source);
    if (freeze) {
      if (manager.isSprinting()) {
        manager.stopSprinting();
      }
      if (manager.isSteppingForward()) {
        manager.stopStepping();
      }
    }

    manager.setFrozen(freeze);
    if (freeze) {
      source.sendSuccess(() -> Component.translatable("commands.tick.status.frozen"), true);
    } else {
      source.sendSuccess(() -> Component.translatable("commands.tick.status.running"), true);
    }
    return freeze ? 1 : 0;
  }

  private static int step(CommandSourceStack source, int ticks) {
    ServerTickRateManager manager = getManager(source);
    boolean success = manager.stepGameIfPaused(ticks);
    if (success) {
      source.sendSuccess(() -> Component.translatable("commands.tick.step.success", ticks), true);
    } else {
      source.sendFailure(Component.translatable("commands.tick.step.fail"));
    }
    return 1;
  }

  private static int stopStepping(CommandSourceStack source) {
    ServerTickRateManager manager = getManager(source);
    boolean success = manager.stopStepping();
    if (success) {
      source.sendSuccess(() -> Component.translatable("commands.tick.step.stop.success"), true);
      return 1;
    } else {
      source.sendFailure(Component.translatable("commands.tick.step.stop.fail"));
      return 0;
    }
  }

  private static int stopSprinting(CommandSourceStack source) {
    ServerTickRateManager manager = getManager(source);
    boolean success = manager.stopSprinting();
    if (success) {
      source.sendSuccess(() -> Component.translatable("commands.tick.sprint.stop.success"), true);
      return 1;
    } else {
      source.sendFailure(Component.translatable("commands.tick.sprint.stop.fail"));
      return 0;
    }
  }
}
