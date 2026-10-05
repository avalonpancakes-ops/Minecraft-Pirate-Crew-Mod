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
                out.accept(ModItems.BANK_COUNTER.get());
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
                out.accept(ModItems.DEBT_COLLECTOR_EGG_F.get());
                out.accept(ModItems.DEBT_COLLECTOR_EGG_D.get());
                out.accept(ModItems.DEBT_COLLECTOR_EGG_C.get());
                out.accept(ModItems.DEBT_COLLECTOR_EGG_B.get());
                out.accept(ModItems.DEBT_COLLECTOR_EGG_A.get());
                out.accept(ModItems.DEBT_COLLECTOR_EGG_S.get());
                out.accept(ModItems.CAPTAINS_LOG.get());
                out.accept(ModItems.AGGRO_STICK.get());
                out.accept(ModItems.SIREN_CONCH.get());
                out.accept(ModItems.MARINE_SPAWN_EGG.get());
                out.accept(ModItems.TIDESTEEL_ORE.get());
                out.accept(ModItems.DEEPSLATE_TIDESTEEL_ORE.get());
                out.accept(ModItems.ABYSSAL_ORE.get());
                out.accept(ModItems.STORMGLASS_ORE.get());
                for (var m : java.util.List.of(ModItems.RAW_TIDESTEEL, ModItems.TIDESTEEL_INGOT, ModItems.ABYSSAL_SHARD, ModItems.ABYSSAL_INGOT,
                        ModItems.KRAKEN_BONE, ModItems.KRAKENBONE_INGOT, ModItems.STORMGLASS_SHARD, ModItems.STORM_CORE, ModItems.STORMFORGED_INGOT,
                        ModItems.LEVIATHAN_SCALE, ModItems.LEVIATHAN_INGOT, ModItems.SOVEREIGN_HEART, ModItems.SOVEREIGN_INGOT,
                        ModItems.MARINE_BADGE, ModItems.COMMODORE_INSIGNIA)) out.accept(m.get());
                for (var set : ModItems.GEAR.values()) for (var item : set.all()) out.accept(item.get());
                for (var m : java.util.List.of(ModItems.SIGNAL_FLARE, ModItems.KRAKEN_LURE, ModItems.STORM_SIGIL, ModItems.LEVIATHAN_HORN,
                        ModItems.ADMIRALS_WARRANT, ModItems.COMMODORE_SPAWN_EGG, ModItems.KRAKEN_SPAWN_EGG, ModItems.TEMPEST_ADMIRAL_SPAWN_EGG,
                        ModItems.LEVIATHAN_SPAWN_EGG, ModItems.FLEET_ADMIRAL_SPAWN_EGG)) out.accept(m.get());
                for (var p : ModItems.SOUL_PACTS.values()) out.accept(p.get());
            })
            .build());
}
