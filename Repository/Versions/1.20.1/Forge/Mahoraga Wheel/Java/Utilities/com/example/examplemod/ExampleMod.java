package com.example.examplemod;

import com.example.examplemod.network.ModNetwork;
import com.example.examplemod.registry.ModCreativeTabs;
import com.example.examplemod.registry.ModItems;
import com.example.examplemod.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("mahoragawheel")
public class ExampleMod {
   public static final String MODID = "mahoragawheel";
   private static final Logger LOGGER = LogUtils.getLogger();

   public ExampleMod() {
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      ModItems.ITEMS.register(modEventBus);
      ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
      ModSounds.SOUNDS.register(modEventBus);
      ModNetwork.register();
      ModLoadingContext.get().registerConfig(Type.SERVER, Config.SPEC);
      LOGGER.info("Mahoraga Wheel mod loading...");
   }
}
