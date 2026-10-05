package com.piratecrew.entity;

import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers who has been placing blocks recently (players and building NPCs), so fighters can
 * notice an enemy walling up or towering and rush them before the wall is finished.
 */
public class BuildTracker {
    private static final Map<UUID, Deque<Long>> RECENT = new HashMap<>();
    private static final long WINDOW = 60; // 3 seconds

    public static void record(Entity builder) {
        long now = builder.level().getGameTime();
        Deque<Long> times = RECENT.computeIfAbsent(builder.getUUID(), k -> new ArrayDeque<>());
        times.addLast(now);
        while (times.size() > 16) times.removeFirst();
        if (RECENT.size() > 512) RECENT.entrySet().removeIf(e -> e.getValue().isEmpty() || now - e.getValue().peekLast() > WINDOW * 4);
    }

    /** Has this entity placed at least {@code count} blocks in the last few seconds? */
    public static boolean isBuilding(Entity e, int count) {
        Deque<Long> times = RECENT.get(e.getUUID());
        if (times == null) return false;
        long now = e.level().getGameTime();
        int n = 0;
        for (long t : times) if (now - t <= WINDOW) n++;
        return n >= count;
    }
}
