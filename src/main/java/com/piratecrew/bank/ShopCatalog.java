package com.piratecrew.bank;

import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * What the banker sells, for rubies. The same list is built on client and server, so a purchase is
 * sent as an index into it. Prices also set what bounty hunters value items at (see RubyValues).
 */
public class ShopCatalog {
    public enum Category {
        FOOD("Food"), MATERIALS("Materials"), COMBAT("Combat"), GEAR("Gear"), BOOKS("Books"), MISC("Misc");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public record Entry(int index, Category category, Supplier<ItemStack> stack, int price) {
        public ItemStack make() {
            return stack.get();
        }
    }

    private static List<Entry> entries;

    public static synchronized List<Entry> all() {
        if (entries == null) build();
        return entries;
    }

    public static Entry get(int index) {
        List<Entry> all = all();
        return index >= 0 && index < all.size() ? all.get(index) : null;
    }

    public static List<Entry> in(Category c) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all()) if (e.category == c) out.add(e);
        return out;
    }

    private static void build() {
        List<Entry> l = new ArrayList<>();
        Builder b = new Builder(l);

        b.cat(Category.FOOD)
                .add(Items.BREAD, 8, 3)
                .add(Items.BAKED_POTATO, 16, 4)
                .add(Items.COOKED_BEEF, 8, 5)
                .add(Items.COOKED_COD, 8, 4)
                .add(Items.CAKE, 1, 4)
                .add(Items.GOLDEN_CARROT, 8, 10)
                .add(Items.GOLDEN_APPLE, 1, 20)
                .add(Items.ENCHANTED_GOLDEN_APPLE, 1, 200);

        b.cat(Category.MATERIALS)
                .add(Items.OAK_LOG, 32, 4)
                .add(Items.COAL, 16, 4)
                .add(Items.COPPER_INGOT, 16, 4)
                .add(Items.IRON_INGOT, 8, 10)
                .add(Items.GOLD_INGOT, 8, 12)
                .add(Items.REDSTONE, 16, 5)
                .add(Items.LAPIS_LAZULI, 16, 6)
                .add(Items.EMERALD, 1, 6)
                .add(Items.DIAMOND, 1, 18)
                .add(Items.NETHERITE_INGOT, 1, 140)
                .add(Items.OBSIDIAN, 8, 12)
                .add(Items.STRING, 16, 4)
                .add(Items.LEATHER, 8, 5)
                .add(Items.GUNPOWDER, 8, 8);

        b.cat(Category.COMBAT)
                .add(Items.ARROW, 32, 5)
                .add(Items.SPECTRAL_ARROW, 16, 8)
                .add(Items.BOW, 1, 6)
                .add(Items.CROSSBOW, 1, 9)
                .add(Items.SHIELD, 1, 6)
                .add(Items.IRON_SWORD, 1, 8)
                .add(Items.DIAMOND_SWORD, 1, 45)
                .add(Items.TRIDENT, 1, 120)
                .add(Items.TOTEM_OF_UNDYING, 1, 160)
                .add(Items.TNT, 4, 12)
                .add(Items.FIREWORK_ROCKET, 16, 10);

        b.cat(Category.GEAR)
                .add(Items.IRON_HELMET, 1, 12)
                .add(Items.IRON_CHESTPLATE, 1, 18)
                .add(Items.IRON_LEGGINGS, 1, 16)
                .add(Items.IRON_BOOTS, 1, 10)
                .add(Items.DIAMOND_HELMET, 1, 75)
                .add(Items.DIAMOND_CHESTPLATE, 1, 120)
                .add(Items.DIAMOND_LEGGINGS, 1, 105)
                .add(Items.DIAMOND_BOOTS, 1, 60)
                .add(Items.IRON_PICKAXE, 1, 9)
                .add(Items.DIAMOND_PICKAXE, 1, 55)
                .add(Items.DIAMOND_AXE, 1, 55)
                .add(Items.FISHING_ROD, 1, 4)
                .add(Items.SHEARS, 1, 4)
                .add(Items.BUCKET, 1, 6)
                .add(Items.FLINT_AND_STEEL, 1, 3);

        b.cat(Category.BOOKS)
                .book(Enchantments.MENDING, 1, 120)
                .book(Enchantments.UNBREAKING, 3, 45)
                .book(Enchantments.BLOCK_EFFICIENCY, 5, 60)
                .book(Enchantments.BLOCK_FORTUNE, 3, 80)
                .book(Enchantments.SILK_TOUCH, 1, 50)
                .book(Enchantments.SHARPNESS, 5, 70)
                .book(Enchantments.ALL_DAMAGE_PROTECTION, 4, 60)
                .book(Enchantments.POWER_ARROWS, 5, 60)
                .book(Enchantments.INFINITY_ARROWS, 1, 70)
                .book(Enchantments.MOB_LOOTING, 3, 80)
                .book(Enchantments.FALL_PROTECTION, 4, 35)
                .book(Enchantments.RESPIRATION, 3, 30)
                .book(Enchantments.DEPTH_STRIDER, 3, 35);

        b.cat(Category.MISC)
                .add(Items.TORCH, 64, 3)
                .add(Items.OAK_BOAT, 1, 2)
                .add(Items.COMPASS, 1, 4)
                .add(Items.MAP, 1, 3)
                .add(Items.SPYGLASS, 1, 8)
                .add(Items.LEAD, 2, 4)
                .add(Items.NAME_TAG, 1, 12)
                .add(Items.SADDLE, 1, 15)
                .add(Items.ENDER_PEARL, 4, 12)
                .add(Items.EXPERIENCE_BOTTLE, 16, 20)
                .add(Items.NAUTILUS_SHELL, 1, 10)
                .add(Items.HEART_OF_THE_SEA, 1, 80);

        entries = Collections.unmodifiableList(l);
    }

    private static class Builder {
        private final List<Entry> list;
        private Category cat = Category.MISC;

        Builder(List<Entry> list) {
            this.list = list;
        }

        Builder cat(Category c) {
            this.cat = c;
            return this;
        }

        Builder add(Item item, int count, int price) {
            list.add(new Entry(list.size(), cat, () -> new ItemStack(item, count), price));
            return this;
        }

        Builder book(Enchantment ench, int level, int price) {
            list.add(new Entry(list.size(), cat, () -> EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ench, level)), price));
            return this;
        }
    }
}
