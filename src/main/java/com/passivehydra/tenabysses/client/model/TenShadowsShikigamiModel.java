package com.passivehydra.tenabysses.client.model;

import com.passivehydra.tenabysses.TenAbyssesMod;
import com.passivehydra.tenabysses.entity.TenShadowsShikigamiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;

public class TenShadowsShikigamiModel extends GeoModel<TenShadowsShikigamiEntity> {
    private static final ResourceLocation DEFAULT_MODEL = rl("jujutsu_kaisen", "geo/entity/divine_dog.geo.json");
    private static final ResourceLocation DEFAULT_ANIMATION = rl("jujutsu_kaisen", "animations/entity/divine_dog.animation.json");
    private static final ResourceLocation DEFAULT_TEXTURE = rl("jujutsu_kaisen", "textures/entity/divine_dog_white.png");

    private static final Map<String, ResourceLocation> MODEL_BY_ID = Map.ofEntries(
        Map.entry("divine_dog_white", rl("jujutsu_kaisen", "geo/entity/divine_dog.geo.json")),
        Map.entry("divine_dog_black", rl("jujutsu_kaisen", "geo/entity/divine_dog.geo.json")),
        Map.entry("divine_dog_totality", rl("jujutsu_kaisen", "geo/entity/divine_dog_totality.geo.json")),
        Map.entry("nue", rl("jujutsu_kaisen", "geo/entity/nue.geo.json")),
        Map.entry("great_serpent", rl("jujutsu_kaisen", "geo/entity/great_serpent_head.geo.json")),
        Map.entry("great_serpent_segment", rl("jujutsu_kaisen", "geo/entity/great_serpent_segment.geo.json")),
        Map.entry("max_elephant", rl("jujutsu_kaisen", "geo/entity/max_elephant.geo.json")),
        Map.entry("round_deer", rl("jujutsu_kaisen", "geo/entity/tranquil_deer.geo.json")),
        Map.entry("piercing_ox", rl("jujutsu_kaisen", "geo/entity/piercing_bull.geo.json")),
        Map.entry("agito", rl("jujutsu_kaisen", "geo/entity/agito.geo.json")),
        Map.entry("mahoraga", rl("jujutsu_kaisen", "geo/entity/mahoraga.geo.json"))
    );

    private static final Map<String, ResourceLocation> ANIM_BY_ID = Map.ofEntries(
        Map.entry("divine_dog_white", rl("jujutsu_kaisen", "animations/entity/divine_dog.animation.json")),
        Map.entry("divine_dog_black", rl("jujutsu_kaisen", "animations/entity/divine_dog.animation.json")),
        Map.entry("divine_dog_totality", rl("jujutsu_kaisen", "animations/entity/divine_dog_totality.animation.json")),
        Map.entry("nue", rl("jujutsu_kaisen", "animations/entity/nue.animation.json")),
        Map.entry("great_serpent", rl("jujutsu_kaisen", "animations/entity/great_serpent_head.animation.json")),
        Map.entry("great_serpent_segment", rl("jujutsu_kaisen", "animations/entity/great_serpent_head.animation.json")),
        Map.entry("max_elephant", rl("jujutsu_kaisen", "animations/entity/max_elephant.animation.json")),
        Map.entry("round_deer", rl("jujutsu_kaisen", "animations/entity/tranquil_deer.animation.json")),
        Map.entry("piercing_ox", rl("jujutsu_kaisen", "animations/entity/piercing_bull.animation.json")),
        Map.entry("agito", rl("jujutsu_kaisen", "animations/entity/agito.animation.json")),
        Map.entry("mahoraga", rl("jujutsu_kaisen", "animations/entity/mahoraga.animation.json"))
    );

    private static final Map<String, ResourceLocation> TEX_BY_ID = Map.ofEntries(
        Map.entry("divine_dog_white", rl("jujutsu_kaisen", "textures/entity/divine_dog_white.png")),
        Map.entry("divine_dog_black", rl("jujutsu_kaisen", "textures/entity/divine_dog_black.png")),
        Map.entry("divine_dog_totality", rl("jujutsu_kaisen", "textures/entity/divine_dog_totality.png")),
        Map.entry("nue", rl("jujutsu_kaisen", "textures/entity/nue.png")),
        Map.entry("great_serpent", rl("jujutsu_kaisen", "textures/entity/great_serpent_head.png")),
        Map.entry("great_serpent_segment", rl("jujutsu_kaisen", "textures/entity/great_serpent_segment.png")),
        Map.entry("max_elephant", rl("jujutsu_kaisen", "textures/entity/max_elephant.png")),
        Map.entry("round_deer", rl("jujutsu_kaisen", "textures/entity/tranquil_deer.png")),
        Map.entry("piercing_ox", rl("jujutsu_kaisen", "textures/entity/piercing_bull.png")),
        Map.entry("agito", rl("jujutsu_kaisen", "textures/entity/agito.png")),
        Map.entry("mahoraga", rl("jujutsu_kaisen", "textures/entity/mahoraga.png"))
    );

    private static ResourceLocation rl(String namespace, String path) {
    return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    @Override
    public ResourceLocation getModelResource(TenShadowsShikigamiEntity entity) {
        return MODEL_BY_ID.getOrDefault(entity.getShikigamiId(), DEFAULT_MODEL);
    }

    @Override
    public ResourceLocation getTextureResource(TenShadowsShikigamiEntity entity) {
        return TEX_BY_ID.getOrDefault(entity.getShikigamiId(), DEFAULT_TEXTURE);
    }

    @Override
    public ResourceLocation getAnimationResource(TenShadowsShikigamiEntity entity) {
        return ANIM_BY_ID.getOrDefault(entity.getShikigamiId(), DEFAULT_ANIMATION);
    }
}
