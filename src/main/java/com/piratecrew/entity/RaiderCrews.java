package com.piratecrew.entity;

import com.piratecrew.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Builds enemy NPC pirate crews: a captain plus crew, gear randomised per member, captain always best equipped. */
public class RaiderCrews {
    /** Gear level weights for crew: none, leather, gold, chainmail, iron, ruby, diamond. */
    private static final int[] GEAR_WEIGHTS = {12, 22, 10, 18, 20, 11, 7};

    private static int rollGear(RandomSource r) {
        int total = 0;
        for (int w : GEAR_WEIGHTS) total += w;
        int roll = r.nextInt(total);
        for (int i = 0; i < GEAR_WEIGHTS.length; i++) {
            roll -= GEAR_WEIGHTS[i];
            if (roll < 0) return i;
        }
        return 0;
    }

    private static PirateTier captainTier(RandomSource r) {
        int roll = r.nextInt(100);
        return roll < 50 ? PirateTier.B : roll < 85 ? PirateTier.A : PirateTier.S;
    }

    /**
     * Spawn a captain and {@code crewSize} crew at the given spots (cycled through, with a little
     * spread). Returns everyone spawned, captain first.
     */
    public static List<RaiderPirateEntity> spawnCrew(ServerLevel level, List<Vec3> spots, int crewSize, boolean onShip) {
        List<RaiderPirateEntity> out = new ArrayList<>();
        if (spots.isEmpty()) return out;
        RandomSource r = level.getRandom();
        int[] gear = new int[crewSize];
        int best = 0;
        for (int i = 0; i < crewSize; i++) {
            gear[i] = rollGear(r);
            best = Math.max(best, gear[i]);
        }
        // The captain is always the best equipped: the crew's best level, sometimes one better.
        int captainGear = Math.min(6, best + (r.nextBoolean() ? 1 : 0));
        for (int i = 0; i <= crewSize; i++) {
            boolean isCaptain = i == 0;
            Vec3 base = spots.get(i % spots.size());
            double spread = i < spots.size() ? 0.3 : 0.9;
            Vec3 pos = base.add((r.nextDouble() - 0.5) * spread, 0.05, (r.nextDouble() - 0.5) * spread);
            RaiderPirateEntity p = ModEntities.RAIDER_PIRATE.get().create(level);
            if (p == null) continue;
            p.moveTo(pos.x, pos.y, pos.z, r.nextFloat() * 360F, 0);
            p.setupRaider(isCaptain ? captainTier(r) : PirateTier.random(r), isCaptain ? captainGear : gear[i - 1], isCaptain, onShip);
            p.finalizeSpawn(level, level.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.EVENT, null, null);
            level.addFreshEntity(p);
            out.add(p);
        }
        return out;
    }
}
