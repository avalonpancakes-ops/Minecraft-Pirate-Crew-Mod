package com.piratecrew.world;

import com.piratecrew.Config;
import com.piratecrew.entity.PirateEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Gives every village exactly one bar.
 *
 * When a chunk holding a village's start loads, the village is queued. Each second we try to place
 * a bar on a flat patch just outside the village's bounding box, facing the village. We only build
 * once the chunks around the chosen spot are loaded, so we never force world generation.
 */
public class VillageBarHandler {
    private enum Result { PLACED, WAIT, FAILED }

    private record Pending(ResourceKey<Level> dim, long key, BoundingBox box, int attempts) {}

    private static final Queue<Pending> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Set<String> QUEUED = ConcurrentHashMap.newKeySet();
    private static int ticks = 0;

    private static final int OFFSET = 10;   // distance from village edge to bar centre
    private static final int MARGIN = 12;   // area that must be loaded around the bar

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (!Config.GENERATE_BARS.get()) return;
        Map<Structure, StructureStart> starts = chunk.getAllStarts();
        if (starts.isEmpty()) return;
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Map.Entry<Structure, StructureStart> e : starts.entrySet()) {
            StructureStart start = e.getValue();
            if (start == null || !start.isValid()) continue;
            if (!registry.wrapAsHolder(e.getKey()).is(StructureTags.VILLAGE)) continue;
            long key = chunk.getPos().toLong();
            if (QUEUED.add(id(level.dimension(), key))) {
                QUEUE.add(new Pending(level.dimension(), key, start.getBoundingBox(), 0));
            }
        }
    }

    private static String id(ResourceKey<Level> dim, long key) {
        return dim.location() + "@" + key;
    }

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        ticks++;
        if (ticks % 20 == 0) processQueue(server);
        if (ticks % Config.BAR_RESTOCK_TICKS.get() == 0) restock(server);
    }

    private static void processQueue(MinecraftServer server) {
        int n = QUEUE.size();
        boolean builtOne = false;
        for (int i = 0; i < n; i++) {
            Pending p = QUEUE.poll();
            if (p == null) break;
            ServerLevel level = server.getLevel(p.dim());
            if (level == null || !Config.GENERATE_BARS.get()) {
                QUEUED.remove(id(p.dim(), p.key()));
                continue;
            }
            BarData data = BarData.get(level);
            if (data.isProcessed(p.key())) {
                QUEUED.remove(id(p.dim(), p.key()));
                continue;
            }
            Result r = builtOne ? Result.WAIT : tryPlace(level, p.box(), p.attempts());
            if (r == Result.WAIT) {
                QUEUE.add(new Pending(p.dim(), p.key(), p.box(), p.attempts() + 1));
            } else {
                builtOne |= r == Result.PLACED;
                data.markProcessed(p.key());
                QUEUED.remove(id(p.dim(), p.key()));
            }
        }
    }

    private record Candidate(BlockPos origin, Direction facing, int score, boolean water) {}

    private static Result tryPlace(ServerLevel level, BoundingBox box, int attempts) {
        BlockPos c = box.getCenter();
        List<Candidate> loaded = new ArrayList<>();
        boolean anyUnloaded = false;

        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int off : new int[]{0, -14, 14}) {
                int x, z;
                switch (side) {
                    case NORTH -> { x = c.getX() + off; z = box.minZ() - OFFSET; }
                    case SOUTH -> { x = c.getX() + off; z = box.maxZ() + OFFSET; }
                    case WEST -> { x = box.minX() - OFFSET; z = c.getZ() + off; }
                    default -> { x = box.maxX() + OFFSET; z = c.getZ() + off; }
                }
                if (!level.hasChunksAt(x - MARGIN, z - MARGIN, x + MARGIN, z + MARGIN)) {
                    anyUnloaded = true;
                    continue;
                }
                Candidate cand = evaluate(level, new BlockPos(x, 0, z), side.getOpposite());
                if (cand != null) loaded.add(cand);
            }
        }

        Candidate best = loaded.stream()
                .filter(k -> !k.water())
                .min(Comparator.comparingInt(Candidate::score))
                .orElse(null);

        boolean good = best != null && best.score() <= 6;
        // Wait for a better spot to load, but don't wait forever (~10 minutes of retries).
        if (!good && anyUnloaded && attempts < 600) return Result.WAIT;
        if (best == null || best.score() > 20) return Result.FAILED;

        BarBuilder.buildAt(level, best.origin(), best.facing(), false);
        return Result.PLACED;
    }

    /** Lower score = flatter, drier ground. */
    private static Candidate evaluate(ServerLevel level, BlockPos centre, Direction facing) {
        var rot = BarBuilder.rotationFor(facing);
        List<Integer> heights = new ArrayList<>();
        int water = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int lx = -1; lx <= BarBuilder.W; lx++) {
            for (int lz = -1; lz <= BarBuilder.D; lz++) {
                BlockPos p = BarBuilder.toWorld(centre, rot, lx, 0, lz);
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ());
                if (!level.getBlockState(new BlockPos(p.getX(), h - 1, p.getZ())).getFluidState().isEmpty()) water++;
                heights.add(h);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        if (heights.isEmpty()) return null;
        Collections.sort(heights);
        int median = heights.get(heights.size() / 2);
        int bumpy = 0;
        for (int h : heights) if (Math.abs(h - median) > 2) bumpy++;
        int score = (max - min) + bumpy / 8;
        boolean wet = water > heights.size() / 10;
        // Floor sits where the ground surface is (replacing the top block).
        return new Candidate(new BlockPos(centre.getX(), median - 1, centre.getZ()), facing, score, wet);
    }

    private static void restock(MinecraftServer server) {
        int min = Config.BAR_MIN_PIRATES.get();
        if (min <= 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            BarData data = BarData.get(level);
            for (BarData.Bar bar : data.bars()) {
                BlockPos c = BarBuilder.interiorCentre(bar);
                if (!level.hasChunksAt(c.getX() - 10, c.getZ() - 10, c.getX() + 10, c.getZ() + 10)) continue;
                if (!level.hasNearbyAlivePlayer(c.getX(), c.getY(), c.getZ(), 96)) continue;
                int free = level.getEntitiesOfClass(PirateEntity.class, new AABB(c).inflate(10, 5, 10), p -> !p.isRecruited()).size();
                if (free < min) BarBuilder.restock(level, bar);
            }
        }
    }
}
