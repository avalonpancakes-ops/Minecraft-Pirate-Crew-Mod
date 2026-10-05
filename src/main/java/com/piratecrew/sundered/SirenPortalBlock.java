package com.piratecrew.sundered;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The swirling sea-green gateway inside a lit ruby frame. Stand in it for a few seconds (instantly
 * in creative) to cross between the Overworld and the Sundered Sea.
 */
public class SirenPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_SHAPE = Block.box(6, 0, 0, 10, 16, 16);
    private static final int PLAYER_WAIT = 80;

    /** entity -> {last game tick seen inside, ticks inside} */
    private static final Map<UUID, long[]> INSIDE = new HashMap<>();

    public SirenPortalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    /** Breaks (and so does the rest of the portal) when the frame or a neighbouring portal block is gone. */
    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean relevant = dir.getAxis() == Direction.Axis.Y || dir.getAxis() == axis;
        if (relevant && !neighbor.is(this) && !PortalShape.isFrame(neighbor)) return Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;
        if (entity.isPassenger() || entity.isVehicle() || !entity.canChangeDimensions() || entity.isOnPortalCooldown()) return;
        long now = level.getGameTime();
        long[] t = INSIDE.computeIfAbsent(entity.getUUID(), k -> new long[]{now, 0});
        if (t[0] == now) return; // already counted this tick (standing in two blocks)
        t[1] = now - t[0] <= 2 ? t[1] + 1 : 1;
        t[0] = now;
        boolean instant = !(entity instanceof Player p) || p.getAbilities().invulnerable;
        if (t[1] >= (instant ? 1 : PLAYER_WAIT)) {
            INSIDE.remove(entity.getUUID());
            SirenTeleporter.travel(entity, sl);
        }
        if (INSIDE.size() > 256) INSIDE.entrySet().removeIf(e -> now - e.getValue()[0] > 40);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CONDUIT_AMBIENT,
                    SoundSource.BLOCKS, 0.5F, r.nextFloat() * 0.4F + 0.8F, false);
        }
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + r.nextDouble(), y = pos.getY() + r.nextDouble(), z = pos.getZ() + r.nextDouble();
            level.addParticle(r.nextBoolean() ? ParticleTypes.DOLPHIN : ParticleTypes.BUBBLE_POP, x, y, z,
                    (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
