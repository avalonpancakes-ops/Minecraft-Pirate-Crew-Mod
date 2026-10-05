package com.piratecrew.item;

import com.piratecrew.PirateCrew;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A torn treasure map. Unrolling it picks a stretch of dry land a few hundred blocks away, buries a
 * treasure chest there, and turns into a real map with a red X over the spot.
 */
public class TreasureMapItem extends Item {
    public TreasureMapItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.success(held);
        BlockPos spot = findLand(sl, player.blockPosition());
        if (spot == null) {
            player.displayClientMessage(Component.literal("The ink runs - no land for this map round here. Try again elsewhere.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(held);
        }
        bury(sl, spot);
        ItemStack map = MapItem.create(sl, spot.getX(), spot.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(sl, map);
        MapItemSavedData.addTargetDecoration(map, spot, "+", MapDecoration.Type.RED_X);
        map.setHoverName(Component.literal("Treasure Map").withStyle(ChatFormatting.GOLD));
        sl.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
        int dist = (int) Math.sqrt(spot.distSqr(player.blockPosition()));
        player.displayClientMessage(Component.literal("X marks the spot: about " + dist + " blocks " + compass(player.blockPosition(), spot) + ". Bring a shovel.")
                .withStyle(ChatFormatting.GOLD), false);
        if (!player.getAbilities().instabuild) held.shrink(1);
        if (held.isEmpty()) return InteractionResultHolder.success(map);
        if (!player.getInventory().add(map)) player.drop(map, false);
        return InteractionResultHolder.success(held);
    }

    /** Dry, solid ground above sea level, 250-650 blocks away. */
    @Nullable
    private static BlockPos findLand(ServerLevel level, BlockPos from) {
        var r = level.random;
        for (int attempt = 0; attempt < 24; attempt++) {
            double ang = r.nextDouble() * Math.PI * 2;
            int d = 250 + r.nextInt(400);
            int x = from.getX() + (int) (Math.cos(ang) * d), z = from.getZ() + (int) (Math.sin(ang) * d);
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
            if (y <= level.getSeaLevel() || y >= level.getMaxBuildHeight() - 8) continue;
            BlockPos top = new BlockPos(x, y - 1, z);
            BlockState ground = level.getBlockState(top);
            if (!level.getFluidState(top.above()).isEmpty()) continue;
            if (ground.is(BlockTags.SAND) || ground.is(BlockTags.DIRT) || ground.is(Blocks.GRAVEL)) return top;
        }
        return null;
    }

    private static void bury(ServerLevel level, BlockPos top) {
        BlockPos chest = top.below(3);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 2);
        if (level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity rc) {
            rc.setLootTable(PirateCrew.id("chests/buried_treasure"), level.random.nextLong());
            rc.setCustomName(Component.literal("Buried Treasure"));
        }
        // a hint on the surface: a little cross of coarse dirt / sandstone
        BlockState mark = level.getBlockState(top).is(BlockTags.SAND) ? Blocks.SMOOTH_SANDSTONE.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
        for (Direction dir : Direction.Plane.HORIZONTAL) level.setBlock(top.relative(dir), mark, 2);
        level.setBlock(top, mark, 2);
    }

    private static String compass(BlockPos from, BlockPos to) {
        double ang = Math.toDegrees(Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX()));
        String[] dirs = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};
        return dirs[(int) Math.floorMod(Math.round(ang / 45.0), 8)];
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Use to unroll: a red X marks buried treasure").withStyle(ChatFormatting.GRAY));
    }
}
