package com.example.examplemod.compat;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class JJKCompat {
   public static final String JUJUTSUCRAFT_MODID = "jujutsucraft";
   public static final String CURSEDFATE_MODID = "cursedfate";
   public static final String JJK_MODID = "cursedfate";
   private static final String[] JUJUTSUCRAFT_MARKERS = new String[]{"jujutsucraft", "jujutsu_craft", "sorceryfight", "sorcery_fight"};
   private static final String[] CURSEDFATE_MARKERS = new String[]{"cursedfate", "cursed_fate"};
   private static final String[] SHARED_MARKERS = new String[]{"jujutsu", "jjk", "cursed"};
   private static final Map<String, String> FRIENDLY_NAMES = new HashMap<>();

   public static boolean isJujutsuCraftLoaded() {
      return true;
   }

   public static boolean isCursedFateLoaded() {
      return true;
   }

   public static boolean isLoaded() {
      return true;
   }

   public static boolean isJJKEntity(Entity entity) {
      if (entity == null) {
         return false;
      }

      String className = entity.getClass().getName().toLowerCase();

      for (String marker : JUJUTSUCRAFT_MARKERS) {
         if (className.contains(marker)) {
            return true;
         }
      }

      for (String marker : CURSEDFATE_MARKERS) {
         if (className.contains(marker)) {
            return true;
         }
      }

      for (String marker : SHARED_MARKERS) {
         if (className.contains(marker)) {
            return true;
         }
      }

      return false;
   }

   private static String getEntityModId(Entity entity) {
      if (entity == null) {
         return null;
      }

      String className = entity.getClass().getName().toLowerCase();

      for (String marker : CURSEDFATE_MARKERS) {
         if (className.contains(marker)) {
            return "cursedfate";
         }
      }

      for (String marker : JUJUTSUCRAFT_MARKERS) {
         if (className.contains(marker)) {
            return "jujutsucraft";
         }
      }

      for (String marker : SHARED_MARKERS) {
         if (className.contains(marker)) {
            return "cursedfate";
         }
      }

      return null;
   }

   public static boolean isGojoEntity(Entity entity) {
      if (entity == null) {
         return false;
      }

      String cn = entity.getClass().getName().toLowerCase();
      return cn.contains("gojo") || cn.contains("limitless");
   }

   public static boolean isGojoDomainEntity(Entity entity) {
      if (entity == null) {
         return false;
      }

      String cn = entity.getClass().getName().toLowerCase();
      return (cn.contains("void") || cn.contains("unlimited") || cn.contains("gojo") || cn.contains("domain")) && isJJKEntity(entity);
   }

   public static boolean isJJKDamage(DamageSource source) {
      String ns = source.m_269150_().m_203543_().map(k -> k.m_135782_().m_135827_()).orElse("minecraft");
      return !ns.equals("jujutsucraft") && !ns.equals("cursedfate") ? isJJKEntity(source.m_7640_()) || isJJKEntity(source.m_7639_()) : true;
   }

   public static boolean isDomainDamage(DamageSource source) {
      Entity direct = source.m_7640_();
      Entity indirect = source.m_7639_();

      for (Entity e : new Entity[]{direct, indirect}) {
         if (e != null) {
            String cn = e.getClass().getName().toLowerCase();
            if (cn.contains("domain")
               || cn.contains("barrier")
               || cn.contains("shrine")
               || cn.contains("void")
               || cn.contains("chimera")
               || cn.contains("coffin")
               || cn.contains("surehit")
               || cn.contains("sure_hit")
               || cn.contains("expansion")
               || cn.contains("unlimited")
               || cn.contains("malevolent")) {
               return true;
            }
         }
      }

      String msgId = source.m_19385_().toLowerCase();
      if (!msgId.contains("domain")
         && !msgId.contains("expansion")
         && !msgId.contains("shrine")
         && !msgId.contains("sure")
         && !msgId.contains("void")
         && !msgId.contains("unlimited")
         && !msgId.contains("malevolent")) {
         String typePath = source.m_269150_().m_203543_().map(k -> k.m_135782_().m_135815_().toLowerCase()).orElse("");
         return typePath.contains("domain") || typePath.contains("shrine") || typePath.contains("void") || typePath.contains("expansion");
      } else {
         return true;
      }
   }

   public static boolean isGojoDomain(DamageSource source) {
      Entity direct = source.m_7640_();
      Entity indirect = source.m_7639_();

      for (Entity e : new Entity[]{direct, indirect}) {
         if (e != null) {
            String cn = e.getClass().getName().toLowerCase();
            if (cn.contains("void") || cn.contains("unlimited") || cn.contains("gojo")) {
               return true;
            }
         }
      }

      String msgId = source.m_19385_().toLowerCase();
      return msgId.contains("void") || msgId.contains("unlimited") || msgId.contains("gojo");
   }

   public static boolean isGojoKey(String key) {
      String low = key.toLowerCase();
      return low.contains("gojo") || low.contains("void") || low.contains("unlimited") || low.contains("limitless") || low.contains("infinity");
   }

   public static String buildDamageKey(DamageSource source) {
      String typeId = source.m_269150_().m_203543_().map(k -> k.m_135782_().toString()).orElse("minecraft:generic");
      Entity directEntity = source.m_7640_();
      Entity indirectEntity = source.m_7639_();
      if (isJJKEntity(directEntity)) {
         return buildEntityKey(directEntity);
      }

      if (isJJKEntity(indirectEntity)) {
         return buildEntityKey(indirectEntity);
      }

      String ns = source.m_269150_().m_203543_().map(k -> k.m_135782_().m_135827_()).orElse("minecraft");
      return !ns.equals("jujutsucraft") && !ns.equals("cursedfate") ? typeId : typeId;
   }

   public static String buildDamageKeyDebug(DamageSource source, Player player) {
      Entity directEntity = source.m_7640_();
      Entity indirectEntity = source.m_7639_();
      String typeId = source.m_269150_().m_203543_().map(k -> k.m_135782_().toString()).orElse("minecraft:generic");
      String directClass = directEntity != null ? directEntity.getClass().getName() : "null";
      String indirectClass = indirectEntity != null ? indirectEntity.getClass().getName() : "null";
      String msgId = source.m_19385_();
      boolean isJJK = isJJKDamage(source);
      player.m_213846_(
         Component.m_237113_("§8[Debug] type=" + typeId + " msgId=" + msgId + " isJJK=" + isJJK + " direct=" + directClass + " indirect=" + indirectClass)
      );
      return buildDamageKey(source);
   }

   private static String buildEntityKey(Entity entity) {
      String simpleName = entity.getClass().getSimpleName();
      String snakeCase = camelToSnake(simpleName);
      String modId = getEntityModId(entity);
      if (modId == null) {
         modId = isCursedFateLoaded() ? "cursedfate" : "jujutsucraft";
      }

      return modId + ":" + snakeCase;
   }

   private static String camelToSnake(String s) {
      return s.replaceAll("([A-Z])", "_$1").toLowerCase().replaceAll("^_+", "");
   }

   public static String getFriendlyName(String key) {
      if (FRIENDLY_NAMES.containsKey(key)) {
         return FRIENDLY_NAMES.get(key);
      }

      String path = key.contains(":") ? key.split(":")[1] : key;
      path = path.replaceAll("_(entity|projectile)$", "");
      String[] words = path.split("_");
      StringBuilder sb = new StringBuilder();

      for (String w : words) {
         if (!w.isEmpty()) {
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
         }
      }

      return sb.toString().trim();
   }

   public static boolean isDomainKey(String key) {
      String low = key.toLowerCase();
      return low.contains("domain")
         || low.contains("shrine")
         || low.contains("void")
         || low.contains("chimera")
         || low.contains("coffin")
         || low.contains("sure_hit")
         || low.contains("expansion")
         || low.contains("love_train");
   }

   public static boolean isJJKKey(String key) {
      return key.startsWith("jujutsucraft:") || key.startsWith("cursedfate:");
   }

   private static void both(String path, String name) {
      FRIENDLY_NAMES.put("jujutsucraft:" + path, name);
      FRIENDLY_NAMES.put("cursedfate:" + path, name);
   }

   static {
      both("infinity", "Infinity (Gojo)");
      both("blue", "Cursed Technique Lapse: Blue");
      both("red", "Cursed Technique Reversal: Red");
      both("hollow_purple", "Hollow Purple");
      both("unlimited_void", "Unlimited Void ▣");
      both("limitless", "Limitless (Gojo)");
      both("dismantle", "Dismantle (Sukuna)");
      both("cleave", "Cleave (Sukuna)");
      both("malevolent_shrine", "Malevolent Shrine ▣");
      both("fire_arrow", "Fire Arrow (Sukuna)");
      both("fuga", "Open / Fuga (Sukuna)");
      both("shrine", "Shrine");
      both("piercing_blood", "Piercing Blood");
      both("blood_manipulation", "Blood Manipulation");
      both("convergence", "Convergence");
      both("slicing_exorcism", "Slicing Exorcism");
      both("blood_rain", "Blood Rain");
      both("blood_edge", "Blood Edge");
      both("disaster_flames", "Disaster Flames");
      both("disaster_flame", "Disaster Flames");
      both("disaster_tides", "Disaster Tides");
      both("coffin_of_the_iron_mountain", "Coffin of the Iron Mountain ▣");
      both("ember_insects", "Ember Insects");
      both("maximum_meteor", "Maximum: Meteor");
      both("star_rage", "Star Rage");
      both("star_rage_punch", "Star Rage Punch");
      both("boogie_woogie", "Boogie Woogie");
      both("love_train", "Private Pure Love Train ▣");
      both("private_pure_love_train", "Private Pure Love Train ▣");
      both("puppet_manipulation", "Puppet Manipulation");
      both("mechamaru", "Mechamaru");
      both("ultra_cannon", "Ultra Cannon");
      both("vessel", "Vessel");
      both("cursed_couple", "Cursed Couple");
      both("rika", "Rika");
      both("cursed_speech", "Cursed Speech");
      both("chimera_shadow_garden", "Chimera Shadow Garden ▣");
      both("ten_shadows", "Ten Shadows Technique");
      both("divine_dog", "Divine Dog");
      both("nue", "Nue");
      both("toad", "Toad");
      both("max_elephant", "Max Elephant");
      both("rabbit_escape", "Rabbit Escape");
      both("mahoraga", "Mahoraga");
      both("heavenly_restriction", "Heavenly Restriction");
      both("cursed_energy", "Cursed Energy");
      both("cursed_energy_melee", "Cursed Strike");
      both("cursed_strike", "Cursed Strike");
      both("black_flash", "Black Flash");
      both("reversed_cursed_technique", "Reversed Cursed Technique");
      both("reverse_cursed_technique", "Reverse Cursed Technique");
      both("divergent_fist", "Divergent Fist");
      both("sure_hit", "Sure-Hit (Domain)");
      both("domain_amplification", "Domain Amplification");
      both("domain_expansion", "Domain Expansion ▣");
      both("domain", "Domain ▣");
      both("binding_vow", "Binding Vow");
      both("simple_domain", "Simple Domain");
      both("falling_blossom_emotion", "Falling Blossom Emotion");
      both("cursed_tool", "Cursed Tool");
      both("inverted_spear", "Inverted Spear of Heaven");
      both("split_soul_katana", "Split Soul Katana");
      both("playful_cloud", "Playful Cloud");
      both("dragon_bone", "Dragon-Bone");
      both("finger_bearer", "Finger Bearer");
      both("finger_bearer_entity", "Finger Bearer");
      both("curse_womb", "Curse Womb");
      both("curse_womb_entity", "Curse Womb");
      both("special_grade_curse", "Special Grade Curse");
      both("special_grade_curse_entity", "Special Grade Curse");
      both("cursed_spirit", "Cursed Spirit");
      both("cursed_spirit_entity", "Cursed Spirit");
      both("curse_user", "Curse User");
      both("curse_user_entity", "Curse User");
      both("sukuna_entity", "Ryomen Sukuna");
      both("gojo_entity", "Satoru Gojo");
      both("domain_entity", "Domain Expansion ▣");
      both("domain_barrier", "Domain Barrier ▣");
      both("domain_barrier_entity", "Domain Barrier ▣");
      both("malevolent_shrine_entity", "Malevolent Shrine ▣");
      both("unlimited_void_entity", "Unlimited Void ▣");
   }
}
