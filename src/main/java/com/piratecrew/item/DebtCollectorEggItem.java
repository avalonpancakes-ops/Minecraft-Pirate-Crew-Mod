package com.piratecrew.item;

import com.piratecrew.entity.BountyHunterEntity;
import com.piratecrew.entity.PirateTier;
import com.piratecrew.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Testing egg: spawns a fully equipped debt collector of one tier that hunts whoever used the egg.
 * It doesn't touch anyone's loan: it can't collect anything and leaves if its target logs off.
 */
public class DebtCollectorEggItem extends ForgeSpawnEggItem {
    private final PirateTier tier;

    public DebtCollectorEggItem(PirateTier tier, int highlight, Properties props) {
        super(ModEntities.BOUNTY_HUNTER, 0x4A0D0D, highlight, props);
        this.tier = tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (!(level instanceof ServerLevel sl) || player == null) return InteractionResult.SUCCESS;
        BlockPos clicked = ctx.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(ctx.getClickedFace());
        BountyHunterEntity h = ModEntities.BOUNTY_HUNTER.get().create(sl);
        if (h == null) return InteractionResult.FAIL;
        h.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180F, 0);
        h.setupHunter(tier, player.getUUID(), -1);
        h.setTest(true);
        h.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null, null);
        sl.addFreshEntity(h);
        if (!player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        if (player.isCreative()) {
            player.displayClientMessage(Component.literal("Test debt collector spawned. Switch to survival to fight it.").withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.CONSUME;
    }

    /** No spawning into water or the air: aim at a block. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Testing: hunts whoever uses it, leaves loans alone").withStyle(ChatFormatting.GRAY));
    }
}
