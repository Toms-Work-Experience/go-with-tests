package com.example.examplemod.event;

import com.example.examplemod.capability.AdaptationProvider;
import com.example.examplemod.client.ClientLavaData;
import com.example.examplemod.item.MaharagaWheelItem;
import com.example.examplemod.network.ModNetwork;
import com.example.examplemod.network.SyncLavaPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.PacketDistributor;

@EventBusSubscriber(modid = "mahoragawheel", bus = Bus.FORGE)
public class LavaSwimEvent {
   private static final Map<UUID, Integer> lavaTickMap = new HashMap<>();
   private static final int TICKS_PER_CYCLE = 200;
   private static final String LAVA_ADAPT_KEY = "mahoraga:lava_slowliness";

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END) {
         Player player = event.player;
         if (!player.m_9236_().f_46443_ && player instanceof ServerPlayer serverPlayer) {
            UUID uuid = player.m_20148_();
            if (player.m_20077_()) {
               ItemStack helmet = player.m_6844_(EquipmentSlot.HEAD);
               if (!(helmet.m_41720_() instanceof MaharagaWheelItem)) {
                  return;
               }

               int ticks = lavaTickMap.getOrDefault(uuid, 0) + 1;
               lavaTickMap.put(uuid, ticks);
               if (ticks == 1) {
                  player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {});
               }

               if (ticks % 200 == 0) {
                  player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {
                     float current = cap.getAdaptation("mahoraga:lava_slowliness");
                     if (!(current >= 1.0F)) {
                        float newAdapt = Math.min(1.0F, current + 0.1F);
                        cap.forceSetAdaptation("mahoraga:lava_slowliness", newAdapt);
                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncLavaPacket(newAdapt));
                     }
                  });
               }

               if (ticks % 100 == 0) {
                  player.getCapability(AdaptationProvider.ADAPTATION_CAP)
                     .ifPresent(
                        cap -> ModNetwork.CHANNEL
                           .send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncLavaPacket(cap.getAdaptation("mahoraga:lava_slowliness")))
                     );
               }
            } else if (lavaTickMap.containsKey(uuid)) {
               int ticks = lavaTickMap.get(uuid);
               lavaTickMap.put(uuid, ticks / 200 * 200);
            }
         }

         if (player.m_9236_().f_46443_ && player.m_20077_()) {
            applyLavaMovement(player, ClientLavaData.lavaAdaptation);
         }
      }
   }

   private static void applyLavaMovement(Player player, float adapt) {
      if (!(adapt < 0.3F)) {
         double mx = player.m_20184_().f_82479_;
         double mz = player.m_20184_().f_82481_;
         if (Math.abs(mx) > 0.001 || Math.abs(mz) > 0.001) {
            double multiplier = 1.0 + adapt * 0.8;
            player.m_20334_(mx * multiplier, player.m_20184_().f_82480_, mz * multiplier);
         }
      }
   }
}
