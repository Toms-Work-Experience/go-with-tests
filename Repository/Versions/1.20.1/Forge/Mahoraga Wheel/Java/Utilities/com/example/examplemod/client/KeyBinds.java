package com.example.examplemod.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "mahoragawheel", bus = Bus.MOD, value = Dist.CLIENT)
public class KeyBinds {
   public static final String KEY_CATEGORY = "key.category.mahoragawheel";
   public static final String KEY_OPEN_MENU = "key.mahoragawheel.open_menu";
   public static final KeyMapping OPEN_MENU_KEY = new KeyMapping(
      "key.mahoragawheel.open_menu", KeyConflictContext.IN_GAME, Type.KEYSYM, 346, "key.category.mahoragawheel"
   );

   @SubscribeEvent
   public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
      event.register(OPEN_MENU_KEY);
   }
}
