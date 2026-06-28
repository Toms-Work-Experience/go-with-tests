package com.example.examplemod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.f_279569_, "mahoragawheel");
   public static final RegistryObject<CreativeModeTab> MAHORAGA_TAB = CREATIVE_MODE_TABS.register(
      "mahoraga_tab",
      () -> CreativeModeTab.builder()
         .m_257737_(() -> new ItemStack((ItemLike)ModItems.MAHORAGA_WHEEL.get()))
         .m_257941_(Component.m_237113_("Mahoraga Mod"))
         .m_257501_((pParameters, pOutput) -> pOutput.m_246326_((ItemLike)ModItems.MAHORAGA_WHEEL.get()))
         .m_257652_()
   );
}
