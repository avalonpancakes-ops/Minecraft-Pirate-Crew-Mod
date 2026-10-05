package com.piratecrew.world;

import com.piratecrew.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Raider camps appear now and then in freshly generated Overworld land: one in every so many new
 * chunks, on flat dry ground, away from villages, bars, banks and other camps.
 */
public class RaiderCampHandler {
    private record Pending(BlockPos centre, int attempts) {}

    private static final Queue<Pending> QUEUE = new ConcurrentLinkedQueue<>();
    private static final int MARGIN = CampBuilder.RADIUS + 4;
    private static int ticks;

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk, boolean newChunk) {
        if (!newChunk || level.dimension() != Level.OVERWORLD || !Config.GENERATE_CAMPS.get()) return;
        if (level.getRandom().nextInt(Math.max(1, Config.CAMP_CHANCE.get())) != 0) return;
        QUEUE.add(new Pending(chunk.getPos().getMiddleBlockPosition(64), 0));
    }

    public static void tick(MinecraftServer server) {
        if (server == null || ++ticks % 20 != 0 || QUEUE.isEmpty()) return;
        ServerLevel level = server.overworld();
        int n = QUEUE.size();
        for (int i = 0; i < n; i++) {
            Pending p = QUEUE.poll();
            if (p == null) break;
            int x = p.centre().getX(), z = p.centre().getZ();
            if (!level.hasChunksAt(x - MARGIN, z - MARGIN, x + MARGIN, z + MARGIN)) {
                if (p.attempts() < 30) QUEUE.add(new Pending(p.centre(), p.attempts() + 1));
                continue;
            }
            BlockPos floor = suitableFloor(level, x, z);
            if (floor != null) {
                CampBuilder.build(level, floor);
                CampData.get(level).add(floor);
                return; // one camp per pass
            }
        }
    }

    /** Ground block at the centre if this is a good camp spot, else null. */
    private static BlockPos suitableFloor(ServerLevel level, int x, int z) {
        BlockPos probe = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
        Holder<Biome> biome = level.getBiome(probe);
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_DEEP_OCEAN)) return null;
        if (CampData.get(level).near(probe, Config.CAMP_SPACING.get())) return null;
        if (BarData.get(level).tooCloseToBuildings(probe, 64)) return null;

        int r = CampBuilder.RADIUS;
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
        if (wet > 2) return null;
        Collections.sort(heights);
        int median = heights.get(heights.size() / 2);
        int bumpy = 0;
        for (int h : heights) if (Math.abs(h - median) > 2) bumpy++;
        if (bumpy > heights.size() / 6 || max - min > 7) return null;
        if (median < level.getSeaLevel()) return null;
        // Keep clear of villages.
        BlockPos floor = new BlockPos(x, median - 1, z);
        BlockPos village = level.findNearestMapStructure(StructureTags.VILLAGE, floor, 3, false);
        if (village != null && village.distSqr(new BlockPos(x, village.getY(), z)) < 96 * 96) return null;
        return floor;
    }
}
