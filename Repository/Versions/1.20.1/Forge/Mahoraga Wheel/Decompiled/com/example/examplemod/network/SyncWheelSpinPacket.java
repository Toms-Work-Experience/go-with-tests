package com.example.examplemod.network;

import com.example.examplemod.item.MaharagaWheelItem;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class SyncWheelSpinPacket {
   private final UUID playerUUID;

   public SyncWheelSpinPacket(UUID playerUUID) {
      this.playerUUID = playerUUID;
   }

   public static void encode(SyncWheelSpinPacket msg, FriendlyByteBuf buf) {
      buf.m_130077_(msg.playerUUID);
   }

   public static SyncWheelSpinPacket decode(FriendlyByteBuf buf) {
      return new SyncWheelSpinPacket(buf.m_130259_());
   }

   public static void handle(SyncWheelSpinPacket msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         if (Minecraft.m_91087_().f_91073_ != null) {
            Player player = Minecraft.m_91087_().f_91073_.m_46003_(msg.playerUUID);
            if (player != null) {
               ItemStack helmet = player.m_6844_(EquipmentSlot.HEAD);
               if (helmet.m_41720_() instanceof MaharagaWheelItem wheelItem) {
                  long instanceId = GeoItem.getId(helmet);
                  AnimatableManager<GeoAnimatable> manager = wheelItem.getAnimatableInstanceCache().getManagerForId(instanceId);
                  if (manager == null) {
                     manager = wheelItem.getAnimatableInstanceCache().getManagerForId(player.m_19879_());
                  }

                  if (manager != null) {
                     manager.tryTriggerAnimation("spin_controller", "spin");
                  }
               }
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
