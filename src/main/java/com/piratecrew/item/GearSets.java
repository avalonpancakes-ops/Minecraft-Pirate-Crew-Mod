package com.piratecrew.item;

import com.piratecrew.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Full-set bonuses for the Sundered Sea armor, and the flavour line each piece carries. A bonus
 * applies to players and crew pirates wearing all four pieces of one tier.
 */
public class GearSets {
    public record Bonus(String text, List<MobEffectInstance> effects) {}

    public static String lore(GearTier t) {
        return switch (t) {
            case TIDESTEEL -> "Sea-forged steel that never rusts.";
            case ABYSSAL -> "Smelted from the lightless deep.";
            case KRAKENBONE -> "Carved from the Kraken's own bones.";
            case STORMFORGED -> "Tempered in Sorel's lightning.";
            case LEVIATHAN -> "Scaled in the Leviathan's hide.";
            case SOVEREIGN -> "Worthy of the Iron Tide himself.";
        };
    }

    private static MobEffectInstance quiet(MobEffect e, int amp) {
        return new MobEffectInstance(e, 60, amp, true, false, true);
    }

    public static Bonus bonus(GearTier t) {
        return switch (t) {
            case TIDESTEEL -> new Bonus("Water Breathing", List.of(quiet(MobEffects.WATER_BREATHING, 0)));
            case ABYSSAL -> new Bonus("Night Vision", List.of(quiet(MobEffects.NIGHT_VISION, 0)));
            case KRAKENBONE -> new Bonus("Dolphin's Grace and Water Breathing",
                    List.of(quiet(MobEffects.DOLPHINS_GRACE, 0), quiet(MobEffects.WATER_BREATHING, 0)));
            case STORMFORGED -> new Bonus("Speed and Jump Boost", List.of(quiet(MobEffects.MOVEMENT_SPEED, 0), quiet(MobEffects.JUMP, 0)));
            case LEVIATHAN -> new Bonus("Conduit Power and Dolphin's Grace",
                    List.of(quiet(MobEffects.CONDUIT_POWER, 0), quiet(MobEffects.DOLPHINS_GRACE, 0)));
            case SOVEREIGN -> new Bonus("Strength, Fire Resistance and Regeneration",
                    List.of(quiet(MobEffects.DAMAGE_BOOST, 0), quiet(MobEffects.FIRE_RESISTANCE, 0), quiet(MobEffects.REGENERATION, 0)));
        };
    }

    @Nullable
    public static GearTier tierOf(Item item) {
        for (GearTier t : GearTier.values()) {
            for (var r : ModItems.GEAR.get(t).all()) if (r.get() == item) return t;
        }
        return null;
    }

    /** The tier of a complete four-piece set being worn, or null. */
    @Nullable
    public static GearTier fullSet(LivingEntity e) {
        for (GearTier t : GearTier.values()) {
            var set = ModItems.GEAR.get(t);
            if (e.getItemBySlot(EquipmentSlot.HEAD).is(set.helmet().get()) && e.getItemBySlot(EquipmentSlot.CHEST).is(set.chestplate().get())
                    && e.getItemBySlot(EquipmentSlot.LEGS).is(set.leggings().get()) && e.getItemBySlot(EquipmentSlot.FEET).is(set.boots().get())) {
                return t;
            }
        }
        return null;
    }

    /** Called every second for players and crew pirates. */
    public static void apply(LivingEntity e) {
        GearTier t = fullSet(e);
        if (t == null) return;
        for (MobEffectInstance m : bonus(t).effects()) e.addEffect(new MobEffectInstance(m));
    }

    public static void tooltip(ItemStack stack, List<Component> lines) {
        GearTier t = tierOf(stack.getItem());
        if (t == null) return;
        lines.add(1, Component.literal(lore(t)).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem) {
            lines.add(2, Component.literal("Full set: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(bonus(t).text()).withStyle(ChatFormatting.YELLOW)));
        }
    }
}
