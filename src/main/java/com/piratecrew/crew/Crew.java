package com.piratecrew.crew;

import com.piratecrew.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class Crew {
    public static final int MAX_VICE_CAPTAINS = 2;

    public record NpcInfo(String name, int tier) {}

    public final UUID id;
    public String name;
    public UUID captain;
    public final List<UUID> viceCaptains = new ArrayList<>();
    /** Real players, including the captain. */
    public final LinkedHashSet<UUID> players = new LinkedHashSet<>();
    /** Recruited pirate NPCs by entity UUID. */
    public final LinkedHashMap<UUID, NpcInfo> npcs = new LinkedHashMap<>();
    public ItemStack icon = new ItemStack(Items.COMPASS);

    public Crew(UUID id, String name, UUID captain) {
        this.id = id;
        this.name = name;
        this.captain = captain;
        if (captain != null) players.add(captain);
    }

    public int size() {
        return players.size() + npcs.size();
    }

    public boolean isFull() {
        return size() >= Config.MAX_CREW_SIZE.get();
    }

    public boolean playerSlotsFull() {
        return players.size() >= Config.MAX_REAL_PLAYERS.get();
    }

    public CrewRole roleOf(UUID player) {
        if (player.equals(captain)) return CrewRole.CAPTAIN;
        if (viceCaptains.contains(player)) return CrewRole.VICE_CAPTAIN;
        if (players.contains(player)) return CrewRole.MEMBER;
        return null;
    }

    /** Captain and vice captains can recruit, invite and kick ordinary members. */
    public boolean isOfficer(UUID player) {
        CrewRole r = roleOf(player);
        return r == CrewRole.CAPTAIN || r == CrewRole.VICE_CAPTAIN;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        if (captain != null) tag.putUUID("Captain", captain);

        ListTag vices = new ListTag();
        for (UUID u : viceCaptains) {
            CompoundTag t = new CompoundTag();
            t.putUUID("U", u);
            vices.add(t);
        }
        tag.put("Vices", vices);

        ListTag ps = new ListTag();
        for (UUID u : players) {
            CompoundTag t = new CompoundTag();
            t.putUUID("U", u);
            ps.add(t);
        }
        tag.put("Players", ps);

        ListTag ns = new ListTag();
        for (Map.Entry<UUID, NpcInfo> e : npcs.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("U", e.getKey());
            t.putString("Name", e.getValue().name());
            t.putInt("Tier", e.getValue().tier());
            ns.add(t);
        }
        tag.put("Npcs", ns);

        tag.put("Icon", icon.save(new CompoundTag()));
        return tag;
    }

    public static Crew load(CompoundTag tag) {
        Crew c = new Crew(tag.getUUID("Id"), tag.getString("Name"), tag.hasUUID("Captain") ? tag.getUUID("Captain") : null);
        for (Tag t : tag.getList("Vices", Tag.TAG_COMPOUND)) c.viceCaptains.add(((CompoundTag) t).getUUID("U"));
        for (Tag t : tag.getList("Players", Tag.TAG_COMPOUND)) c.players.add(((CompoundTag) t).getUUID("U"));
        for (Tag t : tag.getList("Npcs", Tag.TAG_COMPOUND)) {
            CompoundTag n = (CompoundTag) t;
            c.npcs.put(n.getUUID("U"), new NpcInfo(n.getString("Name"), n.getInt("Tier")));
        }
        ItemStack icon = ItemStack.of(tag.getCompound("Icon"));
        if (!icon.isEmpty()) c.icon = icon;
        return c;
    }
}
