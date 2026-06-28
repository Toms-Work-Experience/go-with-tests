package com.example.examplemod.event;

import com.example.examplemod.client.KeyBinds;
import com.example.examplemod.item.MaharagaWheelItem;
import com.example.examplemod.network.ModNetwork;
import com.example.examplemod.network.RequestAdaptationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "mahoragawheel", bus = Bus.FORGE, value = Dist.CLIENT)
public class KeyInputEvent {
   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91074_ != null && mc.f_91080_ == null) {
            while (KeyBinds.OPEN_MENU_KEY.m_90859_()) {
               ItemStack helmet = mc.f_91074_.m_6844_(EquipmentSlot.HEAD);
               if (helmet.m_41720_() instanceof MaharagaWheelItem) {
                  ModNetwork.CHANNEL.sendToServer(new RequestAdaptationPacket());
               }
            }
         }
      }
   }
}
