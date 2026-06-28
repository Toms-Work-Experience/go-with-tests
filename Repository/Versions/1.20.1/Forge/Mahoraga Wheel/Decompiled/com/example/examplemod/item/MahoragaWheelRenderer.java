package com.example.examplemod.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class MahoragaWheelRenderer extends GeoArmorRenderer<MaharagaWheelItem> {
   public MahoragaWheelRenderer() {
      super(new MahoragaWheelModel());
   }

   public void preRender(
      PoseStack poseStack,
      MaharagaWheelItem animatable,
      BakedGeoModel model,
      MultiBufferSource bufferSource,
      VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
      poseStack.m_85837_(0.0, -2.5, 0.0);
   }

   protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
      super.applyBaseTransformations(baseModel);
      GeoBone headBone = this.getHeadBone();
      if (headBone != null) {
         headBone.setRotX(0.0F);
         headBone.setRotY(0.0F);
         headBone.setRotZ(0.0F);
      }
   }
}
