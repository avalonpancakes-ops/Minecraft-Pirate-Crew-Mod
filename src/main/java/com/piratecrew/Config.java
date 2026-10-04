package com.piratecrew;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

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

    // Skins
    public static final ForgeConfigSpec.BooleanValue USE_SKIN_SITE;
    public static final ForgeConfigSpec.ConfigValue<String> SKIN_SITE;
    public static final ForgeConfigSpec.ConfigValue<String> SKIN_LIST;
    public static final ForgeConfigSpec.IntValue SKIN_PAGES_TO_SCAN;
    public static final ForgeConfigSpec.IntValue SKIN_MAX_PAGE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FALLBACK_USERNAMES;

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

        b.comment("Where NPC skins come from").push("skins");
        USE_SKIN_SITE = b.comment("Pull random skins from the skin site below").define("useSkinSite", true);
        SKIN_SITE = b.comment("Base URL of The Skindex").define("skinSite", "https://www.minecraftskins.com");
        SKIN_LIST = b.comment("Which list to sample: 'top' (community favourites) or 'latest' (newest uploads, unfiltered)").define("skinList", "top");
        SKIN_PAGES_TO_SCAN = b.comment("How many random list pages to read at server start (50 skins per page)").defineInRange("pagesToScan", 4, 1, 20);
        SKIN_MAX_PAGE = b.comment("Highest page number to pick from").defineInRange("maxPage", 30, 1, 500);
        FALLBACK_USERNAMES = b.comment("If the skin site can't be reached, pirates wear the skins of these Minecraft accounts (via mc-heads.net). Empty = vanilla skins.")
                .defineListAllowEmpty("fallbackUsernames",
                        List.of("Notch", "jeb_", "Dinnerbone", "Grumm", "Searge", "Marc_IRL", "slicedlime", "Jappa"),
                        o -> o instanceof String s && s.matches("[A-Za-z0-9_]{1,16}"));
        b.pop();

        SPEC = b.build();
    }
}
