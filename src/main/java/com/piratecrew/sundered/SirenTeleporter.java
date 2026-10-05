package com.piratecrew.sundered;

import com.piratecrew.registry.ModBlocks;
import com.piratecrew.registry.ModPoi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * Crossing between the Overworld and the Sundered Sea. Coordinates carry over 1:1. Arrives at the
 * nearest Siren portal within 128 blocks, or builds one: on dry land if there's any nearby, otherwise
 * on a small wooden raft-platform out at sea.
 */
public class SirenTeleporter {
    private static final int SEARCH = 128;

    public static void travel(Entity entity, ServerLevel from) {
        boolean toSea = from.dimension() != SunderedSea.LEVEL;
        ServerLevel to = from.getServer().getLevel(toSea ? SunderedSea.LEVEL : Level.OVERWORLD);
        if (to == null) {
            if (entity instanceof ServerPlayer p) p.displayClientMessage(Component.literal("The Sundered Sea couldn't be reached.").withStyle(ChatFormatting.RED), true);
            return;
        }
        BlockPos target = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        target = new BlockPos(target.getX(), Math.max(to.getMinBuildHeight() + 2, Math.min(to.getMaxBuildHeight() - 8, target.getY())), target.getZ());
        BlockPos arrival = findOrBuild(to, target);
        Vec3 pos = Vec3.atBottomCenterOf(arrival);

        Entity moved = entity.changeDimension(to, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity e, ServerLevel dest, Function<ServerLevel, PortalInfo> vanilla) {
                return new PortalInfo(pos, Vec3.ZERO, e.getYRot(), e.getXRot());
            }

            @Override
            public Entity placeEntity(Entity e, ServerLevel current, ServerLevel dest, float yaw, Function<Boolean, Entity> reposition) {
                return reposition.apply(false);
            }

            @Override
            public boolean playTeleportSound(ServerPlayer player, ServerLevel source, ServerLevel dest) {
                return false;
            }
        });
        if (moved != null) {
            moved.setPortalCooldown();
            to.playSound(null, arrival, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (moved instanceof ServerPlayer p && toSea) {
                p.displayClientMessage(Component.literal("⚓ The Sundered Sea").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), true);
            }
        }
    }

    /** Bottom block of a Siren portal near {@code target}, building a new portal if there isn't one. */
    private static BlockPos findOrBuild(ServerLevel level, BlockPos target) {
        PoiManager poi = level.getPoiManager();
        poi.ensureLoadedAndValid(level, target, SEARCH);
        Optional<BlockPos> found = poi.getInSquare(h -> h.is(ModPoi.SIREN_PORTAL_KEY), target, SEARCH, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(p -> level.getBlockState(p).is(ModBlocks.SIREN_PORTAL.get()))
                .min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(target)).thenComparingInt(BlockPos::getY));
        if (found.isPresent()) {
            BlockPos p = found.get();
            while (level.getBlockState(p.below()).is(ModBlocks.SIREN_PORTAL.get())) p = p.below();
            return p;
        }
        return build(level, target);
    }

    /** Build a ruby-framed portal (2x3 inside) on land near the target, or on a platform at sea level. */
    public static BlockPos build(ServerLevel level, BlockPos target) {
        BlockPos base = null;
        // Look for dry ground nearby, closest first.
        outer:
        for (int r = 0; r <= 24; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = target.getX() + dx, z = target.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos ground = new BlockPos(x, y - 1, z);
                    BlockState g = level.getBlockState(ground);
                    if (g.getFluidState().isEmpty() && g.isSolid() && y > level.getSeaLevel() - 1 && y < level.getMaxBuildHeight() - 10) {
                        base = ground;
                        break outer;
                    }
                }
            }
        }
        boolean platform = base == null;
        if (platform) {
            int y = Math.max(level.getSeaLevel(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ()));
            base = new BlockPos(target.getX(), y, target.getZ());
        }

        Direction right = Direction.EAST;
        // Floor: a 6x4 deck around the frame so there's somewhere to stand.
        for (int dx = -2; dx <= 3; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                BlockPos f = base.offset(dx, 0, dz);
                if (platform || !level.getBlockState(f).isSolid()) level.setBlock(f, Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
                for (int h = 1; h <= 5; h++) {
                    BlockPos a = f.above(h);
                    if (!level.getBlockState(a).isAir()) level.setBlock(a, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        // Frame: 4 wide x 5 tall, inside 2x3.
        BlockPos corner = base.above().relative(right.getOpposite());
        BlockState ruby = ModBlocks.RUBY_BLOCK.get().defaultBlockState();
        for (int w = 0; w < 4; w++) {
            level.setBlock(corner.relative(right, w).below(), ruby, 3);
            level.setBlock(corner.relative(right, w).above(3), ruby, 3);
        }
        for (int h = 0; h < 3; h++) {
            level.setBlock(corner.above(h), ruby, 3);
            level.setBlock(corner.relative(right, 3).above(h), ruby, 3);
        }
        BlockState portal = ModBlocks.SIREN_PORTAL.get().defaultBlockState().setValue(SirenPortalBlock.AXIS, Direction.Axis.X);
        for (int w = 1; w <= 2; w++) for (int h = 0; h < 3; h++) level.setBlock(corner.relative(right, w).above(h), portal, 2 | 16);
        return corner.relative(right, 1);
    }
}
