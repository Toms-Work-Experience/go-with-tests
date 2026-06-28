package com.example.examplemod.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "mahoragawheel");
   public static final RegistryObject<SoundEvent> ADAPTATION_SPIN = SOUNDS.register(
      "adaptation_spin", () -> SoundEvent.m_262824_(new ResourceLocation("mahoragawheel", "adaptation_spin"))
   );
}
