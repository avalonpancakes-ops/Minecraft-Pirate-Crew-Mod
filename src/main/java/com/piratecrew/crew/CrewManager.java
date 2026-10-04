package com.piratecrew.crew;

import com.piratecrew.Config;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.network.CrewSyncPacket;
import com.piratecrew.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** All crew rules live here. Every method runs on the server thread. */
public class CrewManager {
    private static final long INVITE_TICKS = 20 * 60 * 5; // 5 minutes

    // ------------------------------------------------------------------ queries

    public static CrewData data(MinecraftServer server) {
        return CrewData.get(server);
    }

    public static Crew crewOf(ServerPlayer player) {
        return data(player.server).crewOf(player.getUUID());
    }

    /** Crew id of a player or a pirate, or null. Server side only. */
    public static UUID crewIdOf(Entity e) {
        if (e instanceof PirateEntity p) return p.getCrewId();
        if (e instanceof ServerPlayer sp) {
            Crew c = crewOf(sp);
            return c == null ? null : c.id;
        }
        return null;
    }

    public static boolean areCrewmates(Entity a, Entity b) {
        if (a == null || b == null || a.level().isClientSide) return false;
        UUID ca = crewIdOf(a);
        return ca != null && ca.equals(crewIdOf(b));
    }

    public static boolean isInSameCrew(ServerPlayer player, PirateEntity pirate) {
        Crew c = crewOf(player);
        return c != null && c.id.equals(pirate.getCrewId());
    }

    // ------------------------------------------------------------------ messages

    private static void ok(Player p, String msg) {
        p.sendSystemMessage(Component.literal("⚓ ").withStyle(ChatFormatting.GOLD).append(Component.literal(msg).withStyle(ChatFormatting.YELLOW)));
    }

    private static void err(Player p, String msg) {
        p.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
    }

    private static void tellCrew(MinecraftServer server, Crew crew, String msg) {
        for (UUID u : crew.players) {
            ServerPlayer sp = server.getPlayerList().getPlayer(u);
            if (sp != null) ok(sp, msg);
        }
    }

    private static String cleanName(String raw) {
        String s = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        s = s.replaceAll("[^A-Za-z0-9 '\\-_!?.&]", "");
        if (s.length() > 24) s = s.substring(0, 24);
        return s;
    }

    // ------------------------------------------------------------------ crew lifecycle

    public static Crew create(ServerPlayer player, String rawName) {
        CrewData d = data(player.server);
        if (d.crewOf(player.getUUID()) != null) {
            err(player, "You're already in a crew. Leave it first.");
            return null;
        }
        String name = cleanName(rawName);
        if (name.length() < 2) {
            err(player, "Crew names need at least 2 letters.");
            return null;
        }
        if (d.byName(name) != null) {
            err(player, "A crew called \"" + name + "\" already sails these seas.");
            return null;
        }
        Crew crew = new Crew(UUID.randomUUID(), name, player.getUUID());
        d.add(crew);
        d.rememberName(player.getUUID(), player.getGameProfile().getName());
        ok(player, "You are now Captain of " + name + "!");
        syncCrew(player.server, crew);
        return crew;
    }

    public static void disband(ServerPlayer player) {
        CrewData d = data(player.server);
        Crew crew = d.crewOf(player.getUUID());
        if (crew == null) { err(player, "You're not in a crew."); return; }
        if (!player.getUUID().equals(crew.captain)) { err(player, "Only the captain can disband the crew."); return; }
        disbandInternal(player.server, crew);
    }

    private static void disbandInternal(MinecraftServer server, Crew crew) {
        CrewData d = data(server);
        tellCrew(server, crew, crew.name + " has been disbanded.");
        List<UUID> players = new ArrayList<>(crew.players);
        for (UUID npc : new ArrayList<>(crew.npcs.keySet())) releaseNpcEntity(server, npc);
        d.remove(crew);
        for (UUID u : players) {
            ServerPlayer sp = server.getPlayerList().getPlayer(u);
            if (sp != null) sync(sp, false);
        }
    }

    public static void leave(ServerPlayer player) {
        CrewData d = data(player.server);
        Crew crew = d.crewOf(player.getUUID());
        if (crew == null) { err(player, "You're not in a crew."); return; }

        if (player.getUUID().equals(crew.captain)) {
            // Hand the ship to a vice captain, then any other player, otherwise disband.
            UUID next = !crew.viceCaptains.isEmpty() ? crew.viceCaptains.get(0) : null;
            if (next == null) {
                for (UUID u : crew.players) if (!u.equals(player.getUUID())) { next = u; break; }
            }
            if (next == null) {
                disbandInternal(player.server, crew);
                return;
            }
            crew.viceCaptains.remove(next);
            crew.captain = next;
            tellCrew(player.server, crew, d.nameOf(next) + " is the new Captain of " + crew.name + ".");
        }
        d.removePlayer(crew, player.getUUID());
        ok(player, "You left " + crew.name + ".");
        tellCrew(player.server, crew, player.getGameProfile().getName() + " left the crew.");
        sync(player, false);
        syncCrew(player.server, crew);
    }

    // ------------------------------------------------------------------ invites

    public static void inviteByName(ServerPlayer inviter, String targetName) {
        ServerPlayer target = inviter.server.getPlayerList().getPlayerByName(targetName.trim());
        if (target == null) { err(inviter, "No online player named \"" + targetName.trim() + "\"."); return; }
        invite(inviter, target);
    }

    public static void invite(ServerPlayer inviter, ServerPlayer target) {
        CrewData d = data(inviter.server);
        Crew crew = d.crewOf(inviter.getUUID());
        if (crew == null) { err(inviter, "You need a crew first."); return; }
        if (!crew.isOfficer(inviter.getUUID())) { err(inviter, "Only the captain and vice captains can invite."); return; }
        if (target == inviter) { err(inviter, "You can't invite yourself."); return; }
        if (d.crewOf(target.getUUID()) != null) { err(inviter, target.getGameProfile().getName() + " already belongs to a crew."); return; }
        if (crew.isFull()) { err(inviter, "Your crew is full (" + Config.MAX_CREW_SIZE.get() + " members)."); return; }
        if (crew.playerSlotsFull()) { err(inviter, "Your crew already has the maximum of " + Config.MAX_REAL_PLAYERS.get() + " real players. Recruit pirates instead!"); return; }

        long expires = inviter.server.overworld().getGameTime() + INVITE_TICKS;
        d.addInvite(target.getUUID(), new CrewData.Invite(crew.id, inviter.getGameProfile().getName(), expires));

        ok(inviter, "Invited " + target.getGameProfile().getName() + " to " + crew.name + ".");

        MutableComponent accept = Component.literal("[Accept]").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/crew accept " + crew.id))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Join " + crew.name))));
        MutableComponent decline = Component.literal("[Decline]").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/crew decline " + crew.id)));
        target.sendSystemMessage(Component.literal("⚓ " + inviter.getGameProfile().getName() + " invites you to join the crew ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(crew.name).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(". "))
                .append(accept).append(Component.literal(" ")).append(decline));
        sync(target, false);
    }

    public static void accept(ServerPlayer player, UUID crewId) {
        CrewData d = data(player.server);
        if (d.crewOf(player.getUUID()) != null) { err(player, "Leave your current crew first."); return; }
        CrewData.Invite inv = d.takeInvite(player.getUUID(), crewId, player.server.overworld().getGameTime());
        Crew crew = d.byId(crewId);
        if (inv == null || crew == null) { err(player, "That invite has expired."); sync(player, false); return; }
        if (crew.isFull()) { err(player, crew.name + " is full."); return; }
        if (crew.playerSlotsFull()) { err(player, crew.name + " already has " + Config.MAX_REAL_PLAYERS.get() + " real players."); return; }

        d.addPlayer(crew, player.getUUID());
        d.rememberName(player.getUUID(), player.getGameProfile().getName());
        tellCrew(player.server, crew, player.getGameProfile().getName() + " joined " + crew.name + "!");
        syncCrew(player.server, crew);
    }

    public static void decline(ServerPlayer player, UUID crewId) {
        CrewData d = data(player.server);
        d.takeInvite(player.getUUID(), crewId, player.server.overworld().getGameTime());
        ok(player, "Invite declined.");
        sync(player, false);
    }

    // ------------------------------------------------------------------ officers

    public static void kick(ServerPlayer actor, UUID target) {
        CrewData d = data(actor.server);
        Crew crew = d.crewOf(actor.getUUID());
        if (crew == null) return;
        CrewRole myRole = crew.roleOf(actor.getUUID());

        if (crew.npcs.containsKey(target)) {
            if (!crew.isOfficer(actor.getUUID())) { err(actor, "Only officers can dismiss pirates."); return; }
            Crew.NpcInfo info = crew.npcs.remove(target);
            d.setDirty();
            releaseNpcEntity(actor.server, target);
            tellCrew(actor.server, crew, info.name() + " was dismissed from the crew.");
            syncCrew(actor.server, crew);
            return;
        }

        CrewRole theirRole = crew.roleOf(target);
        if (theirRole == null || target.equals(actor.getUUID())) return;
        boolean allowed = myRole == CrewRole.CAPTAIN || (myRole == CrewRole.VICE_CAPTAIN && theirRole == CrewRole.MEMBER);
        if (!allowed) { err(actor, "You don't have the rank to kick them."); return; }

        d.removePlayer(crew, target);
        String name = d.nameOf(target);
        tellCrew(actor.server, crew, name + " was thrown overboard (kicked).");
        ServerPlayer kicked = actor.server.getPlayerList().getPlayer(target);
        if (kicked != null) {
            err(kicked, "You were kicked from " + crew.name + ".");
            sync(kicked, false);
        }
        syncCrew(actor.server, crew);
    }

    public static void promote(ServerPlayer actor, UUID target) {
        CrewData d = data(actor.server);
        Crew crew = d.crewOf(actor.getUUID());
        if (crew == null || !actor.getUUID().equals(crew.captain)) { err(actor, "Only the captain can appoint vice captains."); return; }
        if (crew.roleOf(target) != CrewRole.MEMBER) return;
        if (crew.viceCaptains.size() >= Crew.MAX_VICE_CAPTAINS) {
            err(actor, "You already have " + Crew.MAX_VICE_CAPTAINS + " vice captains. Demote one first.");
            return;
        }
        crew.viceCaptains.add(target);
        d.setDirty();
        tellCrew(actor.server, crew, d.nameOf(target) + " is now a Vice Captain.");
        syncCrew(actor.server, crew);
    }

    public static void demote(ServerPlayer actor, UUID target) {
        CrewData d = data(actor.server);
        Crew crew = d.crewOf(actor.getUUID());
        if (crew == null || !actor.getUUID().equals(crew.captain)) return;
        if (crew.viceCaptains.remove(target)) {
            d.setDirty();
            tellCrew(actor.server, crew, d.nameOf(target) + " is no longer a Vice Captain.");
            syncCrew(actor.server, crew);
        }
    }

    public static void transferCaptain(ServerPlayer actor, UUID target) {
        CrewData d = data(actor.server);
        Crew crew = d.crewOf(actor.getUUID());
        if (crew == null || !actor.getUUID().equals(crew.captain)) return;
        if (!crew.players.contains(target) || target.equals(actor.getUUID())) return;
        crew.viceCaptains.remove(target);
        crew.captain = target;
        // The old captain becomes a vice captain if there's room.
        if (crew.viceCaptains.size() < Crew.MAX_VICE_CAPTAINS) crew.viceCaptains.add(actor.getUUID());
        d.setDirty();
        tellCrew(actor.server, crew, d.nameOf(target) + " is the new Captain of " + crew.name + "!");
        syncCrew(actor.server, crew);
    }

    public static void rename(ServerPlayer actor, String rawName) {
        CrewData d = data(actor.server);
        Crew crew = d.crewOf(actor.getUUID());
        if (crew == null || !actor.getUUID().equals(crew.captain)) { err(actor, "Only the captain can rename the crew."); return; }
        String name = cleanName(rawName);
        if (name.length() < 2) { err(actor, "Crew names need at least 2 letters."); return; }
        Crew other = d.byName(name);
        if (other != null && other != crew) { err(actor, "That name is taken."); return; }
        crew.name = name;
        d.setDirty();
        tellCrew(actor.server, crew, "The crew is now called " + name + ".");
        syncCrew(actor.server, crew);
    }

    public static void setIconFromHand(ServerPlayer actor) {
        CrewData d = data(actor.server);
        Crew crew = d.crewOf(actor.getUUID());
        if (crew == null || !crew.isOfficer(actor.getUUID())) { err(actor, "Only officers can change the crew icon."); return; }
        ItemStack held = actor.getMainHandItem();
        if (held.isEmpty()) { err(actor, "Hold the item you want as your crew icon."); return; }
        ItemStack icon = held.copy();
        icon.setCount(1);
        crew.icon = icon;
        d.setDirty();
        ok(actor, "Crew icon set to " + held.getHoverName().getString() + ".");
        syncCrew(actor.server, crew);
    }

    // ------------------------------------------------------------------ pirates

    /** Called after the ruby payment succeeded checks. Returns the crew the pirate joined or null. */
    public static Crew crewForRecruiting(ServerPlayer player) {
        CrewData d = data(player.server);
        Crew crew = d.crewOf(player.getUUID());
        if (crew == null) {
            crew = create(player, player.getGameProfile().getName() + "'s Crew");
            if (crew == null) crew = create(player, player.getGameProfile().getName() + "'s Crew " + (player.getRandom().nextInt(900) + 100));
            return crew;
        }
        return crew;
    }

    public static void addPirate(MinecraftServer server, Crew crew, PirateEntity pirate) {
        crew.npcs.put(pirate.getUUID(), new Crew.NpcInfo(pirate.getPirateName(), pirate.getTier().ordinal()));
        data(server).setDirty();
        syncCrew(server, crew);
    }

    /** The pirate died or otherwise left for good. */
    public static void onPirateGone(MinecraftServer server, UUID crewId, UUID pirateId, String deathNote) {
        CrewData d = data(server);
        Crew crew = d.byId(crewId);
        if (crew == null) return;
        Crew.NpcInfo info = crew.npcs.remove(pirateId);
        if (info != null) {
            d.setDirty();
            if (deathNote != null) tellCrew(server, crew, deathNote);
            syncCrew(server, crew);
        }
    }

    public static void dismissPirate(ServerPlayer actor, PirateEntity pirate) {
        kick(actor, pirate.getUUID());
    }

    public static boolean crewStillHasPirate(MinecraftServer server, UUID crewId, UUID pirateId) {
        Crew crew = data(server).byId(crewId);
        return crew != null && crew.npcs.containsKey(pirateId);
    }

    private static void releaseNpcEntity(MinecraftServer server, UUID npc) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(npc);
            if (e instanceof PirateEntity p) {
                p.leaveCrew();
                return;
            }
        }
        // Not loaded: the pirate notices it has no crew the next time it loads.
    }

    // ------------------------------------------------------------------ sync

    public static void syncCrew(MinecraftServer server, Crew crew) {
        for (UUID u : crew.players) {
            ServerPlayer sp = server.getPlayerList().getPlayer(u);
            if (sp != null) sync(sp, false);
        }
    }

    public static void sync(ServerPlayer player, boolean open) {
        MinecraftServer server = player.server;
        CrewData d = data(server);
        d.rememberName(player.getUUID(), player.getGameProfile().getName());
        Crew crew = d.crewOf(player.getUUID());

        List<CrewSyncPacket.InviteInfo> invites = new ArrayList<>();
        for (CrewData.Invite inv : d.invitesFor(player.getUUID(), server.overworld().getGameTime())) {
            Crew c = d.byId(inv.crewId());
            if (c != null) invites.add(new CrewSyncPacket.InviteInfo(c.id, c.name, inv.inviterName()));
        }

        if (crew == null) {
            ModNetwork.sendTo(player, new CrewSyncPacket(open, false, new UUID(0, 0), "", ItemStack.EMPTY, 2,
                    Config.MAX_CREW_SIZE.get(), Config.MAX_REAL_PLAYERS.get(), List.of(), invites));
            return;
        }

        List<CrewSyncPacket.Member> members = new ArrayList<>();
        // Captain first, then vices, then deckhands, then pirates (highest tier first).
        List<UUID> order = new ArrayList<>();
        if (crew.captain != null) order.add(crew.captain);
        order.addAll(crew.viceCaptains);
        for (UUID u : crew.players) if (!order.contains(u)) order.add(u);
        for (UUID u : order) {
            CrewRole r = crew.roleOf(u);
            if (r == null) continue;
            boolean online = server.getPlayerList().getPlayer(u) != null;
            members.add(new CrewSyncPacket.Member(u, d.nameOf(u), false, r.ordinal(), 0, online));
        }
        crew.npcs.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().tier(), a.getValue().tier()))
                .forEach(e -> members.add(new CrewSyncPacket.Member(e.getKey(), e.getValue().name(), true,
                        CrewRole.MEMBER.ordinal(), e.getValue().tier(), true)));

        CrewRole myRole = crew.roleOf(player.getUUID());
        ModNetwork.sendTo(player, new CrewSyncPacket(open, true, crew.id, crew.name, crew.icon.copy(),
                myRole == null ? 2 : myRole.ordinal(), Config.MAX_CREW_SIZE.get(), Config.MAX_REAL_PLAYERS.get(), members, invites));
    }
}
