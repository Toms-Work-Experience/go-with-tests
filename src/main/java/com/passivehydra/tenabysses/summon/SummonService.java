package com.passivehydra.tenabysses.summon;

import com.passivehydra.tenabysses.compat.MahoragaWheelCompat;
import com.passivehydra.tenabysses.network.ModNetwork;
import com.passivehydra.tenabysses.network.S2CPlayerProgressPacket;
import com.passivehydra.tenabysses.player.ModCapabilities;
import com.passivehydra.tenabysses.player.PlayerProgressData;
import com.passivehydra.tenabysses.entity.TenShadowsShikigamiEntity;
import com.passivehydra.tenabysses.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.UUID;

public final class SummonService {
    public static final String OWNER_TAG = "TenAbyssesOwner";
    public static final String SHIKIGAMI_TAG = "TenAbyssesShikigami";
    public static final String RITUAL_TAG = "TenAbyssesRitual";
    public static final String SITTING_TAG = "TenAbyssesSitting";
    public static final String GROUP_TAG = "TenAbyssesGroup";
    public static final String ABSORBED_TAG = "TenAbyssesAbsorbed";

    private SummonService() {
    }

    public static void trySummon(ServerPlayer player, ShikigamiType shikigami) {
        player.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> doSummon(player, data, shikigami));
    }

    public static void toggleMahoragaWheel(ServerPlayer player) {
        player.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> {
            if (!data.isUnlocked(ShikigamiType.MAHORAGA.id())) {
                player.displayClientMessage(Component.literal("Mahoraga must be tamed before the wheel can be equipped.").withStyle(ChatFormatting.RED), true);
                return;
            }
            MahoragaWheelCompat.toggleWheel(player);
        });
    }

    public static void unsummonAll(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        UUID ownerId = player.getUUID();
        int removed = 0;
        for (Entity entity : serverLevel.getEntities().getAll()) {
            if (!entity.isAlive()) continue;
            boolean isShikigami = isOwnedSummon(entity) && !isRitual(entity);
            boolean isAbsorbed = isAbsorbedSummon(entity);
            if (!isShikigami && !isAbsorbed) continue;
            try {
                if (ownerId.equals(entity.getPersistentData().getUUID(OWNER_TAG))) {
                    entity.discard();
                    removed++;
                }
            } catch (Exception ignored) {
            }
        }
        if (removed > 0) {
            player.displayClientMessage(Component.literal("Unsummoned " + removed + " shikigami.").withStyle(ChatFormatting.GRAY), true);
        } else {
            player.displayClientMessage(Component.literal("No active shikigami to unsummon.").withStyle(ChatFormatting.DARK_GRAY), true);
        }
    }

    private static void doSummon(ServerPlayer player, PlayerProgressData data, ShikigamiType shikigami) {
        if (shikigami == ShikigamiType.DIVINE_DOG_BLACK) {
            shikigami = ShikigamiType.DIVINE_DOG_WHITE;
        }
        // Block disabled shikigami
        if (!shikigami.menuVisible()) {
            player.displayClientMessage(Component.literal(shikigami.displayName().getString() + " is not available.").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!data.isVoidAwakened()) {
            player.displayClientMessage(Component.literal("Ten Shadows ritual is still locked.").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (shikigami == ShikigamiType.DIVINE_DOG_TOTALITY && !data.isDogLost()) {
            player.displayClientMessage(Component.literal("Divine Dog Totality requires a fallen Divine Dog.").withStyle(ChatFormatting.RED), true);
            return;
        }

        for (String required : shikigami.prerequisiteUnlocks()) {
            if (!data.isUnlocked(required)) {
                player.displayClientMessage(Component.literal("Missing prerequisite: " + required).withStyle(ChatFormatting.RED), true);
                return;
            }
        }

        Level level = player.level();
        long now = level.getGameTime();
        if (data.isOnCooldown(shikigami.id(), now)) {
            long remainingSeconds = Math.max(1, (data.cooldownEnd(shikigami.id()) - now) / 20);
            player.displayClientMessage(Component.literal("On cooldown: " + remainingSeconds + "s").withStyle(ChatFormatting.RED), true);
            return;
        }

        UUID groupId = UUID.randomUUID();
        if (shikigami == ShikigamiType.DIVINE_DOG_WHITE) {
            summonDivineDogs(player, data, now, groupId);
            return;
        }

        // Untamed Mahoraga uses the special cinematic ritual instead of direct spawn
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 5).above();
        if (shikigami == ShikigamiType.MAHORAGA && !data.isUnlocked(shikigami.id())) {
            if (MahoragaRitualManager.hasPendingRitual(player.getUUID())) {
                player.displayClientMessage(Component.literal("Mahoraga ritual already in progress.").withStyle(ChatFormatting.RED), true);
                return;
            }
            MahoragaRitualManager.startRitual(player, pos);
            data.setCooldownEnd(shikigami.id(), level.getGameTime() + shikigami.cooldownTicks());
            sync(player, data);
            return;
        }

        if (shikigami == ShikigamiType.MAHORAGA && MahoragaWheelCompat.isWheelLocked(player)) {
            MahoragaWheelCompat.swapWheelToMahoraga(player);
        }

        TenShadowsShikigamiEntity entity = ModEntities.SHIKIGAMI.get().create(level);
        if (entity == null) {
            player.displayClientMessage(Component.literal("Failed to create shikigami entity.").withStyle(ChatFormatting.RED), false);
            return;
        }

        entity.setShikigamiId(shikigami.id());
        configureFallbackEntity(entity, shikigami);

        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);

        boolean unlocked = data.isUnlocked(shikigami.id());
        UUID ownerId = player.getUUID();
        entity.getPersistentData().putUUID(OWNER_TAG, ownerId);
        entity.getPersistentData().putString(SHIKIGAMI_TAG, shikigami.id());
        entity.getPersistentData().putUUID(GROUP_TAG, groupId);

        if (!unlocked) {
            entity.getPersistentData().putBoolean(RITUAL_TAG, true);
            entity.getPersistentData().putDouble("TenAbyssesSpawnX", pos.getX() + 0.5);
            entity.getPersistentData().putDouble("TenAbyssesSpawnY", pos.getY());
            entity.getPersistentData().putDouble("TenAbyssesSpawnZ", pos.getZ() + 0.5);
            entity.setTarget(player);
            player.displayClientMessage(Component.literal("Ritual begun: defeat " + shikigami.displayName().getString() + " to unlock it.").withStyle(ChatFormatting.GOLD), false);
        } else {
            entity.getPersistentData().putBoolean(RITUAL_TAG, false);
            tameEntityToPlayer(entity, player);
            if (shikigami == ShikigamiType.MAHORAGA) {
                MahoragaWheelCompat.applyWheelStateToMahoraga(player, entity);
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(entity);
        }

        data.setCooldownEnd(shikigami.id(), now + shikigami.cooldownTicks());
        sync(player, data);
    }

    public static void sync(ServerPlayer player, PlayerProgressData data) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), S2CPlayerProgressPacket.fromData(data));
    }

    private static void summonDivineDogs(ServerPlayer player, PlayerProgressData data, long now, UUID groupId) {
        Level level = player.level();
        boolean unlockedWhite = data.isUnlocked(ShikigamiType.DIVINE_DOG_WHITE.id());
        boolean unlockedBlack = data.isUnlocked(ShikigamiType.DIVINE_DOG_BLACK.id());
        boolean unlocked = unlockedWhite && unlockedBlack;

        spawnSingleShikigami(player, ShikigamiType.DIVINE_DOG_WHITE, unlocked, groupId, 0.85D);
        spawnSingleShikigami(player, ShikigamiType.DIVINE_DOG_BLACK, unlocked, groupId, -0.85D);

        data.setCooldownEnd(ShikigamiType.DIVINE_DOG_WHITE.id(), now + ShikigamiType.DIVINE_DOG_WHITE.cooldownTicks());
        data.setCooldownEnd(ShikigamiType.DIVINE_DOG_BLACK.id(), now + ShikigamiType.DIVINE_DOG_WHITE.cooldownTicks());
        sync(player, data);
    }

    private static void spawnSingleShikigami(ServerPlayer player, ShikigamiType type, boolean unlocked, UUID groupId, double sideOffset) {
        TenShadowsShikigamiEntity entity = ModEntities.SHIKIGAMI.get().create(player.level());
        if (entity == null) {
            return;
        }

        entity.setShikigamiId(type.id());
        configureFallbackEntity(entity, type);
        BlockPos pos = player.blockPosition().relative(player.getDirection(), 5).above();
        entity.moveTo(pos.getX() + 0.5D + sideOffset, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);
        entity.getPersistentData().putUUID(OWNER_TAG, player.getUUID());
        entity.getPersistentData().putString(SHIKIGAMI_TAG, type.id());
        entity.getPersistentData().putUUID(GROUP_TAG, groupId);

        if (!unlocked) {
            entity.getPersistentData().putBoolean(RITUAL_TAG, true);
            entity.setTarget(player);
        } else {
            entity.getPersistentData().putBoolean(RITUAL_TAG, false);
            tameEntityToPlayer(entity, player);
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(entity);
        }
    }

    private static void spawnGreatSerpentSegments(ServerLevel level, TenShadowsShikigamiEntity head, UUID ownerId, UUID groupId, boolean ritual) {
        for (int index = 1; index <= 3; index++) {
            TenShadowsShikigamiEntity segment = ModEntities.SHIKIGAMI.get().create(level);
            if (segment == null) {
                continue;
            }
            segment.moveTo(head.getX(), head.getY(), head.getZ(), head.getYRot(), 0.0F);
            segment.markAsSegment(head.getUUID(), index);
            segment.getPersistentData().putUUID(OWNER_TAG, ownerId);
            segment.getPersistentData().putString(SHIKIGAMI_TAG, "great_serpent_segment");
            segment.getPersistentData().putUUID(GROUP_TAG, groupId);
            segment.getPersistentData().putBoolean(RITUAL_TAG, ritual);
            level.addFreshEntity(segment);
        }
    }

    public static boolean isAbsorbedSummon(Entity entity) {
        return entity.getPersistentData().hasUUID(OWNER_TAG)
                && entity.getPersistentData().getBoolean(ABSORBED_TAG);
    }

    public static void toggleAbsorbMode(ServerPlayer player) {
        player.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> {
            boolean next = !data.isAbsorbMode();
            data.setAbsorbMode(next);
            player.displayClientMessage(Component.literal("Absorbing Mode: " + (next ? "ON" : "OFF"))
                    .withStyle(next ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), true);
            sync(player, data);
        });
    }

    public static void summonAbsorbed(ServerPlayer player, String entityTypeId) {
        player.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> {
            if (!data.getAbsorbedMobs().contains(entityTypeId)) {
                player.displayClientMessage(Component.literal("Not absorbed: " + entityTypeId)
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            if (!(player.level() instanceof ServerLevel sl)) return;
            ResourceLocation rl = ResourceLocation.tryParse(entityTypeId);
            if (rl == null) return;
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(rl);
            if (type == null || type == EntityType.PLAYER) return;
            Entity spawned = type.create(sl);
            if (!(spawned instanceof LivingEntity living)) return;
            BlockPos pos = player.blockPosition().relative(player.getDirection(), 3).above();
            living.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), 0);
            living.getPersistentData().putUUID(OWNER_TAG, player.getUUID());
            living.getPersistentData().putBoolean(ABSORBED_TAG, true);
            if (living instanceof Mob mob) mob.setPersistenceRequired();
            sl.addFreshEntity(living);
            player.displayClientMessage(Component.literal("Summoned absorbed creature.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        });
    }

    public static boolean isOwnedSummon(Entity entity) {
        return entity.getPersistentData().hasUUID(OWNER_TAG) && entity.getPersistentData().contains(SHIKIGAMI_TAG);
    }

    public static UUID groupOf(Entity entity) {
        return entity.getPersistentData().hasUUID(GROUP_TAG) ? entity.getPersistentData().getUUID(GROUP_TAG) : null;
    }

    public static UUID ownerOf(Entity entity) {
        return entity.getPersistentData().getUUID(OWNER_TAG);
    }

    public static String shikigamiId(Entity entity) {
        return entity.getPersistentData().getString(SHIKIGAMI_TAG);
    }

    public static boolean isRitual(Entity entity) {
        return entity.getPersistentData().getBoolean(RITUAL_TAG);
    }

    public static void tameEntityToPlayer(Entity entity, ServerPlayer owner) {
        entity.getPersistentData().putBoolean(RITUAL_TAG, false);
        entity.getPersistentData().putBoolean(SITTING_TAG, false);
        if (entity instanceof TamableAnimal tamable) {
            tamable.tame(owner);
            tamable.setOwnerUUID(owner.getUUID());
            tamable.setOrderedToSit(false);
            tamable.setTarget(null);
        }
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setTarget(null);
        }
    }

    public static ServerPlayer resolveOwner(Entity entity, ServerLevel level) {
        if (!isOwnedSummon(entity)) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(Objects.requireNonNull(ownerOf(entity)));
    }

    static void configureFallbackEntity(Entity entity, ShikigamiType shikigami) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        switch (shikigami) {
            case DIVINE_DOG_WHITE, DIVINE_DOG_BLACK -> {
                setAttribute(living, Attributes.MAX_HEALTH, 40.0D);
                setAttribute(living, Attributes.ATTACK_DAMAGE, 8.0D);
                setAttribute(living, Attributes.MOVEMENT_SPEED, 0.38D);   // Fast trackers
                setAttribute(living, Attributes.FOLLOW_RANGE, 56.0D);      // High detection range
                setAttribute(living, Attributes.ARMOR, 2.0D);
            }
            case DIVINE_DOG_TOTALITY -> {
                setAttribute(living, Attributes.MAX_HEALTH, 80.0D);
                setAttribute(living, Attributes.ATTACK_DAMAGE, 18.0D);
                setAttribute(living, Attributes.MOVEMENT_SPEED, 0.40D);
                setAttribute(living, Attributes.FOLLOW_RANGE, 60.0D);
                setAttribute(living, Attributes.ARMOR, 4.0D);
                setAttribute(living, Attributes.ARMOR_TOUGHNESS, 2.0D);
            }
            case NUE -> {
                setAttribute(living, Attributes.MAX_HEALTH, 50.0D);
                setAttribute(living, Attributes.ATTACK_DAMAGE, 12.0D);
                setAttribute(living, Attributes.MOVEMENT_SPEED, 0.33D);
                setAttribute(living, Attributes.FOLLOW_RANGE, 72.0D);      // Aerial scout
                setAttribute(living, Attributes.ARMOR, 3.0D);
            }
            case GREAT_SERPENT -> {
                setAttribute(living, Attributes.MAX_HEALTH, 70.0D);
                setAttribute(living, Attributes.ATTACK_DAMAGE, 11.0D);
                setAttribute(living, Attributes.KNOCKBACK_RESISTANCE, 0.6D);
                setAttribute(living, Attributes.ARMOR, 6.0D);              // Thick scales
                setAttribute(living, Attributes.ARMOR_TOUGHNESS, 2.0D);
            }
            case MAX_ELEPHANT -> {
                setAttribute(living, Attributes.MAX_HEALTH, 140.0D);
                setAttribute(living, Attributes.ATTACK_DAMAGE, 18.0D);
                setAttribute(living, Attributes.MOVEMENT_SPEED, 0.20D);
                setAttribute(living, Attributes.KNOCKBACK_RESISTANCE, 1.0D);
                setAttribute(living, Attributes.ARMOR, 10.0D);             // Living fortress
                setAttribute(living, Attributes.ARMOR_TOUGHNESS, 4.0D);
            }
            case ROUND_DEER -> {
                setAttribute(living, Attributes.MAX_HEALTH, 60.0D);
                setAttribute(living, Attributes.MOVEMENT_SPEED, 0.30D);
                setAttribute(living, Attributes.FOLLOW_RANGE, 64.0D);      // Wide healing aura awareness
                setAttribute(living, Attributes.ARMOR, 3.0D);
            }
            case PIERCING_OX -> {
                setAttribute(living, Attributes.MAX_HEALTH, 90.0D);
                setAttribute(living, Attributes.ATTACK_DAMAGE, 14.0D);
                setAttribute(living, Attributes.MOVEMENT_SPEED, 0.36D);    // Charge speed
                setAttribute(living, Attributes.ARMOR, 5.0D);
                setAttribute(living, Attributes.ARMOR_TOUGHNESS, 3.0D);    // Built for impact
            }
            case AGITO -> {
                setAttribute(living, Attributes.MAX_HEALTH, 160.0D);
                setAttribute(living, Attributes.ATTACK_DAMAGE, 22.0D);
                setAttribute(living, Attributes.MOVEMENT_SPEED, 0.34D);
                setAttribute(living, Attributes.KNOCKBACK_RESISTANCE, 0.8D);
                setAttribute(living, Attributes.FOLLOW_RANGE, 64.0D);
                setAttribute(living, Attributes.ARMOR, 7.0D);              // Shark-god hide
                setAttribute(living, Attributes.ARMOR_TOUGHNESS, 3.0D);
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 1, false, true));
            }
            case MAHORAGA -> {
                setAttribute(living, Attributes.MAX_HEALTH, 280.0D);
                setAttribute(living, Attributes.ATTACK_DAMAGE, 30.0D);
                setAttribute(living, Attributes.KNOCKBACK_RESISTANCE, 1.0D);
                setAttribute(living, Attributes.ARMOR, 10.0D);             // Divine construct
                setAttribute(living, Attributes.ARMOR_TOUGHNESS, 5.0D);
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 2, false, true));
            }
            default -> {
            }
        }

        living.setHealth(living.getMaxHealth());
    }

    private static void setAttribute(LivingEntity entity, Attribute attribute, double baseValue) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(baseValue);
        }
    }
}
