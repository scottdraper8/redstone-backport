package com.squinchmods.redstonebackport.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public class WitchRedstoneModifier extends LootModifier {
  public static final Supplier<Codec<WitchRedstoneModifier>> CODEC =
      Suppliers.memoize(
          () ->
              RecordCodecBuilder.create(
                  inst -> codecStart(inst).apply(inst, WitchRedstoneModifier::new)));

  public WitchRedstoneModifier(LootItemCondition[] conditionsIn) {
    super(conditionsIn);
  }

  @Override
  protected @NotNull ObjectArrayList<ItemStack> doApply(
      ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
    // Remove existing redstone drops to avoid double-dropping
    generatedLoot.removeIf(stack -> stack.is(Items.REDSTONE));

    // Add 4-8 redstone, +1 per looting level
    int looting = context.getLootingModifier();
    int count = context.getRandom().nextInt(5) + 4 + looting;

    generatedLoot.add(new ItemStack(Items.REDSTONE, count));
    return generatedLoot;
  }

  @Override
  public Codec<? extends IGlobalLootModifier> codec() {
    return CODEC.get();
  }
}
