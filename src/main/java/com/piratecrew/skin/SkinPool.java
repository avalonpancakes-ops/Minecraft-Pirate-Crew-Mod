package com.piratecrew.skin;

import com.piratecrew.Config;
import com.piratecrew.PirateCrew;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-side pool of skin URLs. Each pirate is given one URL when it spawns; every client then
 * downloads that same PNG, so everyone sees the same pirate.
 *
 * Source: The Skindex (minecraftskins.com). The gallery pages show 3D previews at
 *   /uploads/preview-skins/YYYY/MM/DD/name-ID.png
 * and the raw 64x64 skin lives at
 *   /uploads/skins/YYYY/MM/DD/name-ID.png
 * so we read a few gallery pages and convert the preview paths.
 *
 * If that fails we fall back to https://mc-heads.net/skin/USERNAME, and if that list is empty the
 * client shows vanilla skins.
 */
public class SkinPool {
    private static final Pattern PREVIEW = Pattern.compile("/uploads/preview-skins/(\\d{4}/\\d{2}/\\d{2}/[A-Za-z0-9_\\-]+-\\d+)\\.png");
    private static final int MAX_CACHED = 3000;
    private static final List<String> POOL = new CopyOnWriteArrayList<>();
    private static volatile boolean loading = false;

    private static Path cacheFile() {
        return FMLPaths.CONFIGDIR.get().resolve("piratecrew_skin_pool.txt");
    }

    /** A random skin URL, or "" if nothing is available yet. */
    public static String randomSkin(RandomSource random) {
        if (!POOL.isEmpty()) return POOL.get(random.nextInt(POOL.size()));
        List<? extends String> names = Config.FALLBACK_USERNAMES.get();
        if (!names.isEmpty() && (!Config.USE_SKIN_SITE.get() || !loading)) {
            return "https://mc-heads.net/skin/" + names.get(random.nextInt(names.size()));
        }
        return "";
    }

    /** Called when the server starts. Loads the disk cache, then refreshes from the site in the background. */
    public static void start() {
        POOL.clear();
        try {
            Path f = cacheFile();
            if (Files.exists(f)) {
                for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                    line = line.trim();
                    if (isAllowed(line)) POOL.add(line);
                }
                PirateCrew.LOGGER.info("Pirate Crew: loaded {} cached skins", POOL.size());
            }
        } catch (Exception e) {
            PirateCrew.LOGGER.warn("Pirate Crew: couldn't read skin cache", e);
        }

        if (!Config.USE_SKIN_SITE.get()) return;
        loading = true;
        Thread t = new Thread(SkinPool::refresh, "PirateCrew-SkinPool");
        t.setDaemon(true);
        t.start();
    }

    private static void refresh() {
        try {
            String site = Config.SKIN_SITE.get().replaceAll("/+$", "");
            boolean latest = "latest".equalsIgnoreCase(Config.SKIN_LIST.get());
            int pages = Config.SKIN_PAGES_TO_SCAN.get();
            int maxPage = Config.SKIN_MAX_PAGE.get();
            Random r = new Random();
            Set<Integer> chosen = new LinkedHashSet<>();
            chosen.add(1);
            while (chosen.size() < Math.min(pages, maxPage)) chosen.add(1 + r.nextInt(maxPage));

            Set<String> found = new LinkedHashSet<>();
            for (int page : chosen) {
                String url = latest ? site + "/latest/" + page + "/" : (page == 1 ? site + "/" : site + "/" + page + "/");
                try {
                    String html = fetch(url);
                    Matcher m = PREVIEW.matcher(html);
                    int before = found.size();
                    while (m.find()) found.add(site + "/uploads/skins/" + m.group(1) + ".png");
                    PirateCrew.LOGGER.info("Pirate Crew: {} skins from {}", found.size() - before, url);
                    Thread.sleep(750); // be polite to the site
                } catch (Exception e) {
                    PirateCrew.LOGGER.warn("Pirate Crew: couldn't read {} ({})", url, e.toString());
                }
            }

            if (!found.isEmpty()) {
                LinkedHashSet<String> merged = new LinkedHashSet<>(found);
                merged.addAll(POOL);
                List<String> list = new ArrayList<>(merged);
                if (list.size() > MAX_CACHED) list = list.subList(0, MAX_CACHED);
                POOL.clear();
                POOL.addAll(list);
                try {
                    Files.write(cacheFile(), list, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    PirateCrew.LOGGER.warn("Pirate Crew: couldn't save skin cache", e);
                }
                PirateCrew.LOGGER.info("Pirate Crew: skin pool ready with {} skins", POOL.size());
            } else if (POOL.isEmpty()) {
                PirateCrew.LOGGER.warn("Pirate Crew: The Skindex gave no skins; using fallback usernames via mc-heads.net");
            }
        } finally {
            loading = false;
        }
    }

    static String fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (PirateCrew Minecraft mod)");
        c.setRequestProperty("Accept", "text/html");
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0 && out.size() < 4_000_000) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    /** Only these hosts may be used as skin sources (clients enforce this too). */
    public static boolean isAllowed(String url) {
        if (url == null || url.isEmpty()) return false;
        return url.startsWith("https://www.minecraftskins.com/uploads/skins/")
                || url.startsWith("https://minecraftskins.com/uploads/skins/")
                || url.startsWith("https://mc-heads.net/skin/")
                || url.startsWith("http://textures.minecraft.net/texture/")
                || url.startsWith("https://textures.minecraft.net/texture/");
    }
}
