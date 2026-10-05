package com.piratecrew.client.codex;

import com.piratecrew.codex.ShowcaseTools.Action;
import com.piratecrew.item.GearTier;
import com.piratecrew.pact.SoulPact;
import com.piratecrew.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Everything in the Captain's Log. Body lines may use § colour codes; blank strings are paragraph gaps.
 */
public class CodexContent {
    public record Section(String title, int color, List<Entry> entries) {}

    public record Entry(String title, String subtitle, Supplier<ItemStack> icon, List<String> body, List<Supplier<ItemStack>> items) {}

    public record Tool(Action action, String title, String subtitle, Supplier<ItemStack> icon, String tooltip) {}

    private static List<Section> sections;

    public static List<Section> sections() {
        if (sections == null) sections = build();
        return sections;
    }

    private static Supplier<ItemStack> icon(Supplier<? extends ItemLike> item) {
        return () -> new ItemStack(item.get());
    }

    private static Supplier<ItemStack> icon(Item item) {
        return () -> new ItemStack(item);
    }

    private static Entry e(String title, String subtitle, Supplier<ItemStack> icon, String... body) {
        return new Entry(title, subtitle, icon, List.of(body), List.of());
    }

    /** The same entry with a row of related items shown under its title. */
    @SafeVarargs
    private static Entry with(Entry e, Supplier<? extends ItemLike>... items) {
        List<Supplier<ItemStack>> list = new ArrayList<>();
        for (Supplier<? extends ItemLike> i : items) list.add(() -> new ItemStack(i.get()));
        return new Entry(e.title(), e.subtitle(), e.icon(), e.body(), list);
    }

    private static List<Section> build() {
        List<Section> out = new ArrayList<>();

        out.add(new Section("PIRATE'S LIFE", 0xE8B84A, List.of(
                e("Rubies", "The coin of the seas", icon(ModItems.RUBY),
                        "Rubies pay for everything: recruiting pirates, the bank's shop, loans and the portal to the Sundered Sea.",
                        "",
                        "§6Ruby ore§r is found underground like diamonds (deepslate too). Ruby gear sits between iron and diamond.",
                        "",
                        "Store them at any §6bank§r: your balance is just a number, so they stop eating inventory space."),
                e("Village Bars", "Where pirates drink", icon(ModItems.PIRATE_SPAWN_EGG),
                        "Every village gets a pirate bar. Free pirates lounge inside, each a tier from §7F§r to §6S§r.",
                        "",
                        "§eRight-click§r a pirate to see its tier and price. Hold rubies and right-click to §arecruit§r it.",
                        "",
                        "Better tiers have more health and damage, aim straighter and drink potions in a fight."),
                e("Your Crew", "Captain, vice captains, pirates", icon(Items.WHITE_BANNER),
                        "Press §eI§r (or /crew) for the crew screen. Invite players, promote vice captains, give orders to your pirates.",
                        "",
                        "Crew pirates §aFollow§r, §aHold§r a spot, §aRoam§r or work jobs. Give them bows, crossbows, tridents, shields and potions; they use them all.",
                        "",
                        "Crewmates can never hurt each other.",
                        "",
                        "§6Rank ups:§r a crew pirate climbs tiers as its bounty grows: D at 100, C 250, B 500, A 1,000, S 2,500, §cSS§r 5,000 and §4SSS§r 10,000."),
                e("Bounties", "Make a name for yourself", icon(ModItems.BOUNTY_BOARD),
                        "Crew members earn a bounty by sinking players and pirates of rival crews, marines and bosses.",
                        "",
                        "§cKilling a wanted rival player§r takes 25% of their bounty: you get it in rubies and on your own head.",
                        "",
                        "§cKilling a rival crew pirate§r pays its §owhole§r bounty in rubies (it's dead for good) and adds 25% to yours.",
                        "",
                        "Only rival crews and debt collectors can take your bounty. Check the §6Bounty Board§r in any bar."),
                e("Emperors of the Sea", "The four greatest crews", icon(Items.GOLDEN_HELMET),
                        "The 4 crews with the highest combined bounty, at least §610,000§r rubies, rule as Emperors.",
                        "",
                        "Every member gets permanent §cStrength§r and §9Resistance§r. Captains and vice captains get Strength III and Resistance II.",
                        "",
                        "Lose your seat, leave or get kicked, and the power goes with it."),
                e("The Bank", "Balance, shop and loans", icon(ModItems.BANK_COUNTER),
                        "Every village has a bank. The §6Banker§r keeps your rubies and runs a §6shop§r: food, materials, gear, enchanted books and the §bSiren Conch§r.",
                        "",
                        "Borrow up to §6500§r rubies at 25% interest. Repay within 7 days..."),
                e("Debt Collectors", "...or else", icon(ModItems.DEBT_COLLECTOR_EGG_S),
                        "Miss a loan and the bank sends a debt collector every day: F, D, C, B, A then S, each only if the last one failed.",
                        "",
                        "They're 5× a pirate's strength, follow you through portals and drop nothing. An §6S collector§r fires eye lasers.",
                        "",
                        "If one kills you, the bank takes what you owe from your bounty, then your account, then your most valuable items."),
                e("Corpses", "What you leave behind", icon(Items.SKELETON_SKULL),
                        "When you die your items stay on your §7corpse§r. Only you can loot it for 2 minutes; after that, anyone can.")
        )));

        out.add(new Section("THE SUNDERED SEA", 0x3FD6D0, List.of(
                e("Siren Portal", "The ticket costs 1,000 rubies", icon(ModItems.SIREN_CONCH),
                        "Build a frame of §cruby blocks§r shaped like a nether portal and light it with a §bSiren Conch§r (bank shop, 1,000 rubies).",
                        "",
                        "Stand in the swirl for 4 seconds to cross. If there's no portal on the far side, one is built on land, or on a raft at sea."),
                e("The Islands", "A lawless archipelago", icon(Items.JUNGLE_SAPLING),
                        "An endless ocean scattered with islands: green §aPalm Isles§r, rocky §7Storm Isles§r and black §7Ember Isles§r.",
                        "",
                        "Villages with bars and banks, shipwrecks, ocean ruins and buried treasure dot the sea. Raider crews camp on the islands."),
                e("New Ores", "Tidesteel, Abyssal, Stormglass", icon(ModItems.TIDESTEEL_ORE),
                        "§bTidesteel ore§r: everywhere in stone and deepslate. Needs a diamond pickaxe.",
                        "",
                        "§dAbyssal ore§r: deep in the deepslate, below y −8. Needs a tidesteel pickaxe.",
                        "",
                        "§9Stormglass ore§r: Storm Isles only. Needs an abyssal pickaxe."),
                e("Marine Outposts", "Walls, a tower, a garrison", icon(ModItems.MARINE_BADGE),
                        "Stone-brick forts with a watchtower and a loot chest, held by the Order of the Tide.",
                        "",
                        "Their chests hold tidesteel, badges, rubies and, rarely, a Soul Pact."),
                e("Pact Shrines", "Where pacts wait", () -> new ItemStack(ModItems.SOUL_PACTS.get(SoulPact.SHADOW).get()),
                        "Rare ruins of mossy pillars around an altar lit by soul lanterns.",
                        "",
                        "The altar chest §dalways§r holds a Soul Pact, with treasure besides.")
        )));

        out.add(new Section("THE ORDER OF THE TIDE", 0x5AC8FF, List.of(
                e("Marines", "They hang pirates", icon(ModItems.MARINE_SPAWN_EGG),
                        "Navy longcoats, teal sashes, brass and black tricornes. Stronger than any Overworld mob.",
                        "",
                        "§bRecruits§r and §aRiflemen§r patrol in squads led by §eSergeants§r and the odd §6Captain§r.",
                        "",
                        "They hunt anyone in a crew or with a bounty, and every pirate NPC. Kills raise your bounty; badges summon the Commodore.")
        )));

        out.add(new Section("BOSSES", 0xE04040, List.of(
                with(e("Commodore Graves", "600 health · Signal Flare", icon(ModItems.SIGNAL_FLARE),
                        "A duelist with a fleet behind him. Calls §ccannon broadsides§r on you, leaps in with a crushing landing, and calls marines at 2/3 and 1/3 health.",
                        "",
                        "§6Summon:§r Signal Flare (7 marine badges + gunpowder + tidesteel ingot), used on land.",
                        "",
                        "§6Drops:§r Kraken Lure, Commodore's Insignia, abyssal shards, tidesteel. Bounty +150."),
                        ModItems.KRAKEN_LURE, ModItems.COMMODORE_INSIGNIA, ModItems.ABYSSAL_SHARD, ModItems.TIDESTEEL_INGOT, ModItems.MARINE_BADGE),
                with(e("The Kraken", "1,000 health · Kraken Lure", icon(ModItems.KRAKEN_LURE),
                        "A ship-sized squid hunting from below: tentacle slams marked in ink, a blinding ink cloud, a tentacle that drags you off boats, geysers and, when hurt, a §bwhirlpool§r.",
                        "",
                        "§6Summon:§r Kraken Lure (insignia + 4 abyssal shards + 2 tropical fish + ink sac) over deep water.",
                        "",
                        "§6Drops:§r Storm Sigil, 8-14 kraken bones. Bounty +250."),
                        ModItems.STORM_SIGIL, ModItems.KRAKEN_BONE, ModItems.ABYSSAL_INGOT, () -> Items.INK_SAC),
                with(e("Tempest Admiral Sorel", "1,600 health · Storm Sigil", icon(ModItems.STORM_SIGIL),
                        "Lightning marked a heartbeat before it lands, a wind gust that throws you skyward, and a blink that puts her behind archers. Below half health the sky turns to §9storm§r.",
                        "",
                        "§6Summon:§r Storm Sigil (4 kraken bones + 4 stormglass + eye of ender) on land.",
                        "",
                        "§6Drops:§r Leviathan Horn, 4-7 storm cores. Bounty +400."),
                        ModItems.LEVIATHAN_HORN, ModItems.STORM_CORE, ModItems.STORMGLASS_SHARD, ModItems.KRAKENBONE_INGOT),
                with(e("The Leviathan", "2,800 health · Leviathan Horn", icon(ModItems.LEVIATHAN_HORN),
                        "An ancient guardian the size of a ship. Its §abeam§r charges faster as it weakens; tail slams, whirlpools, geysers, and broods of guardians.",
                        "",
                        "§6Summon:§r Leviathan Horn (4 storm cores + 4 prismarine crystals + nautilus shell) over deep water.",
                        "",
                        "§6Drops:§r Admiral's Warrant, 8-14 leviathan scales, heart of the sea. Bounty +800."),
                        ModItems.ADMIRALS_WARRANT, ModItems.LEVIATHAN_SCALE, ModItems.STORMFORGED_INGOT, () -> Items.HEART_OF_THE_SEA),
                with(e("Fleet Admiral Vane", "4,500 health · Admiral's Warrant", icon(ModItems.ADMIRALS_WARRANT),
                        "The Iron Tide. Dash strikes that cut through a line of foes, then captains and the whole fleet's guns, then §cthe Iron Tide§r: shockwaves and lightning.",
                        "",
                        "§6Summon:§r Admiral's Warrant (4 leviathan scales + 4 badges + insignia) on land.",
                        "",
                        "§6Drops:§r 3-5 sovereign hearts and a §dguaranteed Soul Pact§r. Bounty +1,500."),
                        ModItems.SOVEREIGN_HEART, ModItems.LEVIATHAN_INGOT, () -> Items.NETHERITE_INGOT, () -> ModItems.SOUL_PACTS.get(SoulPact.BLOOD).get())
        )));

        List<Entry> pacts = new ArrayList<>();
        for (SoulPact p : SoulPact.values()) {
            pacts.add(e(p.title(), p.power + " · " + (p.cooldown / 20) + "s", icon(() -> ModItems.SOUL_PACTS.get(p).get()),
                    "§6Gift:§r " + p.passiveText,
                    "",
                    "§6Power (R):§r " + p.power + ". " + p.powerText,
                    "",
                    "Crew pirates bound to it fight " + (p.ranged ? "§bfrom range§r" : "§cup close§r") + " and use the power on their own.",
                    "",
                    "§9The sea drains every pact:§r in water you are weak, slow and powerless."));
        }
        out.add(new Section("SOUL PACTS", 0xD070FF, pacts));

        List<Entry> gear = new ArrayList<>();
        String[] where = {
                "Tidesteel ore, smelted.",
                "3 abyssal shards + 1 tidesteel ingot.",
                "2 kraken bones + 1 abyssal ingot (makes 2).",
                "Storm core + 2 stormglass + 1 krakenbone ingot (makes 2).",
                "2 leviathan scales + 1 stormforged ingot (makes 2).",
                "Sovereign heart + 1 leviathan ingot (makes 2)."};
        int[][] armor = {{3, 8, 7, 3}, {4, 9, 7, 4}, {5, 11, 9, 5}, {7, 14, 11, 6}, {10, 17, 15, 9}, {14, 20, 18, 13}};
        int[] dmg = {8, 10, 13, 18, 24, 31};
        String[] names = {"Tidesteel", "Abyssal", "Krakenbone", "Stormforged", "Leviathan", "Sovereign"};
        for (GearTier t : GearTier.values()) {
            int i = t.ordinal();
            int total = armor[i][0] + armor[i][1] + armor[i][2] + armor[i][3];
            var set = ModItems.GEAR.get(t);
            gear.add(with(e(names[i] + " Gear", total + " armor · " + dmg[i] + " damage", icon(() -> ModItems.GEAR.get(t).sword().get()),
                    "§6Armor:§r helmet " + armor[i][0] + ", chestplate " + armor[i][1] + ", leggings " + armor[i][2] + ", boots " + armor[i][3] + ".",
                    "§6Cutlass:§r " + dmg[i] + " damage.",
                    "",
                    "§6Ingot:§r " + where[i],
                    "",
                    i >= 2 ? "Boss-forged: the metal shimmers and its gems pulse." : "Armor past 20 keeps cutting the damage you take."),
                    set.helmet(), set.chestplate(), set.leggings(), set.boots(), set.sword(), set.pickaxe(), set.axe()));
        }
        out.add(new Section("GEAR", 0x9AE0A0, gear));
        return out;
    }

    public static List<Tool> tools() {
        return List.of(
                new Tool(Action.SOVEREIGN_KIT, "Sovereign Kit", "Enchanted, full set", icon(() -> ModItems.GEAR.get(GearTier.SOVEREIGN).chestplate().get()), "A full enchanted Sovereign set and tools"),
                new Tool(Action.ALL_TIERS, "Every Set", "All six tiers", icon(() -> ModItems.GEAR.get(GearTier.LEVIATHAN).sword().get()), "One of every Sundered Sea gear set"),
                new Tool(Action.BOSS_SUMMONS, "Summon Items", "Flares to warrants", icon(ModItems.ADMIRALS_WARRANT), "4 of each boss summon item and 4 Siren Conches"),
                new Tool(Action.ALL_PACTS, "All Pacts", "Ten Soul Pacts", icon(() -> ModItems.SOUL_PACTS.get(SoulPact.EMBER).get()), "One of each Soul Pact"),
                new Tool(Action.BOSS_COMMODORE, "Commodore", "Summon here", icon(ModItems.SIGNAL_FLARE), "Commodore Graves appears in front of you"),
                new Tool(Action.BOSS_KRAKEN, "Kraken", "Summon here", icon(ModItems.KRAKEN_LURE), "The Kraken rises in front of you (best over water)"),
                new Tool(Action.BOSS_TEMPEST, "Sorel", "Summon here", icon(ModItems.STORM_SIGIL), "Tempest Admiral Sorel appears in front of you"),
                new Tool(Action.BOSS_LEVIATHAN, "Leviathan", "Summon here", icon(ModItems.LEVIATHAN_HORN), "The Leviathan rises in front of you (best over water)"),
                new Tool(Action.BOSS_VANE, "Vane", "Summon here", icon(() -> ModItems.GEAR.get(GearTier.SOVEREIGN).sword().get()), "Fleet Admiral Vane appears in front of you"),
                new Tool(Action.BUILD_PORTAL, "Siren Portal", "Built and lit", icon(ModItems.SIREN_CONCH), "A lit Siren portal in front of you"),
                new Tool(Action.TO_SEA, "Set Sail", "Sea ⇄ Overworld", icon(Items.OAK_BOAT), "Jump straight to the Sundered Sea, or back home"),
                new Tool(Action.OUTPOST, "Outpost", "Marine fort", icon(ModItems.MARINE_BADGE), "Build a garrisoned marine outpost"),
                new Tool(Action.SHRINE, "Pact Shrine", "With a pact", () -> new ItemStack(Items.SOUL_LANTERN), "Build a Pact Shrine"),
                new Tool(Action.CAMP, "Raider Camp", "With a crew", icon(Items.CAMPFIRE), "Build a raider camp and its crew"),
                new Tool(Action.MARINES, "Marine Squad", "They hunt you", icon(ModItems.MARINE_SPAWN_EGG), "Call a marine squad"),
                new Tool(Action.RAIDERS, "Enemy Crew", "Raiders attack", icon(Items.IRON_SWORD), "Call an enemy pirate crew"),
                new Tool(Action.HEAL, "Heal & Feed", "Fresh as a daisy", icon(Items.GOLDEN_APPLE), "Full health and food, effects cleared"),
                new Tool(Action.CLEAR, "Clear Foes", "Marines, raiders, bosses", icon(Items.BARRIER), "Remove marines, raiders and bosses nearby"),
                new Tool(Action.RUBIES, "Ruby Hoard", "576 rubies", icon(ModItems.RUBY_BLOCK), "A stack of ruby blocks"),
                new Tool(Action.DAY, "Morning", "Set the time", icon(Items.SUNFLOWER), "Set the time to morning"),
                new Tool(Action.NIGHT, "Midnight", "Set the time", icon(Items.CLOCK), "Set the time to night"));
    }
}
