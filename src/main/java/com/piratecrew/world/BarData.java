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

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Villages", processedVillages.stream().mapToLong(Long::longValue).toArray());
        ListTag list = new ListTag();
        for (Bar b : bars) {
            CompoundTag t = new CompoundTag();
            t.put("Origin", NbtUtils.writeBlockPos(b.origin()));
            t.putInt("Facing", b.facing().get2DDataValue());
            list.add(t);
        }
        tag.put("Bars", list);
        return tag;
    }

    public static BarData load(CompoundTag tag) {
        BarData d = new BarData();
        for (long l : tag.getLongArray("Villages")) d.processedVillages.add(l);
        for (Tag t : tag.getList("Bars", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            d.bars.add(new Bar(NbtUtils.readBlockPos(c.getCompound("Origin")), Direction.from2DDataValue(c.getInt("Facing"))));
        }
        return d;
    }
}
