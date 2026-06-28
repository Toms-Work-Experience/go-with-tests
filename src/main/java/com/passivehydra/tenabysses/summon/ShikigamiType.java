package com.passivehydra.tenabysses.summon;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public enum ShikigamiType {
    DIVINE_DOG_WHITE(
            "divine_dog_white",
        "Divine Dogs",
            "jujutsu_kaisen:textures/entity/divine_dog_white.png",
            20 * 10,
            List.of("Fast melee companion", "Ritual unlock on first kill"),
        Set.of(),
        true
    ),
    DIVINE_DOG_BLACK(
            "divine_dog_black",
        "Divine Dog Black",
            "jujutsu_kaisen:textures/entity/divine_dog_black.png",
            20 * 10,
            List.of("Fast melee companion", "Ritual unlock on first kill"),
        Set.of(),
        false
    ),
    DIVINE_DOG_TOTALITY(
            "divine_dog_totality",
        "Divine Dog Totality",
            "jujutsu_kaisen:textures/entity/divine_dog_totality.png",
            20 * 25,
            List.of("Massive claws and bleed pressure", "Unlocked when a Divine Dog falls"),
        Set.of(),
        true
    ),
    NUE(
            "nue",
        "Nue",
            "jujutsu_kaisen:textures/entity/nue.png",
            20 * 30,
            List.of("Flying attacker", "Lightning pressure and chase"),
        Set.of(),
        true
    ),
    GREAT_SERPENT(
            "great_serpent",
        "Great Serpent",
            "jujutsu_kaisen:textures/entity/great_serpent_head.png",
            20 * 30,
            List.of("Removed"),
        Set.of(),
        false  // disabled by user request
    ),
    MAX_ELEPHANT(
            "max_elephant",
        "Max Elephant",
            "jujutsu_kaisen:textures/entity/max_elephant.png",
            20 * 45,
            List.of("Tank summon", "Water pressure and knockback"),
        Set.of(),
        true
    ),
    ROUND_DEER(
            "round_deer",
        "Round Deer",
            "jujutsu_kaisen:textures/entity/tranquil_deer.png",
            20 * 25,
            List.of("Healing and cleanse support", "Avoids direct combat"),
        Set.of(),
        true
    ),
    PIERCING_OX(
            "piercing_ox",
        "Piercing Ox",
            "jujutsu_kaisen:textures/entity/piercing_bull.png",
            20 * 25,
            List.of("Momentum charge attacker", "High knockback impact"),
        Set.of(),
        true
    ),
    AGITO(
            "agito",
        "Agito",
            "jujutsu_kaisen:textures/entity/agito.png",
            20 * 60,
            List.of("Fusion shikigami", "Requires Nue + Round Deer"),
        Set.of("nue", "round_deer"),
        true
    ),
    MAHORAGA(
            "mahoraga",
        "Eight-Handled Sword Divergent Sila Divine General Mahoraga",
            "jujutsu_kaisen:textures/entity/mahoraga.png",
            20 * 90,
            List.of("Boss-tier adaptive shikigami", "Ritual unlock strongly recommended"),
        Set.of(),
        true
    );

    private final String id;
    private final String displayName;
    private final ResourceLocation iconTexture;
    private final int cooldownTicks;
    private final List<String> tooltipLines;
    private final Set<String> prerequisiteUnlocks;
    private final boolean menuVisible;

    ShikigamiType(
            String id,
        String displayName,
            String iconTexture,
            int cooldownTicks,
            List<String> tooltipLines,
        Set<String> prerequisiteUnlocks,
        boolean menuVisible
    ) {
        this.id = id;
    this.displayName = displayName;
        this.iconTexture = fromString(iconTexture);
        this.cooldownTicks = cooldownTicks;
        this.tooltipLines = tooltipLines;
        this.prerequisiteUnlocks = prerequisiteUnlocks;
    this.menuVisible = menuVisible;
    }

    private static ResourceLocation fromString(String value) {
        String[] split = value.split(":", 2);
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid resource location: " + value);
        }
        return ResourceLocation.fromNamespaceAndPath(split[0], split[1]);
    }

    public String id() {
        return id;
    }

    public ResourceLocation iconTexture() {
        return iconTexture;
    }

    public boolean menuVisible() {
        return menuVisible;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }
    /** Look up a ShikigamiType by its string ID. Returns null for unknown IDs. */
    public static ShikigamiType fromId(String id) {
        if (id == null) return null;
        for (ShikigamiType t : values()) {
            if (t.id().equals(id)) return t;
        }
        return null;
    }    public double getBaseHealth() {
        return switch (this) {
            case DIVINE_DOG_WHITE, DIVINE_DOG_BLACK -> 30.0;
            case DIVINE_DOG_TOTALITY -> 100.0;
            case NUE -> 75.0;
            case GREAT_SERPENT -> 120.0;
            case MAX_ELEPHANT -> 200.0;
            case ROUND_DEER -> 100.0;
            case PIERCING_OX -> 90.0;
            case AGITO -> 150.0;
            case MAHORAGA -> 280.0;
        };
    }

    public double getBaseDamage() {
        return switch (this) {
            case DIVINE_DOG_WHITE, DIVINE_DOG_BLACK -> 8.0;
            case DIVINE_DOG_TOTALITY -> 18.0;
            case NUE -> 14.0;
            case GREAT_SERPENT -> 16.0;
            case MAX_ELEPHANT -> 22.0;
            case ROUND_DEER -> 10.0;
            case PIERCING_OX -> 20.0;
            case AGITO -> 16.0;
            case MAHORAGA -> 30.0;
        };
    }

    public double getBaseSpeed() {
        return switch (this) {
            case DIVINE_DOG_WHITE, DIVINE_DOG_BLACK -> 0.38;
            case DIVINE_DOG_TOTALITY -> 0.33;
            case NUE -> 0.28;
            case GREAT_SERPENT -> 0.32;
            case MAX_ELEPHANT -> 0.22;
            case ROUND_DEER -> 0.28;
            case PIERCING_OX -> 0.35;
            case AGITO -> 0.30;
            case MAHORAGA -> 0.30;
        };
    }

    public double getBaseFollowRange() {
        return switch (this) {
            case DIVINE_DOG_WHITE, DIVINE_DOG_BLACK -> 32.0;
            case DIVINE_DOG_TOTALITY -> 40.0;
            case NUE -> 40.0;
            case GREAT_SERPENT -> 36.0;
            case MAX_ELEPHANT -> 48.0;
            case ROUND_DEER -> 36.0;
            case PIERCING_OX -> 36.0;
            case AGITO -> 48.0;
            case MAHORAGA -> 48.0;
        };
    }
    public List<String> tooltipLines() {
        return tooltipLines;
    }

    public Set<String> prerequisiteUnlocks() {
        return prerequisiteUnlocks;
    }

    public Component displayName() {
        return Component.literal(displayName);
    }

    public static ShikigamiType byId(String value) {
        for (ShikigamiType type : values()) {
            if (type.id.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
