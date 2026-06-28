package com.passivehydra.tenabysses.client.render;

import com.passivehydra.tenabysses.client.model.TenShadowsShikigamiModel;
import com.passivehydra.tenabysses.entity.TenShadowsShikigamiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TenShadowsShikigamiRenderer extends GeoEntityRenderer<TenShadowsShikigamiEntity> {
    public TenShadowsShikigamiRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TenShadowsShikigamiModel());
        this.shadowRadius = 0.7F;
    }
}
