package com.piratecrew.bank;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Every player's open loan from the bank, kept in the overworld's data folder. */
public class LoanData extends SavedData {
    private static final String NAME = "piratecrew_loans";
    private final Map<UUID, Loan> loans = new HashMap<>();

    public static class Loan {
        public final UUID player;
        /** Rubies borrowed. */
        public long principal;
        /** Rubies still owed (principal + interest, minus repayments). */
        public long owed;
        /** Overworld day-time tick the loan must be repaid by. */
        public long deadline;
        public boolean defaulted;
        /** Index into the hunter escalation (0 = F, 1 = D ... 5 = S, 6 = S+F ...). */
        public int wave;
        /** Increases every time a wave is sent; hunters from an older wave go home. */
        public int serial;
        /** Day-time tick the next wave of hunters is due. */
        public long nextWaveAt;
        /** The current wave already got the player (so tomorrow's wave doesn't escalate). */
        public boolean waveDone;
        /** Days-left count the player was last reminded at. */
        public int lastReminder = Integer.MAX_VALUE;

        Loan(UUID player) {
            this.player = player;
        }
    }

    public static LoanData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(LoanData::load, LoanData::new, NAME);
    }

    @Nullable
    public Loan get(UUID player) {
        return loans.get(player);
    }

    public Loan create(UUID player) {
        Loan l = new Loan(player);
        loans.put(player, l);
        setDirty();
        return l;
    }

    public void remove(UUID player) {
        if (loans.remove(player) != null) setDirty();
    }

    public Collection<Loan> all() {
        return loans.values();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Loan l : loans.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("U", l.player);
            t.putLong("Principal", l.principal);
            t.putLong("Owed", l.owed);
            t.putLong("Deadline", l.deadline);
            t.putBoolean("Defaulted", l.defaulted);
            t.putInt("Wave", l.wave);
            t.putInt("Serial", l.serial);
            t.putLong("NextWave", l.nextWaveAt);
            t.putBoolean("WaveDone", l.waveDone);
            t.putInt("Reminder", l.lastReminder);
            list.add(t);
        }
        tag.put("Loans", list);
        return tag;
    }

    public static LoanData load(CompoundTag tag) {
        LoanData d = new LoanData();
        for (Tag t : tag.getList("Loans", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Loan l = new Loan(c.getUUID("U"));
            l.principal = c.getLong("Principal");
            l.owed = c.getLong("Owed");
            l.deadline = c.getLong("Deadline");
            l.defaulted = c.getBoolean("Defaulted");
            l.wave = c.getInt("Wave");
            l.serial = c.getInt("Serial");
            l.nextWaveAt = c.getLong("NextWave");
            l.waveDone = c.getBoolean("WaveDone");
            l.lastReminder = c.contains("Reminder") ? c.getInt("Reminder") : Integer.MAX_VALUE;
            d.loans.put(l.player, l);
        }
        return d;
    }
}
