package com.piratecrew.bank;

import com.piratecrew.registry.ModItems;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * What one of an item is worth in rubies, for bounty hunters collecting a debt. Anything the banker
 * sells is worth its shop price; other valuables (netherite gear, elytra, storage blocks...) have
 * their own values. Enchantments add to an item's worth. Everyday blocks are worth nothing.
 */
public class RubyValues {
    private static Map<Item, Double> table;
    private static Map<String, Double> books;

    private static synchronized void build() {
        if (table != null) return;
        Map<Item, Double> t = new HashMap<>();
        Map<String, Double> b = new HashMap<>();
        for (ShopCatalog.Entry e : ShopCatalog.all()) {
            ItemStack s = e.make();
            if (s.getItem() instanceof EnchantedBookItem) {
                Map<Enchantment, Integer> ench = EnchantmentHelper.getEnchantments(s);
                if (ench.size() == 1) {
                    var en = ench.entrySet().iterator().next();
                    b.put(bookKey(en.getKey(), en.getValue()), (double) e.price());
                }
            } else {
                t.put(s.getItem(), e.price() / (double) s.getCount());
            }
        }
        // Currency
        t.put(ModItems.RUBY.get(), 1.0);
        t.put(ModItems.RUBY_BLOCK.get(), 9.0);
        // Storage blocks and raw valuables
        t.put(Items.DIAMOND_BLOCK, 162.0);
        t.put(Items.EMERALD_BLOCK, 54.0);
        t.put(Items.IRON_BLOCK, 11.0);
        t.put(Items.GOLD_BLOCK, 13.5);
        t.put(Items.NETHERITE_BLOCK, 1260.0);
        t.put(Items.LAPIS_BLOCK, 3.4);
        t.put(Items.REDSTONE_BLOCK, 2.8);
        t.put(Items.COAL_BLOCK, 2.2);
        t.put(Items.RAW_IRON, 1.0);
        t.put(Items.RAW_GOLD, 1.2);
        t.put(Items.ANCIENT_DEBRIS, 35.0);
        t.put(Items.NETHERITE_SCRAP, 35.0);
        // Gear the shop doesn't sell
        t.put(Items.DIAMOND_SHOVEL, 20.0);
        t.put(Items.DIAMOND_HOE, 37.0);
        t.put(Items.IRON_AXE, 5.0);
        t.put(Items.IRON_SHOVEL, 2.0);
        t.put(Items.IRON_HOE, 3.0);
        t.put(Items.CHAINMAIL_HELMET, 6.0);
        t.put(Items.CHAINMAIL_CHESTPLATE, 6.0);
        t.put(Items.CHAINMAIL_LEGGINGS, 6.0);
        t.put(Items.CHAINMAIL_BOOTS, 6.0);
        t.put(Items.NETHERITE_SWORD, 185.0);
        t.put(Items.NETHERITE_PICKAXE, 195.0);
        t.put(Items.NETHERITE_AXE, 195.0);
        t.put(Items.NETHERITE_SHOVEL, 160.0);
        t.put(Items.NETHERITE_HOE, 177.0);
        t.put(Items.NETHERITE_HELMET, 215.0);
        t.put(Items.NETHERITE_CHESTPLATE, 260.0);
        t.put(Items.NETHERITE_LEGGINGS, 245.0);
        t.put(Items.NETHERITE_BOOTS, 200.0);
        t.put(ModItems.RUBY_SWORD.get(), 2.5);
        t.put(ModItems.RUBY_PICKAXE.get(), 3.5);
        t.put(ModItems.RUBY_AXE.get(), 3.5);
        t.put(ModItems.RUBY_SHOVEL.get(), 1.5);
        t.put(ModItems.RUBY_HOE.get(), 2.5);
        t.put(ModItems.RUBY_HELMET.get(), 5.0);
        t.put(ModItems.RUBY_CHESTPLATE.get(), 8.0);
        t.put(ModItems.RUBY_LEGGINGS.get(), 7.0);
        t.put(ModItems.RUBY_BOOTS.get(), 4.0);
        t.put(Items.DIAMOND_HORSE_ARMOR, 40.0);
        t.put(Items.GOLDEN_HORSE_ARMOR, 15.0);
        t.put(Items.IRON_HORSE_ARMOR, 10.0);
        // Rare finds
        t.put(Items.ELYTRA, 250.0);
        t.put(Items.NETHER_STAR, 250.0);
        t.put(Items.BEACON, 300.0);
        t.put(Items.CONDUIT, 100.0);
        t.put(Items.SHULKER_SHELL, 20.0);
        t.put(Items.SHULKER_BOX, 40.0);
        t.put(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 60.0);
        t.put(Items.ENCHANTING_TABLE, 40.0);
        t.put(Items.ANVIL, 25.0);
        t.put(Items.BLAZE_ROD, 2.0);
        t.put(Items.GHAST_TEAR, 4.0);
        t.put(Items.ENDER_EYE, 4.0);
        books = b;
        table = t;
    }

    private static String bookKey(Enchantment e, int level) {
        return e.getDescriptionId() + "#" + level;
    }

    /** Rubies one of this item is worth (0 = the hunters don't want it). */
    public static double unitValue(ItemStack s) {
        if (s.isEmpty()) return 0;
        build();
        Map<Enchantment, Integer> ench = EnchantmentHelper.getEnchantments(s);
        if (s.getItem() instanceof EnchantedBookItem) {
            double v = 0;
            for (var en : ench.entrySet()) {
                if (en.getKey().isCurse()) continue;
                Double shop = books.get(bookKey(en.getKey(), en.getValue()));
                v += shop != null ? shop : en.getValue() * (en.getKey().isTreasureOnly() ? 30.0 : 12.0);
            }
            return v;
        }
        double v = table.getOrDefault(s.getItem(), 0.0);
        for (var en : ench.entrySet()) {
            if (!en.getKey().isCurse()) v += en.getValue() * (en.getKey().isTreasureOnly() ? 20.0 : 4.0);
        }
        return v;
    }
}
