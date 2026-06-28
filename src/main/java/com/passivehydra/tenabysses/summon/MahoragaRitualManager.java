package com.passivehydra.tenabysses.summon;

import com.passivehydra.tenabysses.entity.TenShadowsShikigamiEntity;
import com.passivehydra.tenabysses.registry.ModEntities;
import com.passivehydra.tenabysses.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MahoragaRitualManager {
    /**
     * Duration in ticks before Mahoraga actually spawns.
     * The provided audio clip is ~24 s, so we use 480 ticks.
     * Adjust here to match the clip length once the OGG is placed.
     */
    public static final int RITUAL_DURATION = 480;

    // summoner UUID -> game-tick at which Mahoraga should spawn
    private static final Map<UUID, Long> pendingSpawnTick = new HashMap<>();
    // summoner UUID -> exact spawn BlockPos (5 blocks in front of summoner at invocation)
    private static final Map<UUID, BlockPos> pendingSpawnPos = new HashMap<>();
    // summoner UUID -> set of player UUIDs that were frozen for this ritual
    private static final Map<UUID, Set<UUID>> frozenSets = new HashMap<>();

    private MahoragaRitualManager() {
    }

    public static boolean hasPendingRitual(UUID summonerId) {
        return pendingSpawnTick.containsKey(summonerId);
    }

    /**
     * Removes all ritual state for the given summoner.
     * Call this on player logout so the static maps don't hold entries indefinitely.
     */
    public static void cancelRitual(UUID summonerId) {
        pendingSpawnTick.remove(summonerId);
        pendingSpawnPos.remove(summonerId);
        frozenSets.remove(summonerId);
    }

    /**
     * Begins the Mahoraga invocation sequence.
     * Call this instead of directly spawning the entity when Mahoraga is untamed.
     */
    public static void startRitual(ServerPlayer summoner, BlockPos spawnPos) {
        UUID id = summoner.getUUID();
        ServerLevel level = summoner.serverLevel();
        long spawnAt = level.getGameTime() + RITUAL_DURATION;

        pendingSpawnTick.put(id, spawnAt);
        pendingSpawnPos.put(id, spawnPos);

        // Freeze every player within 100 blocks of the spawn point
        Set<UUID> frozen = new HashSet<>();
        AABB box = new AABB(spawnPos).inflate(100.0D);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box, e -> true)) {
            frozen.add(p.getUUID());
            applyFreezeEffects(p, RITUAL_DURATION + 60);
        }
        frozenSets.put(id, frozen);

        // Sculk darkness wave from spawn point outward
        sendSculkWave(level, spawnPos);

        // Atmospheric audio
        level.playSound(null, spawnPos, ModSounds.MAHORAGA_SUMMON.get(), SoundSource.HOSTILE, 5.0F, 1.0F);
        level.playSound(null, spawnPos, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 4.0F, 0.55F);
        level.playSound(null, spawnPos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0F, 0.5F);
    }

    /**
     * Called every server level tick. Handles effect refresh and spawning Mahoraga when ready.
     */
    public static void tick(ServerLevel level) {
        if (pendingSpawnTick.isEmpty()) {
            return;
        }

        long gameTime = level.getGameTime();

        // Refresh freeze every second so players cannot escape
        if (gameTime % 20L == 0L) {
            for (Set<UUID> frozen : frozenSets.values()) {
                for (UUID pid : frozen) {
                    ServerPlayer p = level.getServer().getPlayerList().getPlayer(pid);
                    if (p != null && p.isAlive()) {
                        MobEffectInstance current = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        if (current == null || current.getDuration() < 80) {
                            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 127, false, false));
                        }
                    }
                }
            }
        }

        // Periodic heartbeat for all frozen players
        if (gameTime % 50L == 0L) {
            for (Map.Entry<UUID, Set<UUID>> entry : frozenSets.entrySet()) {
                BlockPos pos = pendingSpawnPos.get(entry.getKey());
                if (pos != null) {
                    level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2.0F, 0.5F);
                }
            }
        }

        // Check for rituals that have completed
        Set<UUID> completed = new HashSet<>();
        for (Map.Entry<UUID, Long> entry : pendingSpawnTick.entrySet()) {
            if (gameTime < entry.getValue()) {
                continue;
            }
            UUID summonerId = entry.getKey();
            BlockPos pos = pendingSpawnPos.get(summonerId);
            if (pos != null) {
                actuallySpawnMahoraga(level, summonerId, pos);
            }
            // Unfreeze
            Set<UUID> frozen = frozenSets.getOrDefault(summonerId, Set.of());
            for (UUID pid : frozen) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(pid);
                if (p != null) {
                    p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    p.removeEffect(MobEffects.DARKNESS);
                }
            }
            completed.add(summonerId);
        }

        completed.forEach(id -> {
            pendingSpawnTick.remove(id);
            pendingSpawnPos.remove(id);
            frozenSets.remove(id);
        });
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private static void applyFreezeEffects(ServerPlayer p, int durationTicks) {
        // Level 127 makes movement speed formula go negative (clamped to 0) – full stop
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 127, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, durationTicks, 0, false, false));
    }

    private static void actuallySpawnMahoraga(ServerLevel level, UUID summonerId, BlockPos pos) {
        TenShadowsShikigamiEntity entity = ModEntities.SHIKIGAMI.get().create(level);
        if (entity == null) {
            return;
        }

        entity.setShikigamiId(ShikigamiType.MAHORAGA.id());
        SummonService.configureFallbackEntity(entity, ShikigamiType.MAHORAGA);
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        entity.getPersistentData().putUUID(SummonService.OWNER_TAG, summonerId);
        entity.getPersistentData().putString(SummonService.SHIKIGAMI_TAG, ShikigamiType.MAHORAGA.id());
        entity.getPersistentData().putBoolean(SummonService.RITUAL_TAG, true);
        // Store spawn position for despawn-when-all-dead logic
        entity.getPersistentData().putDouble("TenAbyssesSpawnX", pos.getX() + 0.5);
        entity.getPersistentData().putDouble("TenAbyssesSpawnY", pos.getY());
        entity.getPersistentData().putDouble("TenAbyssesSpawnZ", pos.getZ() + 0.5);

        ServerPlayer summoner = level.getServer().getPlayerList().getPlayer(summonerId);
        if (summoner != null) {
            entity.setTarget(summoner);
        }

        level.addFreshEntity(entity);

        // Spawn audio: impact/arrival cues
        level.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.5F, 0.75F);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.5F, 0.5F);

        // Visual ring
        for (int i = 0; i < 48; i++) {
            double angle = (2.0 * Math.PI * i) / 48;
            double r = 2.8D;
            double x = pos.getX() + 0.5 + r * Math.cos(angle);
            double z = pos.getZ() + 0.5 + r * Math.sin(angle);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 0.4D, z, 1, 0.05D, 0.5D, 0.05D, 0.06D);
            level.sendParticles(ParticleTypes.SCULK_SOUL, x, pos.getY() + 0.1D, z, 1, 0.1D, 0.15D, 0.1D, 0.01D);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                40, 0.6D, 1.2D, 0.6D, 0.1D);
    }

    private static void sendSculkWave(ServerLevel level, BlockPos center) {
        // Concentric rings expanding outward from spawn point
        for (int radius = 2; radius <= 32; radius += 4) {
            for (int deg = 0; deg < 360; deg += 12) {
                double rad = Math.toRadians(deg);
                double x = center.getX() + 0.5 + radius * Math.cos(rad);
                double z = center.getZ() + 0.5 + radius * Math.sin(rad);
                level.sendParticles(ParticleTypes.SCULK_SOUL, x, center.getY() + 0.2D, z,
                        1, 0.0D, 0.4D, 0.0D, 0.01D);
                level.sendParticles(ParticleTypes.WARPED_SPORE, x, center.getY() + 0.6D, z,
                        1, 0.3D, 0.6D, 0.3D, 0.02D);
            }
        }
        // Burst at spawn point
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
                50, 0.8D, 1.5D, 0.8D, 0.12D);
        level.playSound(null, center, SoundEvents.WARDEN_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.4F);
    }
}
