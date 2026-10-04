package com.piratecrew.registry;

import com.piratecrew.PirateCrew;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PirateCrew.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.piratecrew"))
            .icon(() -> new ItemStack(ModItems.RUBY.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.RUBY.get());
                out.accept(ModItems.RUBY_ORE.get());
                out.accept(ModItems.DEEPSLATE_RUBY_ORE.get());
                out.accept(ModItems.RUBY_BLOCK.get());
                out.accept(ModItems.BOUNTY_BOARD.get());
                out.accept(ModItems.RUBY_SWORD.get());
                out.accept(ModItems.RUBY_PICKAXE.get());
                out.accept(ModItems.RUBY_AXE.get());
                out.accept(ModItems.RUBY_SHOVEL.get());
                out.accept(ModItems.RUBY_HOE.get());
                out.accept(ModItems.RUBY_HELMET.get());
                out.accept(ModItems.RUBY_CHESTPLATE.get());
                out.accept(ModItems.RUBY_LEGGINGS.get());
                out.accept(ModItems.RUBY_BOOTS.get());
                out.accept(ModItems.PIRATE_SPAWN_EGG.get());
            })
            .build());
}
