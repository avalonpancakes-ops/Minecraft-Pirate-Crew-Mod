package com.piratecrew.sundered;

import com.piratecrew.PirateCrew;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * A ruined Pact Shrine: a ring of broken, mossy pillars around an altar where a Soul Pact waits in a
 * chest, lit by soul lanterns. The rare way to find a pact without killing for it.
 */
public class ShrineBuilder {
    public static final int RADIUS = 5;

    public static void build(ServerLevel level, BlockPos floor) {
        RandomSource r = level.getRandom();
        int flags = 2;
        // clear and pave
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > RADIUS * RADIUS + 1) continue;
                BlockPos p = floor.offset(dx, 0, dz);
                for (int h = 1; h <= 7; h++) level.setBlock(p.above(h), Blocks.AIR.defaultBlockState(), flags);
                level.setBlock(p, paving(r), flags);
                level.setBlock(p.below(), Blocks.STONE_BRICKS.defaultBlockState(), flags);
            }
        }
        // broken pillars
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            BlockPos base = floor.offset((int) Math.round(Math.cos(a) * (RADIUS - 1)), 0, (int) Math.round(Math.sin(a) * (RADIUS - 1)));
            int h = 1 + r.nextInt(4);
            for (int y = 1; y <= h; y++) level.setBlock(base.above(y), r.nextInt(3) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                    : r.nextInt(3) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState(), flags);
            if (h >= 3 && r.nextBoolean()) level.setBlock(base.above(h + 1),
                    Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), flags);
            if (h == 4) level.setBlock(base.above(h + 1), Blocks.SOUL_LANTERN.defaultBlockState(), flags);
        }
        // altar
        level.setBlock(floor.above(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), flags);
        BlockPos chest = floor.above(2);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.from2DDataValue(r.nextInt(4))), flags);
        if (level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity rc) rc.setLootTable(PirateCrew.id("chests/pact_shrine"), r.nextLong());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos side = floor.above().relative(d, 2);
            level.setBlock(side, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState(), 3);
            level.setBlock(side.above(), Blocks.SOUL_LANTERN.defaultBlockState(), flags);
        }
        for (int i = 0; i < 4; i++) {
            BlockPos p = floor.offset(r.nextInt(7) - 3, 1, r.nextInt(7) - 3);
            if (level.getBlockState(p).isAir() && p.distManhattan(floor.above()) > 2) level.setBlock(p, Blocks.AMETHYST_CLUSTER.defaultBlockState(), flags);
        }
    }

    private static BlockState paving(RandomSource r) {
        int n = r.nextInt(10);
        return n < 4 ? Blocks.STONE_BRICKS.defaultBlockState() : n < 7 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                : n < 9 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState();
    }
}
