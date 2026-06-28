package com.passivehydra.tenabysses;

import com.passivehydra.tenabysses.compat.CursedFateCompat;
import com.passivehydra.tenabysses.compat.MahoragaWheelCompat;
import com.passivehydra.tenabysses.client.ClientHooks;
import com.passivehydra.tenabysses.network.ModNetwork;
import com.passivehydra.tenabysses.player.PlayerProgressEvents;
import com.passivehydra.tenabysses.registry.ModEntities;
import com.passivehydra.tenabysses.registry.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;

@Mod(TenAbyssesMod.MOD_ID)
public class TenAbyssesMod {
    public static final String MOD_ID = "thetenabysses";

    public TenAbyssesMod() {
        GeckoLib.initialize();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.register(modBus);
        ModSounds.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onEntityAttributes);
        modBus.addListener(PlayerProgressEvents::onRegisterCapabilities);

        MinecraftForge.EVENT_BUS.register(PlayerProgressEvents.class);
        MinecraftForge.EVENT_BUS.register(CursedFateCompat.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::register);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.init();
            MahoragaWheelCompat.configure();
        });
    }

    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        ModEntities.onAttributes(event);
    }
}
