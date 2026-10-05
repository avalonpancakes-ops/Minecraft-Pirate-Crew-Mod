package com.piratecrew.world;

import com.piratecrew.PirateCrew;
import com.piratecrew.entity.RaiderCrews;
import com.piratecrew.entity.RaiderPirateEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A raider pirates' camp: a cleared clearing with a campfire ringed by log seats, three wool tents,
 * a loot chest, barrels, a black flag and lanterns, and an enemy crew lounging around the fire.
 */
public class CampBuilder {
    public static final int RADIUS = 8;

    private static final Block[] TENT_WOOL = {Blocks.WHITE_WOOL, Blocks.BROWN_WOOL, Blocks.RED_WOOL, Blocks.GRAY_WOOL, Blocks.BLACK_WOOL};

    /** {@code floor} is the ground block at the camp's centre; everything stands on floor + 1. */
    public static void build(ServerLevel level, BlockPos floor) {
        RandomSource r = level.getRandom();
        int fy = floor.getY();
        int cx = floor.getX(), cz = floor.getZ();
        int flags = Block.UPDATE_CLIENTS;

        // Clear and level the clearing.
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS + 6) continue;
                int x = cx + dx, z = cz + dz;
                for (int y = fy + 1; y <= fy + 10; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), flags);
                BlockPos ground = new BlockPos(x, fy, z);
                BlockState g = level.getBlockState(ground);
                boolean nearFire = dx * dx + dz * dz <= 6;
                if (nearFire) level.setBlock(ground, Blocks.COARSE_DIRT.defaultBlockState(), flags);
                else if (!g.isSolidRender(level, ground) || !g.getFluidState().isEmpty()) level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), flags);
                for (int y = fy - 1; y >= fy - 5; y--) {
                    BlockPos b = new BlockPos(x, y, z);
                    BlockState s = level.getBlockState(b);
                    if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty()) level.setBlock(b, Blocks.DIRT.defaultBlockState(), flags);
                    else break;
                }
            }
        }

        BlockPos c = new BlockPos(cx, fy + 1, cz);
        // Campfire and log seats.
        level.setBlock(c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), flags | Block.UPDATE_NEIGHBORS);
        level.setBlock(c.offset(3, 0, 0), Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), flags);
        level.setBlock(c.offset(-3, 0, 0), Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), flags);
        level.setBlock(c.offset(0, 0, 3), Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), flags);

        // Tents around the fire, open side facing it.
        tent(level, c.offset(0, 0, -6), Direction.SOUTH, TENT_WOOL[r.nextInt(TENT_WOOL.length)], flags);
        tent(level, c.offset(-6, 0, 1), Direction.EAST, TENT_WOOL[r.nextInt(TENT_WOOL.length)], flags);
        tent(level, c.offset(6, 0, 1), Direction.WEST, TENT_WOOL[r.nextInt(TENT_WOOL.length)], flags);

        // Loot chest by the north tent, barrels by the west one.
        BlockPos chest = c.offset(2, 0, -6);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), flags);
        BlockEntity be = level.getBlockEntity(chest);
        if (be instanceof RandomizableContainerBlockEntity rc) rc.setLootTable(PirateCrew.id("chests/raider_camp"), r.nextLong());
        level.setBlock(c.offset(-6, 0, 3), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), flags);
        level.setBlock(c.offset(-5, 0, 4), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), flags);

        // Black flag on a pole.
        BlockPos pole = c.offset(4, 0, 5);
        for (int y = 0; y < 4; y++) level.setBlock(pole.above(y), Blocks.OAK_FENCE.defaultBlockState(), flags | Block.UPDATE_NEIGHBORS);
        level.setBlock(pole.offset(1, 3, 0), Blocks.BLACK_WOOL.defaultBlockState(), flags);
        level.setBlock(pole.offset(2, 3, 0), Blocks.BLACK_WOOL.defaultBlockState(), flags);
        level.setBlock(pole.offset(1, 2, 0), Blocks.BLACK_WOOL.defaultBlockState(), flags);
        level.setBlock(pole.offset(2, 2, 0), Blocks.BLACK_WOOL.defaultBlockState(), flags);

        // Lanterns on posts.
        for (BlockPos post : new BlockPos[]{c.offset(-4, 0, -3), c.offset(4, 0, -3)}) {
            level.setBlock(post, Blocks.OAK_FENCE.defaultBlockState(), flags | Block.UPDATE_NEIGHBORS);
            level.setBlock(post.above(), Blocks.LANTERN.defaultBlockState(), flags);
        }

        // The crew, lounging round the fire.
        List<Vec3> spots = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + 0.4;
            spots.add(new Vec3(cx + 0.5 + Math.cos(a) * 2.2, fy + 1, cz + 0.5 + Math.sin(a) * 2.2));
        }
        List<RaiderPirateEntity> crew = RaiderCrews.spawnCrew(level, spots, 3 + r.nextInt(3), false);
        for (RaiderPirateEntity p : crew) p.setHome(c);
    }

    /** A 3x3 A-frame tent: wool walls and ridge, a closed back, a bedroll inside. */
    private static void tent(ServerLevel level, BlockPos front, Direction facing, Block wool, int flags) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        BlockState w = wool.defaultBlockState();
        for (int d = 0; d < 3; d++) {
            BlockPos row = front.relative(back, d);
            for (int y = 0; y < 2; y++) {
                level.setBlock(row.relative(right).above(y), w, flags);
                level.setBlock(row.relative(right.getOpposite()).above(y), w, flags);
            }
            level.setBlock(row.above(2), w, flags);
            if (d == 2) {
                level.setBlock(row, w, flags);
                level.setBlock(row.above(), w, flags);
            }
        }
        level.setBlock(front.relative(back, 1), Blocks.RED_CARPET.defaultBlockState(), flags);
        level.setBlock(front, Blocks.RED_CARPET.defaultBlockState(), flags);
    }
}
