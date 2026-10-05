package com.piratecrew.goals;

import com.piratecrew.bank.LoanManager;
import com.piratecrew.bounty.BountyManager;
import com.piratecrew.crew.CrewManager;
import com.piratecrew.crew.EmperorManager;
import com.piratecrew.item.GearTier;
import com.piratecrew.network.GoalSyncPacket;
import com.piratecrew.network.ModNetwork;
import com.piratecrew.pact.SoulPacts;
import com.piratecrew.registry.ModItems;
import com.piratecrew.sundered.SunderedSea;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Tracks each player's Voyage Goals (persisted through death) and announces new ones. */
public class Goals {
    private static final String KEY = "piratecrew_goals";

    private static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static long mask(Player p) {
        return data(p).getLong(KEY);
    }

    public static boolean has(Player p, Goal g) {
        return (mask(p) & g.bit()) != 0;
    }

    public static void grant(ServerPlayer p, Goal g) {
        long m = mask(p);
        if ((m & g.bit()) != 0) return;
        m |= g.bit();
        data(p).putLong(KEY, m);
        ModNetwork.sendTo(p, new GoalSyncPacket(m, g.ordinal()));
        p.sendSystemMessage(Component.literal("✦ Voyage goal: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(g.title).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(" - " + g.description).withStyle(ChatFormatting.GRAY)));
    }

    public static void sync(ServerPlayer p) {
        ModNetwork.sendTo(p, new GoalSyncPacket(mask(p), -1));
    }

    public static void copy(Player from, Player to) {
        long m = mask(from);
        if (m != 0) data(to).putLong(KEY, m | mask(to));
    }

    /** Goals that are states rather than events, checked every couple of seconds. */
    public static void check(ServerPlayer p) {
        UUID crew = CrewManager.crewIdOf(p);
        if (crew != null) {
            grant(p, Goal.CREW);
            if (EmperorManager.rankOf(crew) > 0) grant(p, Goal.EMPEROR);
        }
        int bounty = BountyManager.bountyOf(p.server, p.getUUID());
        if (bounty >= 100) grant(p, Goal.BOUNTY_100);
        if (bounty >= 1000) grant(p, Goal.BOUNTY_1000);
        if (bounty >= 10000) grant(p, Goal.BOUNTY_10000);
        if (LoanManager.loanOf(p.server, p.getUUID()) != null) grant(p, Goal.LOAN);
        if (p.level().dimension() == SunderedSea.LEVEL) grant(p, Goal.SEA);
        if (SoulPacts.of(p) != null) grant(p, Goal.PACT);
        var set = ModItems.GEAR.get(GearTier.SOVEREIGN);
        if (p.getItemBySlot(EquipmentSlot.HEAD).is(set.helmet().get()) && p.getItemBySlot(EquipmentSlot.CHEST).is(set.chestplate().get())
                && p.getItemBySlot(EquipmentSlot.LEGS).is(set.leggings().get()) && p.getItemBySlot(EquipmentSlot.FEET).is(set.boots().get())) {
            grant(p, Goal.SOVEREIGN);
        }
        if (p.getMainHandItem().getItem() instanceof com.piratecrew.item.LegendaryWeaponItem) grant(p, Goal.LEGENDARY);
    }
}
