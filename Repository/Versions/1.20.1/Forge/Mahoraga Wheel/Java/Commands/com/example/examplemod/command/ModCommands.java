package com.example.examplemod.command;

import com.example.examplemod.Config;
import com.example.examplemod.capability.AdaptationCapability;
import com.example.examplemod.capability.AdaptationProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class ModCommands {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                              "mahoragawheel"
                           )
                           .requires(src -> src.m_6761_(2)))
                        .then(
                           Commands.m_82127_("fullyadapt")
                              .then(
                                 Commands.m_82129_("target", EntityArgument.m_91460_())
                                    .then(Commands.m_82129_("damageType", StringArgumentType.greedyString()).executes(ctx -> {
                                       String damageType = StringArgumentType.getString(ctx, "damageType");
                                       Collection<? extends Entity> targets = EntityArgument.m_91461_(ctx, "target");
                                       return fullyAdapt((CommandSourceStack)ctx.getSource(), damageType, targets);
                                    }))
                              )
                        ))
                     .then(
                        Commands.m_82127_("resetadapt")
                           .then(
                              Commands.m_82129_("target", EntityArgument.m_91460_())
                                 .then(Commands.m_82129_("damageType", StringArgumentType.greedyString()).executes(ctx -> {
                                    String damageType = StringArgumentType.getString(ctx, "damageType");
                                    Collection<? extends Entity> targets = EntityArgument.m_91461_(ctx, "target");
                                    return fullyAdapt((CommandSourceStack)ctx.getSource(), damageType, targets);
                                 }))
                           )
                     ))
                  .then(Commands.m_82127_("resetall").then(Commands.m_82129_("target", EntityArgument.m_91460_()).executes(ctx -> {
                     Collection<? extends Entity> targets = EntityArgument.m_91461_(ctx, "target");
                     return resetAll((CommandSourceStack)ctx.getSource(), targets);
                  }))))
               .then(Commands.m_82127_("checkadapt").then(Commands.m_82129_("target", EntityArgument.m_91449_()).executes(ctx -> {
                  Entity target = EntityArgument.m_91452_(ctx, "target");
                  return checkAdapt((CommandSourceStack)ctx.getSource(), target);
               }))))
            .then(
               Commands.m_82127_("cooldown")
                  .then(
                     Commands.m_82129_("seconds", IntegerArgumentType.integer(1))
                        .executes(
                           ctx -> {
                              int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                              int ticks = seconds * 20;
                              Config.MAHORAGA_COOLDOWN.set(ticks);
                              Config.MAHORAGA_COOLDOWN.save();
                              AdaptationCapability.COOLDOWN_TICKS = ticks;
                              ((CommandSourceStack)ctx.getSource())
                                 .m_288197_(
                                    () -> Component.m_237113_("§6[Mahoraga] §aAdaptation time set to §e" + seconds + " seconds §a(+10%) and saved permanently."),
                                    true
                                 );
                              return 1;
                           }
                        )
                  )
            )
      );
   }

   private static int fullyAdapt(CommandSourceStack src, String damageType, Collection<? extends Entity> targets) {
      int count = 0;

      for (Entity entity : targets) {
         if (entity instanceof LivingEntity living) {
            living.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> cap.forceSetAdaptation(damageType, 1.0F));
            src.m_288197_(() -> Component.m_237113_("§6[Mahoraga] §aSet full adaptation to §e" + damageType + " §afor §e" + entity.m_7755_().getString()), true);
            count++;
         }
      }

      if (count == 0) {
         src.m_81352_(Component.m_237113_("§cNo valid living entities found"));
      }

      return count;
   }

   private static int resetAdapt(CommandSourceStack src, String damageType, Collection<? extends Entity> targets) {
      int count = 0;

      for (Entity entity : targets) {
         if (entity instanceof LivingEntity living) {
            living.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> cap.forceSetAdaptation(damageType, 0.0F));
            src.m_288197_(() -> Component.m_237113_("§6[Mahoraga] §7Reset adaptation to §e" + damageType + " §7for §e" + entity.m_7755_().getString()), true);
            count++;
         }
      }

      return count;
   }

   private static int resetAll(CommandSourceStack src, Collection<? extends Entity> targets) {
      int count = 0;

      for (Entity entity : targets) {
         if (entity instanceof LivingEntity living) {
            living.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> cap.resetAll());
            src.m_288197_(() -> Component.m_237113_("§6[Mahoraga] §cReset ALL adaptations for §e" + entity.m_7755_().getString()), true);
            count++;
         }
      }

      return count;
   }

   private static int checkAdapt(CommandSourceStack src, Entity target) {
      if (target instanceof LivingEntity living) {
         living.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {
            src.m_288197_(() -> Component.m_237113_("§6=== Adaptations of " + target.m_7755_().getString() + " ==="), false);
            Map<String, Float> adaptations = cap.getAllAdaptations();
            if (adaptations.isEmpty()) {
               src.m_288197_(() -> Component.m_237113_("§7None"), false);
            } else {
               adaptations.forEach((type, value) -> {
                  int percent = (int)(value * 100.0F);
                  int hits = cap.getPendingHits(type);
                  src.m_288197_(() -> Component.m_237113_("§e" + type + " §f→ §a" + percent + "% §7(pending hits: " + hits + ")"), false);
               });
            }
         });
         return 1;
      } else {
         src.m_81352_(Component.m_237113_("§cTarget is not a living entity"));
         return 0;
      }
   }
}
