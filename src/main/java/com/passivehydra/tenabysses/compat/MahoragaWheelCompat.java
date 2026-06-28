package com.passivehydra.tenabysses.compat;

import com.example.examplemod.capability.AdaptationCapability;
import com.example.examplemod.capability.AdaptationProvider;
import com.example.examplemod.registry.ModItems;
import com.passivehydra.tenabysses.entity.TenShadowsShikigamiEntity;
import com.passivehydra.tenabysses.player.ModCapabilities;
import com.passivehydra.tenabysses.summon.ShikigamiType;
import com.passivehydra.tenabysses.summon.SummonService;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MahoragaWheelCompat {
    public static final String PLAYER_WHEEL_TAG = "TenAbyssesWheelState";
    private static final String WHEEL_ACTIVE_TAG = "Active";
    private static final String STORED_HELMET_TAG = "StoredHelmet";
    private static final String MAHORAGA_WAS_SUMMONED_TAG = "MahoragaWasSummoned";
    private static final ResourceLocation WHEEL_ID = ResourceLocation.fromNamespaceAndPath("mahoragawheel", "mahoraga_wheel");

    private MahoragaWheelCompat() {
    }

    public static void configure() {
        AdaptationCapability.COOLDOWN_TICKS = 7;
    }

    public static boolean isWheelLocked(ServerPlayer player) {
        return player.getPersistentData().getCompound(PLAYER_WHEEL_TAG).getBoolean(WHEEL_ACTIVE_TAG);
    }

    public static void toggleWheel(ServerPlayer player) {
        if (isWheelLocked(player)) {
            unequipWheel(player);
        } else {
            equipWheel(player);
        }
    }
    /** Syncs the live entity's current adapt levels to the owner's wheel capability without discarding. */
    public static void syncLiveAdaptationToWheel(net.minecraft.server.level.ServerPlayer player,
            com.passivehydra.tenabysses.entity.TenShadowsShikigamiEntity entity) {
        player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {
            for (java.util.Map.Entry<String, Integer> entry : entity.copyMahoragaAdaptLevels().entrySet()) {
                int level = entry.getValue();
                if (level > 0) {
                    cap.forceSetAdaptation(wheelKey(entry.getKey()),
                            level / (float) com.passivehydra.tenabysses.entity.TenShadowsShikigamiEntity.MAHORAGA_MAX_ADAPT_LEVEL);
                }
            }
        });
    }
    public static void swapWheelToMahoraga(ServerPlayer player) {
        if (isWheelLocked(player)) {
            unequipWheel(player);
        }
    }

    public static void applyWheelStateToMahoraga(ServerPlayer player, TenShadowsShikigamiEntity entity) {
        player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(capability -> {
            Map<String, Integer> levels = new HashMap<>();
            for (Map.Entry<String, Float> entry : capability.getAllAdaptations().entrySet()) {
                int level = Math.max(0, Math.min(10, Math.round(entry.getValue() * 10.0F)));
                if (level > 0) {
                    levels.put(normalizeKey(entry.getKey()), level);
                }
            }
            if (!levels.isEmpty()) {
                entity.applyMahoragaAdaptLevels(levels);
            }
        });
    }

    public static void enforceWheelLock(ServerPlayer player) {
        if (!isWheelLocked(player)) {
            return;
        }
        ItemStack currentHead = player.getItemBySlot(EquipmentSlot.HEAD);
        if (isWheelItem(currentHead)) {
            return;
        }
        if (!currentHead.isEmpty()) {
            if (!player.getInventory().add(currentHead.copy())) {
                player.drop(currentHead.copy(), false);
            }
        }
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAHORAGA_WHEEL.get()));
    }

    private static void equipWheel(ServerPlayer player) {
        boolean hadMahoraga = findOwnedMahoraga(player) != null;
        syncSummonedMahoragaToWheel(player);
        CompoundTag wheelState = player.getPersistentData().getCompound(PLAYER_WHEEL_TAG);
        ItemStack currentHead = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!currentHead.isEmpty() && !isWheelItem(currentHead)) {
            wheelState.put(STORED_HELMET_TAG, currentHead.save(new CompoundTag()));
        } else {
            wheelState.remove(STORED_HELMET_TAG);
        }
        wheelState.putBoolean(WHEEL_ACTIVE_TAG, true);
        wheelState.putBoolean(MAHORAGA_WAS_SUMMONED_TAG, hadMahoraga);
        player.getPersistentData().put(PLAYER_WHEEL_TAG, wheelState);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAHORAGA_WHEEL.get()));
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Mahoraga Wheel equipped.").withStyle(ChatFormatting.GOLD), true);
    }

    private static void unequipWheel(ServerPlayer player) {
        CompoundTag wheelState = player.getPersistentData().getCompound(PLAYER_WHEEL_TAG);
        boolean hadMahoraga = wheelState.getBoolean(MAHORAGA_WAS_SUMMONED_TAG);
        ItemStack restore = wheelState.contains(STORED_HELMET_TAG) ? ItemStack.of(wheelState.getCompound(STORED_HELMET_TAG)) : ItemStack.EMPTY;
        player.setItemSlot(EquipmentSlot.HEAD, restore);
        wheelState.putBoolean(WHEEL_ACTIVE_TAG, false);
        wheelState.remove(STORED_HELMET_TAG);
        wheelState.remove(MAHORAGA_WAS_SUMMONED_TAG);
        player.getPersistentData().put(PLAYER_WHEEL_TAG, wheelState);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Mahoraga Wheel unequipped.").withStyle(ChatFormatting.GRAY), true);
        // Re-summon Mahoraga if he was summoned when the wheel was equipped
        if (hadMahoraga) {
            player.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data ->
                    data.setCooldownEnd(ShikigamiType.MAHORAGA.id(), 0L));
            SummonService.trySummon(player, ShikigamiType.MAHORAGA);
        }
    }

    /**
     * Called when Mahoraga adapts on the entity side — pushes the new level to the
     * wheel's AdaptationCapability so both stay in sync.  Safe to call even when the
     * wheel is not equipped (no-op in that case).
     */
    public static void pushAdaptationToWheel(TenShadowsShikigamiEntity entity, String category, int newLevel) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel)) {
            return;
        }
        net.minecraft.server.level.ServerPlayer owner = SummonService.resolveOwner(
                entity, (net.minecraft.server.level.ServerLevel) entity.level());
        if (owner == null) {
            return;
        }
        owner.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap ->
                cap.forceSetAdaptation(wheelKey(category), Math.min(1.0F, newLevel / 10.0F)));
    }

    /**
     * Called periodically (once per second) from PlayerProgressEvents.
     * Pulls wheel capability levels into any active Mahoraga, taking the maximum
     * of what the wheel knows vs. what the entity knows (never downgrades).
     */
    public static void periodicWheelSync(ServerPlayer player) {
        if (!isWheelLocked(player)) {
            return;
        }
        TenShadowsShikigamiEntity mahoraga = findOwnedMahoraga(player);
        if (mahoraga == null) {
            return;
        }
        player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(cap -> {
            Map<String, Integer> entityLevels = mahoraga.copyMahoragaAdaptLevels();
            Map<String, Integer> merged = new HashMap<>(entityLevels);
            boolean changed = false;
            for (Map.Entry<String, Float> entry : cap.getAllAdaptations().entrySet()) {
                int wheelLevel = Math.max(0, Math.min(10, Math.round(entry.getValue() * 10.0F)));
                String key = normalizeKey(entry.getKey());
                int entityLevel = merged.getOrDefault(key, 0);
                if (wheelLevel > entityLevel) {
                    merged.put(key, wheelLevel);
                    changed = true;
                }
            }
            if (changed) {
                mahoraga.applyMahoragaAdaptLevels(merged);
            }
        });
    }

    private static void syncSummonedMahoragaToWheel(ServerPlayer player) {
        TenShadowsShikigamiEntity mahoraga = findOwnedMahoraga(player);
        if (mahoraga == null) {
            return;
        }
        player.getCapability(AdaptationProvider.ADAPTATION_CAP).ifPresent(capability -> {
            capability.resetAll();
            for (Map.Entry<String, Integer> entry : mahoraga.copyMahoragaAdaptLevels().entrySet()) {
                capability.forceSetAdaptation(wheelKey(entry.getKey()), Math.min(1.0F, entry.getValue() / 10.0F));
            }
        });
        mahoraga.discard();
    }

    private static TenShadowsShikigamiEntity findOwnedMahoraga(ServerPlayer player) {
        UUID ownerId = player.getUUID();
        for (net.minecraft.world.entity.Entity entity : player.serverLevel().getEntities().getAll()) {
            if (!(entity instanceof TenShadowsShikigamiEntity shikigami)) {
                continue;
            }
            if (!SummonService.isOwnedSummon(entity) || SummonService.isRitual(entity)) {
                continue;
            }
            if (!ownerId.equals(entity.getPersistentData().getUUID(SummonService.OWNER_TAG))) {
                continue;
            }
            if ("mahoraga".equals(SummonService.shikigamiId(entity))) {
                return shikigami;
            }
        }
        return null;
    }

    private static boolean isWheelItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return WHEEL_ID.equals(key);
    }

    private static String normalizeKey(String key) {
        if (key.endsWith(":mob_attack") || key.endsWith(":player_attack") || key.contains("attack:")) {
            return "physical";
        }
        if (key.endsWith(":magic") || key.contains("magic")) {
            return "magic";
        }
        if (key.endsWith(":explosion") || key.contains("explosion")) {
            return "explosion";
        }
        if (key.endsWith(":arrow") || key.contains("projectile") || key.contains("arrow")) {
            return "projectile";
        }
        if (key.endsWith(":on_fire") || key.contains("fire")) {
            return "fire";
        }
        return key;
    }

    private static String wheelKey(String key) {
        return switch (key) {
            case "physical" -> "minecraft:mob_attack";
            case "magic" -> "minecraft:magic";
            case "explosion" -> "minecraft:explosion";
            case "projectile" -> "minecraft:arrow";
            case "fire" -> "minecraft:on_fire";
            default -> key;
        };
    }
}