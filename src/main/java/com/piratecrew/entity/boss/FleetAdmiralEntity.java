package com.piratecrew.entity.boss;

import com.piratecrew.entity.CombatStyle;
import com.piratecrew.item.GearTier;
import com.piratecrew.pact.SoulPacts;
import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Fleet Admiral Vane, the Iron Tide: the Order's supreme commander and the Sundered Sea's final boss
 * (summoned with an Admiral's Warrant). Three phases:
 * 1. Dash strikes that cut through everything in his path, plus cannon fire.
 * 2. (below 2/3) Two marine captains join him and the whole fleet opens fire.
 * 3. (below 1/3) The Iron Tide: tougher and faster, ground-shaking shockwaves and lightning.
 */
public class FleetAdmiralEntity extends MarineBossEntity {
    private int dashTicks;
    private Vec3 dashDir = Vec3.ZERO;
    private final Set<UUID> dashHit = new HashSet<>();
    private int shockwaveTimer = 100;

    public FleetAdmiralEntity(EntityType<? extends FleetAdmiralEntity> type, Level level) {
        super(type, level);
        this.xpReward = 1000;
    }

    @Override protected String bossName() { return "Vane"; }
    @Override protected String bossTitle() { return "Fleet Admiral"; }
    @Override protected String bossSkin() { return "boss_fleet_admiral"; }
    @Override protected ChatFormatting bossColor() { return ChatFormatting.RED; }
    @Override protected BossEvent.BossBarColor barColor() { return BossEvent.BossBarColor.RED; }
    @Override protected double bossHealth() { return 4500; }
    @Override protected double bossDamage() { return 40; }
    @Override protected double bossArmor() { return 12; }
    @Override protected double bossToughness() { return 6; }
    @Override protected float bossScale() { return 1.35F; }
    @Override public int bountyValue() { return 1500; }

    @Override
    protected void equipBoss() {
        setCombatStyle(CombatStyle.BRAWLER);
        setItemSlot(EquipmentSlot.MAINHAND, gear(ModItems.GEAR.get(GearTier.SOVEREIGN).sword().get(), Enchantments.SHARPNESS, 5, Enchantments.FIRE_ASPECT, 2));
        setItemSlot(EquipmentSlot.OFFHAND, gear(Items.SHIELD));
        for (int i = 0; i < 3; i++) getPack().setItem(i, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_HEALING));
        getPack().setItem(3, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 2));
        addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
    }

    private int phase() {
        return (phaseFlags & 2) != 0 ? 3 : (phaseFlags & 1) != 0 ? 2 : 1;
    }

    @Override
    protected void tickBoss(ServerLevel level, LivingEntity target) {
        if (dashTicks > 0) tickDash(level);

        if (crossed(0.66F, 1)) {
            shout("You've sunk enough of my ships. Captains, with me! All batteries, fire at will!");
            callMarines(level, target, 2, Rank.CAPTAIN);
            fx.barrage(target, BossFx.Kind.CANNON, 14, 9.0, 40.0F, 30);
            abilityCooldown = 80;
        }
        if (crossed(0.33F, 2)) {
            shout("Then witness the Iron Tide!");
            level.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.7F);
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, true));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 0, false, true));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 1, false, true));
            BossFx.shockwave(level, this, position(), 8.0, 40.0F, 1.0);
        }
        if (phase() == 3 && --shockwaveTimer <= 0 && onGround()) {
            shockwaveTimer = 100;
            level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
            BossFx.shockwave(level, this, position(), 6.5, 45.0F, 0.8);
        }

        if (abilityCooldown > 0 || dashTicks > 0) return;
        double dist = distanceTo(target);
        int roll = random.nextInt(phase() == 1 ? 2 : 3);
        if (dist > 4 && dist < 22 && canSeeTarget(target) && roll == 0) {
            startDash(target);
            abilityCooldown = phase() == 3 ? 50 : 80;
        } else if (roll == 2) {
            fx.barrage(target, BossFx.Kind.LIGHTNING, phase() == 3 ? 7 : 5, 4.0, 42.0F, 22);
            abilityCooldown = 90;
        } else {
            fx.barrage(target, BossFx.Kind.CANNON, phase() == 1 ? 6 : 9, 5.0, 40.0F, 28);
            abilityCooldown = phase() == 3 ? 80 : 120;
        }
    }

    private void startDash(LivingEntity target) {
        Vec3 d = target.position().subtract(position());
        dashDir = new Vec3(d.x, 0, d.z).normalize();
        setDeltaMovement(dashDir.x * 1.6, 0.1, dashDir.z * 1.6);
        hasImpulse = true;
        dashTicks = 10;
        dashHit.clear();
        level().playSound(null, blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.HOSTILE, 1.5F, 0.8F);
    }

    private void tickDash(ServerLevel level) {
        dashTicks--;
        Vec3 v = dashDir;
        if (dashTicks > 1) {
            setDeltaMovement(dashDir.x * 1.6, getDeltaMovement().y, dashDir.z * 1.6);
            hasImpulse = true;
        }
        level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1, getZ(), 6, 0.3, 0.5, 0.3, 0.1);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.5),
                e -> e != this && e.isAlive() && !(e instanceof com.piratecrew.entity.MarineEntity) && !(e instanceof BountyBoss))) {
            if (!dashHit.add(e.getUUID())) continue;
            e.hurt(damageSources().mobAttack(this), 50.0F);
            Vec3 push = v;
            e.push(push.x * 1.5, 0.5, push.z * 1.5);
            e.hurtMarked = true;
        }
    }

    @Override
    protected void dropBossLoot(DamageSource source, int looting) {
        spawnAtLocation(new ItemStack(ModItems.SOVEREIGN_HEART.get(), 3 + random.nextInt(3 + looting)));
        spawnAtLocation(new ItemStack(ModItems.LEVIATHAN_INGOT.get(), 2 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(ModItems.RUBY.get(), 64));
        spawnAtLocation(new ItemStack(ModItems.RUBY.get(), 36 + random.nextInt(51)));
        spawnAtLocation(new ItemStack(Items.NETHERITE_INGOT, 1 + random.nextInt(2)));
        spawnAtLocation(SoulPacts.randomPactItem(random));
    }
}
