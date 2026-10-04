package com.piratecrew.crew;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** World-wide crew storage, kept in the overworld's data folder. */
public class CrewData extends SavedData {
    private static final String NAME = "piratecrew_crews";

    public record Invite(UUID crewId, String inviterName, long expiresAt) {}

    private final Map<UUID, Crew> crews = new LinkedHashMap<>();
    private final Map<UUID, UUID> playerCrew = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    /** Not saved: invites expire on restart. player -> (crew -> invite) */
    private final Map<UUID, Map<UUID, Invite>> invites = new HashMap<>();

    public static CrewData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CrewData::load, CrewData::new, NAME);
    }

    public Collection<Crew> all() {
        return crews.values();
    }

    public Crew byId(UUID id) {
        return id == null ? null : crews.get(id);
    }

    public Crew crewOf(UUID player) {
        UUID id = playerCrew.get(player);
        return id == null ? null : crews.get(id);
    }

    public Crew byName(String name) {
        for (Crew c : crews.values()) if (c.name.equalsIgnoreCase(name)) return c;
        return null;
    }

    public void add(Crew crew) {
        crews.put(crew.id, crew);
        for (UUID p : crew.players) playerCrew.put(p, crew.id);
        setDirty();
    }

    public void remove(Crew crew) {
        crews.remove(crew.id);
        for (UUID p : crew.players) playerCrew.remove(p);
        for (Map<UUID, Invite> m : invites.values()) m.remove(crew.id);
        setDirty();
    }

    public void addPlayer(Crew crew, UUID player) {
        crew.players.add(player);
        playerCrew.put(player, crew.id);
        invites.remove(player);
        setDirty();
    }

    public void removePlayer(Crew crew, UUID player) {
        crew.players.remove(player);
        crew.viceCaptains.remove(player);
        playerCrew.remove(player);
        setDirty();
    }

    // ---- names ----
    public void rememberName(UUID player, String name) {
        if (!name.equals(names.put(player, name))) setDirty();
    }

    public String nameOf(UUID player) {
        return names.getOrDefault(player, "Unknown sailor");
    }

    // ---- invites ----
    public void addInvite(UUID player, Invite invite) {
        invites.computeIfAbsent(player, k -> new LinkedHashMap<>()).put(invite.crewId(), invite);
    }

    public Invite takeInvite(UUID player, UUID crewId, long now) {
        Map<UUID, Invite> m = invites.get(player);
        if (m == null) return null;
        Invite inv = m.remove(crewId);
        if (inv == null || inv.expiresAt() < now) return null;
        return inv;
    }

    public List<Invite> invitesFor(UUID player, long now) {
        Map<UUID, Invite> m = invites.get(player);
        if (m == null) return List.of();
        m.values().removeIf(i -> i.expiresAt() < now || !crews.containsKey(i.crewId()));
        return new ArrayList<>(m.values());
    }

    // ---- persistence ----
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Crew c : crews.values()) list.add(c.save());
        tag.put("Crews", list);

        ListTag nameList = new ListTag();
        for (Map.Entry<UUID, String> e : names.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("U", e.getKey());
            t.putString("N", e.getValue());
            nameList.add(t);
        }
        tag.put("Names", nameList);
        return tag;
    }

    public static CrewData load(CompoundTag tag) {
        CrewData data = new CrewData();
        for (Tag t : tag.getList("Crews", Tag.TAG_COMPOUND)) {
            Crew c = Crew.load((CompoundTag) t);
            data.crews.put(c.id, c);
            for (UUID p : c.players) data.playerCrew.put(p, c.id);
        }
        for (Tag t : tag.getList("Names", Tag.TAG_COMPOUND)) {
            CompoundTag n = (CompoundTag) t;
            data.names.put(n.getUUID("U"), n.getString("N"));
        }
        return data;
    }
}
