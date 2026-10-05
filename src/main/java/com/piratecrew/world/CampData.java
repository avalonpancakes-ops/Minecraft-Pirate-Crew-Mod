package com.piratecrew.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Where raider camps have been built in a dimension, so they keep their distance from each other. */
public class CampData extends SavedData {
    private static final String NAME = "piratecrew_camps";
    private final List<BlockPos> camps = new ArrayList<>();

    public static CampData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CampData::load, CampData::new, NAME);
    }

    public void add(BlockPos pos) {
        camps.add(pos.immutable());
        setDirty();
    }

    public boolean near(BlockPos pos, double dist) {
        for (BlockPos c : camps) {
            double dx = c.getX() - pos.getX(), dz = c.getZ() - pos.getZ();
            if (dx * dx + dz * dz < dist * dist) return true;
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos p : camps) list.add(LongTag.valueOf(p.asLong()));
        tag.put("Camps", list);
        return tag;
    }

    public static CampData load(CompoundTag tag) {
        CampData d = new CampData();
        for (Tag t : tag.getList("Camps", Tag.TAG_LONG)) d.camps.add(BlockPos.of(((LongTag) t).getAsLong()));
        return d;
    }
}
