package com.piratecrew.skin;

import com.piratecrew.PirateCrew;
import com.piratecrew.entity.PirateTier;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.ModList;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pirate skins bundled inside the mod jar.
 *
 * Skins live in assets/piratecrew/textures/entity/pirate/NAME.png and are listed in
 * assets/piratecrew/pirate_skins.txt as "NAME MODEL [TIER]": MODEL is wide or slim (thin Alex-style
 * arms), TIER is F/D/C/B/A/S. A pirate rolled as a tier wears one of that tier's skins; skins with
 * no tier can be worn by any tier.
 * The list file is read from the jar on both client and server, so each pirate stores just the
 * skin name and every player sees the same skin.
 * Skins whose name starts with hunter_ are bounty hunter skins and are never given to pirates.
 */
public class PirateSkins {
    private static final String LIST = "pirate_skins.txt";
    /** Skins named hunter_* are only worn by bounty hunters. */
    public static final String HUNTER_PREFIX = "hunter_";
    private static List<String> names;
    private static Map<String, Boolean> slim;
    private static Map<String, PirateTier> tiers;

    private static synchronized void load() {
        if (names != null) return;
        names = new ArrayList<>();
        slim = new HashMap<>();
        tiers = new HashMap<>();
        try (InputStream in = open()) {
            if (in == null) {
                PirateCrew.LOGGER.warn("Pirate Crew: no {} found, pirates will use default skins", LIST);
                return;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                if (!parts[0].matches("[a-z0-9_]+")) continue;
                names.add(parts[0]);
                slim.put(parts[0], parts.length > 1 && parts[1].equalsIgnoreCase("slim"));
                if (parts.length > 2) {
                    PirateTier t = PirateTier.byLabel(parts[2]);
                    if (t != null) tiers.put(parts[0], t);
                }
            }
            PirateCrew.LOGGER.info("Pirate Crew: {} pirate skins available", names.size());
        } catch (Exception e) {
            PirateCrew.LOGGER.warn("Pirate Crew: couldn't read {}", LIST, e);
        }
    }

    private static InputStream open() throws Exception {
        try {
            Path p = ModList.get().getModFileById(PirateCrew.MODID).getFile().findResource("assets", PirateCrew.MODID, LIST);
            if (Files.exists(p)) return Files.newInputStream(p);
        } catch (Exception ignored) {
        }
        return PirateSkins.class.getResourceAsStream("/assets/" + PirateCrew.MODID + "/" + LIST);
    }

    /**
     * A random skin for a pirate of the given tier: one of that tier's skins, else an untiered
     * skin, else any skin. Returns "" if the mod ships no skins.
     */
    public static String random(RandomSource random, PirateTier tier) {
        load();
        return pick(random, tier, false);
    }

    /**
     * A skin for a bounty hunter: one of the hunter skins (named hunter_*) for its tier, else any
     * hunter skin, else a pirate skin of that tier if no hunter skins are bundled.
     */
    public static String randomHunter(RandomSource random, PirateTier tier) {
        load();
        String s = pick(random, tier, true);
        return s.isEmpty() ? pick(random, tier, false) : s;
    }

    public static boolean isHunterSkin(String name) {
        return name.startsWith(HUNTER_PREFIX);
    }

    /** Skins reserved for marines and bosses (marine_*, boss_*), never given to pirates or collectors. */
    public static boolean isReserved(String name) {
        return name.startsWith("marine_") || name.startsWith("boss_");
    }

    /** A random skin whose name starts with {@code prefix} (e.g. "marine_captain_"), or "" if none. */
    public static String randomWithPrefix(RandomSource random, String prefix) {
        load();
        List<String> pool = new ArrayList<>();
        for (String n : names) if (n.startsWith(prefix)) pool.add(n);
        return pool.isEmpty() ? "" : pool.get(random.nextInt(pool.size()));
    }

    private static String pick(RandomSource random, PirateTier tier, boolean hunter) {
        if (tier.isAboveS()) tier = PirateTier.S; // SS and SSS wear S skins
        List<String> all = new ArrayList<>();
        for (String n : names) if (!isReserved(n) && isHunterSkin(n) == hunter) all.add(n);
        List<String> pool = new ArrayList<>();
        for (String n : all) if (tiers.get(n) == tier) pool.add(n);
        if (pool.isEmpty()) for (String n : all) if (!tiers.containsKey(n)) pool.add(n);
        if (pool.isEmpty()) pool = all;
        return pool.isEmpty() ? "" : pool.get(random.nextInt(pool.size()));
    }

    public static boolean isValid(String name) {
        load();
        return slim.containsKey(name);
    }

    public static boolean isSlim(String name) {
        load();
        return Boolean.TRUE.equals(slim.get(name));
    }

    public static int count() {
        load();
        return names.size();
    }
}
