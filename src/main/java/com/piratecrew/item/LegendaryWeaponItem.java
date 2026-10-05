package com.piratecrew.item;

import com.piratecrew.pact.PactPowers;
import com.piratecrew.registry.ModParticles;
import com.piratecrew.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** One unique weapon per boss, each with a power of its own. */
public class LegendaryWeaponItem extends SwordItem {
    public enum Legend {
        BROADSIDE("broadside_cutlass", GearTier.ABYSSAL, 4, -2.4F, ChatFormatting.GOLD,
                "Graves' own blade. Every third hit fires a broadside: a burst that hurts all foes around the target."),
        GRASP("krakens_grasp", GearTier.KRAKENBONE, 4, -2.6F, ChatFormatting.LIGHT_PURPLE,
                "Hooked like a tentacle. Hits drag foes toward you and slow them; sometimes they're blinded by ink."),
        STORMCALLER("stormcaller", GearTier.STORMFORGED, 4, -2.2F, ChatFormatting.YELLOW,
                "Sorel's saber. 1 in 4 hits calls lightning down on the target, arcing to another foe nearby."),
        FANG("leviathans_fang", GearTier.LEVIATHAN, 4, -2.4F, ChatFormatting.AQUA,
                "A tooth of the serpent. Hits leave a withering wound; in water each hit heals you, and holding it grants Conduit Power."),
        IRON_TIDE("iron_tide", GearTier.SOVEREIGN, 6, -2.9F, ChatFormatting.RED,
                "Vane's great blade. Use it to send out the Iron Tide: a shockwave that hurls back every foe around you (8 s).");

        public final String id;
        public final GearTier tier;
        public final int damage;
        public final float speed;
        public final ChatFormatting color;
        public final String power;

        Legend(String id, GearTier tier, int damage, float speed, ChatFormatting color, String power) {
            this.id = id;
            this.tier = tier;
            this.damage = damage;
            this.speed = speed;
            this.color = color;
            this.power = power;
        }
    }

    private static final String HITS = "piratecrew_hits";
    public final Legend legend;

    public LegendaryWeaponItem(Legend legend, Properties props) {
        super(legend.tier.tier, legend.damage, legend.speed, props.rarity(Rarity.EPIC).fireResistant());
        this.legend = legend;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (!(attacker.level() instanceof ServerLevel level)) return r;
        Vec3 at = target.position().add(0, target.getBbHeight() / 2, 0);
        switch (legend) {
            case BROADSIDE -> {
                int hits = stack.getOrCreateTag().getInt(HITS) + 1;
                stack.getOrCreateTag().putInt(HITS, hits % 3);
                if (hits >= 3) {
                    level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 0.3, 0.3, 0.3, 0);
                    level.sendParticles(ModParticles.EMBER.get(), at.x, at.y, at.z, 24, 0.8, 0.6, 0.8, 0.08);
                    level.playSound(null, target.blockPosition(), ModSounds.CANNON_FIRE.get(), SoundSource.PLAYERS, 1.2F, 1.3F);
                    for (LivingEntity e : PactPowers.hostiles(attacker, at, 3.0)) {
                        e.hurt(level.damageSources().explosion(attacker, attacker), 8.0F);
                        Vec3 push = e.position().subtract(target.position()).normalize();
                        e.push(push.x * 0.5, 0.35, push.z * 0.5);
                    }
                }
            }
            case GRASP -> {
                Vec3 pull = attacker.position().subtract(target.position());
                if (pull.lengthSqr() > 4) {
                    Vec3 v = pull.normalize().scale(0.9);
                    target.push(v.x, 0.25, v.z);
                    target.hurtMarked = true;
                }
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), attacker);
                if (attacker.getRandom().nextFloat() < 0.15F) {
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), attacker);
                    level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0.05);
                }
            }
            case STORMCALLER -> {
                if (attacker.getRandom().nextFloat() < 0.25F) {
                    bolt(level, target.position());
                    target.hurt(level.damageSources().lightningBolt(), 8.0F);
                    level.sendParticles(ModParticles.SPARK.get(), at.x, at.y, at.z, 20, 0.5, 0.6, 0.5, 0.1);
                    for (LivingEntity e : PactPowers.hostiles(attacker, target.position(), 8.0)) {
                        if (e == target) continue;
                        bolt(level, e.position());
                        e.hurt(level.damageSources().lightningBolt(), 5.0F);
                        break;
                    }
                }
            }
            case FANG -> {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), attacker);
                level.sendParticles(ModParticles.BLOOD.get(), at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0);
                if (attacker.isInWater()) attacker.heal(2.0F);
            }
            case IRON_TIDE -> {
                Vec3 push = target.position().subtract(attacker.position()).normalize();
                target.push(push.x * 0.6, 0.2, push.z * 0.6);
            }
        }
        return r;
    }

    private static void bolt(ServerLevel level, Vec3 at) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(level);
        if (b == null) return;
        b.moveTo(at.x, at.y, at.z);
        b.setVisualOnly(true);
        level.addFreshEntity(b);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (legend != Legend.IRON_TIDE) return super.use(level, player, hand);
        if (level instanceof ServerLevel sl) {
            Vec3 c = player.position();
            for (int i = 0; i < 40; i++) {
                double a = i * Math.PI / 20;
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x + Math.cos(a) * 3.5, c.y + 0.6, c.z + Math.sin(a) * 3.5, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.SPLASH, c.x + Math.cos(a) * 5, c.y + 0.3, c.z + Math.sin(a) * 5, 4, 0.2, 0.1, 0.2, 0.1);
            }
            sl.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.6F);
            sl.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.2F, 0.8F);
            for (LivingEntity e : PactPowers.hostiles(player, c, 6.0)) {
                e.hurt(level.damageSources().playerAttack(player), 10.0F);
                Vec3 push = e.position().subtract(c).normalize();
                e.push(push.x * 1.6, 0.6, push.z * 1.6);
                e.hurtMarked = true;
            }
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, 160);
        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (legend == Legend.FANG && selected && !level.isClientSide && entity instanceof LivingEntity le && le.isInWater()
                && level.getGameTime() % 40 == 0) {
            le.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 100, 0, true, false, true));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(legend.color);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Legendary").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        StringBuilder line = new StringBuilder();
        for (String word : legend.power.split(" ")) {
            if (line.length() + word.length() > 38) {
                lines.add(Component.literal(line.toString().trim()).withStyle(ChatFormatting.GRAY));
                line.setLength(0);
            }
            line.append(word).append(' ');
        }
        if (line.length() > 0) lines.add(Component.literal(line.toString().trim()).withStyle(ChatFormatting.GRAY));
    }
}
