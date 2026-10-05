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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Leviathan (summoned with a Leviathan Horn in open water): an ancient guardian the size of a
 * ship. Its beam charges faster as it weakens, its tail slam smashes anything close, it drags prey
 * in with a whirlpool, raises geysers, and calls its brood of guardians at 75%, 50% and 25%.
 */
public class LeviathanEntity extends Guardian implements BountyBoss {
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("☠ The Leviathan").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    private final BossFx fx = new BossFx();
    private int abilityCooldown = 80;
    private int whirlpoolTicks;
    private int broodFlags;

    public LeviathanEntity(EntityType<? extends LeviathanEntity> type, Level level) {
        super(type, level);
        this.xpReward = 800;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Guardian.createAttributes()
                .add(Attributes.MAX_HEALTH, 2800)
                .add(Attributes.ATTACK_DAMAGE, 55)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override public String epithet() { return "The Ancient Deep"; }
    @Override public int ribbonColor() { return 0x3FD6A0; }
    @Override
    public int bountyValue() {
        return 800;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PirateEntity.class, 10, false, false, e -> !(e instanceof MarineEntity)));
    }

    /** The beam charges faster the more hurt it is. */
    @Override
    public int getAttackDuration() {
        return getHealth() < getMaxHealth() * 0.5F ? 30 : 45;
    }

    @Override public boolean canChangeDimensions() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected float getSoundVolume() { return 3.0F; }
    @Override public float getVoicePitch() { return 0.5F; }
    @Override protected SoundEvent getAmbientSound() { return isInWaterOrBubble() ? SoundEvents.ELDER_GUARDIAN_AMBIENT : SoundEvents.ELDER_GUARDIAN_AMBIENT_LAND; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ELDER_GUARDIAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ELDER_GUARDIAN_DEATH; }

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

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        fx.tick(level, this);
        if (abilityCooldown > 0) abilityCooldown--;

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
        // Close the distance (the beam itself is the vanilla guardian attack).
        if (distanceToSqr(target) > 14 * 14) getNavigation().moveTo(target, 1.2);

        float hp = getHealth() / getMaxHealth();
        for (int i = 0; i < 3; i++) {
            float at = 0.75F - i * 0.25F;
            if (hp <= at && (broodFlags & (1 << i)) == 0) {
                broodFlags |= 1 << i;
                brood(level, target, 2 + i);
            }
        }

        if (whirlpoolTicks > 0) tickWhirlpool(level);
        if (abilityCooldown > 0) return;
        double dist = distanceTo(target);
        if (dist < 8) {
            tailSlam(level);
            abilityCooldown = 70;
        } else if (random.nextInt(3) == 0 && whirlpoolTicks <= 0) {
            whirlpoolTicks = 100;
            BossFx.roar(this, "The Leviathan churns the sea into a whirlpool!", ChatFormatting.DARK_GREEN);
            abilityCooldown = 140;
        } else {
            fx.barrage(target, BossFx.Kind.GEYSER, hp < 0.5F ? 8 : 5, 5.0, 38.0F, 25);
            abilityCooldown = hp < 0.5F ? 80 : 110;
        }
    }

    private void tailSlam(ServerLevel level) {
        level.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_FLOP, SoundSource.HOSTILE, 3.0F, 0.4F);
        level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + getBbHeight() * 0.5, getZ(), 200, 5, 2, 5, 0.4);
        BossFx.shockwave(level, this, position().add(0, getBbHeight() * 0.3, 0), 8.0, 50.0F, 0.8);
    }

    private void tickWhirlpool(ServerLevel level) {
        whirlpoolTicks--;
        Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
        if (tickCount % 2 == 0) {
            for (int i = 0; i < 10; i++) {
                double a = tickCount * 0.25 + i * Math.PI / 5;
                double r = 4 + (tickCount % 25) * 0.7;
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX() + Math.cos(a) * r, getY() + getBbHeight(), getZ() + Math.sin(a) * r, 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
        BossFx.pull(level, this, c, 26, 0.1);
        if (tickCount % 20 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4), e -> e != this && !(e instanceof BountyBoss) && !(e instanceof Guardian))) {
                e.hurt(damageSources().mobAttack(this), 18.0F);
            }
        }
    }

    private void brood(ServerLevel level, LivingEntity target, int count) {
        BossFx.roar(this, "The Leviathan calls its brood!", ChatFormatting.DARK_GREEN);
        level.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 3.0F, 0.6F);
        for (int i = 0; i < count; i++) {
            Guardian g = EntityType.GUARDIAN.create(level);
            if (g == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            g.moveTo(getX() + Math.cos(a) * 5, getY() + 1, getZ() + Math.sin(a) * 5, random.nextFloat() * 360, 0);
            g.finalizeSpawn(level, level.getCurrentDifficultyAt(g.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            g.setTarget(target);
            level.addFreshEntity(g);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        BossFx.announceDefeat(this, "The Leviathan", ChatFormatting.DARK_GREEN, source);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
        spawnAtLocation(new ItemStack(ModItems.ADMIRALS_WARRANT.get()));
        spawnAtLocation(new ItemStack(ModItems.LEVIATHAN_SCALE.get(), 8 + random.nextInt(7 + looting)));
        spawnAtLocation(new ItemStack(ModItems.STORMFORGED_INGOT.get(), 1 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(Items.HEART_OF_THE_SEA));
        spawnAtLocation(new ItemStack(Items.PRISMARINE_SHARD, 12 + random.nextInt(13)));
        spawnAtLocation(new ItemStack(ModItems.RUBY.get(), 60 + random.nextInt(31)));
        if (random.nextFloat() < 0.40F + looting * 0.03F) spawnAtLocation(SoulPacts.randomPactItem(random));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Brood", broodFlags);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        broodFlags = tag.getInt("Brood");
    }
}
