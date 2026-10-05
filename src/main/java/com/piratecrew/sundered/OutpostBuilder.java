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
 * An Order of the Tide outpost on an island: crenellated stone walls with a gate and cannons, a stone
 * watchtower with a lookout, a barracks holding the supplies, archery targets, the Order's banner and
 * a garrison of marines.
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
        int nb = flags | Block.UPDATE_NEIGHBORS;
        BlockState brick = Blocks.STONE_BRICKS.defaultBlockState();
        // A cross of polished andesite paths through the yard.
        for (int i = -6; i <= 6; i++) {
            level.setBlock(new BlockPos(cx + i, fy, cz), Blocks.POLISHED_ANDESITE.defaultBlockState(), flags);
            level.setBlock(new BlockPos(cx, fy, cz + i), Blocks.POLISHED_ANDESITE.defaultBlockState(), flags);
        }

        // Curtain walls two blocks high with crenellations, a gate on the south side.
        for (int i = -7; i <= 7; i++) {
            for (int[] o : new int[][]{{i, -7}, {i, 7}, {-7, i}, {7, i}}) {
                boolean gate = o[1] == 7 && Math.abs(o[0]) <= 1;
                if (gate) continue;
                level.setBlock(c.offset(o[0], 0, o[1]), r.nextFloat() < 0.15F ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : brick, flags);
                level.setBlock(c.offset(o[0], 1, o[1]), r.nextFloat() < 0.1F ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : brick, flags);
                if ((o[0] + o[1]) % 2 == 0) level.setBlock(c.offset(o[0], 2, o[1]), Blocks.STONE_BRICK_WALL.defaultBlockState(), nb);
            }
        }
        // gate posts with lanterns, and cannons on the wall either side of the gate
        for (int gx : new int[]{-2, 2}) {
            level.setBlock(c.offset(gx, 2, 7), brick, flags);
            level.setBlock(c.offset(gx, 3, 7), Blocks.LANTERN.defaultBlockState(), flags);
        }
        for (int gx : new int[]{-5, 5}) {
            level.setBlock(c.offset(gx, 2, 7), Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, Direction.SOUTH), flags);
        }
        for (int[] o : new int[][]{{7, -7}, {7, 7}, {-7, 7}}) level.setBlock(c.offset(o[0], 2, o[1]), Blocks.LANTERN.defaultBlockState(), flags);

        // Watchtower in the north-west corner: solid stone, a lookout platform, a slab roof on posts.
        for (int y = 0; y <= 6; y++) {
            for (int dx = -7; dx <= -5; dx++) for (int dz = -7; dz <= -5; dz++) {
                level.setBlock(c.offset(dx, y, dz), (dx == -6 && dz == -6) ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : brick, flags);
            }
        }
        for (int dx = -7; dx <= -5; dx++) for (int dz = -7; dz <= -5; dz++) level.setBlock(c.offset(dx, 7, dz), Blocks.POLISHED_ANDESITE.defaultBlockState(), flags);
        for (int y = 0; y <= 7; y++) level.setBlock(c.offset(-4, y, -6), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST), flags);
        for (int[] o : new int[][]{{-7, -7}, {-5, -7}, {-7, -5}, {-5, -5}}) {
            level.setBlock(c.offset(o[0], 8, o[1]), Blocks.SPRUCE_FENCE.defaultBlockState(), nb);
            level.setBlock(c.offset(o[0], 9, o[1]), Blocks.SPRUCE_FENCE.defaultBlockState(), nb);
        }
        for (int dx = -8; dx <= -4; dx++) for (int dz = -8; dz <= -4; dz++) {
            level.setBlock(c.offset(dx, 10, dz), Blocks.DARK_OAK_SLAB.defaultBlockState(), flags);
        }
        level.setBlock(c.offset(-6, 11, -6), Blocks.CYAN_BANNER.defaultBlockState(), flags);
        level.setBlock(c.offset(-6, 9, -6), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), flags);

        // Barracks along the north wall: stone walls with log corners, windows, a parapet roof.
        int bx0 = -2, bx1 = 5, bz0 = -6, bz1 = -2;
        for (int x = bx0; x <= bx1; x++) for (int z = bz0; z <= bz1; z++) {
            boolean edge = x == bx0 || x == bx1 || z == bz0 || z == bz1;
            boolean corner = (x == bx0 || x == bx1) && (z == bz0 || z == bz1);
            for (int y = 0; y <= 2; y++) {
                if (!edge) continue;
                BlockState wall = corner ? Blocks.SPRUCE_LOG.defaultBlockState() : brick;
                if (y == 1 && !corner && z == bz1 && (x == 0 || x == 3)) wall = Blocks.GLASS_PANE.defaultBlockState();
                if (z == bz1 && x == 1 && y <= 1) wall = Blocks.AIR.defaultBlockState();   // doorway
                level.setBlock(c.offset(x, y, z), wall, nb);
            }
            level.setBlock(c.offset(x, 3, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), flags);
            if (edge) level.setBlock(c.offset(x, 4, z), Blocks.STONE_BRICK_SLAB.defaultBlockState(), flags);
        }
        level.setBlock(c.offset(1, 2, bz1 + 1), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), flags);
        level.setBlock(c.offset(2, 2, -4), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), flags);

        // Supplies inside the barracks.
        BlockPos chest = c.offset(4, 0, -5);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), flags);
        if (level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity rc) rc.setLootTable(PirateCrew.id("chests/marine_outpost"), r.nextLong());
        level.setBlock(c.offset(3, 0, -5), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), flags);
        level.setBlock(c.offset(4, 0, -4), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), flags);
        level.setBlock(c.offset(-1, 0, -5), Blocks.SMITHING_TABLE.defaultBlockState(), flags);
        level.setBlock(c.offset(0, 0, -5), Blocks.BLAST_FURNACE.defaultBlockState(), flags);

        // Archery targets for the riflemen, crates by the gate, the Order's banner on a pole.
        for (int tz : new int[]{2, 4}) {
            level.setBlock(c.offset(5, 0, tz), Blocks.HAY_BLOCK.defaultBlockState(), flags);
            level.setBlock(c.offset(5, 1, tz), Blocks.TARGET.defaultBlockState(), flags);
        }
        level.setBlock(c.offset(-4, 0, 5), Blocks.BARREL.defaultBlockState(), flags);
        level.setBlock(c.offset(-5, 0, 5), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), flags);
        level.setBlock(c.offset(-5, 1, 5), Blocks.BARREL.defaultBlockState(), flags);
        BlockPos pole = c.offset(-4, 0, 2);
        for (int y = 0; y < 4; y++) level.setBlock(pole.above(y), Blocks.SPRUCE_FENCE.defaultBlockState(), nb);
        level.setBlock(pole.above(4), Blocks.CYAN_BANNER.defaultBlockState(), flags);
        level.setBlock(c.offset(0, 0, 0), Blocks.SPRUCE_FENCE.defaultBlockState(), nb);
        level.setBlock(c.offset(0, 1, 0), Blocks.LANTERN.defaultBlockState(), flags);

        // Garrison.
        MarineEntity.Rank[] garrison = r.nextFloat() < 0.25F
                ? new MarineEntity.Rank[]{MarineEntity.Rank.CAPTAIN, MarineEntity.Rank.SERGEANT, MarineEntity.Rank.RIFLEMAN, MarineEntity.Rank.RECRUIT, MarineEntity.Rank.RECRUIT}
                : new MarineEntity.Rank[]{MarineEntity.Rank.SERGEANT, MarineEntity.Rank.RIFLEMAN, MarineEntity.Rank.RECRUIT, MarineEntity.Rank.RECRUIT};
        for (int i = 0; i < garrison.length; i++) {
            int[][] spots = {{-2, 2}, {2, 2}, {-3, 4}, {3, 4}, {0, 5}};
            int[] sp = spots[i % spots.length];
            MarineEntity m = Marines.spawn(level, new Vec3(cx + 0.5 + sp[0], fy + 1, cz + 0.5 + sp[1]), garrison[i], false);
            if (m != null) m.setHome(c);
        }
        // A rifleman up in the tower.
        MarineEntity lookout = Marines.spawn(level, new Vec3(cx - 5.5, fy + 9, cz - 5.5), MarineEntity.Rank.RIFLEMAN, false);
        if (lookout != null) lookout.setHome(c.offset(-6, 8, -6));
    }
}
