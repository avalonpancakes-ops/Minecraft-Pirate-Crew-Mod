package com.piratecrew.cannon;

import com.piratecrew.registry.ModEntities;
import com.piratecrew.registry.ModItems;
import com.piratecrew.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A ship's cannon. Right-click with a cannonball to fire it the way the barrel points; a redstone
 * pulse fires it too (no ammo needed, so it can guard a deck). Cannonballs burst on impact, hurting
 * whatever they hit without breaking blocks.
 */
public class CannonBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 11, 14);

    public CannonBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // the barrel points the way the player is looking
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.CANNONBALL.get())) return InteractionResult.PASS;
        if (!level.isClientSide) {
            fire((ServerLevel) level, pos, state, player);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) com.piratecrew.goals.Goals.grant(sp, com.piratecrew.goals.Goal.CANNON);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered && !state.getValue(POWERED)) fire((ServerLevel) level, pos, state, null);
        if (powered != state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, powered), 3);
    }

    public static void fire(ServerLevel level, BlockPos pos, BlockState state, @Nullable LivingEntity gunner) {
        Direction dir = state.getValue(FACING);
        Vec3 muzzle = Vec3.atCenterOf(pos).add(dir.getStepX() * 0.95, 0.25, dir.getStepZ() * 0.95);
        CannonballEntity ball = new CannonballEntity(ModEntities.CANNONBALL.get(), level);
        ball.setPos(muzzle.x, muzzle.y, muzzle.z);
        if (gunner != null) ball.setOwner(gunner);
        ball.shoot(dir.getStepX(), 0.16, dir.getStepZ(), 2.4F, 0.6F);
        level.addFreshEntity(ball);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 14, 0.15, 0.15, 0.15, 0.06);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 8, 0.1, 0.1, 0.1, 0.05);
        level.sendParticles(ParticleTypes.POOF, muzzle.x + dir.getStepX() * 0.5, muzzle.y, muzzle.z + dir.getStepZ() * 0.5, 10, 0.2, 0.2, 0.2, 0.08);
        level.playSound(null, pos, ModSounds.CANNON_FIRE.get(), SoundSource.BLOCKS, 3.0F, 0.9F + level.random.nextFloat() * 0.2F);
    }
}
