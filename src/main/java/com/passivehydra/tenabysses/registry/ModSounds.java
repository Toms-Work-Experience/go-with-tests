package com.passivehydra.tenabysses.registry;

import com.passivehydra.tenabysses.TenAbyssesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TenAbyssesMod.MOD_ID);

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(TenAbyssesMod.MOD_ID, name)));
    }

    /** Custom Mahoraga summon audio (place the OGG at
     *  assets/thetenabysses/sounds/entity/mahoraga_summon.ogg) */
        public static final RegistryObject<SoundEvent> MAHORAGA_SUMMON = register("mahoraga.summon");
        public static final RegistryObject<SoundEvent> MAHORAGA_WHEEL = register("mahoraga.wheel");
        public static final RegistryObject<SoundEvent> MAHORAGA_SLAM = register("mahoraga.slam");
        public static final RegistryObject<SoundEvent> DOG_HOWL = register("dog.howl");
        public static final RegistryObject<SoundEvent> NUE_ELECTRIC = register("nue.electric");
        public static final RegistryObject<SoundEvent> AGITO_SPARK = register("agito.spark");
        public static final RegistryObject<SoundEvent> SHIKIGAMI_SLASH = register("shikigami.slash");

    private ModSounds() {
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }
}
