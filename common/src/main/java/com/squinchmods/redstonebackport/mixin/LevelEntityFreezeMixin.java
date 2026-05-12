package com.squinchmods.redstonebackport.mixin;

import com.squinchmods.redstonebackport.tick.TickRateManagerAccess;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelEntityFreezeMixin {
  @Shadow public boolean isClientSide;

  @Inject(method = "guardEntityTick", at = @At("HEAD"), cancellable = true)
  private <T extends Entity> void redstoneBackport$skipFrozenEntity(
      Consumer<T> ticker, T entity, CallbackInfo ci) {
    if (!this.isClientSide) {
      Level self = (Level) (Object) this;
      if (self instanceof ServerLevel serverLevel) {
        if (((TickRateManagerAccess) serverLevel.getServer())
            .redstoneBackport$tickRateManager()
            .isEntityFrozen(entity)) {
          ci.cancel();
        }
      }
    }
  }
}
