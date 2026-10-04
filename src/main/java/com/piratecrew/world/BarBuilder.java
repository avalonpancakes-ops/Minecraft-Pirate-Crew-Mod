package com.piratecrew.world;

import com.piratecrew.Config;
import com.piratecrew.PirateCrew;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.entity.PirateTier;
import com.piratecrew.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the pirate tavern in code so it can be rotated to face the village.
 *
 * Local coordinates: x 0..12 (width), z 0..10 (depth), y 0 = floor. The front door is in the z=0
 * wall and faces local NORTH (-z). Local (6, 0, 5) is the "origin" stored in {@link BarData}.
 */
public class BarBuilder {
    public static final int W = 13, D = 11;
    private static final String[][] TAVERN_NAMES = {
            {"The Salty", "Kraken"}, {"The Rusty", "Anchor"}, {"The Drunken", "Parrot"}, {"The Jolly", "Barnacle"},
            {"The Sunken", "Doubloon"}, {"The Crooked", "Cutlass"}, {"The Black", "Gull"}, {"The Rum", "Runner"}
    };
    /** Places pirates like to stand, in local coordinates (x, z). */
    private static final int[][] SPOTS = {
            {2, 2}, {4, 2}, {8, 2}, {10, 2}, {2, 5}, {10, 5}, {5, 5}, {7, 5}, {6, 8}, {9, 8}
    };

    private final ServerLevel level;
    private final BlockPos origin;
    private final Rotation rot;
    private final List<BlockPos> reshape = new ArrayList<>();

    private BarBuilder(ServerLevel level, BlockPos origin, Direction facing) {
        this.level = level;
        this.origin = origin;
        this.rot = rotationFor(facing);
    }

    public static Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos toWorld(BlockPos origin, Rotation rot, int lx, int ly, int lz) {
        return origin.offset(new BlockPos(lx - 6, ly, lz - 5).rotate(rot));
    }

    private BlockPos at(int lx, int ly, int lz) {
        return toWorld(origin, rot, lx, ly, lz);
    }

    private void set(int lx, int ly, int lz, BlockState state) {
        level.setBlock(at(lx, ly, lz), state.rotate(rot), 2);
    }

    private void setShaped(int lx, int ly, int lz, BlockState state) {
        set(lx, ly, lz, state);
        reshape.add(at(lx, ly, lz));
    }

    /**
     * Build a bar centred on {@code centre}. With {@code surface} the floor goes at ground level,
     * otherwise {@code centre.getY()} is used as the floor height.
     */
    public static BarData.Bar buildAt(ServerLevel level, BlockPos centre, Direction facing, boolean surface) {
        BlockPos origin = centre;
        if (surface) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre.getX(), centre.getZ()) - 1;
            origin = new BlockPos(centre.getX(), y, centre.getZ());
        }
        BarBuilder b = new BarBuilder(level, origin, facing);
        b.build();
        b.spawnInitialPirates();
        BarData.Bar bar = new BarData.Bar(origin, facing);
        BarData.get(level).addBar(bar);
        PirateCrew.LOGGER.debug("Pirate Crew: built bar at {}", origin);
        return bar;
    }

    // ------------------------------------------------------------------ building

    private void build() {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState darkPlanks = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();

        // 1. Clear the volume (including roof overhang) and lay a foundation.
        for (int lx = -1; lx <= W; lx++) {
            for (int lz = -2; lz <= D; lz++) {
                for (int ly = 1; ly <= 13; ly++) {
                    BlockPos p = at(lx, ly, lz);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, air, 2);
                }
            }
        }
        for (int lx = 0; lx < W; lx++) {
            for (int lz = -1; lz < D; lz++) {
                boolean edge = lx == 0 || lx == W - 1 || lz <= 0 || lz == D - 1;
                if (lz == -1 && (lx < 5 || lx > 7)) continue; // porch only in front of the door
                for (int depth = 1; depth <= 12; depth++) {
                    BlockPos p = at(lx, -depth, lz);
                    BlockState s = level.getBlockState(p);
                    if (!(s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty())) break;
                    level.setBlock(p, edge ? cobble : Blocks.DIRT.defaultBlockState(), 2);
                }
            }
        }

        // 2. Floor (stone ring under the walls) and porch.
        for (int lx = 0; lx < W; lx++) {
            for (int lz = 0; lz < D; lz++) {
                boolean edge = lx == 0 || lx == W - 1 || lz == 0 || lz == D - 1;
                set(lx, 0, lz, edge ? stoneBricks : planks);
            }
        }
        for (int lx = 5; lx <= 7; lx++) set(lx, 0, -1, planks);

        // 3. Walls with log posts, windows and a door.
        for (int ly = 1; ly <= 4; ly++) {
            for (int lx = 0; lx < W; lx++) {
                wall(lx, ly, 0);
                wall(lx, ly, D - 1);
            }
            for (int lz = 1; lz < D - 1; lz++) {
                wall(0, ly, lz);
                wall(W - 1, ly, lz);
            }
        }
        for (int ly = 2; ly <= 3; ly++) {
            for (int lx : new int[]{2, 3, 9, 10}) setShaped(lx, ly, 0, Blocks.GLASS_PANE.defaultBlockState());
            for (int lz : new int[]{2, 3, 7, 8}) {
                setShaped(0, ly, lz, Blocks.GLASS_PANE.defaultBlockState());
                setShaped(W - 1, ly, lz, Blocks.GLASS_PANE.defaultBlockState());
            }
            setShaped(6, ly, D - 1, Blocks.GLASS_PANE.defaultBlockState());
        }
        BlockState door = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        set(6, 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(6, 2, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // 4. Ceiling
        for (int lx = 0; lx < W; lx++) for (int lz = 0; lz < D; lz++) set(lx, 5, lz, planks);

        // 5. Gabled roof, ridge running along x
        BlockState stair = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.BOTTOM);
        for (int lz = -1; lz <= D; lz++) {
            int ry = roofY(lz);
            for (int lx = -1; lx <= W; lx++) {
                if (lz == 5) set(lx, ry, lz, darkPlanks);
                else set(lx, ry, lz, stair.setValue(StairBlock.FACING, lz < 5 ? Direction.SOUTH : Direction.NORTH));
            }
            if (lz >= 0 && lz < D) {
                for (int ly = 6; ly < ry; ly++) {
                    set(0, ly, lz, planks);
                    set(W - 1, ly, lz, planks);
                }
            }
        }
        // Jolly Roger on the ridge
        set(6, roofY(5) + 1, 5, Blocks.BLACK_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, 0));

        // 6. Interior
        for (int lz = 1; lz <= 5; lz++) set(6, 1, lz, Blocks.RED_CARPET.defaultBlockState());

        // Bar counter with a gap at x=10 to get behind it
        for (int lx = 2; lx <= 9; lx++) set(lx, 1, 7, darkPlanks);
        set(3, 2, 7, Blocks.BREWING_STAND.defaultBlockState());
        set(8, 2, 7, Blocks.LANTERN.defaultBlockState());
        set(5, 2, 7, Blocks.POTTED_RED_MUSHROOM.defaultBlockState());

        // Kegs and shelves behind the bar
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.NORTH);
        for (int lx = 1; lx <= 4; lx++) {
            set(lx, 1, 9, barrel);
            set(lx, 2, 9, barrel);
        }
        for (int lx = 7; lx <= 11; lx++) set(lx, 1, 9, Blocks.BOOKSHELF.defaultBlockState());
        set(11, 1, 8, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));

        // Tables (fence + pressure plate) with stair chairs
        for (int tx : new int[]{3, 9}) {
            setShaped(tx, 1, 3, Blocks.SPRUCE_FENCE.defaultBlockState());
            set(tx, 2, 3, Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
            BlockState chair = Blocks.SPRUCE_STAIRS.defaultBlockState();
            set(tx - 1, 1, 3, chair.setValue(StairBlock.FACING, Direction.WEST));
            set(tx + 1, 1, 3, chair.setValue(StairBlock.FACING, Direction.EAST));
            set(tx, 1, 2, chair.setValue(StairBlock.FACING, Direction.NORTH));
        }
        // Barrels in the front corners
        set(1, 1, 1, barrel.setValue(BarrelBlock.FACING, Direction.UP));
        set(1, 2, 1, barrel.setValue(BarrelBlock.FACING, Direction.UP));
        set(11, 1, 1, barrel.setValue(BarrelBlock.FACING, Direction.UP));

        // Lights
        BlockState hanging = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        set(3, 4, 3, hanging);
        set(9, 4, 3, hanging);
        set(6, 4, 6, hanging);
        set(6, 4, 1, hanging);
        BlockState torch = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH);
        set(4, 3, -1, torch);
        set(8, 3, -1, torch);

        // Sign over the door
        set(6, 3, -1, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH));
        writeSign(at(6, 3, -1));

        // 7. Fix glass pane / fence connections now that everything is placed
        for (BlockPos p : reshape) {
            BlockState s = level.getBlockState(p);
            BlockState fixed = Block.updateFromNeighbourShapes(s, level, p);
            if (fixed != s) level.setBlock(p, fixed, 2);
        }

        // 8. Path toward the village
        for (int k = 2; k <= 12; k++) {
            for (int lx = 5; lx <= 7; lx++) {
                BlockPos col = at(lx, 0, -k);
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col.getX(), col.getZ()) - 1;
                BlockPos ground = new BlockPos(col.getX(), top, col.getZ());
                BlockState g = level.getBlockState(ground);
                if (g.is(Blocks.GRASS_BLOCK) || g.is(Blocks.DIRT) || g.is(Blocks.COARSE_DIRT) || g.is(Blocks.PODZOL)) {
                    level.setBlock(ground, Blocks.DIRT_PATH.defaultBlockState(), 2);
                    BlockState above = level.getBlockState(ground.above());
                    if (!above.isAir() && above.canBeReplaced()) level.setBlock(ground.above(), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private void wall(int lx, int ly, int lz) {
        boolean post = (lx == 0 || lx == W - 1 || lx == 4 || lx == 8) && (lz == 0 || lz == D - 1)
                || (lz == 5 && (lx == 0 || lx == W - 1));
        set(lx, ly, lz, post ? Blocks.SPRUCE_LOG.defaultBlockState()
                : ly == 1 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
    }

    private static int roofY(int lz) {
        return 5 + Math.min(lz + 1, D - lz);
    }

    private void writeSign(BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity sign)) return;
        String[] name = TAVERN_NAMES[level.random.nextInt(TAVERN_NAMES.length)];
        SignText text = new SignText()
                .setMessage(0, Component.literal("~ Tavern ~"))
                .setMessage(1, Component.literal(name[0]))
                .setMessage(2, Component.literal(name[1]))
                .setMessage(3, Component.literal("Crew for hire"))
                .setColor(DyeColor.YELLOW)
                .setHasGlowingText(true);
        sign.setText(text, true);
        sign.setChanged();
        BlockState s = level.getBlockState(pos);
        level.sendBlockUpdated(pos, s, s, 3);
    }

    // ------------------------------------------------------------------ pirates

    private void spawnInitialPirates() {
        int n = Config.BAR_MAX_PIRATES.get();
        List<int[]> spots = new ArrayList<>(List.of(SPOTS));
        Collections.shuffle(spots, new java.util.Random(level.random.nextLong()));
        for (int i = 0; i < n && i < spots.size(); i++) {
            spawnPirate(level, origin, rot, spots.get(i)[0], spots.get(i)[1]);
        }
    }

    /** Add one fresh pirate at a random spot inside an existing bar. */
    public static void restock(ServerLevel level, BarData.Bar bar) {
        int[] spot = SPOTS[level.random.nextInt(SPOTS.length)];
        spawnPirate(level, bar.origin(), rotationFor(bar.facing()), spot[0], spot[1]);
    }

    public static BlockPos interiorCentre(BarData.Bar bar) {
        return toWorld(bar.origin(), rotationFor(bar.facing()), 6, 1, 5);
    }

    private static void spawnPirate(ServerLevel level, BlockPos origin, Rotation rot, int lx, int lz) {
        PirateEntity pirate = ModEntities.PIRATE.get().create(level);
        if (pirate == null) return;
        BlockPos p = toWorld(origin, rot, lx, 1, lz);
        RandomSource r = level.random;
        pirate.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360F, 0F);
        pirate.initPirate(PirateTier.random(r));
        pirate.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
        pirate.setHome(toWorld(origin, rot, 6, 1, 5));
        level.addFreshEntity(pirate);
    }
}
