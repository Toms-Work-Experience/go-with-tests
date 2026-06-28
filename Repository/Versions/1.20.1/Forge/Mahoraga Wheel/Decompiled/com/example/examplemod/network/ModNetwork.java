package com.example.examplemod.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
   private static final String PROTOCOL = "1";
   public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      ResourceLocation.fromNamespaceAndPath("mahoragawheel", "main"), () -> "1", "1"::equals, "1"::equals
   );

   public static void register() {
      CHANNEL.registerMessage(0, SyncAdaptationPacket.class, SyncAdaptationPacket::encode, SyncAdaptationPacket::decode, SyncAdaptationPacket::handle);
      CHANNEL.registerMessage(1, SyncLavaPacket.class, SyncLavaPacket::encode, SyncLavaPacket::decode, SyncLavaPacket::handle);
      CHANNEL.registerMessage(
         2, RequestAdaptationPacket.class, RequestAdaptationPacket::encode, RequestAdaptationPacket::decode, RequestAdaptationPacket::handle
      );
      CHANNEL.registerMessage(3, SyncDomainStatusPacket.class, SyncDomainStatusPacket::encode, SyncDomainStatusPacket::decode, SyncDomainStatusPacket::handle);
      CHANNEL.registerMessage(4, SyncWheelSpinPacket.class, SyncWheelSpinPacket::encode, SyncWheelSpinPacket::decode, SyncWheelSpinPacket::handle);
   }
}
