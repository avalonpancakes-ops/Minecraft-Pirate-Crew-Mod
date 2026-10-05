package com.piratecrew.item;

import com.piratecrew.sundered.SunderedSea;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Calls one of the Sundered Sea's bosses. Only works in the Sundered Sea (creative players can use
 * it anywhere, for testing). Land bosses appear where you aim; sea bosses need open water at least
 * five blocks deep and rise from it. Only one of each boss can be near at a time.
 */
public class BossSummonItem extends Item {
    private final Supplier<? extends EntityType<? extends Mob>> boss;
    private final boolean water;
    private final String bossName;
    private final String summonLine;

    public BossSummonItem(Supplier<? extends EntityType<? extends Mob>> boss, boolean water, String bossName, String summonLine, Properties props) {
        super(props.stacksTo(16).rarity(Rarity.EPIC).fireResistant());
        this.boss = boss;
        this.water = water;
        this.bossName = bossName;
        this.summonLine = summonLine;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, water ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerLevel server = (ServerLevel) level;

        if (!SunderedSea.isSunderedSea(level) && !player.isCreative()) {
            player.displayClientMessage(Component.literal("Nothing answers here. This only works in the Sundered Sea.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        EntityType<? extends Mob> type = boss.get();
        if (!server.getEntities(type, player.getBoundingBox().inflate(160), e -> e.isAlive()).isEmpty()) {
            player.displayClientMessage(Component.literal(bossName + " is already near.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        Vec3 at = water ? waterSpot(server, player) : landSpot(hit);
        if (at == null) {
            player.displayClientMessage(Component.literal(water ? "Aim at open water, at least five blocks deep." : "Aim at the ground nearby.")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        Mob mob = type.create(server);
        if (mob == null) return InteractionResultHolder.fail(stack);
        mob.moveTo(at.x, at.y, at.z, player.getYRot() + 180F, 0);
        mob.finalizeSpawn(server, server.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null, null);
        mob.setTarget(player);
        server.addFreshEntity(mob);

        server.playSound(null, BlockPos.containing(at), water ? SoundEvents.ELDER_GUARDIAN_CURSE : com.piratecrew.registry.ModSounds.BOSS_HORN.get(), SoundSource.HOSTILE, 4.0F, 0.8F);
        Component msg = Component.literal("☠ " + summonLine).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        for (Player p : server.players()) if (p.distanceToSqr(at) < 96 * 96) p.sendSystemMessage(msg);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(stack);
    }

    @Nullable
    private static Vec3 landSpot(BlockHitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos p = hit.getBlockPos().relative(hit.getDirection());
        return Vec3.atBottomCenterOf(p);
    }

    /** Somewhere 8-20 blocks out along the player's view where the water is deep enough. */
    @Nullable
    private static Vec3 waterSpot(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(1, 0, 0);
        flat = flat.normalize();
        Vec3 best = null;
        for (int d = 8; d <= 24; d += 2) {
            int x = (int) Math.floor(player.getX() + flat.x * d), z = (int) Math.floor(player.getZ() + flat.z * d);
            int surface = level.getSeaLevel() - 1;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, surface, z);
            FluidState top = level.getFluidState(pos);
            if (top.isEmpty()) continue;
            int depth = 0;
            while (depth < 24 && !level.getFluidState(pos).isEmpty()) {
                depth++;
                pos.move(0, -1, 0);
            }
            if (depth >= 5) {
                double y = Math.max(pos.getY() + 1, surface - 8);
                best = new Vec3(x + 0.5, y, z + 0.5);
                if (d >= 12) break;
            }
        }
        return best;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Summons " + bossName).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(water ? "Use while looking out over deep water" : "Use while looking at the ground").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sundered Sea only").withStyle(ChatFormatting.DARK_AQUA));
    }
}
