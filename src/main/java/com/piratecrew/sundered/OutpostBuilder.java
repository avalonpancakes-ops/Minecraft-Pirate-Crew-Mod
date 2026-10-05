package com.piratecrew.sundered;

import com.piratecrew.PirateCrew;
import com.piratecrew.entity.MarineEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * An Order of the Tide outpost on an island: a stone-brick yard behind a low wall, a spruce
 * watchtower, a teal flag, supplies and a garrison of marines.
 */
public class OutpostBuilder {
    public static final int RADIUS = 7;

    public static void build(ServerLevel level, BlockPos floor) {
        RandomSource r = level.getRandom();
        int flags = Block.UPDATE_CLIENTS;
        int fy = floor.getY(), cx = floor.getX(), cz = floor.getZ();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int x = cx + dx, z = cz + dz;
                for (int y = fy + 1; y <= fy + 12; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), flags);
                boolean yard = Math.abs(dx) <= 6 && Math.abs(dz) <= 6;
                BlockState top = yard ? (r.nextFloat() < 0.2F ? Blocks.MOSSY_STONE_BRICKS : r.nextFloat() < 0.1F ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState();
                level.setBlock(new BlockPos(x, fy, z), top, flags);
                for (int y = fy - 1; y >= fy - 6; y--) {
                    BlockPos b = new BlockPos(x, y, z);
                    BlockState s = level.getBlockState(b);
                    if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty()) level.setBlock(b, Blocks.STONE.defaultBlockState(), flags);
                    else break;
                }
            }
        }
        BlockPos c = new BlockPos(cx, fy + 1, cz);
        // Low wall with a gate on each side.
        for (int i = -6; i <= 6; i++) {
            for (BlockPos p : new BlockPos[]{c.offset(i, 0, -6), c.offset(i, 0, 6), c.offset(-6, 0, i), c.offset(6, 0, i)}) {
                if (Math.abs(i) <= 1) continue;
                level.setBlock(p, Blocks.COBBLESTONE_WALL.defaultBlockState(), flags | Block.UPDATE_NEIGHBORS);
            }
        }
        // Watchtower in the north-west corner.
        for (int y = 0; y < 6; y++) {
            for (int[] o : new int[][]{{-5, -5}, {-3, -5}, {-5, -3}, {-3, -3}}) level.setBlock(c.offset(o[0], y, o[1]), Blocks.SPRUCE_LOG.defaultBlockState(), flags);
        }
        for (int dx = -5; dx <= -3; dx++) for (int dz = -5; dz <= -3; dz++) level.setBlock(c.offset(dx, 6, dz), Blocks.SPRUCE_PLANKS.defaultBlockState(), flags);
        for (int y = 0; y <= 6; y++) level.setBlock(c.offset(-2, y, -3), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST), flags);
        for (int dx = -5; dx <= -3; dx++) {
            level.setBlock(c.offset(dx, 7, -5), Blocks.SPRUCE_FENCE.defaultBlockState(), flags | Block.UPDATE_NEIGHBORS);
            if (dx != -3) level.setBlock(c.offset(dx, 7, -3), Blocks.SPRUCE_FENCE.defaultBlockState(), flags | Block.UPDATE_NEIGHBORS);
        }
        level.setBlock(c.offset(-5, 7, -4), Blocks.SPRUCE_FENCE.defaultBlockState(), flags | Block.UPDATE_NEIGHBORS);
        level.setBlock(c.offset(-4, 7, -4), Blocks.LANTERN.defaultBlockState(), flags);

        // Flag of the Order.
        BlockPos pole = c.offset(4, 0, -4);
        for (int y = 0; y < 6; y++) level.setBlock(pole.above(y), Blocks.SPRUCE_FENCE.defaultBlockState(), flags | Block.UPDATE_NEIGHBORS);
        for (int dx = 1; dx <= 2; dx++) for (int dy = 4; dy <= 5; dy++) level.setBlock(pole.offset(dx, dy, 0), (dy == 5 ? Blocks.CYAN_WOOL : Blocks.LIGHT_BLUE_WOOL).defaultBlockState(), flags);

        // Supplies.
        BlockPos chest = c.offset(4, 0, 4);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), flags);
        if (level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity rc) rc.setLootTable(PirateCrew.id("chests/marine_outpost"), r.nextLong());
        level.setBlock(c.offset(3, 0, 4), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), flags);
        level.setBlock(c.offset(4, 0, 3), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), flags);
        level.setBlock(c.offset(-4, 0, 4), Blocks.SMITHING_TABLE.defaultBlockState(), flags);
        level.setBlock(c.offset(-3, 0, 4), Blocks.BLAST_FURNACE.defaultBlockState(), flags);
        level.setBlock(c.offset(0, 0, 0), Blocks.SPRUCE_FENCE.defaultBlockState(), flags | Block.UPDATE_NEIGHBORS);
        level.setBlock(c.offset(0, 1, 0), Blocks.LANTERN.defaultBlockState(), flags);

        // Garrison.
        MarineEntity.Rank[] garrison = r.nextFloat() < 0.25F
                ? new MarineEntity.Rank[]{MarineEntity.Rank.CAPTAIN, MarineEntity.Rank.SERGEANT, MarineEntity.Rank.RIFLEMAN, MarineEntity.Rank.RECRUIT, MarineEntity.Rank.RECRUIT}
                : new MarineEntity.Rank[]{MarineEntity.Rank.SERGEANT, MarineEntity.Rank.RIFLEMAN, MarineEntity.Rank.RECRUIT, MarineEntity.Rank.RECRUIT};
        for (int i = 0; i < garrison.length; i++) {
            double a = i * Math.PI * 2 / garrison.length;
            MarineEntity m = Marines.spawn(level, new Vec3(cx + 0.5 + Math.cos(a) * 2.5, fy + 1, cz + 0.5 + Math.sin(a) * 2.5), garrison[i], false);
            if (m != null) m.setHome(c);
        }
        // A rifleman up in the tower.
        MarineEntity lookout = Marines.spawn(level, new Vec3(cx - 3.5, fy + 8, cz - 3.5), MarineEntity.Rank.RIFLEMAN, false);
        if (lookout != null) lookout.setHome(c.offset(-4, 7, -4));
    }
}
