package com.example.examplemod.registry;

import com.example.examplemod.item.MaharagaWheelItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mahoragawheel");
   public static final RegistryObject<Item> MAHORAGA_WHEEL = ITEMS.register("mahoraga_wheel", MaharagaWheelItem::new);
}
