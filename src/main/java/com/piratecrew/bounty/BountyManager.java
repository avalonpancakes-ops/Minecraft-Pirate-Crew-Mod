package com.piratecrew.bounty;

import com.mojang.authlib.properties.Property;
import com.piratecrew.Config;
import com.piratecrew.crew.Crew;
import com.piratecrew.crew.CrewData;
import com.piratecrew.crew.CrewManager;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.network.BountyBoardPacket;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
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
 * outside their crew. Whoever kills a wanted crew member (anyone outside that crew) claims the full
 * bounty: a player gets the rubies in their inventory, a pirate drops them where the target fell.
 * The killer also takes a share of the target's bounty onto their own head.
 */
public class BountyManager {
    /** killer+victim -> game time of the last counted kill, to stop two friends farming each other. */
    private static final Map<String, Long> RECENT = new HashMap<>();

    public static void onDeath(LivingEntity victim, @Nullable Entity killerEntity) {
        if (victim.level().isClientSide) return;
        if (!(victim instanceof ServerPlayer) && !(victim instanceof PirateEntity)) return;
        MinecraftServer server = victim.getServer();
        if (server == null) return;
        BountyData data = BountyData.get(server);

        LivingEntity killer = killerEntity instanceof ServerPlayer || killerEntity instanceof PirateEntity
                ? (LivingEntity) killerEntity : null;
        UUID victimCrew = CrewManager.crewIdOf(victim);
        UUID killerCrew = killer == null ? null : CrewManager.crewIdOf(killer);
        boolean sameCrew = victimCrew != null && victimCrew.equals(killerCrew);

        BountyData.Entry victimEntry = data.get(victim.getUUID());
        int claimed = 0;

        if (killer != null && killer != victim && !sameCrew) {
            long now = server.overworld().getGameTime();
            String pair = killer.getUUID() + ">" + victim.getUUID();
            Long last = RECENT.get(pair);
            boolean counts = last == null || now - last > Config.BOUNTY_REPEAT_COOLDOWN.get() * 20L;
            if (counts) RECENT.put(pair, now);
            if (RECENT.size() > 5000) RECENT.entrySet().removeIf(e -> now - e.getValue() > 72000);

            // 1. Claim the victim's bounty
            if (victimEntry != null && victimEntry.amount > 0 && counts) {
                claimed = victimEntry.amount;
                payOut(killer, victim, claimed);
                announceClaim(server, killer, victim, claimed);
                victimEntry.amount = 0;
            }

            // 2. Killer's bounty grows (crew members only)
            if (killerCrew != null && counts) {
                int gain;
                if (victim instanceof ServerPlayer) {
                    gain = Config.BOUNTY_PER_PLAYER_KILL.get();
                } else {
                    PirateEntity p = (PirateEntity) victim;
                    gain = Config.BOUNTY_PER_PIRATE_KILL.get() + p.getTier().ordinal() * Config.BOUNTY_PER_PIRATE_TIER.get();
                    if (p.isRecruited()) gain *= 2;
                }
                gain += (int) Math.round(claimed * Config.BOUNTY_SHARE.get());
                if (gain > 0) {
                    BountyData.Entry k = data.getOrCreate(killer.getUUID());
                    refresh(k, killer, killerCrew);
                    k.amount += gain;
                    if (victim instanceof ServerPlayer) k.playerKills++;
                    else k.pirateKills++;
                    if (killer instanceof ServerPlayer kp) {
                        kp.displayClientMessage(Component.literal("Your bounty rose to " + k.amount + " rubies!").withStyle(ChatFormatting.GOLD), true);
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

    private static void payOut(LivingEntity killer, LivingEntity victim, int amount) {
        List<ItemStack> stacks = new ArrayList<>();
        int left = amount;
        while (left > 0) {
            int n = Math.min(64, left);
            stacks.add(new ItemStack(ModItems.RUBY.get(), n));
            left -= n;
        }
        for (ItemStack s : stacks) {
            if (killer instanceof ServerPlayer sp) {
                ItemHandlerHelper.giveItemToPlayer(sp, s);
            } else {
                ItemEntity item = new ItemEntity(victim.level(), victim.getX(), victim.getY() + 0.5, victim.getZ(), s);
                item.setDefaultPickUpDelay();
                victim.level().addFreshEntity(item);
            }
        }
    }

    private static void announceClaim(MinecraftServer server, LivingEntity killer, LivingEntity victim, int amount) {
        Component msg = Component.literal("☠ ").withStyle(ChatFormatting.DARK_RED)
                .append(Component.literal(displayName(killer)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" claimed the ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(amount + "-ruby").withStyle(ChatFormatting.RED))
                .append(Component.literal(" bounty on ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(displayName(victim)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));
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
