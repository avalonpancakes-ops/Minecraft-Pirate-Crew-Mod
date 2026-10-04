package com.piratecrew.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Per-dimension record of which villages already have a bar, and where the bars are. */
public class BarData extends SavedData {
    private static final String NAME = "piratecrew_bars";

    /** origin = world position of the bar's floor centre; facing = direction the front door faces. */
    public record Bar(BlockPos origin, Direction facing) {}

    private final Set<Long> processedVillages = new HashSet<>();
    private final List<Bar> bars = new ArrayList<>();
    /** Bars (by origin) that already got their bounty board. */
    private final Set<Long> boarded = new HashSet<>();
    private final Set<Long> bankVillages = new HashSet<>();
    private final List<Bar> banks = new ArrayList<>();

    public static BarData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BarData::load, BarData::new, NAME);
    }

    public boolean isProcessed(long villageKey) {
        return processedVillages.contains(villageKey);
    }

    public void markProcessed(long villageKey) {
        if (processedVillages.add(villageKey)) setDirty();
    }

    public void addBar(Bar bar) {
        bars.add(bar);
        setDirty();
    }

    public List<Bar> bars() {
        return bars;
    }

    public boolean isBankProcessed(long villageKey) {
        return bankVillages.contains(villageKey);
    }

    public void markBankProcessed(long villageKey) {
        if (bankVillages.add(villageKey)) setDirty();
    }

    public void addBank(Bar bank) {
        banks.add(bank);
        setDirty();
    }

    public List<Bar> banks() {
        return banks;
    }

    /** True if a building centred here would come within {@code dist} blocks of a bar or bank. */
    public boolean tooCloseToBuildings(BlockPos centre, double dist) {
        for (List<Bar> list : List.of(bars, banks)) {
            for (Bar b : list) {
                double dx = b.origin().getX() - centre.getX(), dz = b.origin().getZ() - centre.getZ();
                if (dx * dx + dz * dz < dist * dist) return true;
            }
        }
        return false;
    }

    public boolean hasBoard(Bar bar) {
        return boarded.contains(bar.origin().asLong());
    }

    public void markBoard(Bar bar) {
        if (boarded.add(bar.origin().asLong())) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Villages", processedVillages.stream().mapToLong(Long::longValue).toArray());
        tag.putLongArray("Boarded", boarded.stream().mapToLong(Long::longValue).toArray());
        ListTag list = new ListTag();
        for (Bar b : bars) {
            CompoundTag t = new CompoundTag();
            t.put("Origin", NbtUtils.writeBlockPos(b.origin()));
            t.putInt("Facing", b.facing().get2DDataValue());
            list.add(t);
        }
        tag.put("Bars", list);
        tag.putLongArray("BankVillages", bankVillages.stream().mapToLong(Long::longValue).toArray());
        ListTag bankList = new ListTag();
        for (Bar b : banks) {
            CompoundTag t = new CompoundTag();
            t.put("Origin", NbtUtils.writeBlockPos(b.origin()));
            t.putInt("Facing", b.facing().get2DDataValue());
            bankList.add(t);
        }
        tag.put("Banks", bankList);
        return tag;
    }

    public static BarData load(CompoundTag tag) {
        BarData d = new BarData();
        for (long l : tag.getLongArray("Villages")) d.processedVillages.add(l);
        for (long l : tag.getLongArray("Boarded")) d.boarded.add(l);
        for (Tag t : tag.getList("Bars", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.bars.add(new Bar(NbtUtils.readBlockPos(c.getCompound("Origin")), Direction.from2DDataValue(c.getInt("Facing"))));
        }
        for (long l : tag.getLongArray("BankVillages")) d.bankVillages.add(l);
        for (Tag t : tag.getList("Banks", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.banks.add(new Bar(NbtUtils.readBlockPos(c.getCompound("Origin")), Direction.from2DDataValue(c.getInt("Facing"))));
        }
        return d;
    }
}
