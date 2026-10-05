package com.piratecrew.pact;

import com.piratecrew.crew.CrewManager;
import com.piratecrew.entity.BankerEntity;
import com.piratecrew.entity.PirateEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** What each Soul Pact does: passive gifts, on-hit effects, and the active powers. */
public class PactPowers {
    private static final String SHADOW_STRIKE = "piratecrew_shadow_strike";

    /** The sea drains every pact. */
    public static boolean drained(LivingEntity e) {
        return e.isInWater();
    }

    // ------------------------------------------------------------------ passives (every second)

    public static void passives(LivingEntity e, SoulPact pact) {
        if (drained(e)) {
            e.addEffect(quiet(MobEffects.WEAKNESS, 40, 0));
            e.addEffect(quiet(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            return;
        }
        switch (pact) {
            case EMBER -> e.addEffect(quiet(MobEffects.FIRE_RESISTANCE, 60, 0));
            case TEMPEST -> e.addEffect(quiet(MobEffects.MOVEMENT_SPEED, 60, 0));
            case IRON -> e.addEffect(quiet(MobEffects.DAMAGE_RESISTANCE, 60, 0));
            case GALE -> e.addEffect(quiet(MobEffects.JUMP, 60, 1));
            case QUAKE -> e.addEffect(quiet(MobEffects.DIG_SPEED, 60, 1));
            case SHADOW -> {
                if (e instanceof Player) e.addEffect(quiet(MobEffects.NIGHT_VISION, 300, 0));
                if (e.isShiftKeyDown()) e.addEffect(quiet(MobEffects.INVISIBILITY, 30, 0));
            }
            case GRAVITY -> {
                if (e.isShiftKeyDown() && !e.onGround()) e.addEffect(quiet(MobEffects.SLOW_FALLING, 30, 0));
            }
            default -> {
            }
        }
    }

    private static MobEffectInstance quiet(MobEffect effect, int ticks, int amp) {
        return new MobEffectInstance(effect, ticks, amp, true, false, true);
    }

    // ------------------------------------------------------------------ hits

    /** The holder landed a melee blow. Returns the (possibly changed) damage. */
    public static float onMeleeHit(LivingEntity attacker, SoulPact pact, LivingEntity victim, float amount) {
        if (drained(attacker)) return amount;
        switch (pact) {
            case EMBER -> victim.setSecondsOnFire(4);
            case FROST -> {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                victim.setTicksFrozen(Math.min(victim.getTicksFrozen() + 60, 300));
            }
            case VENOM -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), attacker);
            case BLOOD -> attacker.heal(amount * 0.15F);
            case SHADOW -> {
                long until = attacker.getPersistentData().getLong(SHADOW_STRIKE);
                if (until > attacker.level().getGameTime()) {
                    attacker.getPersistentData().remove(SHADOW_STRIKE);
                    if (attacker.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1, victim.getZ(), 20, 0.4, 0.5, 0.4, 0.2);
                        sl.sendParticles(com.piratecrew.registry.ModParticles.WISP.get(), victim.getX(), victim.getY() + 1, victim.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                    }
                    return amount * 2.0F;
                }
            }
            default -> {
            }
        }
        return amount;
    }

    /** The holder was struck in melee. */
    public static void onStruck(LivingEntity holder, SoulPact pact, LivingEntity attacker) {
        if (pact == SoulPact.FROST && !drained(holder)) attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
    }

    // ------------------------------------------------------------------ crew pirates

    /** Crew pirates use their pact on their own: passives each second, the power when it fits the fight. */
    public static void pirateTick(PirateEntity p) {
        SoulPact pact = p.getPact();
        if (pact == null || !(p.level() instanceof ServerLevel)) return;
        if (p.tickCount % 20 == 0) passives(p, pact);
        if (p.pactCooldown > 0) {
            p.pactCooldown--;
            return;
        }
        LivingEntity t = p.getTarget();
        if (t == null || !t.isAlive() || drained(p) || p.tickCount % 5 != 0) return;
        double d = p.distanceTo(t);
        boolean sees = p.getSensing().hasLineOfSight(t);
        boolean use = switch (pact) {
            case EMBER, TEMPEST, VENOM, GRAVITY -> sees && d > 3 && d < 28;
            case FROST, QUAKE -> d < 5.5;
            case IRON -> d < 8 && p.getHealth() < p.getMaxHealth() * 0.8F;
            case GALE -> sees && d > 4 && d < 11;
            case SHADOW -> sees && d > 4 && d < 24;
            case BLOOD -> d < 5.5 && p.getHealth() < p.getMaxHealth() * 0.85F;
        };
        if (use && activate(p, pact, t, t.getEyePosition())) p.pactCooldown = pact.cooldown * 3 / 2;
    }

    // ------------------------------------------------------------------ powers

    /** Use a pact's power. {@code target} may be null (players aiming at a block). */
    public static boolean activate(LivingEntity caster, SoulPact pact, @Nullable LivingEntity target, Vec3 aim) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        double power = caster.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? caster.getAttributeValue(Attributes.ATTACK_DAMAGE) : 4.0;
        Vec3 eye = caster.getEyePosition();
        Vec3 dir = aim.subtract(eye);
        dir = dir.lengthSqr() < 1.0E-4 ? caster.getLookAngle() : dir.normalize();
        switch (pact) {
            case EMBER -> flameBurst(level, caster, eye, dir, power);
            case TEMPEST -> thunderstrike(level, caster, target != null ? target.position() : aim, power);
            case FROST -> frostNova(level, caster, power);
            case IRON -> ironSkin(level, caster);
            case GALE -> galeDash(level, caster, dir, power);
            case SHADOW -> {
                if (target == null) {
                    if (caster instanceof Player pl) pl.displayClientMessage(net.minecraft.network.chat.Component.literal("Look at a target to step behind it."), true);
                    return false;
                }
                return shadowStep(level, caster, target);
            }
            case QUAKE -> quake(level, caster, power);
            case VENOM -> venomCloud(level, caster, target != null ? target.position() : aim, power);
            case GRAVITY -> gravityWell(level, caster, target != null ? target.position() : aim, power);
            case BLOOD -> crimsonDrain(level, caster, power);
        }
        return true;
    }

    private static void flameBurst(ServerLevel level, LivingEntity caster, Vec3 eye, Vec3 dir, double power) {
        for (int i = -2; i <= 2; i++) {
            Vec3 d = dir.yRot((float) Math.toRadians(i * 9));
            SmallFireball ball = new SmallFireball(level, caster, d.x, d.y, d.z);
            ball.setPos(eye.x + d.x, eye.y - 0.2 + d.y, eye.z + d.z);
            level.addFreshEntity(ball);
        }
        for (int i = 0; i < 40; i++) {
            double r = 1 + level.random.nextDouble() * 6;
            Vec3 p = eye.add(dir.scale(r)).add((level.random.nextDouble() - 0.5) * r * 0.5, (level.random.nextDouble() - 0.5) * r * 0.4, (level.random.nextDouble() - 0.5) * r * 0.5);
            level.sendParticles(i % 2 == 0 ? com.piratecrew.registry.ModParticles.EMBER.get() : ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0.02);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.8F);
        for (LivingEntity e : hostiles(caster, eye, 7)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            if (to.length() > 7 || to.normalize().dot(dir) < 0.8) continue;
            e.hurt(src(caster), (float) (6 + power * 0.4));
            e.setSecondsOnFire(5);
        }
    }

    private static void thunderstrike(ServerLevel level, LivingEntity caster, Vec3 at, double power) {
        bolt(level, at);
        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity e : hostiles(caster, at, 3)) {
            e.hurt(level.damageSources().lightningBolt(), (float) (14 + power * 0.6));
            e.setSecondsOnFire(3);
            hit.add(e);
        }
        // Arcs to up to three more foes nearby.
        List<LivingEntity> near = new ArrayList<>(hostiles(caster, at, 10));
        near.removeAll(hit);
        near.sort((a, b) -> Double.compare(a.distanceToSqr(at), b.distanceToSqr(at)));
        for (int i = 0; i < Math.min(3, near.size()); i++) {
            LivingEntity e = near.get(i);
            bolt(level, e.position());
            e.hurt(level.damageSources().lightningBolt(), (float) (8 + power * 0.3));
        }
    }

    private static void bolt(ServerLevel level, Vec3 at) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(level);
        if (b == null) return;
        b.moveTo(at.x, at.y, at.z);
        b.setVisualOnly(true);
        level.addFreshEntity(b);
        level.sendParticles(com.piratecrew.registry.ModParticles.SPARK.get(), at.x, at.y + 0.5, at.z, 30, 1.2, 0.8, 1.2, 0.1);
    }

    private static void frostNova(ServerLevel level, LivingEntity caster, double power) {
        Vec3 c = caster.position();
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI / 24;
            level.sendParticles(com.piratecrew.registry.ModParticles.FROST.get(), c.x + Math.cos(a) * 6, c.y + 0.5, c.z + Math.sin(a) * 6, 3, 0.3, 0.3, 0.3, 0.02);
        }
        level.sendParticles(com.piratecrew.registry.ModParticles.FROST.get(), c.x, c.y + 1, c.z, 90, 4, 1, 4, 0.05);
        level.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 1, c.z, 60, 4, 1, 4, 0.05);
        level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
        level.playSound(null, caster.blockPosition(), SoundEvents.POWDER_SNOW_PLACE, SoundSource.PLAYERS, 1.5F, 0.5F);
        for (LivingEntity e : hostiles(caster, c, 7)) {
            e.hurt(level.damageSources().freeze(), (float) (8 + power * 0.4));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            e.setTicksFrozen(300);
        }
        // The sea around freezes over.
        BlockPos base = caster.blockPosition();
        BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-5, -2, -5), base.offset(5, 1, 5))) {
            if (p.distSqr(base) > 30) continue;
            if (level.getBlockState(p).is(Blocks.WATER) && level.getFluidState(p).isSource() && level.getBlockState(p.above()).isAir()) {
                level.setBlockAndUpdate(p, ice);
                level.scheduleTick(p, Blocks.FROSTED_ICE, 60 + level.random.nextInt(60));
            }
        }
    }

    private static void ironSkin(ServerLevel level, LivingEntity caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 2));
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()),
                caster.getX(), caster.getY() + 1, caster.getZ(), 60, 0.4, 0.8, 0.4, 0.1);
        level.playSound(null, caster.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.6F);
    }

    private static void galeDash(ServerLevel level, LivingEntity caster, Vec3 dir, double power) {
        Vec3 flat = new Vec3(dir.x, Math.max(-0.2, Math.min(0.4, dir.y)), dir.z).normalize();
        Vec3 from = caster.position();
        Vec3 to = from.add(flat.scale(10));
        caster.setDeltaMovement(flat.x * 2.2, flat.y * 1.2 + 0.35, flat.z * 2.2);
        caster.hurtMarked = true;
        caster.fallDistance = 0;
        for (int i = 0; i < 20; i++) {
            Vec3 p = from.lerp(to, i / 20.0);
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 1, p.z, 2, 0.3, 0.3, 0.3, 0.02);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.5F, 1.4F);
        for (LivingEntity e : hostiles(caster, from.lerp(to, 0.5), 7)) {
            if (distanceToSegment(e.position(), from, to) > 2.5) continue;
            e.hurt(src(caster), (float) (8 + power * 0.5));
            Vec3 side = e.position().subtract(from).subtract(flat.scale(e.position().subtract(from).dot(flat)));
            Vec3 push = side.lengthSqr() < 0.01 ? flat : side.normalize();
            e.push(push.x * 1.2, 0.6, push.z * 1.2);
            e.hurtMarked = true;
        }
    }

    private static boolean shadowStep(ServerLevel level, LivingEntity caster, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 behind = target.position().subtract(look.x * 1.6, 0, look.z * 1.6);
        Vec3 from = caster.position();
        // Behind the target, else beside it, else just above it: anywhere the caster fits (water is fine).
        Vec3 side = new Vec3(-look.z, 0, look.x).normalize().scale(1.6);
        Vec3 spot = null;
        for (Vec3 c : new Vec3[]{behind, target.position().add(side), target.position().subtract(side), target.position().add(0, target.getBbHeight(), 0)}) {
            if (level.noCollision(caster, caster.getBoundingBox().move(c.subtract(caster.position())))) {
                spot = c;
                break;
            }
        }
        if (spot == null) {
            if (caster instanceof Player pl) pl.displayClientMessage(net.minecraft.network.chat.Component.literal("No room to step behind your target."), true);
            return false;
        }
        caster.teleportTo(spot.x, spot.y, spot.z);
        caster.resetFallDistance();
        level.sendParticles(com.piratecrew.registry.ModParticles.WISP.get(), from.x, from.y + 1, from.z, 30, 0.3, 0.6, 0.3, 0.02);
        level.sendParticles(com.piratecrew.registry.ModParticles.WISP.get(), caster.getX(), caster.getY() + 1, caster.getZ(), 25, 0.3, 0.6, 0.3, 0.03);
        level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
        caster.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, true));
        caster.getPersistentData().putLong(SHADOW_STRIKE, level.getGameTime() + 100);
        if (caster instanceof net.minecraft.world.entity.Mob m) m.setTarget(target);
        return true;
    }

    private static void quake(ServerLevel level, LivingEntity caster, double power) {
        Vec3 c = caster.position();
        BlockState ground = level.getBlockState(caster.blockPosition().below());
        if (ground.isAir()) ground = Blocks.DIRT.defaultBlockState();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), c.x, c.y + 0.2, c.z, 200, 3.5, 0.2, 3.5, 0.3);
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 8, 3, 0.2, 3, 0);
        level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.5F);
        for (LivingEntity e : hostiles(caster, c, 7)) {
            e.hurt(src(caster), (float) (12 + power * 0.6));
            Vec3 push = e.position().subtract(c).normalize();
            e.push(push.x * 0.8, 0.9, push.z * 0.8);
            e.hurtMarked = true;
        }
    }

    private static void venomCloud(ServerLevel level, LivingEntity caster, Vec3 at, double power) {
        Vec3 eye = caster.getEyePosition();
        for (int i = 0; i < 16; i++) {
            Vec3 p = eye.lerp(at, i / 16.0);
            level.sendParticles(ParticleTypes.ITEM_SLIME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.LLAMA_SPIT, SoundSource.PLAYERS, 1.5F, 0.6F);
        float dmg = (float) (2 + power * 0.1);
        for (int i = 0; i < 14; i++) {
            later(level, i * 10, () -> {
                level.sendParticles(ParticleTypes.SNEEZE, at.x, at.y + 0.5, at.z, 25, 2.5, 0.6, 2.5, 0.01);
                level.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.3, at.z, 10, 2.5, 0.3, 2.5, 0);
                for (LivingEntity e : hostiles(caster, at, 3.5)) {
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1), caster);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), caster);
                    e.hurt(level.damageSources().indirectMagic(caster, caster), dmg);
                }
            });
        }
    }

    private static void gravityWell(ServerLevel level, LivingEntity caster, Vec3 at, double power) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 120, 4, 2, 4, 0.2);
        level.sendParticles(com.piratecrew.registry.ModParticles.WISP.get(), at.x, at.y + 1, at.z, 60, 3, 1.5, 3, 0.02);
        level.playSound(null, BlockPos.containing(at), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5F, 0.6F);
        for (LivingEntity e : hostiles(caster, at, 9)) {
            Vec3 pull = at.subtract(e.position());
            Vec3 v = pull.lengthSqr() < 0.01 ? Vec3.ZERO : pull.normalize().scale(Math.min(1.2, pull.length() * 0.18));
            e.setDeltaMovement(v.x, 0.9, v.z);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 2));
        }
        float dmg = (float) (14 + power * 0.6);
        later(level, 25, () -> {
            level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 1, at.z, 10, 3, 1, 3, 0);
            level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.4F);
            for (LivingEntity e : hostiles(caster, at.add(0, 3, 0), 7)) {
                e.removeEffect(MobEffects.LEVITATION);
                e.setDeltaMovement(0, -2.2, 0);
                e.hurtMarked = true;
                e.hurt(src(caster), dmg);
            }
        });
    }

    private static void crimsonDrain(ServerLevel level, LivingEntity caster, double power) {
        DustParticleOptions blood = new DustParticleOptions(new Vector3f(0.7F, 0.02F, 0.05F), 1.6F);
        float drained = 0;
        for (LivingEntity e : hostiles(caster, caster.position(), 6)) {
            float dmg = (float) (6 + power * 0.3);
            if (e.hurt(level.damageSources().indirectMagic(caster, caster), dmg)) drained += dmg;
            Vec3 from = e.position().add(0, e.getBbHeight() / 2, 0);
            Vec3 to = caster.position().add(0, 1, 0);
            for (int i = 0; i < 10; i++) {
                Vec3 p = from.lerp(to, i / 10.0);
                level.sendParticles(i % 2 == 0 ? com.piratecrew.registry.ModParticles.BLOOD.get() : blood, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
        level.sendParticles(blood, caster.getX(), caster.getY() + 1, caster.getZ(), 25, 2.5, 0.8, 2.5, 0);
        level.sendParticles(com.piratecrew.registry.ModParticles.BLOOD.get(), caster.getX(), caster.getY() + 1.5, caster.getZ(), 25, 2.5, 0.8, 2.5, 0);
        level.playSound(null, caster.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2.0F, 0.8F);
        caster.heal(Math.min(12.0F, drained * 0.5F));
    }

    // ------------------------------------------------------------------ helpers

    private static DamageSource src(LivingEntity caster) {
        return caster instanceof Player p ? caster.damageSources().playerAttack(p) : caster.damageSources().mobAttack(caster);
    }

    /** Everything a pact power may hurt: not the caster, its crew, its leader, townsfolk or creative players. */
    public static List<LivingEntity> hostiles(LivingEntity caster, Vec3 center, double radius) {
        Player leader = caster instanceof PirateEntity pe ? pe.getLeader() : null;
        return caster.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), e ->
                e != caster && e != leader && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof AbstractVillager) && !(e instanceof BankerEntity)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && !CrewManager.areCrewmates(caster, e)
                        && e.position().distanceToSqr(center) <= (radius + e.getBbWidth()) * (radius + e.getBbWidth()));
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = Math.max(0, Math.min(1, p.subtract(a).dot(ab) / Math.max(1.0E-6, ab.lengthSqr())));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    // ------------------------------------------------------------------ delayed effects

    private record Task(ServerLevel level, long at, Runnable run) {}

    private static final List<Task> TASKS = new ArrayList<>();

    private static void later(ServerLevel level, int ticks, Runnable run) {
        if (ticks <= 0) run.run();
        else TASKS.add(new Task(level, level.getGameTime() + ticks, run));
    }

    /** Server tick. */
    public static void tick() {
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.level.getGameTime() >= t.at) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) {
            try {
                t.run.run();
            } catch (Exception e) {
                com.piratecrew.PirateCrew.LOGGER.warn("Pirate Crew: a pact effect failed", e);
            }
        }
    }
}
