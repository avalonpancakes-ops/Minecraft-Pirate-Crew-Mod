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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tempest Admiral Sorel (summoned with a Storm Sigil). A storm-caller: lightning strikes marked a
 * moment before they land, a wind gust that throws attackers into the air, and a blink that puts her
 * behind archers. Below half health she calls a thunderstorm and lightning falls on everyone nearby.
 */
public class TempestAdmiralEntity extends MarineBossEntity {
    private int blinkCooldown;

    public TempestAdmiralEntity(EntityType<? extends TempestAdmiralEntity> type, Level level) {
        super(type, level);
    }

    @Override protected String bossName() { return "Sorel"; }
    @Override protected String bossTitle() { return "Tempest Admiral"; }
    @Override protected String bossSkin() { return "boss_tempest"; }
    @Override protected ChatFormatting bossColor() { return ChatFormatting.LIGHT_PURPLE; }
    @Override protected BossEvent.BossBarColor barColor() { return BossEvent.BossBarColor.PURPLE; }
    @Override protected double bossHealth() { return 1600; }
    @Override protected double bossDamage() { return 26; }
    @Override protected double bossArmor() { return 8; }
    @Override protected double bossToughness() { return 2; }
    @Override protected float bossScale() { return 1.2F; }
    @Override public String epithet() { return "Mistress of Storms"; }
    @Override public int ribbonColor() { return 0xD070FF; }
    @Override public int bountyValue() { return 400; }

    @Override
    protected void equipBoss() {
        setCombatStyle(CombatStyle.BALANCED);
        setItemSlot(EquipmentSlot.MAINHAND, gear(ModItems.GEAR.get(GearTier.KRAKENBONE).sword().get(), Enchantments.SHARPNESS, 4));
        getPack().setItem(0, gear(Items.CROSSBOW, Enchantments.QUICK_CHARGE, 3, Enchantments.MULTISHOT, 1));
        getPack().setItem(1, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_HEALING));
        getPack().setItem(2, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_HEALING));
        addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
    }

    private boolean storm() {
        return (phaseFlags & 1) != 0;
    }

    @Override
    protected void tickBoss(ServerLevel level, LivingEntity target) {
        if (blinkCooldown > 0) blinkCooldown--;
        if (crossed(0.5F, 1)) {
            shout("The sky answers to me! Let the storm take you!");
            level.setWeatherParameters(0, 6000, true, true);
            callMarines(level, target, 2, Rank.RIFLEMAN);
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 0, false, true));
        }
        if (crossed(0.25F, 2)) {
            shout("Sergeants! Hold them while I finish this!");
            callMarines(level, target, 2, Rank.SERGEANT);
        }
        // During the storm, lightning keeps falling on everyone near her.
        if (storm() && tickCount % 50 == 0) {
            for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(32), p -> !p.isCreative() && !p.isSpectator())) {
                fx.mark(p.position(), BossFx.Kind.LIGHTNING, 24.0F, 25);
            }
        }
        double dist = distanceTo(target);
        if (blinkCooldown <= 0 && (dist > 16 || (!canSeeTarget(target) && dist > 4))) {
            blinkBehind(level, target);
            return;
        }
        if (abilityCooldown > 0) return;
        if (dist < 6) {
            gust(level);
            abilityCooldown = storm() ? 70 : 100;
        } else {
            fx.barrage(target, BossFx.Kind.LIGHTNING, storm() ? 7 : 4, 3.5, 35.0F, 20);
            abilityCooldown = storm() ? 80 : 120;
        }
    }

    /** Throw everything nearby back and up. */
    private void gust(ServerLevel level) {
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 60, 3, 0.5, 3, 0.25);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 10, 2.5, 0.3, 2.5, 0);
        level.playSound(null, blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 1.5F, 1.6F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(8, 4, 8),
                e -> e != this && e.isAlive() && !(e instanceof com.piratecrew.entity.MarineEntity) && !(e instanceof BountyBoss))) {
            Vec3 push = e.position().subtract(position()).normalize().scale(2.0);
            e.hurt(damageSources().mobAttack(this), 15.0F);
            e.push(push.x, 0.9, push.z);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1));
        }
    }

    /** Vanish in a crack of thunder and reappear just behind the target. */
    private void blinkBehind(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 behind = target.position().subtract(look.x * 3, 0, look.z * 3);
        level.sendParticles(com.piratecrew.registry.ModParticles.SPARK.get(), getX(), getY() + 1, getZ(), 40, 0.5, 1, 0.5, 0.1);
        boolean moved = randomTeleport(behind.x, behind.y, behind.z, true)
                || randomTeleport(target.getX() + random.nextInt(7) - 3, target.getY(), target.getZ() + random.nextInt(7) - 3, true);
        if (moved) {
            level.sendParticles(com.piratecrew.registry.ModParticles.SPARK.get(), getX(), getY() + 1, getZ(), 40, 0.5, 1, 0.5, 0.1);
            level.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.8F, 1.8F);
            getLookControl().setLookAt(target);
        }
        blinkCooldown = storm() ? 60 : 100;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getDirectEntity() instanceof Projectile && blinkCooldown <= 0
                && source.getEntity() instanceof LivingEntity shooter && random.nextFloat() < 0.4F) {
            setTarget(shooter);
            blinkBehind((ServerLevel) level(), shooter);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl && storm()) sl.setWeatherParameters(6000, 0, false, false);
    }

    @Override
    protected void dropBossLoot(DamageSource source, int looting) {
        spawnAtLocation(new ItemStack(ModItems.LEVIATHAN_HORN.get()));
        spawnAtLocation(new ItemStack(ModItems.STORM_CORE.get(), 4 + random.nextInt(4 + looting)));
        spawnAtLocation(new ItemStack(ModItems.STORMGLASS_SHARD.get(), 6 + random.nextInt(7)));
        spawnAtLocation(new ItemStack(ModItems.KRAKENBONE_INGOT.get(), 1 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(ModItems.RUBY.get(), 40 + random.nextInt(31)));
        if (random.nextFloat() < 0.35F + looting * 0.03F) spawnAtLocation(SoulPacts.randomPactItem(random));
    }
}
