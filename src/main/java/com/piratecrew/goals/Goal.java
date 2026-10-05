package com.piratecrew.goals;

import com.piratecrew.item.GearTier;
import com.piratecrew.pact.SoulPact;
import com.piratecrew.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/** Voyage Goals: milestones of a pirate's career, shown in the Captain's Log with a toast when reached. */
public enum Goal {
    FIRST_RUBY("Ruby Fever", "Mine your first ruby ore", () -> new ItemStack(ModItems.RUBY.get())),
    RECRUIT("All Hands on Deck", "Recruit a pirate", () -> new ItemStack(ModItems.PIRATE_SPAWN_EGG.get())),
    CREW("Under Your Own Flag", "Found or join a crew", () -> new ItemStack(Items.BLACK_BANNER)),
    BOUNTY_100("Wanted", "Reach a bounty of 100 rubies", () -> new ItemStack(ModItems.BOUNTY_BOARD.get())),
    BOUNTY_1000("Infamous", "Reach a bounty of 1,000 rubies", () -> new ItemStack(ModItems.RUBY_BLOCK.get())),
    LOAN("In the Red", "Take a loan from the bank", () -> new ItemStack(ModItems.BANK_COUNTER.get())),
    PORTAL("The Siren's Call", "Light a Siren portal", () -> new ItemStack(ModItems.SIREN_CONCH.get())),
    SEA("Sundered", "Cross into the Sundered Sea", () -> new ItemStack(Items.OAK_BOAT)),
    TIDESTEEL("Tidesteel", "Mine tidesteel ore", () -> new ItemStack(ModItems.RAW_TIDESTEEL.get())),
    ABYSSAL("Into the Abyss", "Mine abyssal ore", () -> new ItemStack(ModItems.ABYSSAL_SHARD.get())),
    STORMGLASS("Glass and Thunder", "Mine stormglass ore", () -> new ItemStack(ModItems.STORMGLASS_SHARD.get())),
    MARINE("Mutineer", "Defeat a marine of the Order", () -> new ItemStack(ModItems.MARINE_BADGE.get())),
    CAPTAIN("Brass Buttons", "Defeat a marine captain", () -> new ItemStack(ModItems.GEAR.get(GearTier.ABYSSAL).sword().get())),
    COMMODORE("Broadside", "Defeat Commodore Graves", () -> new ItemStack(ModItems.SIGNAL_FLARE.get())),
    KRAKEN("Release the Kraken", "Defeat the Kraken", () -> new ItemStack(ModItems.KRAKEN_LURE.get())),
    SOREL("Eye of the Storm", "Defeat Tempest Admiral Sorel", () -> new ItemStack(ModItems.STORM_SIGIL.get())),
    LEVIATHAN("Leviathan's Bane", "Defeat the Leviathan", () -> new ItemStack(ModItems.LEVIATHAN_HORN.get())),
    VANE("The Iron Tide Breaks", "Defeat Fleet Admiral Vane", () -> new ItemStack(ModItems.ADMIRALS_WARRANT.get())),
    PACT("Soul Bound", "Bind a Soul Pact to your soul", () -> new ItemStack(ModItems.SOUL_PACTS.get(SoulPact.EMBER).get())),
    PACT_PIRATE("Kindred Spirits", "Give a Soul Pact to a crew pirate", () -> new ItemStack(ModItems.SOUL_PACTS.get(SoulPact.FROST).get())),
    SOVEREIGN("Sovereign", "Wear a full set of Sovereign armor", () -> new ItemStack(ModItems.GEAR.get(GearTier.SOVEREIGN).chestplate().get())),
    SSS("Monster of the Deep", "Raise a crew pirate to SSS tier", () -> new ItemStack(Items.WITHER_SKELETON_SKULL)),
    BOUNTY_10000("Legend of the Seas", "Reach a bounty of 10,000 rubies", () -> new ItemStack(Items.NETHER_STAR)),
    EMPEROR("Emperor of the Sea", "Sail with one of the Four Emperors", () -> new ItemStack(Items.GOLDEN_HELMET));

    public final String title, description;
    public final Supplier<ItemStack> icon;

    Goal(String title, String description, Supplier<ItemStack> icon) {
        this.title = title;
        this.description = description;
        this.icon = icon;
    }

    public long bit() {
        return 1L << ordinal();
    }

    public static Goal byId(int id) {
        Goal[] v = values();
        return id >= 0 && id < v.length ? v[id] : null;
    }
}
