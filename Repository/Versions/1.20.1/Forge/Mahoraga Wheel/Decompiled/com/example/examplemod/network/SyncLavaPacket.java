package com.example.examplemod.network;

import com.example.examplemod.client.ClientLavaData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class SyncLavaPacket {
   public final float adaptation;

   public SyncLavaPacket(float adaptation) {
      this.adaptation = adaptation;
   }

   public static void encode(SyncLavaPacket packet, FriendlyByteBuf buf) {
      buf.writeFloat(packet.adaptation);
   }

   public static SyncLavaPacket decode(FriendlyByteBuf buf) {
      return new SyncLavaPacket(buf.readFloat());
   }

   public static void handle(SyncLavaPacket packet, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> ClientLavaData.lavaAdaptation = packet.adaptation);
      ctx.get().setPacketHandled(true);
   }
}
