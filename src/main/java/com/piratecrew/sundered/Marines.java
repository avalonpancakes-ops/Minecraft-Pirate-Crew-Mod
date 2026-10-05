package com.piratecrew.sundered;

import com.piratecrew.bank.LoanManager;
import com.piratecrew.entity.MarineEntity;
import com.piratecrew.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Spawns Order of the Tide patrol squads on the islands around players in the Sundered Sea. */
public class Marines {
    private static int ticks;

    public static MarineEntity spawn(ServerLevel level, Vec3 pos, MarineEntity.Rank rank, boolean patrol) {
        MarineEntity m = ModEntities.MARINE.get().create(level);
        if (m == null) return null;
        m.moveTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360F, 0);
        m.setupMarine(rank);
        m.setPatrol(patrol);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(m);
        return m;
    }

    /** A sergeant (sometimes a captain) leading recruits and riflemen. */
    public static List<MarineEntity> spawnSquad(ServerLevel level, BlockPos near, int size, boolean patrol) {
        RandomSource r = level.getRandom();
        List<MarineEntity> out = new ArrayList<>();
        Vec3 base = LoanManager.findSpot(level, near, 0, 3, r);
        if (base == null) return out;
        for (int i = 0; i < size; i++) {
            MarineEntity.Rank rank = i == 0 ? (r.nextFloat() < 0.12F ? MarineEntity.Rank.CAPTAIN : MarineEntity.Rank.SERGEANT)
                    : (r.nextFloat() < 0.4F ? MarineEntity.Rank.RIFLEMAN : MarineEntity.Rank.RECRUIT);
            Vec3 spot = LoanManager.findSpot(level, BlockPos.containing(base), 0, 3, r);
            MarineEntity m = spawn(level, spot != null ? spot : base, rank, patrol);
            if (m != null) out.add(m);
        }
        return out;
    }

    public static void tick(MinecraftServer server) {
        if (server == null || ++ticks % 400 != 0) return;
        ServerLevel sea = server.getLevel(SunderedSea.LEVEL);
        if (sea == null || sea.players().isEmpty() || sea.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        RandomSource r = sea.getRandom();
        for (ServerPlayer p : sea.players()) {
            if (p.isSpectator() || p.isCreative()) continue;
            int nearby = sea.getEntitiesOfClass(MarineEntity.class, p.getBoundingBox().inflate(64)).size();
            if (nearby >= 6 || r.nextFloat() > 0.3F) continue;
            Vec3 spot = LoanManager.findSpot(sea, p.blockPosition(), 24, 40, r);
            if (spot == null) continue;
            spawnSquad(sea, BlockPos.containing(spot), 2 + r.nextInt(3), true);
        }
    }
}
