package com.example.examplemod.network;

import com.example.examplemod.client.ClientLavaData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class SyncDomainStatusPacket {
   public final int durationTicks;

   public SyncDomainStatusPacket(int durationTicks) {
      this.durationTicks = durationTicks;
   }

   public static void encode(SyncDomainStatusPacket packet, FriendlyByteBuf buf) {
      buf.writeInt(packet.durationTicks);
   }

   public static SyncDomainStatusPacket decode(FriendlyByteBuf buf) {
      return new SyncDomainStatusPacket(buf.readInt());
   }

   public static void handle(SyncDomainStatusPacket packet, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         if (packet.durationTicks > ClientLavaData.domainActiveTimer) {
            ClientLavaData.domainActiveTimer = packet.durationTicks;
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
