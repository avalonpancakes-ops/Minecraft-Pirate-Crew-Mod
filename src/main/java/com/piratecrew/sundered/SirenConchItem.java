package com.piratecrew.sundered;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The ticket to the Sundered Sea (1,000 rubies at any bank). Sound it inside a ruby-block frame
 * shaped like a nether portal and the sea answers.
 */
public class SirenConchItem extends Item {
    public SirenConchItem(Properties props) {
        super(props.stacksTo(16).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos inside = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!PortalShape.light(level, inside) && !PortalShape.light(level, ctx.getClickedPos())) {
            if (player != null) player.displayClientMessage(Component.literal(
                    "Sound the conch inside a frame of ruby blocks shaped like a nether portal.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        level.playSound(null, inside, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.5F, 0.8F);
        level.playSound(null, inside, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.BLOCKS, 0.6F, 1.4F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.NAUTILUS, inside.getX() + 0.5, inside.getY() + 1.0, inside.getZ() + 0.5, 60, 0.6, 1.2, 0.6, 0.6);
        }
        if (player != null) {
            if (!player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            player.displayClientMessage(Component.literal("The Sundered Sea answers the siren's call...").withStyle(ChatFormatting.DARK_AQUA), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Ticket to the Sundered Sea").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("Use inside a ruby-block frame shaped").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("like a nether portal to open it.").withStyle(ChatFormatting.GRAY));
    }
}
