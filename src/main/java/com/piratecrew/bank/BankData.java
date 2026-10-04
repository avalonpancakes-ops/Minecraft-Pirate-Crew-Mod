package com.piratecrew.bank;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Every player's ruby bank balance, kept in the overworld's data folder. */
public class BankData extends SavedData {
    private static final String NAME = "piratecrew_bank";
    private final Map<UUID, Long> balances = new HashMap<>();

    public static BankData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BankData::load, BankData::new, NAME);
    }

    public long balance(UUID player) {
        return balances.getOrDefault(player, 0L);
    }

    public void add(UUID player, long amount) {
        if (amount == 0) return;
        balances.merge(player, amount, Long::sum);
        if (balances.get(player) <= 0) balances.remove(player);
        setDirty();
    }

    /** Takes up to {@code amount}; returns how much was actually taken. */
    public long take(UUID player, long amount) {
        long have = balance(player);
        long taken = Math.min(have, Math.max(0, amount));
        add(player, -taken);
        return taken;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Long> e : balances.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("U", e.getKey());
            t.putLong("B", e.getValue());
            list.add(t);
        }
        tag.put("Accounts", list);
        return tag;
    }

    public static BankData load(CompoundTag tag) {
        BankData d = new BankData();
        for (Tag t : tag.getList("Accounts", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.balances.put(c.getUUID("U"), c.getLong("B"));
        }
        return d;
    }
}
