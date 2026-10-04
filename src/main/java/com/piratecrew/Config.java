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
    public static final ForgeConfigSpec.IntValue BAR_MIN_PIRATES;
    public static final ForgeConfigSpec.IntValue BAR_MAX_PIRATES;
    public static final ForgeConfigSpec.IntValue BAR_RESTOCK_TICKS;

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
        BAR_MIN_PIRATES = b.comment("A bar restocks pirates when it has fewer than this many unrecruited pirates").defineInRange("minPirates", 3, 0, 20);
        BAR_MAX_PIRATES = b.comment("Pirates placed in a freshly built bar").defineInRange("startPirates", 6, 0, 20);
        BAR_RESTOCK_TICKS = b.comment("Ticks between restock checks (24000 = one Minecraft day)").defineInRange("restockTicks", 12000, 200, 1000000);
        b.pop();

        SPEC = b.build();
    }
}
