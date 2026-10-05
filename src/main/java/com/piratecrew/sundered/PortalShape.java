package com.piratecrew.sundered;

import com.piratecrew.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Finds the inside of a ruby-block portal frame, shaped like a nether portal: an upright rectangle
 * 2-21 blocks wide and 3-21 tall inside (corners optional).
 */
public class PortalShape {
    public static final int MAX = 21;

    public static boolean isFrame(BlockState s) {
        return s.is(ModBlocks.RUBY_BLOCK.get());
    }

    private static boolean isEmpty(BlockState s) {
        return s.isAir() || s.is(BlockTags.FIRE) || s.is(ModBlocks.SIREN_PORTAL.get());
    }

    /** The positions inside the frame around {@code start}, for frames along {@code axis} (X or Z). */
    public static Optional<List<BlockPos>> find(Level level, BlockPos start, Direction.Axis axis) {
        if (!isEmpty(level.getBlockState(start))) return Optional.empty();
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction left = right.getOpposite();

        // Down to the bottom of the opening.
        BlockPos p = start;
        for (int i = 0; i < MAX && isEmpty(level.getBlockState(p.below())); i++) p = p.below();
        if (!isFrame(level.getBlockState(p.below()))) return Optional.empty();
        // Left to the edge.
        for (int i = 0; i < MAX && isEmpty(level.getBlockState(p.relative(left))); i++) p = p.relative(left);
        if (!isFrame(level.getBlockState(p.relative(left)))) return Optional.empty();
        BlockPos corner = p;

        // Width along the bottom row.
        int width = 0;
        while (width <= MAX && isEmpty(level.getBlockState(corner.relative(right, width)))) {
            if (!isFrame(level.getBlockState(corner.relative(right, width).below()))) return Optional.empty();
            width++;
        }
        if (width < 2 || width > MAX || !isFrame(level.getBlockState(corner.relative(right, width)))) return Optional.empty();

        // Height: rows of empty space with frame at both ends, capped by a full row of frame.
        int height = 0;
        while (height <= MAX) {
            BlockPos row = corner.above(height);
            boolean allFrame = true, allEmpty = true;
            for (int w = 0; w < width; w++) {
                BlockState s = level.getBlockState(row.relative(right, w));
                if (!isFrame(s)) allFrame = false;
                if (!isEmpty(s)) allEmpty = false;
            }
            if (allFrame) break;
            if (!allEmpty || !isFrame(level.getBlockState(row.relative(left))) || !isFrame(level.getBlockState(row.relative(right, width)))) {
                return Optional.empty();
            }
            height++;
        }
        if (height < 3 || height > MAX) return Optional.empty();

        List<BlockPos> inside = new ArrayList<>();
        for (int h = 0; h < height; h++) for (int w = 0; w < width; w++) inside.add(corner.relative(right, w).above(h));
        return Optional.of(inside);
    }

    /** Fill a frame with portal. Returns true if one was found around {@code start}. */
    public static boolean light(Level level, BlockPos start) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Optional<List<BlockPos>> inside = find(level, start, axis);
            if (inside.isPresent()) {
                BlockState portal = ModBlocks.SIREN_PORTAL.get().defaultBlockState().setValue(SirenPortalBlock.AXIS, axis);
                for (BlockPos p : inside.get()) level.setBlock(p, portal, 2 | 16);
                return true;
            }
        }
        return false;
    }
}
