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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A built-in resource pack that makes ruby tool textures by recolouring the player's own
 * vanilla iron tool textures while the game loads: grey metal becomes ruby, the wooden handle is
 * left alone. Nothing from Minecraft is shipped in the mod jar. If a vanilla texture can't be
 * read, the pack provides nothing for it and the hand-drawn texture in the mod is used instead.
 */
public class RubyToolsPack implements PackResources {
    private static final String ID = "piratecrew_ruby_tools";

    /** our texture -> vanilla texture it's made from */
    private static final Map<ResourceLocation, ResourceLocation> SOURCES = Map.of(
            PirateCrew.id("textures/item/ruby_axe.png"), new ResourceLocation("minecraft", "textures/item/iron_axe.png"),
            PirateCrew.id("textures/item/ruby_shovel.png"), new ResourceLocation("minecraft", "textures/item/iron_shovel.png"));

    /** Ruby colour ramp by brightness: {brightness, r, g, b} */
    private static final int[][] RAMP = {
            {0, 34, 0, 10}, {50, 74, 4, 20}, {100, 130, 10, 34}, {150, 190, 22, 50},
            {200, 236, 70, 96}, {255, 255, 188, 198}};

    private final Map<ResourceLocation, byte[]> cache = new ConcurrentHashMap<>();
    private final byte[] mcmeta;

    public RubyToolsPack() {
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "Pirate Crew ruby tool textures");
        pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        this.mcmeta = GsonHelper.toStableString(root).getBytes(StandardCharsets.UTF_8);
    }

    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        Pack pack = Pack.readMetaAndCreate(ID, Component.literal("Pirate Crew ruby tools"), true,
                id -> new RubyToolsPack(), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack != null) event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    // ------------------------------------------------------------------ recolouring

    @Nullable
    private byte[] generate(ResourceLocation target) {
        byte[] cached = cache.get(target);
        if (cached != null) return cached.length == 0 ? null : cached;
        byte[] result = new byte[0];
        try (InputStream in = openVanilla(SOURCES.get(target))) {
            if (in != null) {
                BufferedImage src = ImageIO.read(in);
                if (src != null) result = toPng(recolour(src));
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

    private static BufferedImage recolour(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                int a = argb >>> 24, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
                boolean metal = a > 0 && (max - min) <= Math.max(18, max * 0.15);
                if (metal) {
                    int lum = (r * 30 + g * 59 + b * 11) / 100;
                    int[] c = ramp(lum);
                    argb = (a << 24) | (c[0] << 16) | (c[1] << 8) | c[2];
                }
                out.setRGB(x, y, argb);
            }
        }
        return out;
    }

    private static int[] ramp(int lum) {
        for (int i = 1; i < RAMP.length; i++) {
            if (lum <= RAMP[i][0]) {
                int[] lo = RAMP[i - 1], hi = RAMP[i];
                float t = (lum - lo[0]) / (float) (hi[0] - lo[0]);
                return new int[]{
                        Math.round(lo[1] + (hi[1] - lo[1]) * t),
                        Math.round(lo[2] + (hi[2] - lo[2]) * t),
                        Math.round(lo[3] + (hi[3] - lo[3]) * t)};
            }
        }
        int[] last = RAMP[RAMP.length - 1];
        return new int[]{last[1], last[2], last[3]};
    }

    private static byte[] toPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ PackResources

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (String.join("/", path).equals("pack.mcmeta")) return () -> new ByteArrayInputStream(mcmeta);
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation loc) {
        if (type != PackType.CLIENT_RESOURCES || !SOURCES.containsKey(loc)) return null;
        byte[] bytes = generate(loc);
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) return;
        for (ResourceLocation loc : SOURCES.keySet()) {
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
