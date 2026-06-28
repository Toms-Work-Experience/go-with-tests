package com.example.examplemod.capability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

public class AdaptationCapability {
   private final Map<String, Integer> pendingHits = new LinkedHashMap<>();
   private final Map<String, Float> adaptations = new LinkedHashMap<>();
   private final Map<String, Integer> cooldowns = new LinkedHashMap<>();
   private final Map<String, Integer> lastHealThreshold = new LinkedHashMap<>();
   private final Set<String> knownTypes = new LinkedHashSet<>();
   private static final float ADAPT_PER_CYCLE = 0.1F;
   public static int COOLDOWN_TICKS = 1000;

   public float getAdaptation(String damageType) {
      return this.adaptations.getOrDefault(damageType, 0.0F);
   }

   public int getPendingHits(String damageType) {
      return this.pendingHits.getOrDefault(damageType, 0);
   }

   public boolean isOnCooldown(String damageType) {
      return this.cooldowns.getOrDefault(damageType, 0) > 0;
   }

   public int getCooldownTicks(String damageType) {
      return this.cooldowns.getOrDefault(damageType, 0);
   }

   public Map<String, Float> getAllAdaptations() {
      return this.adaptations;
   }

   public Map<String, Integer> getAllPendingHits() {
      return this.pendingHits;
   }

   public Map<String, Integer> getAllCooldowns() {
      return this.cooldowns;
   }

   public AdaptationCapability.HitInfo onDamageReceived(String damageType) {
      if (this.getAdaptation(damageType) >= 1.0F) {
         return new AdaptationCapability.HitInfo(AdaptationCapability.HitResult.NONE, damageType);
      }

      boolean isNew = !this.knownTypes.contains(damageType);
      if (isNew) {
         this.knownTypes.add(damageType);
         this.adaptations.put(damageType, 0.0F);
      }

      if (!this.cooldowns.containsKey(damageType)) {
         float currentAdapt = this.adaptations.getOrDefault(damageType, 0.0F);
         int reductionStages = Math.round(currentAdapt * 10.0F);
         float multiplier = 1.0F - reductionStages * 0.05F;
         int calculatedCooldown = Math.max(20, Math.round(COOLDOWN_TICKS * multiplier));
         this.cooldowns.put(damageType, calculatedCooldown);
         return new AdaptationCapability.HitInfo(AdaptationCapability.HitResult.NEW_TYPE, damageType);
      } else {
         return new AdaptationCapability.HitInfo(AdaptationCapability.HitResult.ON_COOLDOWN, damageType);
      }
   }

   public AdaptationCapability.HitInfo onDamageReceived(String damageType, int weight) {
      return this.onDamageReceived(damageType);
   }

   public void tick() {
      for (String key : new ArrayList<>(this.cooldowns.keySet())) {
         int cd = this.cooldowns.get(key) - 1;
         if (cd <= 0) {
            float currentAdapt = this.adaptations.getOrDefault(key, 0.0F);
            if (currentAdapt < 1.0F) {
               float newAdapt = Math.min(1.0F, currentAdapt + 0.1F);
               this.adaptations.put(key, newAdapt);
            }

            this.cooldowns.remove(key);
         } else {
            this.cooldowns.put(key, cd);
         }
      }
   }

   public CompoundTag serializeNBT() {
      CompoundTag tag = new CompoundTag();
      ListTag orderList = new ListTag();

      for (String k : this.knownTypes) {
         orderList.add(StringTag.m_129297_(k));
      }

      tag.m_128365_("order", orderList);
      CompoundTag adaptTag = new CompoundTag();
      this.adaptations.forEach(adaptTag::m_128350_);
      tag.m_128365_("adaptations", adaptTag);
      CompoundTag hitsTag = new CompoundTag();
      this.pendingHits.forEach(hitsTag::m_128405_);
      tag.m_128365_("pendingHits", hitsTag);
      CompoundTag cdTag = new CompoundTag();
      this.cooldowns.forEach(cdTag::m_128405_);
      tag.m_128365_("cooldowns", cdTag);
      CompoundTag healTag = new CompoundTag();
      this.lastHealThreshold.forEach(healTag::m_128405_);
      tag.m_128365_("lastHealThreshold", healTag);
      return tag;
   }

   public void deserializeNBT(CompoundTag tag) {
      this.adaptations.clear();
      this.pendingHits.clear();
      this.cooldowns.clear();
      this.knownTypes.clear();
      this.lastHealThreshold.clear();
      CompoundTag adaptTag = tag.m_128469_("adaptations");
      CompoundTag hitsTag = tag.m_128469_("pendingHits");
      CompoundTag cdTag = tag.m_128469_("cooldowns");
      CompoundTag healTag = tag.m_128469_("lastHealThreshold");
      if (tag.m_128425_("order", 9)) {
         ListTag orderList = tag.m_128437_("order", 8);

         for (int i = 0; i < orderList.size(); i++) {
            String k = orderList.m_128778_(i);
            this.knownTypes.add(k);
            if (adaptTag.m_128441_(k)) {
               this.adaptations.put(k, adaptTag.m_128457_(k));
            }

            if (hitsTag.m_128441_(k)) {
               this.pendingHits.put(k, hitsTag.m_128451_(k));
            }

            if (cdTag.m_128441_(k)) {
               this.cooldowns.put(k, cdTag.m_128451_(k));
            }

            if (healTag.m_128441_(k)) {
               this.lastHealThreshold.put(k, healTag.m_128451_(k));
            }
         }
      } else {
         adaptTag.m_128431_().forEach(kx -> {
            this.adaptations.put(kx, adaptTag.m_128457_(kx));
            this.knownTypes.add(kx);
         });
         hitsTag.m_128431_().forEach(kx -> this.pendingHits.put(kx, hitsTag.m_128451_(kx)));
         cdTag.m_128431_().forEach(kx -> this.cooldowns.put(kx, cdTag.m_128451_(kx)));
         healTag.m_128431_().forEach(kx -> this.lastHealThreshold.put(kx, healTag.m_128451_(kx)));
      }
   }

   public void addAdaptationInstantly(String key, float amount) {
      float current = this.adaptations.getOrDefault(key, 0.0F);
      if (current < 1.0F) {
         float newAdapt = Math.min(1.0F, current + amount);
         this.adaptations.put(key, newAdapt);
         this.knownTypes.add(key);
      }
   }

   public void forceSetAdaptation(String damageType, float value) {
      if (value <= 0.0F) {
         this.adaptations.remove(damageType);
         this.pendingHits.remove(damageType);
         this.cooldowns.remove(damageType);
         this.knownTypes.remove(damageType);
      } else {
         this.adaptations.put(damageType, Math.min(1.0F, value));
         this.knownTypes.add(damageType);
         this.pendingHits.put(damageType, 0);
         this.cooldowns.remove(damageType);
      }
   }

   public void resetAll() {
      this.adaptations.clear();
      this.pendingHits.clear();
      this.cooldowns.clear();
      this.knownTypes.clear();
   }

   public int getLastHealThreshold(String damageType) {
      return this.lastHealThreshold.getOrDefault(damageType, 0);
   }

   public void setLastHealThreshold(String damageType, int threshold) {
      this.lastHealThreshold.put(damageType, threshold);
   }

   public static class HitInfo {
      public final AdaptationCapability.HitResult result;
      public final String damageType;

      public HitInfo(AdaptationCapability.HitResult result, String damageType) {
         this.result = result;
         this.damageType = damageType;
      }
   }

   public enum HitResult {
      NONE,
      NEW_TYPE,
      CYCLE_COMPLETE,
      FULL_IMMUNITY,
      ON_COOLDOWN;
   }
}
