package com.piratecrew.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.piratecrew.PirateCrew;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.skin.SkinPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Downloads, caches (memory + disk) and registers pirate skin textures on the client. */
public class ClientSkins {
    private static final int MAX_BYTES = 1024 * 1024;

    private static class Entry {
        volatile ResourceLocation texture;
        volatile boolean slim;
    }

    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService DOWNLOADER = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "PirateCrew-SkinDownload");
        t.setDaemon(true);
        return t;
    });

    public static ResourceLocation texture(PirateEntity pirate) {
        Entry e = entry(pirate);
        if (e != null && e.texture != null) return e.texture;
        return DefaultPlayerSkin.getDefaultSkin(pirate.getUUID());
    }

    public static boolean isSlim(PirateEntity pirate) {
        Entry e = entry(pirate);
        if (e != null && e.texture != null) return e.slim;
        return "slim".equals(DefaultPlayerSkin.getSkinModelName(pirate.getUUID()));
    }

    private static Entry entry(PirateEntity pirate) {
        String url = pirate.getSkinUrl();
        if (!SkinPool.isAllowed(url)) return null;
        return CACHE.computeIfAbsent(url, u -> {
            Entry e = new Entry();
            DOWNLOADER.submit(() -> load(u, e));
            return e;
        });
    }

    private static void load(String url, Entry entry) {
        try {
            String hash = sha1(url);
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("piratecrew_skins");
            Path file = dir.resolve(hash + ".png");
            byte[] bytes;
            if (Files.exists(file)) {
                bytes = Files.readAllBytes(file);
            } else {
                bytes = download(url);
                Files.createDirectories(dir);
                Files.write(file, bytes);
            }

            NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes));
            img = normalise(img);
            if (img == null) {
                PirateCrew.LOGGER.debug("Pirate Crew: skin {} has an unsupported size, using default", url);
                return;
            }
            // Slim (Alex-style) arms leave this pixel empty; check before the alpha fix below fills it in.
            boolean slim = ((img.getPixelRGBA(54, 20) >>> 24) & 0xFF) == 0 && ((img.getPixelRGBA(55, 20) >>> 24) & 0xFF) == 0;
            // Same clean-up vanilla does for player skins: the inner body layer must be fully opaque,
            // otherwise stray transparent pixels render as holes or see-through patches.
            setNoAlpha(img, 0, 0, 32, 16);
            setNoAlpha(img, 0, 16, 64, 32);
            setNoAlpha(img, 16, 48, 48, 64);
            final NativeImage finalImg = img;
            ResourceLocation id = PirateCrew.id("pirate_skins/" + hash);
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(finalImg));
                entry.slim = slim;
                entry.texture = id;
            });
        } catch (Exception ex) {
            PirateCrew.LOGGER.debug("Pirate Crew: couldn't load skin {} ({})", url, ex.toString());
        }
    }

    private static byte[] download(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(12000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (PirateCrew Minecraft mod)");
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_BYTES) throw new IllegalStateException("skin too large");
            }
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    /**
     * Returns a 64x64 skin: legacy 64x32 skins are upgraded, HD skins (128, 256...) are scaled down.
     * Returns null for anything else.
     */
    private static NativeImage normalise(NativeImage img) {
        int w = img.getWidth(), h = img.getHeight();
        if (w == 64 && h == 64) return img;
        if (w == 64 && h == 32) {
            NativeImage up = upgradeLegacySkin(img);
            clearSolidHat(up);
            return up;
        }
        if (w >= 128 && w % 64 == 0 && (h == w || h == w / 2)) {
            int f = w / 64;
            NativeImage small = new NativeImage(64, h / f, true);
            for (int y = 0; y < h / f; y++) {
                for (int x = 0; x < 64; x++) {
                    small.setPixelRGBA(x, y, img.getPixelRGBA(x * f, y * f));
                }
            }
            img.close();
            return normalise(small);
        }
        img.close();
        return null;
    }

    private static void setNoAlpha(NativeImage img, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                img.setPixelRGBA(x, y, img.getPixelRGBA(x, y) | 0xFF000000);
            }
        }
    }

    /** Old skins often filled the hat area with a solid colour; if it has no transparency at all, hide it. */
    private static void clearSolidHat(NativeImage img) {
        for (int x = 32; x < 64; x++) {
            for (int y = 0; y < 16; y++) {
                if (((img.getPixelRGBA(x, y) >>> 24) & 0xFF) < 128) return;
            }
        }
        for (int x = 32; x < 64; x++) {
            for (int y = 0; y < 16; y++) {
                img.setPixelRGBA(x, y, img.getPixelRGBA(x, y) & 0x00FFFFFF);
            }
        }
    }

    /** Old 64x32 skins: copy into a 64x64 image and mirror the right arm/leg onto the left. */
    private static NativeImage upgradeLegacySkin(NativeImage old) {
        NativeImage img = new NativeImage(64, 64, true);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                img.setPixelRGBA(x, y, old.getPixelRGBA(x, y));
            }
        }
        old.close();
        // left leg from right leg
        img.copyRect(4, 16, 16, 32, 4, 4, true, false);
        img.copyRect(8, 16, 16, 32, 4, 4, true, false);
        img.copyRect(0, 20, 24, 32, 4, 12, true, false);
        img.copyRect(4, 20, 16, 32, 4, 12, true, false);
        img.copyRect(8, 20, 8, 32, 4, 12, true, false);
        img.copyRect(12, 20, 16, 32, 4, 12, true, false);
        // left arm from right arm
        img.copyRect(44, 16, -8, 32, 4, 4, true, false);
        img.copyRect(48, 16, -8, 32, 4, 4, true, false);
        img.copyRect(40, 20, 0, 32, 4, 12, true, false);
        img.copyRect(44, 20, -8, 32, 4, 12, true, false);
        img.copyRect(48, 20, -16, 32, 4, 12, true, false);
        img.copyRect(52, 20, -8, 32, 4, 12, true, false);
        return img;
    }

    private static String sha1(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
