package com.piratecrew.entity.boss;

import com.piratecrew.entity.MarineEntity;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.pact.SoulPacts;
import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Kraken (summoned with a Kraken Lure in open water). A colossal squid that hunts from below:
 * tentacle slams marked by ink on the water, an ink cloud that blinds, a tentacle that drags its prey
 * off boats and shores, geysers, and below half health a whirlpool that pulls everything in.
 */
public class KrakenEntity extends Monster implements BountyBoss {
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("☠ The Kraken").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_10);
    private final BossFx fx = new BossFx();
    private int abilityCooldown = 60;
    private int biteCooldown;
    private int whirlpoolTicks;
    private boolean enraged;

    public KrakenEntity(EntityType<? extends KrakenEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.03F, 0.02F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000)
                .add(Attributes.ATTACK_DAMAGE, 30)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.ARMOR_TOUGHNESS, 2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 1.2)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override public String epithet() { return "Terror of the Deep"; }
    @Override public int ribbonColor() { return 0xC04A8A; }
    @Override
    public int bountyValue() {
        return 250;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 40) {
            @Override
            public boolean canUse() {
                return getTarget() == null && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PirateEntity.class, 10, false, false, e -> !(e instanceof MarineEntity)));
    }

    @Override public MobType getMobType() { return MobType.WATER; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean canChangeDimensions() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }
    @Override protected float getSoundVolume() { return 3.0F; }
    @Override public float getVoicePitch() { return 0.45F; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SQUID_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ELDER_GUARDIAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ELDER_GUARDIAN_DEATH; }

    @Override
    public void travel(Vec3 input) {
        if (isEffectiveAi() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    // ------------------------------------------------------------------ boss bar

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    // ------------------------------------------------------------------ fighting

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        fx.tick(level, this);
        if (abilityCooldown > 0) abilityCooldown--;
        if (biteCooldown > 0) biteCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.level() != level) {
            whirlpoolTicks = 0;
            if (tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(getMaxHealth() * 0.01F);
            return;
        }
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) {
            setTarget(null);
            return;
        }

        // Hunt from below: keep the body just under the prey.
        double surface = waterSurface();
        double wantY = Math.min(target.getY() - getBbHeight() * 0.55, surface - getBbHeight() * 0.6);
        getMoveControl().setWantedPosition(target.getX(), wantY, target.getZ(), 1.0);
        getLookControl().setLookAt(target, 10, 10);

        if (!enraged && getHealth() < getMaxHealth() * 0.5F) {
            enraged = true;
            BossFx.roar(this, "The Kraken thrashes in fury!", ChatFormatting.DARK_PURPLE);
            level.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 3.0F, 0.5F);
            abilityCooldown = 0;
        }

        double dist = Math.sqrt(getBoundingBox().distanceToSqr(target.position()));
        if (dist < 3.0 && biteCooldown <= 0) {
            doHurtTarget(target);
            biteCooldown = 30;
        }

        if (whirlpoolTicks > 0) tickWhirlpool(level);
        if (abilityCooldown > 0) return;

        int roll = random.nextInt(enraged ? 5 : 4);
        if (roll == 4 && whirlpoolTicks <= 0) {
            whirlpoolTicks = 100;
            BossFx.roar(this, "A whirlpool opens beneath the Kraken!", ChatFormatting.AQUA);
            abilityCooldown = 120;
        } else if (roll == 3 && dist > 5 && dist < 26) {
            drag(level, target);
            abilityCooldown = enraged ? 60 : 90;
        } else if (roll == 2 && dist < 14) {
            ink(level);
            abilityCooldown = enraged ? 70 : 100;
        } else if (roll == 1) {
            fx.barrage(target, BossFx.Kind.GEYSER, enraged ? 7 : 5, 5.0, 25.0F, 25);
            abilityCooldown = enraged ? 70 : 100;
        } else {
            // Tentacles: one at the target, a couple more around it.
            fx.barrage(target, BossFx.Kind.SLAM, enraged ? 4 : 2, 3.5, 35.0F, 18);
            abilityCooldown = enraged ? 50 : 80;
        }
    }

    private double waterSurface() {
        var pos = blockPosition().mutable();
        int top = Math.min(level().getMaxBuildHeight() - 1, pos.getY() + 24);
        while (pos.getY() < top && !level().getFluidState(pos).isEmpty()) pos.move(0, 1, 0);
        return level().getFluidState(blockPosition()).isEmpty() ? getY() + getBbHeight() : pos.getY();
    }

    private void ink(ServerLevel level) {
        level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + getBbHeight() * 0.6, getZ(), 300, 5, 3, 5, 0.1);
        level.playSound(null, blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.HOSTILE, 3.0F, 0.4F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10), e -> e != this && !(e instanceof BountyBoss))) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            e.hurt(damageSources().mobAttack(this), 6.0F);
        }
    }

    /** A tentacle lashes out and hauls the target (and anyone beside it) toward the Kraken. */
    private void drag(ServerLevel level, LivingEntity target) {
        Vec3 from = position().add(0, getBbHeight() * 0.6, 0);
        Vec3 to = target.position().add(0, 1, 0);
        int steps = (int) (from.distanceTo(to) * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) Math.max(1, steps));
            level.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.HOSTILE, 2.0F, 0.4F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(to, to).inflate(2.5), e -> e != this && !(e instanceof BountyBoss))) {
            if (e.getVehicle() != null) e.stopRiding();
            Vec3 pull = from.subtract(e.position()).normalize().scale(1.6);
            e.hurt(damageSources().mobAttack(this), 10.0F);
            e.setDeltaMovement(pull.x, Math.max(0.35, pull.y * 0.5), pull.z);
            e.hurtMarked = true;
        }
    }

    private void tickWhirlpool(ServerLevel level) {
        whirlpoolTicks--;
        Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
        if (tickCount % 2 == 0) {
            for (int i = 0; i < 8; i++) {
                double a = (tickCount * 0.3) + i * Math.PI / 4;
                double r = 3 + (tickCount % 20) * 0.6;
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX() + Math.cos(a) * r, waterSurface() - 0.5, getZ() + Math.sin(a) * r, 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
        BossFx.pull(level, this, c, 22, 0.09);
        if (tickCount % 20 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4), e -> e != this && !(e instanceof BountyBoss))) {
                e.hurt(damageSources().mobAttack(this), 14.0F);
            }
        }
    }

    // ------------------------------------------------------------------ death and loot

    @Override
    public void die(DamageSource source) {
        super.die(source);
        BossFx.announceDefeat(this, "The Kraken", ChatFormatting.DARK_PURPLE, source);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
        spawnAtLocation(new ItemStack(ModItems.STORM_SIGIL.get()));
        spawnAtLocation(new ItemStack(ModItems.KRAKEN_BONE.get(), 8 + random.nextInt(7 + looting)));
        spawnAtLocation(new ItemStack(ModItems.ABYSSAL_INGOT.get(), 1 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(Items.INK_SAC, 8 + random.nextInt(9)));
        spawnAtLocation(new ItemStack(ModItems.RUBY.get(), 30 + random.nextInt(21)));
        if (random.nextFloat() < 0.30F + looting * 0.03F) spawnAtLocation(SoulPacts.randomPactItem(random));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", enraged);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        enraged = tag.getBoolean("Enraged");
    }
}
