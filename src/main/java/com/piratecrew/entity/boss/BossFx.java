package com.piratecrew.entity.boss;

import com.piratecrew.entity.MarineEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Shared boss effects: telegraphed strikes (cannon fire, lightning, geysers), shockwaves and pulls. */
public class BossFx {
    public enum Kind { CANNON, LIGHTNING, GEYSER, SLAM }

    /** A strike marked on the ground that lands after a short warning. */
    public static class Strike {
        final Vec3 pos;
        final Kind kind;
        final float damage;
        int timer;

        Strike(Vec3 pos, Kind kind, float damage, int timer) {
            this.pos = pos;
            this.kind = kind;
            this.damage = damage;
            this.timer = timer;
        }
    }

    private final List<Strike> strikes = new ArrayList<>();

    public void mark(Vec3 pos, Kind kind, float damage, int warningTicks) {
        strikes.add(new Strike(pos, kind, damage, warningTicks));
    }

    /** Mark {@code count} strikes scattered around a target. */
    public void barrage(LivingEntity target, Kind kind, int count, double spread, float damage, int warningTicks) {
        var r = target.getRandom();
        for (int i = 0; i < count; i++) {
            Vec3 p = i == 0 ? target.position() : target.position().add((r.nextDouble() - 0.5) * 2 * spread, 0, (r.nextDouble() - 0.5) * 2 * spread);
            mark(p, kind, damage, warningTicks + i * 3);
        }
    }

    public void tick(ServerLevel level, LivingEntity owner) {
        for (Iterator<Strike> it = strikes.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            s.timer--;
            if (s.timer > 0) {
                if (s.timer % 4 == 0) {
                    switch (s.kind) {
                        case CANNON -> level.sendParticles(ParticleTypes.FLAME, s.pos.x, s.pos.y + 0.1, s.pos.z, 6, 0.6, 0.0, 0.6, 0.01);
                        case LIGHTNING -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, s.pos.x, s.pos.y + 0.2, s.pos.z, 8, 0.6, 0.2, 0.6, 0.05);
                        case GEYSER -> level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, s.pos.x, s.pos.y + 0.2, s.pos.z, 12, 0.5, 0.2, 0.5, 0.05);
                        case SLAM -> level.sendParticles(ParticleTypes.SQUID_INK, s.pos.x, s.pos.y + 0.2, s.pos.z, 10, 1.2, 0.1, 1.2, 0.01);
                    }
                }
                if (s.kind == Kind.CANNON && s.timer == 12) level.playSound(null, s.pos.x, s.pos.y, s.pos.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 1.0F, 0.5F);
                continue;
            }
            it.remove();
            land(level, owner, s);
        }
    }

    private static void land(ServerLevel level, LivingEntity owner, Strike s) {
        double radius = s.kind == Kind.LIGHTNING ? 2.5 : s.kind == Kind.SLAM ? 3.5 : 3.0;
        switch (s.kind) {
            case CANNON -> {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.pos.x, s.pos.y + 0.5, s.pos.z, 1, 0, 0, 0, 0);
                level.playSound(null, s.pos.x, s.pos.y, s.pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.9F);
            }
            case LIGHTNING -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(s.pos.x, s.pos.y, s.pos.z);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
            }
            case GEYSER -> {
                level.sendParticles(ParticleTypes.SPLASH, s.pos.x, s.pos.y + 1, s.pos.z, 80, 0.6, 2.0, 0.6, 0.3);
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, s.pos.x, s.pos.y + 1, s.pos.z, 60, 0.4, 3.0, 0.4, 0.4);
                level.playSound(null, s.pos.x, s.pos.y, s.pos.z, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            case SLAM -> {
                level.sendParticles(ParticleTypes.SPLASH, s.pos.x, s.pos.y + 0.5, s.pos.z, 120, 2.0, 0.5, 2.0, 0.4);
                level.sendParticles(ParticleTypes.EXPLOSION, s.pos.x, s.pos.y + 0.5, s.pos.z, 4, 1.5, 0.2, 1.5, 0);
                level.playSound(null, s.pos.x, s.pos.y, s.pos.z, SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 2.5F, 0.5F);
                level.playSound(null, s.pos.x, s.pos.y, s.pos.z, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 1.5F, 0.5F);
            }
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(s.pos, s.pos).inflate(radius, 3, radius),
                e -> e != owner && e.isAlive() && !(e instanceof MarineEntity) && !(e instanceof BountyBoss))) {
            double d = e.position().distanceTo(s.pos);
            float dmg = (float) (s.damage * Math.max(0.35, 1.0 - d / (radius + 1)));
            e.hurt(switch (s.kind) {
                case LIGHTNING -> level.damageSources().lightningBolt();
                case SLAM -> level.damageSources().mobAttack(owner);
                default -> level.damageSources().explosion(owner, owner);
            }, dmg);
            if (s.kind == Kind.LIGHTNING) e.setSecondsOnFire(4);
            Vec3 push = e.position().subtract(s.pos).normalize().scale(s.kind == Kind.GEYSER ? 0.3 : 0.8);
            e.push(push.x, s.kind == Kind.GEYSER ? 1.3 : 0.45, push.z);
            e.hurtMarked = true;
        }
    }

    /** Damage and launch everything around a point. */
    public static void shockwave(ServerLevel level, LivingEntity owner, Vec3 at, double radius, float damage, double lift) {
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 12, radius / 3, 0.2, radius / 3, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y + 0.5, at.z, 16, radius / 2, 0.1, radius / 2, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 0.6F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, 3, radius),
                e -> e != owner && e.isAlive() && !(e instanceof MarineEntity) && !(e instanceof BountyBoss))) {
            e.hurt(level.damageSources().mobAttack(owner), damage);
            Vec3 push = e.position().subtract(at).normalize().scale(1.1);
            e.push(push.x, lift, push.z);
            e.hurtMarked = true;
        }
    }

    /** Drag everything within range toward a point. */
    public static void pull(ServerLevel level, LivingEntity owner, Vec3 to, double radius, double strength) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(to, to).inflate(radius),
                e -> e != owner && e.isAlive() && !(e instanceof BountyBoss) && !(e instanceof MarineEntity))) {
            Vec3 d = to.subtract(e.position());
            double len = d.length();
            if (len < 1.5) continue;
            Vec3 v = d.scale(strength / len);
            e.push(v.x, v.y * 0.5, v.z);
            e.hurtMarked = true;
        }
    }

    /** Tell the whole server a boss fell, and to whom. */
    public static void announceDefeat(LivingEntity boss, String name, net.minecraft.ChatFormatting color, net.minecraft.world.damagesource.DamageSource source) {
        if (boss.level().isClientSide || boss.level().getServer() == null) return;
        net.minecraft.world.entity.Entity killer = source.getEntity();
        String by = killer instanceof net.minecraft.world.entity.player.Player p ? " by " + p.getGameProfile().getName()
                : killer instanceof com.piratecrew.entity.PirateEntity pe ? " by " + pe.getPirateName() : "";
        boss.level().getServer().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal(
                "\u2693 " + name + " has been defeated" + by + "!").withStyle(color, net.minecraft.ChatFormatting.BOLD), false);
    }

    /** A line shown above the hotbar of everyone near a boss. */
    public static void roar(LivingEntity boss, String line, net.minecraft.ChatFormatting color) {
        for (net.minecraft.world.entity.player.Player p : boss.level().players()) {
            if (p.distanceToSqr(boss) < 64 * 64) p.displayClientMessage(net.minecraft.network.chat.Component.literal(line).withStyle(color, net.minecraft.ChatFormatting.BOLD), true);
        }
    }
}
