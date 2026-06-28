package com.passivehydra.tenabysses.player;

import com.passivehydra.tenabysses.compat.CursedFateCompat;
import com.passivehydra.tenabysses.compat.MahoragaWheelCompat;
import com.passivehydra.tenabysses.entity.TenShadowsShikigamiEntity;
import com.passivehydra.tenabysses.summon.MahoragaRitualManager;
import com.passivehydra.tenabysses.summon.SummonService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import java.util.UUID;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class PlayerProgressEvents {
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerProgressData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ModCapabilities.PLAYER_PROGRESS_ID, new PlayerProgressProvider());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // Handle BOTH death (isWasDeath=true) and End-portal exit (isWasDeath=false).
        // For End-portal: caps are still live, no revive needed.
        // For death: caps have already been invalidated, must revive first.
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
        }
        event.getOriginal().getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(oldData ->
                event.getEntity().getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(newData -> newData.copyFrom(oldData))
        );
        if (event.getOriginal().getPersistentData().contains(MahoragaWheelCompat.PLAYER_WHEEL_TAG)) {
            event.getEntity().getPersistentData().put(MahoragaWheelCompat.PLAYER_WHEEL_TAG,
                event.getOriginal().getPersistentData().getCompound(MahoragaWheelCompat.PLAYER_WHEEL_TAG).copy());
        }
        if (event.isWasDeath()) {
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> {
                // First-join CursedFate init: set CT=None, unlock RCT + Binding Vows
                if (!data.isCursedfateInitDone()) {
                    CursedFateCompat.initPlayerCursedFate(player);
                    data.setCursedfateInitDone(true);
                }
                SummonService.sync(player, data);
            });
        }
    }

    /** Clean up in-memory ritual state when a player logs out to prevent map bloat. */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MahoragaRitualManager.cancelRitual(player.getUUID());
        }
    }

    /**
     * Mirror the capability data into getPersistentData() every time the player file is written.
     * getPersistentData() is stored as "ForgeData" in the .dat file and is always persisted,
     * making it a reliable backup for the ICapabilitySerializable mechanism.
     */
    @SubscribeEvent
    public static void onSaveToFile(PlayerEvent.SaveToFile event) {
        event.getEntity().getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data ->
                event.getEntity().getPersistentData().put("TenAbyssesProgressBackup", data.serializeNBT()));
    }

    /**
     * On file load, restore from the backup if it exists.
     * This fires after Forge's own ICapabilitySerializable deserialization, so in the normal
     * case both sources carry the same data. If ICapabilitySerializable ever fails silently,
     * this backup ensures the data survives server restarts.
     */
    @SubscribeEvent
    public static void onLoadFromFile(PlayerEvent.LoadFromFile event) {
        if (!event.getEntity().getPersistentData().contains("TenAbyssesProgressBackup")) return;
        event.getEntity().getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data ->
                data.deserializeNBT(event.getEntity().getPersistentData()
                        .getCompound("TenAbyssesProgressBackup")));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> SummonService.sync(player, data));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> SummonService.sync(player, data));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();

        if (dead instanceof ServerPlayer player && event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            player.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> {
                if (!data.isVoidAwakened()) {
                    data.setVoidAwakened(true);
                    player.displayClientMessage(Component.literal("Ten Shadows awakened: summon menu unlocked.").withStyle(ChatFormatting.AQUA), false);
                    SummonService.sync(player, data);
                }
            });
        }

        Entity killer = event.getSource().getEntity();
        // Some modded damage types (DoT, reflection, boss phases) report null from getEntity() — fall back to directEntity
        if (killer == null) killer = event.getSource().getDirectEntity();
        if (!(killer instanceof ServerPlayer)) {
            if (SummonService.isOwnedSummon(dead)
                    && !SummonService.isRitual(dead)
                    && ("divine_dog_white".equals(SummonService.shikigamiId(dead)) || "divine_dog_black".equals(SummonService.shikigamiId(dead)))) {
                markDogLost(dead, dead.level());
            }
            // Credit absorption to owner when a shikigami or absorbed summon kills the mob
            if (killer != null
                    && (SummonService.isOwnedSummon(killer) || SummonService.isAbsorbedSummon(killer))
                    && !(dead instanceof Player)
                    && !SummonService.isOwnedSummon(dead)
                    && !SummonService.isAbsorbedSummon(dead)
                    && killer.getPersistentData().hasUUID(SummonService.OWNER_TAG)
                    && dead.level() instanceof ServerLevel summonKillLevel) {
                ServerPlayer summonOwner = summonKillLevel.getServer().getPlayerList()
                        .getPlayer(killer.getPersistentData().getUUID(SummonService.OWNER_TAG));
                if (summonOwner != null) {
                    tryAbsorb(summonOwner, dead);
                }
            }
            return;
        }
        ServerPlayer playerKiller = (ServerPlayer) killer;

        if (SummonService.isOwnedSummon(dead) && SummonService.isRitual(dead)) {
            if (!playerKiller.getUUID().equals(SummonService.ownerOf(dead))) {
                return;
            }

            String id = SummonService.shikigamiId(dead);
            playerKiller.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> {
                if ("divine_dog_white".equals(id) || "divine_dog_black".equals(id)) {
                    if (ritualDogPairCleared(dead, playerKiller.serverLevel())) {
                        data.unlock("divine_dog_white");
                        data.unlock("divine_dog_black");
                        playerKiller.displayClientMessage(Component.literal("Unlocked shikigami: divine_dogs").withStyle(ChatFormatting.GREEN), false);
                    }
                } else if (!"great_serpent_segment".equals(id)) {
                    data.unlock(id);
                    playerKiller.displayClientMessage(Component.literal("Unlocked shikigami: " + id).withStyle(ChatFormatting.GREEN), false);
                }
                SummonService.sync(playerKiller, data);
            });
        }

        if (SummonService.isOwnedSummon(dead)
                && !SummonService.isRitual(dead)
                && ("divine_dog_white".equals(SummonService.shikigamiId(dead)) || "divine_dog_black".equals(SummonService.shikigamiId(dead)))) {
            markDogLost(dead, dead.level());
        }

        // Absorption: player (or their summon) kills a non-shikigami, non-absorbed mob while absorb mode is ON
        if (!(dead instanceof Player)
                && !SummonService.isOwnedSummon(dead)
                && !SummonService.isAbsorbedSummon(dead)) {
            tryAbsorb(playerKiller, dead);
        }
    }

    private static void tryAbsorb(ServerPlayer killer, LivingEntity dead) {
        killer.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> {
            if (!data.isAbsorbMode()) return;
            if (killer.experienceLevel < 50) {
                killer.displayClientMessage(
                        Component.literal("Need 50 levels to absorb! You have " + killer.experienceLevel + ".")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(dead.getType());
            if (key == null) return;
            String typeId = key.toString();
            if (data.addAbsorbed(typeId)) {
                killer.giveExperienceLevels(-25);
                killer.displayClientMessage(
                        Component.literal("Absorbed: " + dead.getType().getDescription().getString() + " (25 levels consumed)")
                                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
                SummonService.sync(killer, data);
            } else {
                killer.displayClientMessage(
                        Component.literal(dead.getType().getDescription().getString() + " already absorbed.")
                                .withStyle(ChatFormatting.GRAY), true);
            }
        });
    }

    private static void markDogLost(Entity dead, Level level) {
        if (!(level instanceof ServerLevel serverLevel) || !SummonService.isOwnedSummon(dead)) {
            return;
        }

        ServerPlayer owner = SummonService.resolveOwner(dead, serverLevel);
        if (owner == null) {
            return;
        }

        owner.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> {
            if (!data.isDogLost()) {
                data.setDogLost(true);
                owner.displayClientMessage(Component.literal("A Divine Dog has fallen. Divine Dog Totality can now be summoned.").withStyle(ChatFormatting.GOLD), false);
                SummonService.sync(owner, data);
            }
        });
    }

    private static boolean ritualDogPairCleared(Entity dead, ServerLevel level) {
        UUID groupId = SummonService.groupOf(dead);
        if (groupId == null) {
            return true;
        }
        // Scoped AABB query — far cheaper than iterating every loaded entity
        AABB searchBox = new AABB(dead.getX() - 100, dead.getY() - 100, dead.getZ() - 100,
                dead.getX() + 100, dead.getY() + 100, dead.getZ() + 100);
        for (TenShadowsShikigamiEntity entity : level.getEntitiesOfClass(TenShadowsShikigamiEntity.class, searchBox)) {
            if (entity == dead || !entity.isAlive()) {
                continue;
            }
            if (!SummonService.isRitual(entity)) {
                continue;
            }
            if (!groupId.equals(SummonService.groupOf(entity))) {
                continue;
            }
            String id = SummonService.shikigamiId(entity);
            if ("divine_dog_white".equals(id) || "divine_dog_black".equals(id)) {
                return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getSide().isClient()) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        Entity target = event.getTarget();

        if (!SummonService.isOwnedSummon(target) || SummonService.isRitual(target)) {
            return;
        }
        if (!target.getPersistentData().getUUID(SummonService.OWNER_TAG).equals(player.getUUID())) {
            return;
        }
        if (!player.isCrouching() || !player.getMainHandItem().isEmpty()) {
            return;
        }

        boolean sitting = !target.getPersistentData().getBoolean(SummonService.SITTING_TAG);
        target.getPersistentData().putBoolean(SummonService.SITTING_TAG, sitting);

        if (target instanceof TamableAnimal tamable) {
            tamable.setOrderedToSit(sitting);
        }
        if (target instanceof Mob mob) {
            mob.setTarget(null);
        }

        player.displayClientMessage(Component.literal(sitting ? "Shikigami set to sit." : "Shikigami set to follow."), true);
        event.setCanceled(true);
    }

    /**
     * Block absorbed summons from ever self-selecting an invalid target.
     * LivingChangeTargetEvent fires before the target is applied and is @Cancelable.
     * Cancelling it means the mob's AI goal never gets to assign a wrong target at all.
     */
    @SubscribeEvent
    public static void onAbsorbedSetTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (!SummonService.isAbsorbedSummon(entity)) return;
        if (!entity.getPersistentData().hasUUID(SummonService.OWNER_TAG)) return;
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget == null) return; // Clearing target is always allowed
        if (!(entity.level() instanceof ServerLevel sl)) { event.setCanceled(true); return; }
        ServerPlayer owner = sl.getServer().getPlayerList()
                .getPlayer(entity.getPersistentData().getUUID(SummonService.OWNER_TAG));
        if (owner == null) { event.setCanceled(true); return; }
        boolean valid = (owner.getLastHurtMob() != null && owner.getLastHurtMob() == newTarget)
                || (owner.getLastHurtByMob() != null && owner.getLastHurtByMob() == newTarget);
        if (!valid) {
            event.setCanceled(true);
        }
    }

    /** Prevent absorbed summons from attacking anyone except the owner's active target/attacker. */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity source = event.getSource().getEntity();
        if (source == null) return;
        if (!SummonService.isAbsorbedSummon(source)) return;
        if (!source.getPersistentData().hasUUID(SummonService.OWNER_TAG)) return;

        LivingEntity target = event.getEntity();
        UUID ownerUUID = source.getPersistentData().getUUID(SummonService.OWNER_TAG);

        // Always block friendly-fire on owner
        if (target instanceof Player player && player.getUUID().equals(ownerUUID)) {
            event.setCanceled(true);
            return;
        }

        // Block attack unless target is exactly what owner is currently fighting
        if (!(source.level() instanceof ServerLevel sl)) { event.setCanceled(true); return; }
        ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner == null) { event.setCanceled(true); return; }
        boolean valid = (owner.getLastHurtMob() != null && owner.getLastHurtMob() == target)
                || (owner.getLastHurtByMob() != null && owner.getLastHurtByMob() == target);
        if (!valid) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) event.level;
        if (level.getGameTime() % 10L != 0L) {
            return;
        }

        // Drive the Mahoraga ritual sequence
        MahoragaRitualManager.tick(level);

        // Untamed despawn: if summoner dead and no living players within 25 blocks of spawn point
        if (level.getGameTime() % 20L == 0L) {
            for (ServerPlayer knownPlayer : level.players()) {
                AABB scanBox = knownPlayer.getBoundingBox().inflate(120.0D);
                for (Mob mob : level.getEntitiesOfClass(Mob.class, scanBox,
                        e -> SummonService.isOwnedSummon(e) && SummonService.isRitual(e))) {
                    UUID ownerId;
                    try {
                        ownerId = mob.getPersistentData().getUUID(SummonService.OWNER_TAG);
                    } catch (Exception ignored) {
                        continue;
                    }
                    ServerPlayer summoner = level.getServer().getPlayerList().getPlayer(ownerId);
                    if (summoner != null && summoner.isAlive()) {
                        continue; // summoner still alive
                    }
                    double spawnX = mob.getPersistentData().getDouble("TenAbyssesSpawnX");
                    double spawnY = mob.getPersistentData().getDouble("TenAbyssesSpawnY");
                    double spawnZ = mob.getPersistentData().getDouble("TenAbyssesSpawnZ");
                    if (spawnX == 0 && spawnY == 0 && spawnZ == 0) {
                        continue;
                    }
                    AABB nearSpawn = new AABB(spawnX - 25, spawnY - 25, spawnZ - 25,
                            spawnX + 25, spawnY + 25, spawnZ + 25);
                    boolean witnessPresent = !level.getEntitiesOfClass(ServerPlayer.class, nearSpawn,
                            p -> p.isAlive()).isEmpty();
                    if (!witnessPresent) {
                        mob.discard();
                    }
                }
            }
        }
        for (ServerPlayer owner : level.players()) {
            if (!owner.isAlive()) {
                continue;
            }

            if (level.getGameTime() % 20L == 0L) {
                MahoragaWheelCompat.enforceWheelLock(owner);
                MahoragaWheelCompat.periodicWheelSync(owner);
            }

            // Single AABB shared by both the shikigami and absorbed entity loops
            AABB searchBox = owner.getBoundingBox().inflate(96.0D);
            UUID ownerUUID = owner.getUUID();

            // Hoist owner combat targets once per player per tick — not once per entity
            LivingEntity ownerTarget = owner.getLastHurtMob();
            LivingEntity ownerAttacker = owner.getLastHurtByMob();

            for (Mob mob : level.getEntitiesOfClass(Mob.class, searchBox, entity ->
                    SummonService.isOwnedSummon(entity)
                            && ownerUUID.equals(entity.getPersistentData().getUUID(SummonService.OWNER_TAG)))) {
                if (SummonService.isRitual(mob)) {
                    mob.setTarget(owner);
                    continue;
                }

                boolean sitting = mob.getPersistentData().getBoolean(SummonService.SITTING_TAG);
                if (sitting) {
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                    continue;
                }

                LivingEntity mobOwnerTarget = ownerTarget;
                LivingEntity mobOwnerAttacker = ownerAttacker;

                if (mobOwnerTarget != null && (mobOwnerTarget == mob
                        || (SummonService.isOwnedSummon(mobOwnerTarget)
                        && ownerUUID.equals(mobOwnerTarget.getPersistentData().getUUID(SummonService.OWNER_TAG))))) {
                    mobOwnerTarget = null;
                }
                if (mobOwnerAttacker != null && (mobOwnerAttacker == mob
                        || (SummonService.isOwnedSummon(mobOwnerAttacker)
                        && ownerUUID.equals(mobOwnerAttacker.getPersistentData().getUUID(SummonService.OWNER_TAG))))) {
                    mobOwnerAttacker = null;
                }

                if (mobOwnerAttacker != null && mobOwnerAttacker.isAlive()) {
                    mob.setTarget(mobOwnerAttacker);
                } else if (mobOwnerTarget != null && mobOwnerTarget.isAlive()) {
                    mob.setTarget(mobOwnerTarget);
                } else {
                    mob.setTarget(null);
                }

                if (mob.distanceToSqr(owner) > 36.0D * 36.0D) {
                    mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                } else if (mob.distanceToSqr(owner) > 14.0D * 14.0D && mob.getTarget() == null) {
                    mob.getNavigation().moveTo(owner, 1.2D);
                }
            }
            // Use LivingEntity so non-Mob bosses are also controlled (reuse same AABB as shikigami loop)
            // Only bother scanning if this player actually has absorbed entities registered
            boolean hasAbsorbed = owner.getCapability(ModCapabilities.PLAYER_PROGRESS)
                    .map(d -> !d.getAbsorbedMobs().isEmpty()).orElse(false);
            if (hasAbsorbed) {
            for (LivingEntity absorbedEntity : level.getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                    SummonService.isAbsorbedSummon(entity)
                            && ownerUUID.equals(entity.getPersistentData().getUUID(SummonService.OWNER_TAG)))) {
                // Valid target already computed above for this owner
                LivingEntity validTarget = null;
                if (ownerAttacker != null && ownerAttacker.isAlive()
                        && !SummonService.isAbsorbedSummon(ownerAttacker)
                        && !SummonService.isOwnedSummon(ownerAttacker)
                        && !(ownerAttacker instanceof Player)) {
                    validTarget = ownerAttacker;
                } else if (ownerTarget != null && ownerTarget.isAlive()
                        && !SummonService.isAbsorbedSummon(ownerTarget)
                        && !SummonService.isOwnedSummon(ownerTarget)
                        && !(ownerTarget instanceof Player)) {
                    validTarget = ownerTarget;
                }
                // Force-set every tick — overrides boss AI that would otherwise re-pick a target
                if (absorbedEntity instanceof Mob absorbedMob) {
                    absorbedMob.setTarget(validTarget);
                    if (absorbedEntity.distanceToSqr(owner) > 36.0D * 36.0D) {
                        absorbedMob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                    } else if (absorbedEntity.distanceToSqr(owner) > 14.0D * 14.0D && validTarget == null) {
                        absorbedMob.getNavigation().moveTo(owner, 1.2D);
                    }
                } else {
                    // Non-Mob LivingEntity (rare boss base type) — teleport if too far
                    if (absorbedEntity.distanceToSqr(owner) > 36.0D * 36.0D) {
                        absorbedEntity.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                    }
                }
            }
            } // end hasAbsorbed
        }
    }
}
