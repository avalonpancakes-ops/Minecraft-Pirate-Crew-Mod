package com.piratecrew.bounty;

import com.mojang.authlib.properties.Property;
import com.piratecrew.Config;
import com.piratecrew.bank.BankManager;
import com.piratecrew.crew.Crew;
import com.piratecrew.crew.CrewData;
import com.piratecrew.crew.CrewManager;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.network.BountyBoardPacket;
import com.piratecrew.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bounties, paid in rubies.
 *
 * Crew members (players and recruited pirates) earn a bounty by killing players and pirates from
 * outside their crew. Killing a wanted rival player takes 25% of their bounty: they lose it, and the
 * killer gets it in rubies and adds it to their own bounty. Killing a wanted rival crew pirate pays its
 * whole bounty in rubies (it's dead for good) and adds 25% to the killer's bounty. Pirates have no
 * bank account, so a crew pirate's rubies go to its captain. Crew pirates climb tiers as their
 * bounty grows (see PirateEntity.checkBountyPromotion).
 */
public class BountyManager {
    /** killer+victim -> game time of the last counted kill, to stop two friends farming each other. */
    private static final Map<String, Long> RECENT = new HashMap<>();

    public static void onDeath(LivingEntity victim, @Nullable Entity killerEntity) {
        if (victim.level().isClientSide) return;
        if (victim instanceof com.piratecrew.entity.boss.BountyBoss boss) {
            onBossKill(victim, killerEntity, boss.bountyValue());
            return;
        }
        if (!(victim instanceof ServerPlayer) && !(victim instanceof PirateEntity)) return;
        // Bank business, not piracy: bounty hunters neither earn nor carry bounties.
        if (victim instanceof com.piratecrew.entity.BountyHunterEntity || killerEntity instanceof com.piratecrew.entity.BountyHunterEntity) return;
        // Enemy NPC crews count as pirates when killed (same bounty as any free pirate of their tier),
        // but they have no crew, so their own kills earn them nothing.
        if (killerEntity instanceof com.piratecrew.entity.RaiderPirateEntity) return;
        // The Order hangs pirates; it doesn't collect their bounties.
        if (killerEntity instanceof com.piratecrew.entity.MarineEntity) return;
        MinecraftServer server = victim.getServer();
        if (server == null) return;
        BountyData data = BountyData.get(server);

        LivingEntity killer = killerEntity instanceof ServerPlayer || killerEntity instanceof PirateEntity
                ? (LivingEntity) killerEntity : null;
        UUID victimCrew = CrewManager.crewIdOf(victim);
        UUID killerCrew = killer == null ? null : CrewManager.crewIdOf(killer);
        boolean sameCrew = victimCrew != null && victimCrew.equals(killerCrew);

        BountyData.Entry victimEntry = data.get(victim.getUUID());
        int bonus = 0;

        if (killer != null && killer != victim && !sameCrew) {
            long now = server.overworld().getGameTime();
            String pair = killer.getUUID() + ">" + victim.getUUID();
            Long last = RECENT.get(pair);
            boolean counts = last == null || now - last > Config.BOUNTY_REPEAT_COOLDOWN.get() * 20L;
            if (counts) RECENT.put(pair, now);
            if (RECENT.size() > 5000) RECENT.entrySet().removeIf(e -> now - e.getValue() > 72000);

            // 1. Claim from the victim's bounty. Only a rival crew can (plus the bank's debt collectors,
            //    handled in LoanManager); mobs, raiders and crewless players can't.
            //    - A player victim loses 25% of their bounty; the killer gets that 25% in rubies and adds it to their bounty.
            //    - A pirate victim is dead for good: its whole bounty is paid in rubies, and the killer adds 25% to their bounty.
            //    Pirates have no bank, so a crew pirate's rubies go to its captain's bank.
            if (victimEntry != null && victimEntry.amount > 0 && counts && killerCrew != null) {
                int full = victimEntry.amount;
                int share = Math.max(1, (int) Math.round(full * Config.BOUNTY_SHARE.get()));
                boolean pirateVictim = victim instanceof PirateEntity;
                int paid = pirateVictim ? full : share;
                bonus = share;
                if (killer instanceof ServerPlayer kp) {
                    BankManager.credit(server, kp.getUUID(), paid);
                    kp.sendSystemMessage(Component.literal(String.format("%,d", paid) + " rubies were deposited in your bank.").withStyle(ChatFormatting.GREEN));
                    announceClaim(server, killer, victim, paid, null);
                } else {
                    Crew crew = CrewData.get(server).byId(killerCrew);
                    if (crew != null && crew.captain != null) {
                        BankManager.credit(server, crew.captain, paid);
                        ServerPlayer cap = server.getPlayerList().getPlayer(crew.captain);
                        if (cap != null) cap.sendSystemMessage(Component.literal("Your pirate " + displayName(killer) + " sank "
                                + displayName(victim) + ": " + String.format("%,d", paid) + " rubies were deposited in your bank.").withStyle(ChatFormatting.GREEN));
                        announceClaim(server, killer, victim, paid, crew.name);
                    }
                }
                if (pirateVictim) {
                    victimEntry.amount = 0;
                } else {
                    victimEntry.amount = Math.max(0, full - share);
                    if (victim instanceof ServerPlayer vp) {
                        vp.sendSystemMessage(Component.literal(String.format("\u2620 %s took %,d rubies (25%%) of your bounty. It's now %,d.",
                                displayName(killer), share, victimEntry.amount)).withStyle(ChatFormatting.RED));
                    }
                }
            }

            // 2. Killer's bounty grows (crew members only)
            if (killerCrew != null && counts) {
                int gain;
                if (victim instanceof ServerPlayer) {
                    gain = Config.BOUNTY_PER_PLAYER_KILL.get();
                } else if (victim instanceof com.piratecrew.entity.MarineEntity m) {
                    // Fighting the Order of the Tide makes a name for a pirate.
                    gain = 5 + 4 * m.getRank().ordinal();
                } else {
                    PirateEntity p = (PirateEntity) victim;
                    gain = Config.BOUNTY_PER_PIRATE_KILL.get() + p.getTier().ordinal() * Config.BOUNTY_PER_PIRATE_TIER.get();
                    if (p.isRecruited()) gain *= 2;
                }
                gain += bonus;
                if (gain > 0) {
                    BountyData.Entry k = data.getOrCreate(killer.getUUID());
                    refresh(k, killer, killerCrew);
                    k.amount += gain;
                    if (victim instanceof ServerPlayer) k.playerKills++;
                    else k.pirateKills++;
                    if (killer instanceof ServerPlayer kp) {
                        kp.displayClientMessage(Component.literal("Your bounty rose to " + k.amount + " rubies!").withStyle(ChatFormatting.GOLD), true);
                        toast(kp, k.amount, gain);
                    } else if (killer instanceof PirateEntity kp) {
                        data.setDirty();
                        kp.checkBountyPromotion();
                        k.tier = kp.getTier().ordinal();
                    }
                }
            }
        }

        // Dead pirates are gone for good: tear down their poster.
        if (victim instanceof PirateEntity) data.remove(victim.getUUID());
        data.setDirty();

        // Refresh crew screens so the new bounties show.
        CrewData crews = CrewData.get(server);
        if (killerCrew != null && crews.byId(killerCrew) != null) CrewManager.syncCrew(server, crews.byId(killerCrew));
        if (victimCrew != null && !victimCrew.equals(killerCrew) && crews.byId(victimCrew) != null) CrewManager.syncCrew(server, crews.byId(victimCrew));
    }

    /**
     * Felling a Sundered Sea boss makes a pirate famous: the crew member who lands the killing blow
     * (player or crew pirate) gets the boss's bounty value added to their own bounty.
     */
    private static void onBossKill(LivingEntity boss, @Nullable Entity killerEntity, int value) {
        MinecraftServer server = boss.getServer();
        if (server == null || value <= 0) return;
        if (!(killerEntity instanceof ServerPlayer) && !(killerEntity instanceof PirateEntity)) return;
        LivingEntity killer = (LivingEntity) killerEntity;
        UUID crewId = CrewManager.crewIdOf(killer);
        if (crewId == null) {
            if (killer instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("Join a crew to earn a bounty for kills like that.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        BountyData data = BountyData.get(server);
        BountyData.Entry k = data.getOrCreate(killer.getUUID());
        refresh(k, killer, crewId);
        k.amount += value;
        k.pirateKills++;
        data.setDirty();
        if (killer instanceof PirateEntity kp) {
            kp.checkBountyPromotion();
            k.tier = kp.getTier().ordinal();
        } else if (killer instanceof ServerPlayer sp) {
            toast(sp, k.amount, value);
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal("☠ The bounty on ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(displayName(killer)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" rose by " + String.format("%,d", value) + " rubies for slaying ").withStyle(ChatFormatting.YELLOW))
                .append(boss.getDisplayName().copy().withStyle(ChatFormatting.RED))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW)), false);
        Crew crew = CrewData.get(server).byId(crewId);
        if (crew != null) CrewManager.syncCrew(server, crew);
    }

    /** The WANTED toast when a player's bounty climbs. */
    private static void toast(ServerPlayer p, int amount, int gain) {
        com.piratecrew.network.ModNetwork.sendTo(p, new com.piratecrew.network.ToastPacket(
                new net.minecraft.world.item.ItemStack(com.piratecrew.registry.ModItems.BOUNTY_BOARD.get()),
                "WANTED: " + String.format("%,d", amount), String.format("Your bounty rose by %,d rubies", gain), 0xD03030));
    }

    private static void announceClaim(MinecraftServer server, LivingEntity killer, LivingEntity victim, int amount, @Nullable String forCrew) {
        Component msg = Component.literal("\u2620 ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(displayName(killer)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" claimed ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.format("%,d", amount) + " rubies").withStyle(ChatFormatting.RED))
                .append(Component.literal(" of the bounty on ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(displayName(victim)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(forCrew != null ? " for " + forCrew + "!" : "!").withStyle(ChatFormatting.YELLOW));
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    private static String displayName(LivingEntity e) {
        if (e instanceof PirateEntity p) return p.getPirateName();
        if (e instanceof ServerPlayer sp) return sp.getGameProfile().getName();
        return e.getName().getString();
    }

    /** Keep a poster's name, face and crew up to date. */
    private static void refresh(BountyData.Entry e, LivingEntity who, @Nullable UUID crewId) {
        e.crewId = crewId;
        if (who instanceof PirateEntity p) {
            e.npc = true;
            e.name = p.getPirateName();
            e.skin = p.getSkinName();
            e.tier = p.getTier().ordinal();
        } else if (who instanceof ServerPlayer sp) {
            e.npc = false;
            e.name = sp.getGameProfile().getName();
            Property tex = sp.getGameProfile().getProperties().get("textures").stream().findFirst().orElse(null);
            if (tex != null) {
                e.textures = tex.getValue();
                e.texturesSig = tex.getSignature() == null ? "" : tex.getSignature();
            }
        }
    }

    public static void onLogin(ServerPlayer player) {
        BountyData data = BountyData.get(player.server);
        BountyData.Entry e = data.get(player.getUUID());
        if (e != null) {
            refresh(e, player, CrewManager.crewIdOf(player));
            data.setDirty();
        }
    }

    public static int bountyOf(MinecraftServer server, UUID id) {
        return BountyData.get(server).amountOf(id);
    }

    public static int[] killsOf(MinecraftServer server, UUID id) {
        BountyData.Entry e = BountyData.get(server).get(id);
        return e == null ? new int[]{0, 0} : new int[]{e.playerKills, e.pirateKills};
    }

    /** Send the wanted list to a player and open the bounty board. */
    public static void openBoard(ServerPlayer player) {
        MinecraftServer server = player.server;
        CrewData crews = CrewData.get(server);
        List<BountyBoardPacket.Poster> posters = new ArrayList<>();
        for (BountyData.Entry e : BountyData.get(server).wanted()) {
            if (posters.size() >= 60) break;
            Crew crew = e.npc ? crews.byId(e.crewId) : crews.crewOf(e.id);
            posters.add(new BountyBoardPacket.Poster(e.id, e.npc, e.name, e.skin, e.tier, e.textures, e.texturesSig,
                    crew == null ? "" : crew.name, e.amount, e.playerKills, e.pirateKills));
        }
        ModNetwork.sendTo(player, new BountyBoardPacket(posters));
    }
}
