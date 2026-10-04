package com.piratecrew.world;

import com.piratecrew.PirateCrew;
import com.piratecrew.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * The village bank: a stone hall with a teller counter (two Bank Counters behind iron bars).
 *
 * Local coordinates: x 0..8, z 0..8, y 0 = floor. The door is in the z=0 wall facing local NORTH.
 * Local (4, 0, 4) is the origin stored in {@link BarData}.
 */
public class BankBuilder {
    public static final int W = 9, D = 9;

    private final ServerLevel level;
    private final BlockPos origin;
    private final Rotation rot;
    private final List<BlockPos> reshape = new ArrayList<>();

    private BankBuilder(ServerLevel level, BlockPos origin, Direction facing) {
        this.level = level;
        this.origin = origin;
        this.rot = BarBuilder.rotationFor(facing);
    }

    public static BlockPos toWorld(BlockPos origin, Rotation rot, int lx, int ly, int lz) {
        return origin.offset(new BlockPos(lx - 4, ly, lz - 4).rotate(rot));
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

    public static BarData.Bar buildAt(ServerLevel level, BlockPos centre, Direction facing, boolean surface) {
        BlockPos origin = centre;
        if (surface) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre.getX(), centre.getZ()) - 1;
            origin = new BlockPos(centre.getX(), y, centre.getZ());
        }
        new BankBuilder(level, origin, facing).build();
        BarData.Bar bank = new BarData.Bar(origin, facing);
        BarData.get(level).addBank(bank);
        PirateCrew.LOGGER.debug("Pirate Crew: built bank at {}", origin);
        return bank;
    }

    private void build() {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        BlockState andesite = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState smooth = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();

        // 1. Clear and lay a foundation
        for (int lx = -1; lx <= W; lx++)
            for (int lz = -2; lz <= D; lz++)
                for (int ly = 1; ly <= 9; ly++) {
                    BlockPos p = at(lx, ly, lz);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, air, 2);
                }
        for (int lx = 0; lx < W; lx++)
            for (int lz = -1; lz < D; lz++) {
                if (lz == -1 && (lx < 3 || lx > 5)) continue;
                boolean edge = lx == 0 || lx == W - 1 || lz <= 0 || lz == D - 1;
                for (int depth = 1; depth <= 12; depth++) {
                    BlockPos p = at(lx, -depth, lz);
                    BlockState s = level.getBlockState(p);
                    if (!(s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty())) break;
                    level.setBlock(p, edge ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 2);
                }
            }

        // 2. Floor and front step
        for (int lx = 0; lx < W; lx++)
            for (int lz = 0; lz < D; lz++) {
                boolean edge = lx == 0 || lx == W - 1 || lz == 0 || lz == D - 1;
                set(lx, 0, lz, edge ? smooth : ((lx + lz) % 2 == 0 ? andesite : Blocks.POLISHED_DIORITE.defaultBlockState()));
            }
        for (int lx = 3; lx <= 5; lx++) set(lx, 0, -1, Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabType.TOP));

        // 3. Walls: stone bricks, chiseled corners, andesite base trim
        for (int ly = 1; ly <= 4; ly++) {
            for (int lx = 0; lx < W; lx++) { wall(lx, ly, 0); wall(lx, ly, D - 1); }
            for (int lz = 1; lz < D - 1; lz++) { wall(0, ly, lz); wall(W - 1, ly, lz); }
        }
        // Front pillars either side of the door
        for (int ly = 1; ly <= 4; ly++) {
            set(3, ly, -1, Blocks.QUARTZ_PILLAR.defaultBlockState());
            set(5, ly, -1, Blocks.QUARTZ_PILLAR.defaultBlockState());
        }
        // Windows (iron bars)
        for (int ly = 2; ly <= 3; ly++) {
            setShaped(1, ly, 0, bars);
            setShaped(7, ly, 0, bars);
            for (int lz : new int[]{2, 6}) {
                setShaped(0, ly, lz, bars);
                setShaped(W - 1, ly, lz, bars);
            }
        }
        BlockState door = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        set(4, 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(4, 2, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // 4. Ceiling, flat roof with a parapet and a portico over the door
        for (int lx = 0; lx < W; lx++) for (int lz = 0; lz < D; lz++) set(lx, 5, lz, bricks);
        for (int lx = 0; lx < W; lx++) {
            set(lx, 6, 0, (lx % 2 == 0) ? chiseled : Blocks.STONE_BRICK_SLAB.defaultBlockState());
            set(lx, 6, D - 1, (lx % 2 == 0) ? chiseled : Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
        for (int lz = 1; lz < D - 1; lz++) {
            set(0, 6, lz, (lz % 2 == 0) ? chiseled : Blocks.STONE_BRICK_SLAB.defaultBlockState());
            set(W - 1, 6, lz, (lz % 2 == 0) ? chiseled : Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
        for (int lx = 2; lx <= 6; lx++) set(lx, 5, -1, Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabType.BOTTOM));
        set(2, 5, -1, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP));
        set(6, 5, -1, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.TOP));

        // 5. Interior: red carpet to the teller counter
        for (int lz = 1; lz <= 4; lz++) set(4, 1, lz, Blocks.RED_CARPET.defaultBlockState());
        // Counter across z=5 with two Bank Counters, iron bars above, a gate at the east end
        BlockState counter = ModBlocks.BANK_COUNTER.get().defaultBlockState();
        for (int lx = 1; lx <= 6; lx++) {
            boolean teller = lx == 3 || lx == 5;
            set(lx, 1, 5, teller ? counter : andesite);
            if (!teller) setShaped(lx, 2, 5, bars);
            setShaped(lx, 3, 5, bars);
        }
        set(7, 1, 5, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        // Behind the counter: the vault
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
        for (int lx = 1; lx <= 3; lx++) set(lx, 1, 7, barrel);
        set(1, 2, 7, barrel);
        set(5, 1, 7, Blocks.SMITHING_TABLE.defaultBlockState());
        set(6, 1, 7, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        set(7, 1, 7, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH));
        // Benches for waiting customers
        BlockState bench = Blocks.SPRUCE_STAIRS.defaultBlockState();
        set(1, 1, 2, bench.setValue(StairBlock.FACING, Direction.WEST));
        set(1, 1, 3, bench.setValue(StairBlock.FACING, Direction.WEST));
        set(7, 1, 2, bench.setValue(StairBlock.FACING, Direction.EAST));
        set(7, 1, 3, bench.setValue(StairBlock.FACING, Direction.EAST));
        set(1, 1, 1, Blocks.POTTED_FERN.defaultBlockState());
        set(7, 1, 1, Blocks.POTTED_FERN.defaultBlockState());
        // Lights
        BlockState hanging = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        set(4, 4, 2, hanging);
        set(2, 4, 6, hanging);
        set(6, 4, 6, hanging);
        BlockState torch = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH);
        set(2, 2, -1, torch);
        set(6, 2, -1, torch);

        // 6. Sign over the door
        set(4, 3, -1, Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH));
        writeSign(at(4, 3, -1));

        for (BlockPos p : reshape) {
            BlockState s = level.getBlockState(p);
            BlockState fixed = Block.updateFromNeighbourShapes(s, level, p);
            if (fixed != s) level.setBlock(p, fixed, 2);
        }

        // 7. Path toward the village
        for (int k = 2; k <= 10; k++) {
            for (int lx = 3; lx <= 5; lx++) {
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
        boolean corner = (lx == 0 || lx == W - 1) && (lz == 0 || lz == D - 1);
        BlockState s = corner ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                : ly == 1 ? Blocks.POLISHED_ANDESITE.defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();
        set(lx, ly, lz, s);
    }

    private void writeSign(BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity sign)) return;
        SignText text = new SignText()
                .setMessage(0, Component.literal("~ BANK ~"))
                .setMessage(1, Component.literal("Ruby Exchange"))
                .setMessage(2, Component.literal("Deposits &"))
                .setMessage(3, Component.literal("Withdrawals"))
                .setColor(DyeColor.YELLOW)
                .setHasGlowingText(true);
        sign.setText(text, true);
        sign.setChanged();
        BlockState s = level.getBlockState(pos);
        level.sendBlockUpdated(pos, s, s, 3);
    }
}
