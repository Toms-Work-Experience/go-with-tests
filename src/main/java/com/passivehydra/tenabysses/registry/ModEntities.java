package com.passivehydra.tenabysses.registry;

import com.passivehydra.tenabysses.TenAbyssesMod;
import com.passivehydra.tenabysses.entity.TenShadowsShikigamiEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TenAbyssesMod.MOD_ID);

    public static final RegistryObject<EntityType<TenShadowsShikigamiEntity>> SHIKIGAMI = ENTITY_TYPES.register("shikigami", () ->
            EntityType.Builder.of(TenShadowsShikigamiEntity::new, MobCategory.CREATURE)
                    .sized(0.95F, 1.35F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("shikigami"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(SHIKIGAMI.get(), TenShadowsShikigamiEntity.createAttributes().build());
    }
}
