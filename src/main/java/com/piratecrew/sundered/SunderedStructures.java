package com.piratecrew.sundered;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Places Order of the Tide outposts on flat island ground as the Sundered Sea is explored. */
public class SunderedStructures {
    public static final TagKey<Biome> ISLES = TagKey.create(Registries.BIOME, new ResourceLocation("piratecrew", "sundered_isles"));
    private static final int OUTPOST_CHANCE = 30;   // 1 in N new island chunks
    private static final int OUTPOST_SPACING = 260;

    private record Pending(BlockPos centre, int attempts) {}

    private static final Queue<Pending> QUEUE = new ConcurrentLinkedQueue<>();
    private static int ticks;

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk, boolean newChunk) {
        if (!newChunk || level.dimension() != SunderedSea.LEVEL) return;
        BlockPos mid = chunk.getPos().getMiddleBlockPosition(64);
        int h = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, mid.getX() & 15, mid.getZ() & 15);
        if (h <= level.getSeaLevel() + 2) return; // open water
        if (level.getRandom().nextInt(OUTPOST_CHANCE) != 0) return;
        QUEUE.add(new Pending(mid, 0));
    }

    public static void tick(MinecraftServer server) {
        if (server == null || ++ticks % 20 != 0 || QUEUE.isEmpty()) return;
        ServerLevel level = server.getLevel(SunderedSea.LEVEL);
        if (level == null) {
            QUEUE.clear();
            return;
        }
        int n = QUEUE.size();
        int margin = OutpostBuilder.RADIUS + 4;
        for (int i = 0; i < n; i++) {
            Pending p = QUEUE.poll();
            if (p == null) break;
            int x = p.centre().getX(), z = p.centre().getZ();
            if (!level.hasChunksAt(x - margin, z - margin, x + margin, z + margin)) {
                if (p.attempts() < 30) QUEUE.add(new Pending(p.centre(), p.attempts() + 1));
                continue;
            }
            BlockPos floor = flatIslandGround(level, x, z, OutpostBuilder.RADIUS);
            Data data = Data.get(level);
            if (floor != null && !data.near(floor, OUTPOST_SPACING)) {
                OutpostBuilder.build(level, floor);
                data.add(floor);
                return;
            }
        }
    }

    /** Ground block at (x, z) if the area around it is flat, dry island land; else null. */
    public static BlockPos flatIslandGround(ServerLevel level, int x, int z, int r) {
        Holder<Biome> biome = level.getBiome(new BlockPos(x, level.getSeaLevel() + 4, z));
        if (!biome.is(ISLES)) return null;
        List<Integer> heights = new ArrayList<>();
        int wet = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dz = -r; dz <= r; dz += 2) {
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                if (!level.getFluidState(new BlockPos(x + dx, h - 1, z + dz)).isEmpty()) wet++;
                heights.add(h);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        if (wet > 1) return null;
        Collections.sort(heights);
        int median = heights.get(heights.size() / 2);
        if (median <= level.getSeaLevel() + 1 || max - min > 8) return null;
        return new BlockPos(x, median - 1, z);
    }

    /** Where outposts have been built. */
    public static class Data extends SavedData {
        private final List<BlockPos> outposts = new ArrayList<>();

        public static Data get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(Data::load, Data::new, "piratecrew_outposts");
        }

        public void add(BlockPos p) {
            outposts.add(p.immutable());
            setDirty();
        }

        public boolean near(BlockPos p, double dist) {
            for (BlockPos o : outposts) {
                double dx = o.getX() - p.getX(), dz = o.getZ() - p.getZ();
                if (dx * dx + dz * dz < dist * dist) return true;
            }
            return false;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag l = new ListTag();
            for (BlockPos p : outposts) l.add(LongTag.valueOf(p.asLong()));
            tag.put("Outposts", l);
            return tag;
        }

        public static Data load(CompoundTag tag) {
            Data d = new Data();
            for (Tag t : tag.getList("Outposts", Tag.TAG_LONG)) d.outposts.add(BlockPos.of(((LongTag) t).getAsLong()));
            return d;
        }
    }
}
