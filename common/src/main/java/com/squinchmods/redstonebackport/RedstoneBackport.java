package com.squinchmods.redstonebackport;

import net.minecraft.resources.ResourceLocation;

public class RedstoneBackport {
  public static final String MOD_ID = "redstone_backport";

  public static ResourceLocation id(String path) {
    return new ResourceLocation(MOD_ID, path);
  }
}
