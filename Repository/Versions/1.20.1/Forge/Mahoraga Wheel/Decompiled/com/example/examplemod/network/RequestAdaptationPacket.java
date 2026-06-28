package com.example.examplemod.network;

import com.example.examplemod.capability.AdaptationProvider;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

public class RequestAdaptationPacket {
   public static void encode(RequestAdaptationPacket packet, FriendlyByteBuf buf) {
   }

   public static RequestAdaptationPacket decode(FriendlyByteBuf buf) {
      return new RequestAdaptationPacket();
   }

   public static void handle(RequestAdaptationPacket packet, Supplier<Context> ctx) {
      ctx.get()
         .enqueueWork(
            () -> {
               ServerPlayer player = ctx.get().getSender();
               if (player != null) {
                  player.getCapability(AdaptationProvider.ADAPTATION_CAP)
                     .ifPresent(
                        cap -> ModNetwork.CHANNEL
                           .send(
                              PacketDistributor.PLAYER.with(() -> player),
                              new SyncAdaptationPacket(cap.getAllAdaptations(), cap.getAllPendingHits(), cap.getAllCooldowns())
                           )
                     );
               }
            }
         );
      ctx.get().setPacketHandled(true);
   }
}
