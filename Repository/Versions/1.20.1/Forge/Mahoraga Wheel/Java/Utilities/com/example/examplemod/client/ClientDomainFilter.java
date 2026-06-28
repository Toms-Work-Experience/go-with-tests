package com.example.examplemod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "mahoragawheel", bus = Bus.FORGE, value = Dist.CLIENT)
public class ClientDomainFilter {
   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         if (ClientLavaData.domainActiveTimer > 0) {
            ClientLavaData.domainActiveTimer--;
         }
      }
   }
}
