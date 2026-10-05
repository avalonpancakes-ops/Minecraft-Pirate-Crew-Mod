package com.piratecrew.registry;

import com.piratecrew.PirateCrew;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The Jolly Roger: a skull-and-crossbones banner pattern, applied at a loom with its pattern item. */
public class ModBannerPatterns {
    public static final DeferredRegister<BannerPattern> PATTERNS = DeferredRegister.create(Registries.BANNER_PATTERN, PirateCrew.MODID);
    public static final String JOLLY_ROGER_HASH = "pcjolly";
    public static final RegistryObject<BannerPattern> JOLLY_ROGER = PATTERNS.register("jolly_roger", () -> new BannerPattern(JOLLY_ROGER_HASH));
    public static final TagKey<BannerPattern> JOLLY_ROGER_ITEM_TAG = TagKey.create(Registries.BANNER_PATTERN, PirateCrew.id("pattern_item/jolly_roger"));

    /** A black banner with a white Jolly Roger, as raiders fly it. */
    public static ItemStack jollyRogerBanner() {
        ItemStack stack = new ItemStack(Items.BLACK_BANNER);
        ListTag patterns = new ListTag();
        CompoundTag p = new CompoundTag();
        p.putString("Pattern", JOLLY_ROGER_HASH);
        p.putInt("Color", DyeColor.WHITE.getId());
        patterns.add(p);
        CompoundTag be = new CompoundTag();
        be.put("Patterns", patterns);
        stack.addTagElement("BlockEntityTag", be);
        return stack;
    }
}
