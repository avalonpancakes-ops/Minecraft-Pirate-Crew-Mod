package com.piratecrew.pact;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Pact Mastery: the more a pact is used, the stronger the bond. Every use of its moves and every
 * kill made while bound earns mastery; five ranks shorten cooldowns, strengthen the powers, unlock a
 * second move (the Technique, rank III) and finally an Ultimate Form (rank V) that lasts 20 seconds.
 * Mastery is kept per pact, so a soul that changes pacts doesn't lose what it learned.
 */
public class PactMastery {
    public static final int[] RANK_AT = {0, 25, 60, 120, 200};
    public static final String[] RANK_NAMES = {"Bound", "Attuned", "Adept", "Master", "Ascended"};
    private static final float[] COOLDOWN = {1.0F, 0.85F, 0.75F, 0.65F, 0.6F};
    private static final float[] POWER = {1.0F, 1.0F, 1.1F, 1.25F, 1.3F};
    public static final int TECHNIQUE_RANK = 3, ULTIMATE_RANK = 5;
    public static final int ULT_TICKS = 400, ULT_COOLDOWN = 6000;

    private static final String MASTERY = "piratecrew_mastery";
    private static final String TECH_READY = "piratecrew_tech_ready";
    private static final String ULT_UNTIL = "piratecrew_ult_until";
    private static final String ULT_READY = "piratecrew_ult_ready";

    // ------------------------------------------------------------------ the new moves

    public static String techniqueName(SoulPact p) {
        return switch (p) {
            case EMBER -> "Meteor";
            case TEMPEST -> "Static Field";
            case FROST -> "Glacial Lance";
            case IRON -> "Anchor Slam";
            case GALE -> "Cyclone";
            case SHADOW -> "Nightfall";
            case QUAKE -> "Fissure";
            case VENOM -> "Serpent Spray";
            case GRAVITY -> "Repulse";
            case BLOOD -> "Hemorrhage";
        };
    }

    public static String techniqueText(SoulPact p) {
        return switch (p) {
            case EMBER -> "A meteor of fire falls where you aim and bursts, setting everything around it ablaze.";
            case TEMPEST -> "For 6 seconds, lightning leaps from you to the nearest foe every second.";
            case FROST -> "A line of ice spikes bursts from the ground ahead, freezing everything it touches.";
            case IRON -> "Leap up and slam down like a dropped anchor: a shockwave that drags foes in and slows them.";
            case GALE -> "A whirlwind where you aim sucks foes in, lifts them, then flings them away.";
            case SHADOW -> "Darkness falls: nearby foes are blinded and weakened while you vanish, quick and primed to strike double.";
            case QUAKE -> "The ground splits in a line ahead, erupting under foes and hurling them up.";
            case VENOM -> "Spray venom in a cone: deadly poison and withering.";
            case GRAVITY -> "Blast every foe around you away and leave them floating.";
            case BLOOD -> "Open the wounds of every foe near you: they bleed for 6 seconds and you drink half of it.";
        };
    }

    public static String ultimateName(SoulPact p) {
        return switch (p) {
            case EMBER -> "Inferno Form";
            case TEMPEST -> "Storm Avatar";
            case FROST -> "Glacier Form";
            case IRON -> "Iron Colossus";
            case GALE -> "Wind Spirit";
            case SHADOW -> "Phantom Form";
            case QUAKE -> "Titan Form";
            case VENOM -> "Hydra Form";
            case GRAVITY -> "Event Horizon";
            case BLOOD -> "Crimson Lord";
        };
    }

    public static String ultimateText(SoulPact p) {
        return switch (p) {
            case EMBER -> "Wreathed in fire: Strength, and a blazing aura burns everything close.";
            case TEMPEST -> "Speed III, and lightning strikes a nearby foe every 2 seconds.";
            case FROST -> "Resistance; foes near you freeze in place and the sea freezes under your feet.";
            case IRON -> "Resistance III and Strength II: nearly unbreakable.";
            case GALE -> "Speed, high jumps and slow falling; a third of all blows simply miss you.";
            case SHADOW -> "Invisible and quick, and every blow strikes double.";
            case QUAKE -> "Strength II; a shockwave bursts from you every 2 seconds.";
            case VENOM -> "Regeneration; a poison mist around you sickens and weakens foes.";
            case GRAVITY -> "Everything near is dragged toward you and lifted off its feet; you fall slowly.";
            case BLOOD -> "Regeneration II and Strength; your blows steal 40% as life and you drain everyone close.";
        };
    }

    // ------------------------------------------------------------------ storage

    /** Players keep mastery through death (persisted tag); pirates keep it with the entity. */
    private static CompoundTag store(LivingEntity e) {
        CompoundTag root = e.getPersistentData();
        if (!(e instanceof Player)) return root;
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static int points(LivingEntity e, SoulPact pact) {
        return store(e).getCompound(MASTERY).getInt(pact.id);
    }

    public static int rank(int points) {
        int r = 1;
        for (int i = 1; i < RANK_AT.length; i++) if (points >= RANK_AT[i]) r = i + 1;
        return r;
    }

    public static int rank(LivingEntity e, SoulPact pact) {
        return rank(points(e, pact));
    }

    /** Award mastery; announces a new rank. */
    public static void award(LivingEntity e, SoulPact pact, int amount) {
        if (amount <= 0 || e.level().isClientSide) return;
        CompoundTag tag = store(e);
        CompoundTag m = tag.getCompound(MASTERY);
        int before = m.getInt(pact.id);
        int after = Math.min(before + amount, 100000);
        m.putInt(pact.id, after);
        tag.put(MASTERY, m);
        int oldRank = rank(before), newRank = rank(after);
        if (newRank > oldRank) onRankUp(e, pact, newRank);
    }

    /** Operators can set it directly (testing). */
    public static void setPoints(LivingEntity e, SoulPact pact, int points) {
        CompoundTag tag = store(e);
        CompoundTag m = tag.getCompound(MASTERY);
        m.putInt(pact.id, Math.max(0, points));
        tag.put(MASTERY, m);
    }

    private static void onRankUp(LivingEntity e, SoulPact pact, int rank) {
        if (!(e.level() instanceof ServerLevel level)) return;
        burst(level, e, pact, 60);
        level.playSound(null, e.blockPosition(), com.piratecrew.registry.ModSounds.PACT_BIND.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        if (e instanceof ServerPlayer p) {
            String unlock = rank == TECHNIQUE_RANK ? " New move: " + techniqueName(pact) + " (Technique key, G)."
                    : rank == ULTIMATE_RANK ? " Ultimate unlocked: " + ultimateName(pact) + " (Ultimate key, V)."
                    : " Cooldowns shorter" + (rank >= 4 ? ", powers stronger." : ".");
            p.sendSystemMessage(Component.literal("✦ " + pact.title() + " mastery: " + RANK_NAMES[rank - 1] + " (rank " + roman(rank) + ")!")
                    .withStyle(pact.color, ChatFormatting.BOLD)
                    .append(Component.literal(unlock).withStyle(ChatFormatting.GRAY).withStyle(s -> s.withBold(false))));
            com.piratecrew.network.ModNetwork.sendTo(p, new com.piratecrew.network.ToastPacket(
                    new net.minecraft.world.item.ItemStack(com.piratecrew.registry.ModItems.SOUL_PACTS.get(pact).get()),
                    pact.label + " Mastery " + roman(rank), rank == TECHNIQUE_RANK ? "Unlocked " + techniqueName(pact)
                    : rank == ULTIMATE_RANK ? "Unlocked " + ultimateName(pact) : RANK_NAMES[rank - 1], pact.seal));
            if (rank == ULTIMATE_RANK) com.piratecrew.goals.Goals.grant(p, com.piratecrew.goals.Goal.ASCENDED);
        } else if (e instanceof com.piratecrew.entity.PirateEntity pe) {
            pe.updateDisplayName();
        }
    }

    public static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }

    // ------------------------------------------------------------------ scaling

    public static float cooldownFactor(LivingEntity e, SoulPact pact) {
        float f = COOLDOWN[rank(e, pact) - 1];
        return ultActive(e) ? f * 0.25F : f;
    }

    public static float powerFactor(LivingEntity e, SoulPact pact) {
        float f = POWER[rank(e, pact) - 1];
        return ultActive(e) ? f * 1.5F : f;
    }

    public static int powerCooldown(LivingEntity e, SoulPact pact) {
        return Math.max(10, Math.round(pact.cooldown * cooldownFactor(e, pact)));
    }

    public static int techniqueCooldown(LivingEntity e, SoulPact pact) {
        return Math.max(20, Math.round(pact.cooldown * 1.5F * cooldownFactor(e, pact)));
    }

    // ------------------------------------------------------------------ technique / ultimate timers

    public static long techReady(LivingEntity e) {
        return store(e).getLong(TECH_READY);
    }

    public static void setTechReady(LivingEntity e, long at) {
        store(e).putLong(TECH_READY, at);
    }

    public static long ultUntil(LivingEntity e) {
        return store(e).getLong(ULT_UNTIL);
    }

    public static long ultReady(LivingEntity e) {
        return store(e).getLong(ULT_READY);
    }

    public static boolean ultActive(LivingEntity e) {
        return ultUntil(e) > e.level().getGameTime();
    }

    /** Enter the Ultimate Form. */
    public static void startUltimate(LivingEntity e, SoulPact pact) {
        long now = e.level().getGameTime();
        store(e).putLong(ULT_UNTIL, now + ULT_TICKS);
        store(e).putLong(ULT_READY, now + ULT_COOLDOWN);
        if (!(e.level() instanceof ServerLevel level)) return;
        burst(level, e, pact, 160);
        level.sendParticles(com.piratecrew.registry.ModParticles.GLYPH.get(), e.getX(), e.getY() + 1.2, e.getZ(), 30, 1.2, 1.0, 1.2, 0.04);
        level.playSound(null, e.blockPosition(), com.piratecrew.registry.ModSounds.PACT_BIND.get(), SoundSource.PLAYERS, 2.0F, 0.7F);
        level.playSound(null, e.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
        if (e instanceof Player p) {
            p.displayClientMessage(Component.literal("✦ " + ultimateName(pact).toUpperCase() + " ✦").withStyle(pact.color, ChatFormatting.BOLD), true);
        }
        // tidy cooldowns so the form's speed shows straight away
        PactPowers.ultimateBegin(e, pact);
    }

    public static void endUltimate(LivingEntity e, SoulPact pact) {
        store(e).putLong(ULT_UNTIL, 0);
        if (e instanceof Player p) p.displayClientMessage(Component.literal(ultimateName(pact) + " fades.").withStyle(ChatFormatting.GRAY), true);
    }

    /** Ultimate Form upkeep, every 5 ticks while active (aura) and its effects every second. */
    public static void tickUltimate(LivingEntity e, SoulPact pact) {
        if (!(e.level() instanceof ServerLevel level)) return;
        long until = ultUntil(e);
        if (until == 0) return;
        long now = level.getGameTime();
        if (now >= until) {
            store(e).putLong(ULT_UNTIL, 0);
            if (e instanceof Player p) p.displayClientMessage(Component.literal(ultimateName(pact) + " fades.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (now % 5 != 0) return;
        int c = pact.seal;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F), 1.4F);
        double a = now * 0.35;
        for (int i = 0; i < 6; i++) {
            double ang = a + i * Math.PI / 3;
            level.sendParticles(dust, e.getX() + Math.cos(ang) * 0.9, e.getY() + 0.2 + (i % 3) * 0.6, e.getZ() + Math.sin(ang) * 0.9, 1, 0, 0, 0, 0);
        }
        if (now % 20 == 0) PactPowers.ultimateTick(level, e, pact, now);
    }

    private static void burst(ServerLevel level, LivingEntity e, SoulPact pact, int count) {
        int c = pact.seal;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F), 1.8F);
        level.sendParticles(dust, e.getX(), e.getY() + 1, e.getZ(), count, 0.8, 1.0, 0.8, 0.1);
    }
}
