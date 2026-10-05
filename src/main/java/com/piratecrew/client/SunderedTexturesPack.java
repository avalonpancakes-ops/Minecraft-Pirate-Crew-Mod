package com.piratecrew.client;

import com.google.gson.JsonObject;
import com.piratecrew.PirateCrew;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in resource pack that skins the Kraken and the Leviathan by recolouring the player's own
 * vanilla squid and elder guardian textures while the game loads (their models use vanilla's UV
 * layouts). Nothing from Minecraft ships in the jar; if a vanilla texture can't be read, the texture
 * bundled in the mod is used instead.
 */
public class SunderedTexturesPack implements PackResources {
    private static final String ID = "piratecrew_sundered_textures";

    private record Recipe(ResourceLocation source, int[][] ramp) {}

    // Colour ramps by luminance {lum, r, g, b}. Keep in sync with tools/gen_sundered.py.
    static final int[][] TIDESTEEL = {{0, 8, 30, 36}, {70, 30, 92, 104}, {140, 70, 160, 168}, {200, 140, 220, 218}, {255, 225, 255, 250}};
    static final int[][] ABYSSAL = {{0, 6, 4, 16}, {70, 28, 18, 66}, {140, 70, 48, 150}, {200, 128, 98, 225}, {255, 210, 190, 255}};
    static final int[][] KRAKENBONE = {{0, 34, 16, 34}, {70, 96, 66, 92}, {140, 168, 140, 156}, {200, 222, 206, 210}, {255, 255, 248, 240}};
    static final int[][] STORMFORGED = {{0, 8, 12, 36}, {70, 32, 60, 128}, {140, 80, 140, 224}, {200, 170, 210, 250}, {255, 250, 248, 150}};
    static final int[][] LEVIATHAN = {{0, 4, 26, 22}, {70, 16, 88, 72}, {140, 50, 170, 136}, {200, 130, 230, 190}, {255, 220, 255, 235}};
    static final int[][] SOVEREIGN = {{0, 36, 6, 6}, {70, 128, 26, 18}, {140, 210, 110, 28}, {200, 248, 196, 70}, {255, 255, 246, 190}};
    static final int[][] MARINE = {{0, 10, 22, 40}, {90, 30, 80, 100}, {170, 60, 170, 170}, {255, 230, 220, 140}};
    static final int[][] SIREN = {{0, 40, 6, 20}, {80, 150, 30, 70}, {160, 240, 110, 130}, {255, 255, 230, 220}};
    static final int[][] PORTAL = {{0, 10, 30, 40}, {80, 20, 110, 120}, {160, 60, 200, 190}, {255, 210, 255, 240}};
    static final int[][] KRAKEN = {{0, 20, 4, 18}, {80, 90, 20, 60}, {160, 170, 60, 110}, {255, 250, 190, 200}};

    private static final Map<ResourceLocation, Recipe> RECIPES = new HashMap<>();

    private static void item(String name, String vanilla, int[][] ramp) {
        RECIPES.put(PirateCrew.id("textures/item/" + name + ".png"), new Recipe(new ResourceLocation("minecraft", "textures/" + vanilla + ".png"), ramp));
    }

    private static void tex(String path, String vanilla, int[][] ramp) {
        RECIPES.put(PirateCrew.id("textures/" + path + ".png"), new Recipe(new ResourceLocation("minecraft", "textures/" + vanilla + ".png"), ramp));
    }

    static {
        // Items and the portal have their own hand-drawn textures; only the giant sea beasts reuse vanilla shapes.
        tex("entity/kraken", "entity/squid/squid", KRAKEN);
        tex("entity/leviathan", "entity/guardian_elder", LEVIATHAN);
    }

    private final Map<ResourceLocation, byte[]> cache = new ConcurrentHashMap<>();
    private final byte[] mcmeta;

    public SunderedTexturesPack() {
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "Pirate Crew Sundered Sea textures");
        pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        this.mcmeta = GsonHelper.toStableString(root).getBytes(StandardCharsets.UTF_8);
    }

    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        Pack pack = Pack.readMetaAndCreate(ID, Component.literal("Pirate Crew Sundered Sea"), true,
                id -> new SunderedTexturesPack(), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    @Nullable
    private byte[] generate(ResourceLocation target) {
        byte[] cached = cache.get(target);
        if (cached != null) return cached.length == 0 ? null : cached;
        byte[] result = new byte[0];
        Recipe r = RECIPES.get(target);
        try (InputStream in = openVanilla(r.source())) {
            if (in != null) {
                BufferedImage src = ImageIO.read(in);
                if (src != null) result = toPng(recolour(src, r.ramp()));
            }
        } catch (Exception e) {
            PirateCrew.LOGGER.warn("Pirate Crew: couldn't recolour {}, using the built-in texture", target, e);
        }
        cache.put(target, result);
        return result.length == 0 ? null : result;
    }

    @Nullable
    private static InputStream openVanilla(ResourceLocation loc) throws IOException {
        try {
            IoSupplier<InputStream> s = Minecraft.getInstance().getVanillaPackResources().getResource(PackType.CLIENT_RESOURCES, loc);
            if (s != null) return s.get();
        } catch (Exception ignored) {
        }
        return Minecraft.class.getResourceAsStream("/assets/" + loc.getNamespace() + "/" + loc.getPath());
    }

    /** Every visible pixel is recoloured by its brightness, keeping the shape's shading. */
    private static BufferedImage recolour(BufferedImage src, int[][] ramp) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                int a = argb >>> 24;
                if (a > 0) {
                    int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                    int lum = Math.min(255, (r * 30 + g * 59 + b * 11) / 100);
                    int[] c = ramp(ramp, lum);
                    argb = (a << 24) | (c[0] << 16) | (c[1] << 8) | c[2];
                }
                out.setRGB(x, y, argb);
            }
        }
        return out;
    }

    private static int[] ramp(int[][] ramp, int lum) {
        for (int i = 1; i < ramp.length; i++) {
            if (lum <= ramp[i][0]) {
                int[] lo = ramp[i - 1], hi = ramp[i];
                float t = (lum - lo[0]) / (float) Math.max(1, hi[0] - lo[0]);
                return new int[]{Math.round(lo[1] + (hi[1] - lo[1]) * t), Math.round(lo[2] + (hi[2] - lo[2]) * t), Math.round(lo[3] + (hi[3] - lo[3]) * t)};
            }
        }
        int[] last = ramp[ramp.length - 1];
        return new int[]{last[1], last[2], last[3]};
    }

    private static byte[] toPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (String.join("/", path).equals("pack.mcmeta")) return () -> new ByteArrayInputStream(mcmeta);
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation loc) {
        if (type != PackType.CLIENT_RESOURCES || !RECIPES.containsKey(loc)) return null;
        byte[] bytes = generate(loc);
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) return;
        for (ResourceLocation loc : RECIPES.keySet()) {
            if (loc.getNamespace().equals(namespace) && loc.getPath().startsWith(path)) {
                byte[] bytes = generate(loc);
                if (bytes != null) output.accept(loc, () -> new ByteArrayInputStream(bytes));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(PirateCrew.MODID) : Set.of();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        JsonObject json = GsonHelper.parse(new String(mcmeta, StandardCharsets.UTF_8));
        if (!json.has(serializer.getMetadataSectionName())) return null;
        return serializer.fromJson(GsonHelper.getAsJsonObject(json, serializer.getMetadataSectionName()));
    }

    @Override
    public String packId() {
        return ID;
    }

    @Override
    public void close() {
    }
}
