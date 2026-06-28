package com.example.examplemod;

import com.example.examplemod.capability.AdaptationCapability;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@EventBusSubscriber(modid = "mahoragawheel", bus = Bus.MOD)
public class Config {
   private static final Builder BUILDER = new Builder();
   public static final IntValue MAHORAGA_COOLDOWN = BUILDER.comment("Base cooldown in ticks for adaptation (+10%)")
      .defineInRange("mahoragaCooldownTicks", 1000, 20, Integer.MAX_VALUE);
   static final ForgeConfigSpec SPEC = BUILDER.build();
   public static int mahoragaCooldownTicks;

   @SubscribeEvent
   static void onLoad(ModConfigEvent event) {
      mahoragaCooldownTicks = (Integer)MAHORAGA_COOLDOWN.get();
      AdaptationCapability.COOLDOWN_TICKS = mahoragaCooldownTicks;
   }
}
