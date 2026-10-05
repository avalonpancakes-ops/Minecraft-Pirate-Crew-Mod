package com.piratecrew.item;

import com.piratecrew.PirateCrew;
import com.piratecrew.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

/**
 * The Sundered Sea gear tiers, each beyond the last. Every tier has an armor set, a sword, a pickaxe
 * and an axe.
 *
 * <pre>
 *  Tier         Armor H/C/L/B  Tough  KB    Sword  Durability  Comes from
 *  Tidesteel    3/8/7/3  =21   2.5    0.05  8      1800        Tidesteel ore
 *  Abyssal      4/9/7/4  =24   3.5    0.10  10     2400        Abyssal ore (deep)
 *  Krakenbone   5/11/9/5 =30   5      0.12  13     3000        The Kraken
 *  Stormforged  7/14/11/6=38   7      0.15  18     3800        Tempest Admiral + Stormglass ore
 *  Leviathan    10/17/15/9=51  9      0.20  24     5000        The Leviathan
 *  Sovereign    14/20/18/13=65 12     0.30  31     8000        Fleet Admiral Vane
 * </pre>
 * (Vanilla diamond is 3/8/6/3 with sword 7; netherite the same armor, toughness 3, sword 8.)
 */
public enum GearTier {
    //            id             lvl  uses  speed  dmg   ench  boots legs chest helm  durMult tough  kb     fireproof rarity
    TIDESTEEL("tidesteel",       4, 1800, 9.0F,  4.0F, 16, new int[]{3, 7, 8, 3},     42, 2.5F, 0.05F, false, Rarity.UNCOMMON),
    ABYSSAL("abyssal",           5, 2400, 10.0F, 6.0F, 17, new int[]{4, 7, 9, 4},     50, 3.5F, 0.10F, true, Rarity.UNCOMMON),
    KRAKENBONE("krakenbone",     6, 3000, 11.0F, 9.0F, 18, new int[]{5, 9, 11, 5},    58, 5.0F, 0.12F, true, Rarity.RARE),
    STORMFORGED("stormforged",   7, 3800, 12.0F, 14.0F, 20, new int[]{6, 11, 14, 7},  66, 7.0F, 0.15F, true, Rarity.RARE),
    LEVIATHAN("leviathan",       8, 5000, 13.0F, 20.0F, 22, new int[]{9, 15, 17, 10}, 75, 9.0F, 0.20F, true, Rarity.EPIC),
    SOVEREIGN("sovereign",       9, 8000, 15.0F, 27.0F, 25, new int[]{13, 18, 20, 14}, 90, 12.0F, 0.30F, true, Rarity.EPIC);

    public final String id;
    public final int toolLevel;
    public final boolean fireResistant;
    public final Rarity rarity;
    /** Blocks that need at least this tier's pickaxe. */
    public final TagKey<Block> needsTag;
    public final Tier tier;
    public final ArmorMaterial material;

    GearTier(String id, int level, int uses, float speed, float damage, int enchant, int[] defense, int durMult,
             float toughness, float knockback, boolean fireResistant, Rarity rarity) {
        this.id = id;
        this.toolLevel = level;
        this.fireResistant = fireResistant;
        this.rarity = rarity;
        this.needsTag = BlockTags.create(PirateCrew.id("needs_" + id + "_tool"));
        this.tier = new ForgeTier(level, uses, speed, damage, enchant, needsTag, () -> Ingredient.of(ModItems.ingot(this)));
        this.material = new Material(id, defense, durMult, enchant, toughness, knockback, this);
    }

    private static boolean registered;

    /** Put the tiers in Forge's tier order, each after the one before (the first after netherite). */
    public static synchronized void registerTiers() {
        if (registered) return;
        registered = true;
        Object previous = Tiers.NETHERITE;
        for (GearTier t : values()) {
            TierSortingRegistry.registerTier(t.tier, PirateCrew.id(t.id), List.of(previous), List.of());
            previous = t.tier;
        }
    }

    /** Displayed sword damage (1 base + tier bonus + 3 for swords). */
    public int swordDamage() {
        return Math.round(1 + tier.getAttackDamageBonus() + 3);
    }

    private record Material(String id, int[] defense, int durMult, int enchant, float toughness, float knockback,
                            GearTier gear) implements ArmorMaterial {
        private static final int[] BASE = {13, 15, 16, 11}; // boots, leggings, chestplate, helmet

        private static int index(ArmorItem.Type type) {
            return switch (type) {
                case BOOTS -> 0;
                case LEGGINGS -> 1;
                case CHESTPLATE -> 2;
                case HELMET -> 3;
            };
        }

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return BASE[index(type)] * durMult;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return defense[index(type)];
        }

        @Override
        public int getEnchantmentValue() {
            return enchant;
        }

        @Override
        public SoundEvent getEquipSound() {
            return gear.ordinal() >= 3 ? SoundEvents.ARMOR_EQUIP_NETHERITE : SoundEvents.ARMOR_EQUIP_DIAMOND;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.ingot(gear));
        }

        @Override
        public String getName() {
            return PirateCrew.MODID + ":" + id;
        }

        @Override
        public float getToughness() {
            return toughness;
        }

        @Override
        public float getKnockbackResistance() {
            return knockback;
        }
    }
}
