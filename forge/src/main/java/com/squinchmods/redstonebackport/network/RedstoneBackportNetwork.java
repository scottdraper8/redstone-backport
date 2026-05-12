package com.squinchmods.redstonebackport.network;

import com.squinchmods.redstonebackport.Platform;
import com.squinchmods.redstonebackport.RedstoneBackport;
import com.squinchmods.redstonebackport.tick.ClientTickRateAccess;
import com.squinchmods.redstonebackport.tick.TickRateManager;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RedstoneBackportNetwork {

  private static final String PROTOCOL_VERSION = "1";

  public static final SimpleChannel INSTANCE =
      NetworkRegistry.newSimpleChannel(
          RedstoneBackport.id("main"),
          () -> PROTOCOL_VERSION,
          PROTOCOL_VERSION::equals,
          PROTOCOL_VERSION::equals);

  private RedstoneBackportNetwork() {}

  public static void init() {
    INSTANCE.registerMessage(
        0,
        TickingStateMessage.class,
        TickingStateMessage::write,
        TickingStateMessage::read,
        TickingStateMessage::handle);
    INSTANCE.registerMessage(
        1,
        TickingStepMessage.class,
        TickingStepMessage::write,
        TickingStepMessage::read,
        TickingStepMessage::handle);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  public record TickingStateMessage(float tickRate, boolean frozen) {

    public void write(FriendlyByteBuf buf) {
      buf.writeFloat(this.tickRate);
      buf.writeBoolean(this.frozen);
    }

    public static TickingStateMessage read(FriendlyByteBuf buf) {
      return new TickingStateMessage(buf.readFloat(), buf.readBoolean());
    }

    public static void handle(TickingStateMessage msg, Supplier<NetworkEvent.Context> ctx) {
      if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
        ctx.get()
            .enqueueWork(
                () -> {
                  Minecraft mc = Minecraft.getInstance();
                  if (mc.level != null) {
                    TickRateManager manager =
                        ((ClientTickRateAccess) mc.level).redstoneBackport$clientTickRateManager();
                    manager.setTickRate(msg.tickRate);
                    manager.setFrozen(msg.frozen);
                  }
                });
      }
      ctx.get().setPacketHandled(true);
    }
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  public record TickingStepMessage(int frozenTicksToRun) {

    public void write(FriendlyByteBuf buf) {
      buf.writeVarInt(this.frozenTicksToRun);
    }

    public static TickingStepMessage read(FriendlyByteBuf buf) {
      return new TickingStepMessage(buf.readVarInt());
    }

    public static void handle(TickingStepMessage msg, Supplier<NetworkEvent.Context> ctx) {
      if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
        ctx.get()
            .enqueueWork(
                () -> {
                  Minecraft mc = Minecraft.getInstance();
                  if (mc.level != null) {
                    TickRateManager manager =
                        ((ClientTickRateAccess) mc.level).redstoneBackport$clientTickRateManager();
                    manager.setFrozenTicksToRun(msg.frozenTicksToRun);
                  }
                });
      }
      ctx.get().setPacketHandled(true);
    }
  }

  public static final class Broadcaster implements Platform.TickStateBroadcaster {

    @Override
    public void broadcastState(MinecraftServer server, float tickRate, boolean frozen) {
      INSTANCE.send(PacketDistributor.ALL.noArg(), new TickingStateMessage(tickRate, frozen));
    }

    @Override
    public void broadcastStep(MinecraftServer server, int frozenTicksToRun) {
      INSTANCE.send(PacketDistributor.ALL.noArg(), new TickingStepMessage(frozenTicksToRun));
    }

    @Override
    public void sendState(ServerPlayer player, float tickRate, boolean frozen) {
      INSTANCE.send(
          PacketDistributor.PLAYER.with(() -> player), new TickingStateMessage(tickRate, frozen));
    }

    @Override
    public void sendStep(ServerPlayer player, int frozenTicksToRun) {
      INSTANCE.send(
          PacketDistributor.PLAYER.with(() -> player), new TickingStepMessage(frozenTicksToRun));
    }
  }
}
