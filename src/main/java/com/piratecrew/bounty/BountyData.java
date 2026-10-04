package com.piratecrew.bounty;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** Every wanted player and pirate, kept in the overworld's data folder. */
public class BountyData extends SavedData {
    private static final String NAME = "piratecrew_bounties";

    public static class Entry {
        public final UUID id;
        public boolean npc;
        public String name = "";
        /** Pirates: bundled skin name. */
        public String skin = "";
        public int tier;
        /** Players: Mojang "textures" property so their face can be drawn even when offline. */
        public String textures = "";
        public String texturesSig = "";
        /** Crew the pirate belonged to (players are looked up live). */
        @Nullable public UUID crewId;
        public int amount;
        public int playerKills;
        public int pirateKills;

        public Entry(UUID id) {
            this.id = id;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            t.putBoolean("Npc", npc);
            t.putString("Name", name);
            t.putString("Skin", skin);
            t.putInt("Tier", tier);
            t.putString("Tex", textures);
            t.putString("TexSig", texturesSig);
            if (crewId != null) t.putUUID("Crew", crewId);
            t.putInt("Amount", amount);
            t.putInt("PlayerKills", playerKills);
            t.putInt("PirateKills", pirateKills);
            return t;
        }

        static Entry load(CompoundTag t) {
            Entry e = new Entry(t.getUUID("Id"));
            e.npc = t.getBoolean("Npc");
            e.name = t.getString("Name");
            e.skin = t.getString("Skin");
            e.tier = t.getInt("Tier");
            e.textures = t.getString("Tex");
            e.texturesSig = t.getString("TexSig");
            e.crewId = t.hasUUID("Crew") ? t.getUUID("Crew") : null;
            e.amount = t.getInt("Amount");
            e.playerKills = t.getInt("PlayerKills");
            e.pirateKills = t.getInt("PirateKills");
            return e;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public static BountyData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BountyData::load, BountyData::new, NAME);
    }

    @Nullable
    public Entry get(UUID id) {
        return entries.get(id);
    }

    public Entry getOrCreate(UUID id) {
        return entries.computeIfAbsent(id, Entry::new);
    }

    public int amountOf(UUID id) {
        Entry e = entries.get(id);
        return e == null ? 0 : e.amount;
    }

    public void remove(UUID id) {
        if (entries.remove(id) != null) setDirty();
    }

    /** Everyone with a bounty, biggest first. */
    public List<Entry> wanted() {
        List<Entry> list = new ArrayList<>();
        for (Entry e : entries.values()) if (e.amount > 0) list.add(e);
        list.sort((a, b) -> Integer.compare(b.amount, a.amount));
        return list;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) list.add(e.save());
        tag.put("Entries", list);
        return tag;
    }

    public static BountyData load(CompoundTag tag) {
        BountyData d = new BountyData();
        for (Tag t : tag.getList("Entries", Tag.TAG_COMPOUND)) {
            Entry e = Entry.load((CompoundTag) t);
            d.entries.put(e.id, e);
        }
        return d;
    }
}
