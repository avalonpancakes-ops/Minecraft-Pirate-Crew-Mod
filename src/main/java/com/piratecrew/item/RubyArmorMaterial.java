package com.piratecrew.item;

import com.piratecrew.PirateCrew;
import com.piratecrew.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Armor points (boots/legs/chest/helmet):
 *   Iron 2/5/6/2 = 15, toughness 0
 *   Ruby 2/6/7/3 = 18, toughness 1
 *   Diamond 3/6/8/3 = 20, toughness 2
 */
public class RubyArmorMaterial implements ArmorMaterial {
    public static final RubyArmorMaterial INSTANCE = new RubyArmorMaterial();

    // Same base durability table vanilla uses: boots, leggings, chestplate, helmet
    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};
    private static final int DURABILITY_MULTIPLIER = 25; // iron 15, diamond 33

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[index(type)] * DURABILITY_MULTIPLIER;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 2;
            case LEGGINGS -> 6;
            case CHESTPLATE -> 7;
            case HELMET -> 3;
        };
    }

    private static int index(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 0;
            case LEGGINGS -> 1;
            case CHESTPLATE -> 2;
            case HELMET -> 3;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.RUBY.get());
    }

    @Override
    public String getName() {
        // Forge resolves "modid:name" to assets/modid/textures/models/armor/name_layer_N.png
        return PirateCrew.MODID + ":ruby";
    }

    @Override
    public float getToughness() {
        return 1.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
