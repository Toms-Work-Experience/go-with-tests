package com.example.examplemod.event;

import com.example.examplemod.capability.AdaptationCapability;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "mahoragawheel", bus = Bus.MOD)
public class ModBusEvents {
   @SubscribeEvent
   public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
      event.register(AdaptationCapability.class);
   }
}
