package com.example.examplemod.event;

import com.example.examplemod.capability.AdaptationCapability;
import com.example.examplemod.capability.AdaptationProvider;
import com.example.examplemod.command.ModCommands;
import com.example.examplemod.compat.JJKCompat;
import com.example.examplemod.item.MaharagaWheelItem;
import com.example.examplemod.network.ModNetwork;
import com.example.examplemod.network.SyncDomainStatusPacket;
import com.example.examplemod.network.SyncWheelSpinPacket;
import com.example.examplemod.registry.ModSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent.Applicable;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = "mahoragawheel", bus = Bus.FORGE)
public class ModEvents {
   public static final String INFINITY_KEY_JC = "jujutsucraft:infinity";
   private static final String PERSISTENT_KEY = "mahoragawheel_adaptation_backup";
   private static final Map<UUID, Integer> jjkImmuneUntil = new HashMap<>();

   private static boolean isJJKImmuneActive(Player player) {
      return jjkImmuneUntil.containsKey(player.m_20148_()) && player.f_19797_ <= jjkImmuneUntil.get(player.m_20148_());
   }

   @SubscribeEvent
   public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
      if (event.getObject() instanceof Player) {
         event.addCapability(ResourceLocation.fromNamespaceAndPath("mahoragawheel", "adaptation"), new AdaptationProvider());
      }
   }

   @SubscribeEvent
   public static void onPlayerAttackEntity(AttackEntityEvent event) {
      Player player = event.getEntity();
      Entity target = event.getTarget();
      ItemStack helmet = player.m_6844_(EquipmentSlot.HEAD);
      if (helmet.m_41720_() instanceof MaharagaWheelItem) {
         if (JJKCompat.isLoaded()) {
            if (JJKCompat.isGojoEntity(target)) {
               player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {
                  String infinityKey = getInfinityKeyForEntity(target);
                  if (infinityKey != null) {
                     float currentAdapt = cap.getAdaptation(infinityKey);
                     if (!(currentAdapt >= 1.0F)) {
                        cap.onDamageReceived(infinityKey);
                     }
                  }
               });
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onLivingAttackDefend(LivingAttackEvent event) {
      LivingEntity entity = event.getEntity();
      DamageSource source = event.getSource();
      if (entity instanceof Player player) {
         ItemStack helmet = player.m_6844_(EquipmentSlot.HEAD);
         if (helmet.m_41720_() instanceof MaharagaWheelItem) {
            player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {
               String damageKey = buildKey(source);
               if (cap.getAdaptation(damageKey) >= 1.0F) {
                  event.setCanceled(true);
                  if (JJKCompat.isLoaded() && JJKCompat.isGojoDomain(source)) {
                     jjkImmuneUntil.put(player.m_20148_(), player.f_19797_ + 60);
                     if (player instanceof ServerPlayer sp) {
                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncDomainStatusPacket(60));
                     }
                  }
               }
            });
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void onInfinityBypass(LivingAttackEvent event) {
      if (event.isCanceled()) {
         Entity target = event.getEntity();
         if (JJKCompat.isLoaded() && JJKCompat.isJJKEntity(target)) {
            Player player = findPlayerWithInfinityBypass(event.getSource(), target);
            if (player != null) {
               event.setCanceled(false);
            }
         }
      }
   }

   private static Player findPlayerWithInfinityBypass(DamageSource source, Entity target) {
      if (source.m_7639_() instanceof Player p && hasInfinityBypass(p)) {
         return p;
      } else if (source.m_7640_() instanceof Player p && hasInfinityBypass(p)) {
         return p;
      } else if (source.m_7640_() instanceof Projectile proj && proj.m_19749_() instanceof Player p && hasInfinityBypass(p)) {
         return p;
      } else {
         if (target.m_9236_() != null) {
            for (Player p : target.m_9236_().m_45976_(Player.class, target.m_20191_().m_82400_(32.0))) {
               if (hasInfinityBypass(p)) {
                  return p;
               }
            }
         }

         return null;
      }
   }

   private static boolean hasInfinityBypass(Player player) {
      ItemStack helmet = player.m_6844_(EquipmentSlot.HEAD);
      return !(helmet.m_41720_() instanceof MaharagaWheelItem)
         ? false
         : player.getCapability(AdaptationProvider.ADAPTATION_CAP).map(cap -> cap.getAdaptation("jujutsucraft:infinity") >= 1.0F).orElse(false);
   }

   private static String getInfinityKeyForEntity(Entity entity) {
      if (entity == null) {
         return null;
      }

      String cn = entity.getClass().getName().toLowerCase();

      for (String marker : new String[]{"jujutsucraft", "jujutsu_craft", "sorceryfight", "sorcery_fight"}) {
         if (cn.contains(marker)) {
            return "jujutsucraft:infinity";
         }
      }

      return null;
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onEffectApply(Applicable event) {
      if (event.getEntity() instanceof Player player) {
         ItemStack helmet = player.m_6844_(EquipmentSlot.HEAD);
         if (helmet.m_41720_() instanceof MaharagaWheelItem) {
            MobEffectInstance effectInst = event.getEffectInstance();
            if (effectInst != null) {
               if (effectInst.m_19544_().m_19483_() == MobEffectCategory.HARMFUL) {
                  if (JJKCompat.isLoaded()) {
                     player.getCapability(AdaptationProvider.ADAPTATION_CAP)
                        .ifPresent(
                           cap -> {
                              boolean hasGojoImmunity = cap.getAllAdaptations()
                                 .entrySet()
                                 .stream()
                                 .anyMatch(e -> JJKCompat.isGojoKey(e.getKey()) && e.getValue() >= 1.0F);
                              if (hasGojoImmunity) {
                                 event.setResult(Result.DENY);
                              }
                           }
                        );
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onKnockBack(LivingKnockBackEvent event) {
      if (event.getEntity() instanceof Player player) {
         ItemStack helmet = player.m_6844_(EquipmentSlot.HEAD);
         if (helmet.m_41720_() instanceof MaharagaWheelItem) {
            DamageSource lastSource = player.m_21225_();
            if (lastSource != null) {
               player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {
                  String key = buildKey(lastSource);
                  float adapt = cap.getAdaptation(key);
                  if (adapt >= 1.0F) {
                     event.setCanceled(true);
                  } else if (adapt > 0.3F) {
                     event.setStrength(event.getStrength() * (1.0F - adapt));
                  }
               });
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onPlayerAttackEnemy(LivingHurtEvent event) {
      if (event.getSource().m_7639_() instanceof Player player) {
         if (!(event.getAmount() <= 0.0F)) {
            ItemStack helmet = player.m_6844_(EquipmentSlot.HEAD);
            if (helmet.m_41720_() instanceof MaharagaWheelItem) {
               LivingEntity target = event.getEntity();
               boolean hasArmor = target.m_21230_() > 0;
               boolean hasResist = target.m_21023_(MobEffects.f_19606_);
               if (hasArmor || hasResist) {
                  String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.m_6095_()).toString();
                  String resistKey = entityId + "_resistance";
                  player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {
                     float adapt = cap.getAdaptation(resistKey);
                     if (adapt >= 1.0F) {
                        float rawDamage = event.getAmount();
                        event.setCanceled(true);
                        float newHealth = target.m_21223_() - rawDamage;
                        target.m_21153_(Math.max(0.0F, newHealth));
                        target.f_19802_ = 0;
                        target.m_6469_(target.m_269291_().m_269264_(), 1.0E-4F);
                        if (newHealth <= 0.0F) {
                           target.m_6667_(event.getSource());
                        }
                     } else {
                        cap.onDamageReceived(resistKey);
                     }
                  });
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLivingHurt(LivingHurtEvent event) {
      LivingEntity entity = event.getEntity();
      DamageSource source = event.getSource();
      if (entity instanceof Player player) {
         ItemStack helmet = player.m_6844_(EquipmentSlot.HEAD);
         if (helmet.m_41720_() instanceof MaharagaWheelItem) {
            entity.getCapability(AdaptationProvider.ADAPTATION_CAP)
               .ifPresent(
                  cap -> {
                     boolean jjkLoaded = JJKCompat.isLoaded();
                     String damageKey;
                     if (jjkLoaded && JJKCompat.isJJKDamage(source)) {
                        damageKey = JJKCompat.buildDamageKey(source);
                        boolean isJJK = true;
                        boolean isDomain = JJKCompat.isDomainDamage(source);
                     } else {
                        damageKey = source.m_269150_().m_203543_().map(k -> k.m_135782_().toString()).orElse("minecraft:generic");
                        if ((damageKey.equals("minecraft:mob_attack") || damageKey.equals("minecraft:player_attack")) && source.m_7639_() != null) {
                           String entityId = ForgeRegistries.ENTITY_TYPES.getKey(source.m_7639_().m_6095_()).toString();
                           damageKey = entityId + "_melee";
                        }

                        boolean isJJK = false;
                        boolean isDomain = false;
                     }

                     float adaptation = cap.getAdaptation(damageKey);
                     if (adaptation >= 1.0F) {
                        event.setCanceled(true);
                        boolean isGojoDomain = JJKCompat.isGojoDomain(source);
                        if (isGojoDomain && entity instanceof ServerPlayer sp) {
                           jjkImmuneUntil.put(sp.m_20148_(), sp.f_19797_ + 60);
                           ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncDomainStatusPacket(60));
                        }

                        player.m_21220_()
                           .stream()
                           .filter(e -> e.m_19544_().m_19483_() == MobEffectCategory.HARMFUL)
                           .map(e -> e.m_19544_())
                           .toList()
                           .forEach(player::m_21195_);
                     } else {
                        if (adaptation > 0.0F) {
                           event.setAmount(event.getAmount() * (1.0F - adaptation));
                        }

                        if (JJKCompat.isGojoDomain(source) && entity instanceof ServerPlayer sp) {
                           ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncDomainStatusPacket(20));
                        }

                        AdaptationCapability.HitInfo info = cap.onDamageReceived(damageKey);
                     }
                  }
               );
         }
      }
   }

   @SubscribeEvent
   public static void onLivingTick(LivingTickEvent event) {
      LivingEntity entity = event.getEntity();
      if (entity instanceof Player player) {
         ItemStack helmet = player.m_6844_(EquipmentSlot.HEAD);
         boolean hasHelmet = helmet.m_41720_() instanceof MaharagaWheelItem;
         player.getCapability(AdaptationProvider.ADAPTATION_CAP)
            .ifPresent(
               cap -> {
                  if (hasHelmet && !player.m_9236_().m_5776_()) {
                     cap.tick();
                     cap.getAllAdaptations()
                        .forEach(
                           (key, adapt) -> {
                              int pct = Math.round(adapt * 100.0F);
                              int currentThreshold = pct / 20 * 20;
                              int lastThreshold = cap.getLastHealThreshold(key);
                              if (currentThreshold > lastThreshold && currentThreshold > 0) {
                                 cap.setLastHealThreshold(key, currentThreshold);
                                 boolean isDomain = key.toLowerCase().contains("domain");
                                 player.m_5634_(isDomain ? 15.0F : 10.0F);
                                 if (currentThreshold == 100) {
                                    player.m_9236_()
                                       .m_6263_(
                                          null,
                                          player.m_20185_(),
                                          player.m_20186_(),
                                          player.m_20189_(),
                                          (SoundEvent)ModSounds.ADAPTATION_SPIN.get(),
                                          SoundSource.PLAYERS,
                                          1.0F,
                                          1.0F
                                       );
                                    if (player.m_9236_() instanceof ServerLevel sl) {
                                       sl.m_8767_(
                                          ParticleTypes.f_123797_, player.m_20185_(), player.m_20186_() + 2.0, player.m_20189_(), 10, 0.2, 0.2, 0.2, 0.05
                                       );
                                       ModNetwork.CHANNEL
                                          .send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new SyncWheelSpinPacket(player.m_20148_()));
                                    }
                                 }
                              }
                           }
                        );
                  }

                  if (entity.f_19797_ % 5 == 0 && hasHelmet && JJKCompat.isLoaded()) {
                     boolean hasGojoImmunity = cap.getAllAdaptations()
                        .entrySet()
                        .stream()
                        .anyMatch(e -> JJKCompat.isGojoKey(e.getKey()) && e.getValue() >= 1.0F);
                     if (hasGojoImmunity) {
                        player.m_21220_()
                           .stream()
                           .filter(e -> e.m_19544_().m_19483_() == MobEffectCategory.HARMFUL)
                           .map(e -> e.m_19544_())
                           .toList()
                           .forEach(player::m_21195_);

                        for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
                           ItemStack stack = player.m_150109_().m_8020_(i);
                           if (!stack.m_41619_() && player.m_36335_().m_41519_(stack.m_41720_())) {
                              player.m_36335_().m_41527_(stack.m_41720_());
                           }
                        }

                        ItemStack offhand = player.m_21206_();
                        if (!offhand.m_41619_() && player.m_36335_().m_41519_(offhand.m_41720_())) {
                           player.m_36335_().m_41527_(offhand.m_41720_());
                        }
                     }
                  }
               }
            );
         if (entity.f_19797_ % 5 == 0) {
            jjkImmuneUntil.entrySet().removeIf(entry -> player.f_19797_ > entry.getValue() + 200);
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerClone(Clone event) {
      if (event.isWasDeath()) {
         event.getEntity().getPersistentData().m_128473_("mahoragawheel_adaptation_backup");
      } else {
         event.getOriginal().reviveCaps();
         event.getOriginal()
            .getCapability(AdaptationProvider.ADAPTATION_CAP)
            .ifPresent(
               oldCap -> event.getEntity().getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(newCap -> newCap.deserializeNBT(oldCap.serializeNBT()))
            );
         event.getOriginal().invalidateCaps();
      }
   }

   @SubscribeEvent
   public static void onPlayerLogin(PlayerLoggedInEvent event) {
      Player player = event.getEntity();
      player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {
         if (cap.getAllAdaptations().isEmpty()) {
            CompoundTag persistent = player.getPersistentData();
            if (persistent.m_128441_("mahoragawheel_adaptation_backup")) {
               CompoundTag backup = persistent.m_128469_("mahoragawheel_adaptation_backup");
               cap.deserializeNBT(backup);
            }
         }
      });
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerLoggedOutEvent event) {
      Player player = event.getEntity();
      player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {
         if (!cap.getAllAdaptations().isEmpty()) {
            player.getPersistentData().m_128365_("mahoragawheel_adaptation_backup", cap.serializeNBT());
         }
      });
   }

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent event) {
      ModCommands.register(event.getDispatcher());
   }

   private static String buildKey(DamageSource source) {
      return JJKCompat.isLoaded() && JJKCompat.isJJKDamage(source)
         ? JJKCompat.buildDamageKey(source)
         : source.m_269150_().m_203543_().map(k -> k.m_135782_().toString()).orElse("minecraft:generic");
   }
}
