package com.example.examplemod.network;

import com.example.examplemod.client.screen.MaharagaWheelScreen;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class SyncAdaptationPacket {
   public final Map<String, Float> adaptations;
   public final Map<String, Integer> pendingHits;
   public final Map<String, Integer> cooldowns;

   public SyncAdaptationPacket(Map<String, Float> adaptations, Map<String, Integer> pendingHits, Map<String, Integer> cooldowns) {
      this.adaptations = adaptations;
      this.pendingHits = pendingHits;
      this.cooldowns = cooldowns;
   }

   public static void encode(SyncAdaptationPacket packet, FriendlyByteBuf buf) {
      buf.writeInt(packet.adaptations.size());
      packet.adaptations.forEach((k, v) -> {
         buf.m_130070_(k);
         buf.writeFloat(v);
      });
      buf.writeInt(packet.pendingHits.size());
      packet.pendingHits.forEach((k, v) -> {
         buf.m_130070_(k);
         buf.writeInt(v);
      });
      buf.writeInt(packet.cooldowns.size());
      packet.cooldowns.forEach((k, v) -> {
         buf.m_130070_(k);
         buf.writeInt(v);
      });
   }

   public static SyncAdaptationPacket decode(FriendlyByteBuf buf) {
      Map<String, Float> adaptations = new LinkedHashMap<>();
      int aSize = buf.readInt();

      for (int i = 0; i < aSize; i++) {
         adaptations.put(buf.m_130277_(), buf.readFloat());
      }

      Map<String, Integer> pendingHits = new LinkedHashMap<>();
      int hSize = buf.readInt();

      for (int i = 0; i < hSize; i++) {
         pendingHits.put(buf.m_130277_(), buf.readInt());
      }

      Map<String, Integer> cooldowns = new LinkedHashMap<>();
      int cSize = buf.readInt();

      for (int i = 0; i < cSize; i++) {
         cooldowns.put(buf.m_130277_(), buf.readInt());
      }

      return new SyncAdaptationPacket(adaptations, pendingHits, cooldowns);
   }

   public static void handle(SyncAdaptationPacket packet, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         adaptations = new HashMap<>(packet.adaptations);
         MaharagaWheelScreen.openWithData(packet.adaptations, packet.pendingHits, packet.cooldowns);
      });
      ctx.get().setPacketHandled(true);
   }
}
