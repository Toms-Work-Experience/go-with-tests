package com.passivehydra.tenabysses.entity;

import com.passivehydra.tenabysses.registry.ModSounds;
import com.passivehydra.tenabysses.summon.ShikigamiType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TenShadowsShikigamiEntity extends TamableAnimal implements GeoEntity {
    public static final int MAHORAGA_MAX_ADAPT_LEVEL = 10;
    private static final float MAHORAGA_RESISTANCE_PER_LEVEL = 0.10F;
    private static final double MAHORAGA_ATTRIBUTE_SCALE_PER_LEVEL = 0.015D;
    private static final double MAHORAGA_BASE_HEALTH = 280.0D;
    private static final double MAHORAGA_BASE_DAMAGE = 30.0D;
    private static final double MAHORAGA_BASE_SPEED = 0.30D;
    private static final double MAHORAGA_BASE_FOLLOW_RANGE = 48.0D;
    private static final EntityDataAccessor<String> SHIKIGAMI_ID = SynchedEntityData.defineId(TenShadowsShikigamiEntity.class, EntityDataSerializers.STRING);
    public static final String PARENT_TAG = "TenAbyssesParent";
    public static final String SEGMENT_TAG = "TenAbyssesSegment";
    public static final String SEGMENT_INDEX_TAG = "TenAbyssesSegmentIndex";
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, Integer> mahoragaAdaptLevels = new HashMap<>();
    private final Map<String, Integer> mahoragaAdaptTimer = new HashMap<>();
    private double chargeDistance;
    private double lastX;
    private double lastZ;
    private long lastHeavyHitTick;
    private boolean specialAirborne;
    private boolean wasOnGround;

    public TenShadowsShikigamiEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1D, 8.0F, 2.0F, false));
        this.goalSelector.addGoal(5, new WideRoamGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SHIKIGAMI_ID, ShikigamiType.DIVINE_DOG_WHITE.id());
    }

    public String getShikigamiId() {
        return this.entityData.get(SHIKIGAMI_ID);
    }

    public void setShikigamiId(String id) {
        this.entityData.set(SHIKIGAMI_ID, id);
        refreshDimensions();
    }

    public void markAsSegment(UUID parentId, int index) {
        getPersistentData().putBoolean(SEGMENT_TAG, true);
        getPersistentData().putUUID(PARENT_TAG, parentId);
        getPersistentData().putInt(SEGMENT_INDEX_TAG, index);
        setShikigamiId("great_serpent_segment");
        setCustomName(Component.literal("Great Serpent Segment"));
        setCustomNameVisible(false);
        refreshDimensions();
    }

    public boolean isSegment() {
        return getPersistentData().getBoolean(SEGMENT_TAG);
    }

    public boolean isFriendlyToOwner(LivingEntity target) {
        if (target == null) {
            return false;
        }
        LivingEntity owner = getOwner();
        if (owner != null && target.getUUID().equals(owner.getUUID())) {
            return true;
        }
        if (target instanceof TenShadowsShikigamiEntity shikigami) {
            LivingEntity targetOwner = shikigami.getOwner();
            return owner != null && targetOwner != null && owner.getUUID().equals(targetOwner.getUUID());
        }
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("ShikigamiId", getShikigamiId());
        tag.putDouble("ChargeDistance", chargeDistance);
        tag.putLong("LastHeavyHitTick", lastHeavyHitTick);
        tag.putBoolean("SpecialAirborne", specialAirborne);

        CompoundTag adaptTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : mahoragaAdaptLevels.entrySet()) {
            adaptTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("MahoragaAdaptLevels", adaptTag);

        CompoundTag adaptHitsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : mahoragaAdaptTimer.entrySet()) {
            adaptHitsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("MahoragaAdaptHits", adaptHitsTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ShikigamiId")) {
            setShikigamiId(tag.getString("ShikigamiId"));
        }
        chargeDistance = tag.getDouble("ChargeDistance");
        lastHeavyHitTick = tag.getLong("LastHeavyHitTick");
        specialAirborne = tag.getBoolean("SpecialAirborne");

        mahoragaAdaptLevels.clear();
        CompoundTag adaptTag = tag.getCompound("MahoragaAdaptLevels");
        for (String key : adaptTag.getAllKeys()) {
            mahoragaAdaptLevels.put(key, adaptTag.getInt(key));
        }

        mahoragaAdaptTimer.clear();
        CompoundTag adaptHitsTag = tag.getCompound("MahoragaAdaptHits");
        for (String key : adaptHitsTag.getAllKeys()) {
            mahoragaAdaptTimer.put(key, adaptHitsTag.getInt(key));
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob ageableMob) {
        return null;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return switch (getShikigamiId()) {
            case "divine_dog_white", "divine_dog_black" -> EntityDimensions.scalable(0.8F, 1.3F);
            case "divine_dog_totality" -> EntityDimensions.scalable(1.6F, 2.6F);
            case "nue" -> EntityDimensions.scalable(0.9F, 2.6F);
            case "great_serpent" -> EntityDimensions.scalable(1.4375F, 0.8125F);
            case "great_serpent_segment" -> EntityDimensions.scalable(0.9375F, 0.8125F);
            case "max_elephant" -> EntityDimensions.scalable(3.8F, 3.6F);
            case "round_deer" -> EntityDimensions.scalable(3.8F, 3.6F);
            case "piercing_ox" -> EntityDimensions.scalable(2.0F, 1.8F);
            case "agito" -> EntityDimensions.scalable(1.6F, 4.0F);
            case "mahoraga" -> EntityDimensions.scalable(1.4F, 5.6F);
            default -> super.getDimensions(pose);
        };
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // Make the struck mob fight back against the shikigami
        if (target instanceof net.minecraft.world.entity.Mob mob) {
            mob.setTarget(this);
        }
        if (!(target instanceof LivingEntity livingTarget)) {
            return super.doHurtTarget(target);
        }
        if (isFriendlyToOwner(livingTarget)) {
            setTarget(null);
            return false;
        }

        boolean result = super.doHurtTarget(target);
        if (!result) {
            return false;
        }

        // Make the hit mob turn and fight back against the shikigami
        if (livingTarget instanceof net.minecraft.world.entity.Mob hitMob && !hitMob.isAlliedTo(this)) {
            hitMob.setTarget(this);
        }

        String id = getShikigamiId();
        switch (id) {
            case "divine_dog_totality" -> {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                playSound(ModSounds.DOG_HOWL.get(), 1.5F, 0.8F);
                spawnAttackParticles(ParticleTypes.CRIT, 12);
            }
            case "nue" -> {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                summonLightningNear(livingTarget);
                playSound(ModSounds.NUE_ELECTRIC.get(), 1.0F, 1.0F);
            }
            case "great_serpent" -> {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
                livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                spawnAttackParticles(ParticleTypes.SMOKE, 10);
            }
            case "max_elephant" -> {
                livingTarget.knockback(1.6D, getX() - livingTarget.getX(), getZ() - livingTarget.getZ());
                playSound(SoundEvents.RAVAGER_ROAR, 1.2F, 0.85F);
                spawnAttackParticles(ParticleTypes.SPLASH, 18);
            }
            case "piercing_ox" -> {
                float bonus = (float) Math.min(16.0D, chargeDistance * 0.45D);
                livingTarget.hurt(damageSources().mobAttack(this), bonus);
                livingTarget.knockback(1.8D, getX() - livingTarget.getX(), getZ() - livingTarget.getZ());
                chargeDistance = 0.0D;
                playSound(SoundEvents.RAVAGER_STEP, 1.3F, 0.9F);
                spawnAttackParticles(ParticleTypes.CLOUD, 14);
            }
            case "agito" -> {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                summonLightningNear(livingTarget);
                heal(2.0F);
                playSound(ModSounds.AGITO_SPARK.get(), 1.0F, 1.0F);
                spawnAttackParticles(ParticleTypes.ELECTRIC_SPARK, 16);
            }
            case "mahoraga" -> {
                performMahoragaSweep(livingTarget);
                playSound(ModSounds.SHIKIGAMI_SLASH.get(), 1.2F, 0.9F);
                spawnAttackParticles(ParticleTypes.SWEEP_ATTACK, 10);
            }
            default -> {
                playSound(ModSounds.SHIKIGAMI_SLASH.get(), 0.8F, 1.1F);
                spawnAttackParticles(ParticleTypes.CRIT, 8);
            }
        }

        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Round Deer: healing aura for owner + allies, never self
        if ("round_deer".equals(getShikigamiId()) && tickCount % 60 == 0) {
            applyDeerRegenAura();
        }
        if (level().isClientSide()) {
            return;
        }

        String id = getShikigamiId();
        LivingEntity target = getTarget();
        long gameTime = level().getGameTime();

        if (isSegment()) {
            followSerpentParent();
            return;
        }

        double dx = getX() - lastX;
        double dz = getZ() - lastZ;
        lastX = getX();
        lastZ = getZ();
        double moved = Math.sqrt(dx * dx + dz * dz);

        if ("piercing_ox".equals(id)) {
            if (target != null && !isFriendlyToOwner(target)) {
                chargeDistance = Math.min(120.0D, chargeDistance + moved * 6.0D);
                if (gameTime % 6L == 0L) {
                    ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.25D, getZ(), 4, 0.2D, 0.1D, 0.2D, 0.02D);
                }
            } else {
                chargeDistance = Math.max(0.0D, chargeDistance - 1.5D);
            }
        }

        if (isFlyingType()) {
            setNoGravity(true);
            handleFlyingMovement(target);
        } else {
            setNoGravity(false);
        }

        if (("divine_dog_white".equals(id) || "divine_dog_black".equals(id) || "divine_dog_totality".equals(id))
            && target != null
            && onGround()
            && (target.getY() - getY()) > 2.0D
            && distanceTo(target) > 4.5F
            && gameTime % 80L == 0L
            && !isFriendlyToOwner(target)) {
            leapTowards(target, "divine_dog_totality".equals(id) ? 1.1D : 0.9D, 0.42D);
        }

        if ("mahoraga".equals(id)
            && target != null
            && onGround()
            && (target.getY() - getY()) > 2.0D
            && distanceTo(target) > 5.0F
            && gameTime % 100L == 0L
            && !isFriendlyToOwner(target)) {
            leapTowards(target, 1.0D, 0.55D);
            playSound(ModSounds.MAHORAGA_WHEEL.get(), 1.0F, 0.9F);
            specialAirborne = true;
        }

        if ("round_deer".equals(id) && gameTime % 20L == 0L) {
            AABB healBox = getBoundingBox().inflate(12.0D);
            for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class, healBox, living ->
                    living != this && (isFriendlyToOwner(living) || (living instanceof TenShadowsShikigamiEntity sh && isFriendlyToOwner(sh))) && living.isAlive())) {
                // Regen IV for 3 seconds - intense pulse healing; deer never heals itself
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 3, false, true));
                cleanseNegative(ally);
            }
            ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0D, getZ(), 8, 0.5D, 0.3D, 0.5D, 0.03D);
        }

        if (("nue".equals(id) || "agito".equals(id)) && target != null && gameTime % 40L == 0L && !isFriendlyToOwner(target)) {
            summonLightningNear(target);
            ((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0D, target.getZ(), 14, 0.4D, 0.4D, 0.4D, 0.05D);
        }

        if ("max_elephant".equals(id) && target != null && gameTime % 35L == 0L && !isFriendlyToOwner(target)) {
            target.hurt(damageSources().mobAttack(this), 6.0F);
            target.knockback(1.3D, getX() - target.getX(), getZ() - target.getZ());
            ((ServerLevel) level()).sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + 0.6D, target.getZ(), 20, 0.35D, 0.35D, 0.35D, 0.08D);
        }

        if ("mahoraga".equals(id)) {
            if (gameTime % 20L == 0L && gameTime - lastHeavyHitTick > 80L) {
                heal(2.0F);
            }
            tickAdaptationTimers();
            if (gameTime % 30L == 0L) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 2.2D, getZ(), 10, 0.6D, 0.25D, 0.6D, 0.01D);
            }
        }

        if (specialAirborne && !wasOnGround && onGround()) {
            specialAirborne = false;
            performGroundSlam();
        }
        wasOnGround = onGround();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Prevent self-damage loops (AoE hitting self, etc.)
        if (source.getEntity() == this) {
            return false;
        }
        if (isFriendlyToOwner(source.getEntity() instanceof LivingEntity living ? living : null)) {
            return false;
        }

        if (!"mahoraga".equals(getShikigamiId())) {
            return super.hurt(source, amount);
        }

        String category = damageCategory(source);
        int level = mahoragaAdaptLevels.getOrDefault(category, 0);
        float scaled = (float) (amount * (1.0F - Math.min(0.95F, level * MAHORAGA_RESISTANCE_PER_LEVEL)));
        boolean hurt = super.hurt(source, scaled);
        if (!hurt) {
            return false;
        }

        if (amount >= 14.0F) {
            lastHeavyHitTick = level().getGameTime();
        }

        if (level < MAHORAGA_MAX_ADAPT_LEVEL) {
            int timer = mahoragaAdaptTimer.getOrDefault(category, 200);
            timer -= 70;
            mahoragaAdaptTimer.put(category, timer);
            if (timer <= 0) {
                advanceMahoragaAdaptation(category, level + 1);
            }
        }

        return true;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // Never target self or allies
        if (target == this || (target != null && isFriendlyToOwner(target))) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::mainAnimation));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private void cleanseNegative(LivingEntity entity) {
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getEffect().isBeneficial()) {
                continue;
            }
            entity.removeEffect(effect.getEffect());
        }
    }

    private void summonLightningNear(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
        if (bolt == null) {
            return;
        }
        bolt.moveTo(target.getX(), target.getY(), target.getZ());
        bolt.setVisualOnly(true);
        serverLevel.addFreshEntity(bolt);
        target.hurt(damageSources().lightningBolt(), 5.0F);
    }

    private void performMahoragaSweep(LivingEntity primaryTarget) {
        AABB box = getBoundingBox().inflate(3.0D);
        for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e != primaryTarget && !isFriendlyToOwner(e))) {
            nearby.hurt(damageSources().mobAttack(this), 8.0F);
        }
    }

    private void spawnAttackParticles(net.minecraft.core.particles.ParticleOptions particle, int count) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(particle, getX(), getY() + 0.8D, getZ(), count, 0.35D, 0.25D, 0.35D, 0.04D);
    }

    private String damageCategory(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return "fire";
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return "explosion";
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return "projectile";
        }
        if (source.is(DamageTypes.FALL)) {
            return "fall";
        }
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return "magic";
        }
        return "physical";
    }

    private PlayState mainAnimation(AnimationState<TenShadowsShikigamiEntity> state) {
        String id = getShikigamiId();
        String animation;

        if ("great_serpent".equals(id)) {
            animation = swinging ? "attack.bite" : "misc.grab";
        } else if ("great_serpent_segment".equals(id)) {
            animation = "misc.grab";
        } else if (isFlyingType()) {
            animation = state.isMoving() ? "move.fly" : "misc.idle";
        } else if ("mahoraga".equals(id) && getTarget() != null) {
            animation = state.isMoving() ? "move.walk" : "misc.idle_battle";
        } else if ("piercing_ox".equals(id) && chargeDistance > 10.0D) {
            animation = "move.run";
        } else if (state.isMoving()) {
            animation = "move.walk";
        } else {
            animation = "misc.idle";
        }

        state.setAndContinue(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }

    private boolean isFlyingType() {
        // Only Nue flies; Agito stays grounded
        return "nue".equals(getShikigamiId());
    }

    private void handleFlyingMovement(@Nullable LivingEntity target) {
        LivingEntity anchor = target != null && !isFriendlyToOwner(target) ? target : getOwner();
        if (anchor == null) {
            return;
        }

        double targetX = anchor.getX();
        double targetY = anchor.getY() + (target == null ? 4.0D : 2.5D);
        double targetZ = anchor.getZ();
        double dx = targetX - getX();
        double dy = targetY - getY();
        double dz = targetZ - getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.001D) {
            return;
        }

        double speed = target == null ? 0.18D : 0.28D;
        setDeltaMovement(getDeltaMovement().scale(0.84D).add(dx / distance * speed, dy / distance * 0.12D, dz / distance * speed));
        getNavigation().stop();
        lookAt(anchor, 20.0F, 20.0F);

        if (target != null && onGround()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.42D, 0.0D));
        }
    }

    private void leapTowards(LivingEntity target, double horizontalSpeed, double verticalSpeed) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.001D) {
            return;
        }
        setDeltaMovement(dx / distance * horizontalSpeed, verticalSpeed, dz / distance * horizontalSpeed);
        hasImpulse = true;
    }

    private void performGroundSlam() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB slamBox = getBoundingBox().inflate(3.5D, 1.5D, 3.5D);
        for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class, slamBox, e -> e != this && !isFriendlyToOwner(e))) {
            nearby.hurt(damageSources().mobAttack(this), 10.0F);
            nearby.knockback(1.2D, getX() - nearby.getX(), getZ() - nearby.getZ());
        }
        serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 24, 0.7D, 0.1D, 0.7D, 0.08D);
        playSound(ModSounds.MAHORAGA_SLAM.get(), 1.0F, 0.85F);
    }

    private void tickAdaptationTimers() {
        if (mahoragaAdaptTimer.isEmpty()) {
            return;
        }
        Map<String, Integer> snapshot = new HashMap<>(mahoragaAdaptTimer);
        for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
            int next = entry.getValue() - 1;
            if (next <= 0) {
                int currentLevel = mahoragaAdaptLevels.getOrDefault(entry.getKey(), 0);
                if (currentLevel < MAHORAGA_MAX_ADAPT_LEVEL) {
                    advanceMahoragaAdaptation(entry.getKey(), currentLevel + 1);
                } else {
                    mahoragaAdaptTimer.remove(entry.getKey());
                }
            } else {
                mahoragaAdaptTimer.put(entry.getKey(), next);
            }
        }
    }

    private void advanceMahoragaAdaptation(String category, int newLevel) {
        mahoragaAdaptTimer.remove(category);
        mahoragaAdaptLevels.put(category, Math.min(MAHORAGA_MAX_ADAPT_LEVEL, newLevel));
        refreshMahoragaCombatAttributes();
        setHealth(getMaxHealth());
        playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 0.8F + random.nextFloat() * 0.3F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 2.4D, getZ(), 18, 0.7D, 0.2D, 0.7D, 0.06D);
        }
        // Push adaptation to the wheel capability so both stay in sync
        com.passivehydra.tenabysses.compat.MahoragaWheelCompat.pushAdaptationToWheel(this, category, mahoragaAdaptLevels.get(category));
    }

    public Map<String, Integer> copyMahoragaAdaptLevels() {
        return new HashMap<>(mahoragaAdaptLevels);
    }

    public void applyMahoragaAdaptLevels(Map<String, Integer> levels) {
        mahoragaAdaptLevels.clear();
        mahoragaAdaptTimer.clear();
        for (Map.Entry<String, Integer> entry : levels.entrySet()) {
            mahoragaAdaptLevels.put(entry.getKey(), Math.min(MAHORAGA_MAX_ADAPT_LEVEL, Math.max(0, entry.getValue())));
        }
        refreshMahoragaCombatAttributes();
        setHealth(getMaxHealth());
        // Sync live adaptation state to the owner's Mahoraga Wheel capability
        if (getOwner() instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.passivehydra.tenabysses.compat.MahoragaWheelCompat.syncLiveAdaptationToWheel(sp, this);
        }
    }

    private void refreshMahoragaCombatAttributes() {
        if (!"mahoraga".equals(getShikigamiId())) {
            return;
        }
        int totalAdaptations = mahoragaAdaptLevels.values().stream().mapToInt(Integer::intValue).sum();
        double multiplier = 1.0D + (totalAdaptations * MAHORAGA_ATTRIBUTE_SCALE_PER_LEVEL);
        setScaledAttribute(Attributes.MAX_HEALTH, MAHORAGA_BASE_HEALTH * multiplier);
        setScaledAttribute(Attributes.ATTACK_DAMAGE, MAHORAGA_BASE_DAMAGE * multiplier);
        setScaledAttribute(Attributes.MOVEMENT_SPEED, MAHORAGA_BASE_SPEED * multiplier);
        setScaledAttribute(Attributes.FOLLOW_RANGE, MAHORAGA_BASE_FOLLOW_RANGE * multiplier);
    }

    @Override
    public void setTame(boolean tame) {
        super.setTame(tame);
        if (tame && !"mahoraga".equals(getShikigamiId())) {
            initBaseAttributes();
        }
    }

    private void initBaseAttributes() {
        ShikigamiType type = ShikigamiType.fromId(getShikigamiId());
        if (type == null) return;
        var maxHealth = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(type.getBaseHealth());
        var damage = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(type.getBaseDamage());
        var speed = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(type.getBaseSpeed());
        var followRange = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        if (followRange != null) followRange.setBaseValue(type.getBaseFollowRange() + 45.0);
        setHealth((float) type.getBaseHealth());
    }

    private void applyDeerRegenAura() {
        net.minecraft.world.entity.LivingEntity owner = getOwner();
        if (owner != null) {
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.REGENERATION, 80, 3, false, false));
        }
        level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        getBoundingBox().inflate(16D))
                .stream()
                .filter(e -> e != this) // never self
                .filter(e -> {
                    if (e instanceof TenShadowsShikigamiEntity ally) {
                        return ally.getOwnerUUID() != null && ally.getOwnerUUID().equals(getOwnerUUID());
                    }
                    return e == owner;
                })
                .forEach(e -> e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, 80, 3, false, false)));
    }

    private void setScaledAttribute(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** Wider random roaming — 55 blocks xz instead of the vanilla 10-block default. */
    private static class WideRoamGoal extends RandomStrollGoal {
        WideRoamGoal(PathfinderMob mob, double speed) {
            super(mob, speed);
        }

        @Override
        protected @org.jetbrains.annotations.Nullable Vec3 getPosition() {
            return DefaultRandomPos.getPos(this.mob, 55, 7);
        }
    }

    private void followSerpentParent() {
        if (!(level() instanceof ServerLevel serverLevel) || !getPersistentData().hasUUID(PARENT_TAG)) {
            return;
        }
        Entity parent = serverLevel.getEntity(getPersistentData().getUUID(PARENT_TAG));
        if (!(parent instanceof LivingEntity livingParent) || !livingParent.isAlive()) {
            discard();
            return;
        }
        setNoGravity(true);
        int index = Math.max(1, getPersistentData().getInt(SEGMENT_INDEX_TAG));
        double yaw = Math.toRadians(livingParent.getYRot() + 180.0F);
        double spacing = 1.35D * index;
        double targetX = livingParent.getX() + Math.cos(yaw) * spacing;
        double targetY = livingParent.getY() + 0.2D;
        double targetZ = livingParent.getZ() + Math.sin(yaw) * spacing;
        setPos(targetX, targetY, targetZ);
        setYRot(livingParent.getYRot());
        setTarget(null);
    }
}
