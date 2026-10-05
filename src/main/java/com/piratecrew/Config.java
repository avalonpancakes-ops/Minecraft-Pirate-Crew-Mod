package com.piratecrew;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SPEC;

    // Recruiting
    public static final ForgeConfigSpec.IntValue COST_F;
    public static final ForgeConfigSpec.IntValue COST_D;
    public static final ForgeConfigSpec.IntValue COST_C;
    public static final ForgeConfigSpec.IntValue COST_B;
    public static final ForgeConfigSpec.IntValue COST_A;
    public static final ForgeConfigSpec.IntValue COST_S;

    // Crew
    public static final ForgeConfigSpec.IntValue MAX_CREW_SIZE;
    public static final ForgeConfigSpec.IntValue MAX_REAL_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue FRIENDLY_FIRE;

    // Bars
    public static final ForgeConfigSpec.BooleanValue GENERATE_BARS;
    public static final ForgeConfigSpec.BooleanValue GENERATE_BANKS;
    public static final ForgeConfigSpec.IntValue BAR_MIN_PIRATES;
    public static final ForgeConfigSpec.IntValue BAR_MAX_PIRATES;
    public static final ForgeConfigSpec.IntValue BAR_RESTOCK_TICKS;

    // Bounties
    public static final ForgeConfigSpec.IntValue BOUNTY_PER_PLAYER_KILL;
    public static final ForgeConfigSpec.IntValue BOUNTY_PER_PIRATE_KILL;
    public static final ForgeConfigSpec.IntValue BOUNTY_PER_PIRATE_TIER;
    public static final ForgeConfigSpec.DoubleValue BOUNTY_SHARE;
    public static final ForgeConfigSpec.DoubleValue BOUNTY_CAPTAIN_CUT;
    public static final ForgeConfigSpec.IntValue BOUNTY_REPEAT_COOLDOWN;

    // Loans
    public static final ForgeConfigSpec.IntValue LOAN_MAX;
    public static final ForgeConfigSpec.DoubleValue LOAN_INTEREST;
    public static final ForgeConfigSpec.IntValue LOAN_DAYS;
    public static final ForgeConfigSpec.DoubleValue HUNTER_STRENGTH;
    public static final ForgeConfigSpec.IntValue HUNTER_MAX_PER_WAVE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Ruby cost to recruit a pirate of each tier").push("recruiting");
        COST_F = b.defineInRange("costF", 5, 0, 10000);
        COST_D = b.defineInRange("costD", 10, 0, 10000);
        COST_C = b.defineInRange("costC", 18, 0, 10000);
        COST_B = b.defineInRange("costB", 30, 0, 10000);
        COST_A = b.defineInRange("costA", 48, 0, 10000);
        COST_S = b.defineInRange("costS", 72, 0, 10000);
        b.pop();

        b.push("crew");
        MAX_CREW_SIZE = b.comment("Maximum members in a crew (players + NPCs)").defineInRange("maxCrewSize", 20, 1, 200);
        MAX_REAL_PLAYERS = b.comment("Maximum real players in a crew").defineInRange("maxRealPlayers", 7, 1, 200);
        FRIENDLY_FIRE = b.comment("Allow crew members (players and pirates) to hurt each other").define("friendlyFire", false);
        b.pop();

        b.push("bars");
        GENERATE_BARS = b.comment("Generate one pirate bar next to every village").define("generateBars", true);
        GENERATE_BANKS = b.comment("Generate one ruby bank next to every village (never overlapping the bar)").define("generateBanks", true);
        BAR_MIN_PIRATES = b.comment("A bar restocks pirates when it has fewer than this many unrecruited pirates").defineInRange("minPirates", 3, 0, 20);
        BAR_MAX_PIRATES = b.comment("Pirates placed in a freshly built bar").defineInRange("startPirates", 6, 0, 20);
        BAR_RESTOCK_TICKS = b.comment("Ticks between restock checks (24000 = one Minecraft day)").defineInRange("restockTicks", 12000, 200, 1000000);
        b.pop();

        b.comment("Ruby bounties on crew members who kill players and pirates").push("bounties");
        BOUNTY_PER_PLAYER_KILL = b.comment("Rubies added to a crew member's bounty for killing a player").defineInRange("perPlayerKill", 10, 0, 10000);
        BOUNTY_PER_PIRATE_KILL = b.comment("Rubies added for killing an F-tier pirate (doubled if the pirate belonged to a crew)").defineInRange("perPirateKill", 2, 0, 10000);
        BOUNTY_PER_PIRATE_TIER = b.comment("Extra rubies per tier above F for pirate kills (D +1x, C +2x ... S +5x)").defineInRange("perPirateTier", 1, 0, 10000);
        BOUNTY_SHARE = b.comment("Share of a claimed bounty that's added to the killer's own bounty (0.25 = 25%)").defineInRange("claimShare", 0.25, 0.0, 10.0);
        BOUNTY_CAPTAIN_CUT = b.comment("When a crew pirate kills a wanted target, its captain gets this share of the bounty in their bank (0.25 = 25%)").defineInRange("captainCut", 0.25, 0.0, 1.0);
        BOUNTY_REPEAT_COOLDOWN = b.comment("Seconds before killing the same target again counts toward bounties (stops kill farming)").defineInRange("repeatKillCooldown", 600, 0, 86400);
        b.pop();

        b.comment("Ruby loans from the banker, and the debt collectors sent after players who don't pay").push("loans");
        LOAN_MAX = b.comment("Most rubies a player can borrow").defineInRange("maxLoan", 500, 1, 1000000);
        LOAN_INTEREST = b.comment("Interest added to a loan (0.25 = 25%)").defineInRange("interest", 0.25, 0.0, 10.0);
        LOAN_DAYS = b.comment("Minecraft days to repay a loan before debt collectors are sent").defineInRange("repayDays", 7, 1, 1000);
        HUNTER_STRENGTH = b.comment("Debt collectors have this many times the health and damage of a pirate of the same tier").defineInRange("hunterStrength", 5.0, 1.0, 50.0);
        HUNTER_MAX_PER_WAVE = b.comment("Most debt collectors sent in one day").defineInRange("maxHuntersPerWave", 6, 1, 50);
        b.pop();

        SPEC = b.build();
    }
}
