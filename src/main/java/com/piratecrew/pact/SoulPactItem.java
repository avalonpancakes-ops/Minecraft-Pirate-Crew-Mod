package com.piratecrew.pact;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/** A Soul Pact scroll. Use it to bind the pact to your soul, or use it on a crew pirate to give it to them. */
public class SoulPactItem extends Item {
    public final SoulPact pact;

    public SoulPactItem(SoulPact pact, Properties props) {
        super(props.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.pact = pact;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withStyle(pact.color);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!SoulPacts.bind((ServerPlayer) player, pact)) return InteractionResultHolder.fail(stack);
        bindEffects((ServerLevel) level, player, pact);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    public static void bindEffects(ServerLevel level, net.minecraft.world.entity.LivingEntity who, SoulPact pact) {
        int c = pact.seal;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F), 1.5F);
        level.sendParticles(dust, who.getX(), who.getY() + 1, who.getZ(), 80, 0.6, 1.0, 0.6, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, who.getX(), who.getY() + 1, who.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
        level.sendParticles(com.piratecrew.registry.ModParticles.GLYPH.get(), who.getX(), who.getY() + 1.2, who.getZ(), 16, 0.8, 0.8, 0.8, 0.02);
        level.playSound(null, who.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 2.0F, 0.7F);
        level.playSound(null, who.blockPosition(), com.piratecrew.registry.ModSounds.PACT_BIND.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Gift: ").withStyle(ChatFormatting.GOLD).append(Component.literal(pact.passiveText).withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.literal("Power: " + pact.power).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" - " + pact.powerText).withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.literal("Crew pirates fight " + (pact.ranged ? "from range" : "up close") + " with it").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Use: bind to your soul (one pact per soul)").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("Use on a crew pirate: give it to them").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("The sea drains every pact").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
    }
}
