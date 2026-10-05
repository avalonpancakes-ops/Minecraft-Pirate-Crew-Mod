package com.piratecrew.crew;

import com.piratecrew.Config;
import com.piratecrew.bounty.BountyData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The Four Emperors of the Sea: the crews with the highest combined bounty (every player and pirate
 * in the crew added up), as long as it's at least 1,000 rubies. Every player in an Emperor crew has
 * permanent Strength and Resistance; its captain and vice captains get Strength III and Resistance II.
 * Leave the crew, get kicked, or have the crew knocked out of the top four and the buffs go.
 */
public class EmperorManager {
    private static final List<UUID> EMPERORS = new ArrayList<>();
    private static boolean ranked;
    private static int ticks;

    public static long totalBounty(MinecraftServer server, Crew crew) {
        BountyData data = BountyData.get(server);
        long total = 0;
        for (UUID p : crew.players) total += data.amountOf(p);
        for (UUID n : crew.npcs.keySet()) total += data.amountOf(n);
        return total;
    }

    /** 1-4 for an Emperor crew, 0 otherwise. */
    public static int rankOf(UUID crewId) {
        int i = EMPERORS.indexOf(crewId);
        return i < 0 ? 0 : i + 1;
    }

    public static List<UUID> emperors() {
        return List.copyOf(EMPERORS);
    }

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        ticks++;
        if (ticks % 100 == 0 || !ranked) rerank(server);
        if (ticks % 40 == 0) applyBuffs(server);
    }

    private static void rerank(MinecraftServer server) {
        CrewData crews = CrewData.get(server);
        long min = Config.EMPEROR_MIN_BOUNTY.get();
        List<Crew> sorted = new ArrayList<>();
        for (Crew c : crews.all()) if (totalBounty(server, c) >= min) sorted.add(c);
        sorted.sort(Comparator.comparingLong((Crew c) -> totalBounty(server, c)).reversed());
        List<UUID> now = new ArrayList<>();
        for (int i = 0; i < Math.min(Config.EMPEROR_COUNT.get(), sorted.size()); i++) now.add(sorted.get(i).id);
        if (now.equals(EMPERORS)) {
            ranked = true;
            return;
        }

        List<UUID> before = new ArrayList<>(EMPERORS);
        EMPERORS.clear();
        EMPERORS.addAll(now);
        if (ranked) {
            for (UUID id : now) {
                if (before.contains(id)) continue;
                Crew c = crews.byId(id);
                if (c == null) continue;
                server.getPlayerList().broadcastSystemMessage(Component.literal("♛ ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(c.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                        .append(Component.literal(" is now one of the Emperors of the Sea! (#" + rankOf(id) + ", "
                                + String.format("%,d", totalBounty(server, c)) + " rubies in bounties)").withStyle(ChatFormatting.YELLOW)), false);
                for (UUID p : c.players) {
                    ServerPlayer sp = server.getPlayerList().getPlayer(p);
                    if (sp != null) sp.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            for (UUID id : before) {
                if (now.contains(id)) continue;
                Crew c = crews.byId(id);
                if (c == null) continue;
                server.getPlayerList().broadcastSystemMessage(Component.literal("♛ ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(c.name).withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD))
                        .append(Component.literal(" has been toppled from the Emperors of the Sea.").withStyle(ChatFormatting.GRAY)), false);
            }
        }
        ranked = true;
        // Refresh crew screens so the title shows / goes.
        for (UUID id : before) {
            Crew c = crews.byId(id);
            if (c != null) CrewManager.syncCrew(server, c);
        }
        for (UUID id : now) {
            Crew c = crews.byId(id);
            if (c != null && !before.contains(id)) CrewManager.syncCrew(server, c);
        }
        applyBuffs(server);
    }

    /** Our buffs are infinite and "ambient"; anything else (potions, beacons) is left alone. */
    private static boolean isOurs(MobEffectInstance e) {
        return e.isInfiniteDuration() && e.isAmbient();
    }

    private static void applyBuffs(MinecraftServer server) {
        CrewData crews = CrewData.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Crew c = crews.crewOf(p.getUUID());
            int rank = c == null ? 0 : rankOf(c.id);
            boolean officer = c != null && c.isOfficer(p.getUUID());
            set(p, MobEffects.DAMAGE_BOOST, rank == 0 ? -1 : officer ? 2 : 0);
            set(p, MobEffects.DAMAGE_RESISTANCE, rank == 0 ? -1 : officer ? 1 : 0);
        }
    }

    private static void set(ServerPlayer p, MobEffect effect, int amplifier) {
        MobEffectInstance cur = p.getEffect(effect);
        if (amplifier < 0) {
            if (cur != null && isOurs(cur)) p.removeEffect(effect);
            return;
        }
        if (cur == null) {
            p.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, true));
        } else if (isOurs(cur) && cur.getAmplifier() != amplifier) {
            p.removeEffect(effect);
            p.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, true));
        }
        // A stronger or timed potion is active: leave it; ours comes back when it runs out.
    }

    /** Chat list for /crew emperors. */
    public static void list(ServerPlayer to) {
        MinecraftServer server = to.server;
        CrewData crews = CrewData.get(server);
        to.sendSystemMessage(Component.literal("♛ The Emperors of the Sea").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (EMPERORS.isEmpty()) {
            to.sendSystemMessage(Component.literal(String.format("  None yet. A crew needs %,d rubies of combined bounty to claim a seat.",
                    Config.EMPEROR_MIN_BOUNTY.get())).withStyle(ChatFormatting.GRAY));
            return;
        }
        for (int i = 0; i < EMPERORS.size(); i++) {
            Crew c = crews.byId(EMPERORS.get(i));
            if (c == null) continue;
            to.sendSystemMessage(Component.literal("  #" + (i + 1) + " ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(c.name).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(String.format(" - %,d rubies", totalBounty(server, c))).withStyle(ChatFormatting.RED)));
        }
    }
}
