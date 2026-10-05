package com.piratecrew.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Creative-only testing tool that sets mobs on each other.
 * Right-click a mob to pick it (it glows), then right-click another: they fight.
 * Sneak + right-click a mob: every mob within 16 blocks goes for it.
 * Sneak + right-click the air: clear the pick.
 */
public class AggroStickItem extends Item {
    private static final String TAG = "AggroPick";

    public AggroStickItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID(TAG);
    }

    /** Called from the entity-interact event, before the mob's own right-click (trading, recruiting...). */
    public static InteractionResult useOnEntity(ItemStack stack, Player player, LivingEntity target) {
        if (!player.isCreative()) {
            if (!player.level().isClientSide) player.displayClientMessage(Component.literal("The aggro stick only works in creative mode.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!(player.level() instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        if (!(target instanceof Mob mob)) {
            player.displayClientMessage(Component.literal("Only mobs can be set to fight.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            // Everything nearby piles on this one.
            List<Mob> others = sl.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(16), m -> m != mob && m.isAlive());
            for (Mob m : others) provoke(m, mob);
            clear(stack, sl);
            sl.playSound(null, mob.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6F, 1.4F);
            player.displayClientMessage(Component.literal(others.size() + " mobs are going after ").withStyle(ChatFormatting.GOLD)
                    .append(mob.getDisplayName()), true);
            return InteractionResult.CONSUME;
        }

        Entity picked = picked(stack, sl);
        if (picked == null || picked == mob || !picked.isAlive()) {
            clear(stack, sl);
            stack.getOrCreateTag().putUUID(TAG, mob.getUUID());
            mob.setGlowingTag(true);
            sl.playSound(null, mob.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.6F);
            player.displayClientMessage(Component.literal("Picked ").withStyle(ChatFormatting.YELLOW).append(mob.getDisplayName())
                    .append(Component.literal(". Right-click another mob to make them fight.").withStyle(ChatFormatting.YELLOW)), true);
            return InteractionResult.CONSUME;
        }

        if (picked instanceof Mob a) {
            provoke(a, mob);
            provoke(mob, a);
            sl.playSound(null, mob.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6F, 1.4F);
            player.displayClientMessage(a.getDisplayName().copy().append(Component.literal(" vs ").withStyle(ChatFormatting.RED))
                    .append(mob.getDisplayName()).append(Component.literal(". Fight!").withStyle(ChatFormatting.RED)), true);
        }
        clear(stack, sl);
        return InteractionResult.CONSUME;
    }

    /** Make {@code attacker} go for {@code victim} and treat it as the one who started it. */
    private static void provoke(Mob attacker, LivingEntity victim) {
        attacker.setTarget(victim);
        attacker.setLastHurtByMob(victim);
        if (attacker instanceof net.minecraft.world.entity.NeutralMob neutral) {
            neutral.setPersistentAngerTarget(victim.getUUID());
            neutral.startPersistentAngerTimer();
        }
    }

    @Nullable
    private static Entity picked(ItemStack stack, ServerLevel sl) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG)) return null;
        return sl.getEntity(tag.getUUID(TAG));
    }

    private static void clear(ItemStack stack, ServerLevel sl) {
        Entity e = picked(stack, sl);
        if (e != null) e.setGlowingTag(false);
        if (stack.hasTag()) stack.getTag().remove(TAG);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && level instanceof ServerLevel sl && stack.hasTag() && stack.getTag().hasUUID(TAG)) {
            clear(stack, sl);
            player.displayClientMessage(Component.literal("Pick cleared.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Creative only").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Right-click a mob, then another: they fight").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + right-click a mob: everything nearby attacks it").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + right-click air: clear the pick").withStyle(ChatFormatting.GRAY));
    }
}
