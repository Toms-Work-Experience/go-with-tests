package com.example.examplemod.item;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MahoragaWheelModel extends GeoModel<MaharagaWheelItem> {
   public ResourceLocation getModelResource(MaharagaWheelItem object) {
      return new ResourceLocation("mahoragawheel", "geo/item/mahoraga_wheel.geo.json");
   }

   public ResourceLocation getTextureResource(MaharagaWheelItem object) {
      return new ResourceLocation("mahoragawheel", "textures/item/mahoraga_wheel_texture.png");
   }

   public ResourceLocation getAnimationResource(MaharagaWheelItem animatable) {
      return new ResourceLocation("mahoragawheel", "animations/item/mahoraga_wheel.animation.json");
   }
}
