package com.piratecrew.registry;

import com.piratecrew.entity.PirateTier;
import com.piratecrew.item.GearTier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.piratecrew.PirateCrew;
import com.piratecrew.item.ModTiers;
import com.piratecrew.item.RubyArmorMaterial;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PirateCrew.MODID);

    public static final RegistryObject<Item> RUBY = ITEMS.register("ruby", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RUBY_ORE = blockItem("ruby_ore", ModBlocks.RUBY_ORE);
    public static final RegistryObject<Item> DEEPSLATE_RUBY_ORE = blockItem("deepslate_ruby_ore", ModBlocks.DEEPSLATE_RUBY_ORE);
    public static final RegistryObject<Item> RUBY_BLOCK = blockItem("ruby_block", ModBlocks.RUBY_BLOCK);
    public static final RegistryObject<Item> BOUNTY_BOARD = blockItem("bounty_board", ModBlocks.BOUNTY_BOARD);
    public static final RegistryObject<Item> BANK_COUNTER = blockItem("bank_counter", ModBlocks.BANK_COUNTER);

    // Tools & weapons (damage/speed values chosen to sit between iron and diamond)
    public static final RegistryObject<Item> RUBY_SWORD = ITEMS.register("ruby_sword",
            () -> new SwordItem(ModTiers.RUBY, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> RUBY_PICKAXE = ITEMS.register("ruby_pickaxe",
            () -> new PickaxeItem(ModTiers.RUBY, 1, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> RUBY_AXE = ITEMS.register("ruby_axe",
            () -> new AxeItem(ModTiers.RUBY, 5.5F, -3.05F, new Item.Properties()));
    public static final RegistryObject<Item> RUBY_SHOVEL = ITEMS.register("ruby_shovel",
            () -> new ShovelItem(ModTiers.RUBY, 1.5F, -3.0F, new Item.Properties()));
    public static final RegistryObject<Item> RUBY_HOE = ITEMS.register("ruby_hoe",
            () -> new HoeItem(ModTiers.RUBY, -2, -0.5F, new Item.Properties()));

    // Armor
    public static final RegistryObject<Item> RUBY_HELMET = ITEMS.register("ruby_helmet",
            () -> new ArmorItem(RubyArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> RUBY_CHESTPLATE = ITEMS.register("ruby_chestplate",
            () -> new ArmorItem(RubyArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> RUBY_LEGGINGS = ITEMS.register("ruby_leggings",
            () -> new ArmorItem(RubyArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> RUBY_BOOTS = ITEMS.register("ruby_boots",
            () -> new ArmorItem(RubyArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> PIRATE_SPAWN_EGG = ITEMS.register("pirate_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PIRATE, 0x3B2A1A, 0xC0392B, new Item.Properties()));

    public static final RegistryObject<Item> DEBT_COLLECTOR_EGG_F = collectorEgg(PirateTier.F, 0xAAAAAA);
    public static final RegistryObject<Item> DEBT_COLLECTOR_EGG_D = collectorEgg(PirateTier.D, 0x55FF55);
    public static final RegistryObject<Item> DEBT_COLLECTOR_EGG_C = collectorEgg(PirateTier.C, 0x55FFFF);
    public static final RegistryObject<Item> DEBT_COLLECTOR_EGG_B = collectorEgg(PirateTier.B, 0x5555FF);
    public static final RegistryObject<Item> DEBT_COLLECTOR_EGG_A = collectorEgg(PirateTier.A, 0xFF55FF);
    public static final RegistryObject<Item> DEBT_COLLECTOR_EGG_S = collectorEgg(PirateTier.S, 0xFFAA00);

    // ------------------------------------------------------------------ Sundered Sea materials

    public static final RegistryObject<Item> TIDESTEEL_ORE = blockItem("tidesteel_ore", ModBlocks.TIDESTEEL_ORE);
    public static final RegistryObject<Item> DEEPSLATE_TIDESTEEL_ORE = blockItem("deepslate_tidesteel_ore", ModBlocks.DEEPSLATE_TIDESTEEL_ORE);
    public static final RegistryObject<Item> ABYSSAL_ORE = blockItem("abyssal_ore", ModBlocks.ABYSSAL_ORE);
    public static final RegistryObject<Item> STORMGLASS_ORE = blockItem("stormglass_ore", ModBlocks.STORMGLASS_ORE);

    public static final RegistryObject<Item> RAW_TIDESTEEL = material("raw_tidesteel", Rarity.COMMON, false, false);
    public static final RegistryObject<Item> TIDESTEEL_INGOT = material("tidesteel_ingot", Rarity.UNCOMMON, false, false);
    public static final RegistryObject<Item> ABYSSAL_SHARD = material("abyssal_shard", Rarity.UNCOMMON, true, false);
    public static final RegistryObject<Item> ABYSSAL_INGOT = material("abyssal_ingot", Rarity.UNCOMMON, true, false);
    public static final RegistryObject<Item> KRAKEN_BONE = material("kraken_bone", Rarity.RARE, true, false);
    public static final RegistryObject<Item> KRAKENBONE_INGOT = material("krakenbone_ingot", Rarity.RARE, true, false);
    public static final RegistryObject<Item> STORMGLASS_SHARD = material("stormglass_shard", Rarity.RARE, true, false);
    public static final RegistryObject<Item> STORM_CORE = material("storm_core", Rarity.RARE, true, true);
    public static final RegistryObject<Item> STORMFORGED_INGOT = material("stormforged_ingot", Rarity.RARE, true, false);
    public static final RegistryObject<Item> LEVIATHAN_SCALE = material("leviathan_scale", Rarity.EPIC, true, false);
    public static final RegistryObject<Item> LEVIATHAN_INGOT = material("leviathan_ingot", Rarity.EPIC, true, false);
    public static final RegistryObject<Item> SOVEREIGN_HEART = material("sovereign_heart", Rarity.EPIC, true, true);
    public static final RegistryObject<Item> SOVEREIGN_INGOT = material("sovereign_ingot", Rarity.EPIC, true, true);
    public static final RegistryObject<Item> MARINE_BADGE = material("marine_badge", Rarity.COMMON, false, false);
    public static final RegistryObject<Item> COMMODORE_INSIGNIA = material("commodore_insignia", Rarity.RARE, true, true);

    private static RegistryObject<Item> material(String name, Rarity rarity, boolean fireproof, boolean foil) {
        return ITEMS.register(name, () -> {
            Item.Properties p = new Item.Properties().rarity(rarity);
            if (fireproof) p.fireResistant();
            return foil ? new Item(p) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            } : new Item(p);
        });
    }

    /** The ingot that crafts and repairs a tier's gear. */
    public static Item ingot(GearTier t) {
        return switch (t) {
            case TIDESTEEL -> TIDESTEEL_INGOT.get();
            case ABYSSAL -> ABYSSAL_INGOT.get();
            case KRAKENBONE -> KRAKENBONE_INGOT.get();
            case STORMFORGED -> STORMFORGED_INGOT.get();
            case LEVIATHAN -> LEVIATHAN_INGOT.get();
            case SOVEREIGN -> SOVEREIGN_INGOT.get();
        };
    }

    // ------------------------------------------------------------------ Sundered Sea gear

    public record GearSet(RegistryObject<Item> helmet, RegistryObject<Item> chestplate, RegistryObject<Item> leggings,
                          RegistryObject<Item> boots, RegistryObject<Item> sword, RegistryObject<Item> pickaxe,
                          RegistryObject<Item> axe) {
        public List<RegistryObject<Item>> all() {
            return List.of(helmet, chestplate, leggings, boots, sword, pickaxe, axe);
        }
    }

    public static final Map<GearTier, GearSet> GEAR = new EnumMap<>(GearTier.class);

    static {
        GearTier.registerTiers();
        for (GearTier t : GearTier.values()) GEAR.put(t, gearSet(t));
    }

    private static GearSet gearSet(GearTier t) {
        java.util.function.Supplier<Item.Properties> props = () -> {
            Item.Properties p = new Item.Properties().rarity(t.rarity);
            if (t.fireResistant) p.fireResistant();
            return p;
        };
        return new GearSet(
                ITEMS.register(t.id + "_helmet", () -> new ArmorItem(t.material, ArmorItem.Type.HELMET, props.get())),
                ITEMS.register(t.id + "_chestplate", () -> new ArmorItem(t.material, ArmorItem.Type.CHESTPLATE, props.get())),
                ITEMS.register(t.id + "_leggings", () -> new ArmorItem(t.material, ArmorItem.Type.LEGGINGS, props.get())),
                ITEMS.register(t.id + "_boots", () -> new ArmorItem(t.material, ArmorItem.Type.BOOTS, props.get())),
                ITEMS.register(t.id + "_sword", () -> new SwordItem(t.tier, 3, -2.4F, props.get())),
                ITEMS.register(t.id + "_pickaxe", () -> new PickaxeItem(t.tier, 1, -2.8F, props.get())),
                ITEMS.register(t.id + "_axe", () -> new AxeItem(t.tier, 5.0F, -3.0F, props.get())));
    }

    public static final RegistryObject<Item> MARINE_SPAWN_EGG = ITEMS.register("marine_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MARINE, 0x1F2A3A, 0x1FA39A, new Item.Properties()));

    public static final RegistryObject<Item> SIREN_CONCH = ITEMS.register("siren_conch",
            () -> new com.piratecrew.sundered.SirenConchItem(new Item.Properties()));

    // ------------------------------------------------------------------ boss summons

    public static final RegistryObject<Item> SIGNAL_FLARE = ITEMS.register("signal_flare",
            () -> new com.piratecrew.item.BossSummonItem(ModEntities.COMMODORE, false, "Commodore Graves",
                    "A flare bursts over the island. Commodore Graves has come to hang a pirate!", new Item.Properties()));
    public static final RegistryObject<Item> KRAKEN_LURE = ITEMS.register("kraken_lure",
            () -> new com.piratecrew.item.BossSummonItem(ModEntities.KRAKEN, true, "the Kraken",
                    "The water darkens with ink... the Kraken rises!", new Item.Properties()));
    public static final RegistryObject<Item> STORM_SIGIL = ITEMS.register("storm_sigil",
            () -> new com.piratecrew.item.BossSummonItem(ModEntities.TEMPEST_ADMIRAL, false, "Tempest Admiral Sorel",
                    "Thunder rolls across the sea. Tempest Admiral Sorel descends!", new Item.Properties()));
    public static final RegistryObject<Item> LEVIATHAN_HORN = ITEMS.register("leviathan_horn",
            () -> new com.piratecrew.item.BossSummonItem(ModEntities.LEVIATHAN, true, "the Leviathan",
                    "The horn's call echoes into the deep. The Leviathan answers!", new Item.Properties()));
    public static final RegistryObject<Item> ADMIRALS_WARRANT = ITEMS.register("admirals_warrant",
            () -> new com.piratecrew.item.BossSummonItem(ModEntities.FLEET_ADMIRAL, false, "Fleet Admiral Vane",
                    "The warrant is served. Fleet Admiral Vane, the Iron Tide, has come for you!", new Item.Properties()));

    public static final RegistryObject<Item> COMMODORE_SPAWN_EGG = ITEMS.register("commodore_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.COMMODORE, 0x18224A, 0xC9A227, new Item.Properties()));
    public static final RegistryObject<Item> KRAKEN_SPAWN_EGG = ITEMS.register("kraken_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KRAKEN, 0x5A143C, 0xAA3C6E, new Item.Properties()));
    public static final RegistryObject<Item> TEMPEST_ADMIRAL_SPAWN_EGG = ITEMS.register("tempest_admiral_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.TEMPEST_ADMIRAL, 0x2C2646, 0x5AC8FF, new Item.Properties()));
    public static final RegistryObject<Item> LEVIATHAN_SPAWN_EGG = ITEMS.register("leviathan_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LEVIATHAN, 0x105848, 0x82E6BE, new Item.Properties()));
    public static final RegistryObject<Item> FLEET_ADMIRAL_SPAWN_EGG = ITEMS.register("fleet_admiral_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FLEET_ADMIRAL, 0x0E0E10, 0xAA141E, new Item.Properties()));

    // ------------------------------------------------------------------ soul pacts

    public static final Map<com.piratecrew.pact.SoulPact, RegistryObject<Item>> SOUL_PACTS = new EnumMap<>(com.piratecrew.pact.SoulPact.class);

    static {
        for (com.piratecrew.pact.SoulPact p : com.piratecrew.pact.SoulPact.values()) {
            SOUL_PACTS.put(p, ITEMS.register("soul_pact_" + p.id, () -> new com.piratecrew.pact.SoulPactItem(p, new Item.Properties())));
        }
    }

    public static final RegistryObject<Item> CAPTAINS_LOG = ITEMS.register("captains_log",
            () -> new com.piratecrew.item.CaptainsLogItem(new Item.Properties()));

    public static final RegistryObject<Item> TREASURE_MAP = ITEMS.register("treasure_map",
            () -> new com.piratecrew.item.TreasureMapItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // Music discs (synthesized by tools/gen_discs.py). Lengths in ticks match the recordings.
    public static final RegistryObject<Item> DISC_SAILOR = ITEMS.register("music_disc_sailor",
            () -> new RecordItem(4, ModSounds.DISC_SAILOR, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 102 * 20));
    public static final RegistryObject<Item> DISC_JIG = ITEMS.register("music_disc_jig",
            () -> new RecordItem(7, ModSounds.DISC_JIG, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 107 * 20));
    public static final RegistryObject<Item> DISC_KRAKEN = ITEMS.register("music_disc_kraken",
            () -> new RecordItem(11, ModSounds.DISC_KRAKEN, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 116 * 20));

    public static final RegistryObject<Item> AGGRO_STICK = ITEMS.register("aggro_stick",
            () -> new com.piratecrew.item.AggroStickItem(new Item.Properties()));

    private static RegistryObject<Item> collectorEgg(PirateTier tier, int color) {
        return ITEMS.register("debt_collector_" + tier.label.toLowerCase() + "_spawn_egg",
                () -> new com.piratecrew.item.DebtCollectorEggItem(tier, color, new Item.Properties()));
    }

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
