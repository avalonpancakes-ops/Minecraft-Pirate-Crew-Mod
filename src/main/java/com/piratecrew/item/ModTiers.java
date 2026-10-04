package com.piratecrew.item;

import com.piratecrew.PirateCrew;
import com.piratecrew.registry.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

/**
 * Ruby sits between iron and diamond.
 *   Iron:    250 uses, speed 6, +2 dmg, ench 14
 *   Ruby:    900 uses, speed 7, +2.5 dmg, ench 16
 *   Diamond: 1561 uses, speed 8, +3 dmg, ench 10
 */
public class ModTiers {
    public static final TagKey<Block> NEEDS_RUBY_TOOL = BlockTags.create(PirateCrew.id("needs_ruby_tool"));

    public static final Tier RUBY = TierSortingRegistry.registerTier(
            new ForgeTier(2, 900, 7.0F, 2.5F, 16, NEEDS_RUBY_TOOL, () -> Ingredient.of(ModItems.RUBY.get())),
            PirateCrew.id("ruby"), List.<Object>of(Tiers.IRON), List.<Object>of(Tiers.DIAMOND));
}
