package com.piratecrew.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.systems.RenderSystem;
import com.piratecrew.PirateCrew;
import com.piratecrew.network.BountyBoardPacket;
import com.piratecrew.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/** Draws a WANTED poster: the poster frame, a portrait in the box, the name and the ruby bounty. */
public class PosterArt {
    public static final ResourceLocation POSTER = PirateCrew.id("textures/gui/wanted_poster.png");
    private static final int TEX_W = 310, TEX_H = 450;

    // Layout inside the poster, as fractions of its width/height
    private static final float BOX_X0 = 0.1224F, BOX_X1 = 0.9002F, BOX_Y0 = 0.2195F, BOX_Y1 = 0.6330F;
    private static final float TEXT_X0 = 0.205F, TEXT_X1 = 0.800F;
    private static final float NAME_Y = 0.735F, BOUNTY_Y = 0.825F;

    private static final int INK = 0xFF2B1A0E;

    /** Skin texture + arm style for a poster, resolved once and cached by the screen. */
    public record Face(ResourceLocation skin, boolean slim) {}

    public static Face faceFor(BountyBoardPacket.Poster p) {
        if (p.npc()) return new Face(PirateRenderer.texture(p.skin(), p.id()), PirateRenderer.isSlim(p.skin(), p.id()));
        Minecraft mc = Minecraft.getInstance();
        // Online players: the client already has their skin.
        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(p.id());
            if (info != null) return new Face(info.getSkinLocation(), "slim".equals(info.getModelName()));
        }
        // Offline players: rebuild their profile from the textures the server saved.
        try {
            GameProfile profile = new GameProfile(p.id(), p.name());
            if (!p.textures().isEmpty()) {
                profile.getProperties().put("textures", new Property("textures", p.textures(),
                        p.texturesSig().isEmpty() ? null : p.texturesSig()));
            }
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = mc.getSkinManager().getInsecureSkinInformation(profile);
            MinecraftProfileTexture tex = map.get(MinecraftProfileTexture.Type.SKIN);
            if (tex != null) {
                return new Face(mc.getSkinManager().registerTexture(tex, MinecraftProfileTexture.Type.SKIN),
                        "slim".equals(tex.getMetadata("model")));
            }
        } catch (Exception ignored) {
        }
        return new Face(DefaultPlayerSkin.getDefaultSkin(p.id()), "slim".equals(DefaultPlayerSkin.getSkinModelName(p.id())));
    }

    public static void draw(GuiGraphics g, Font font, BountyBoardPacket.Poster p, Face face, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        g.blit(POSTER, x, y, w, h, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        int bx0 = x + Math.round(w * BOX_X0), bx1 = x + Math.round(w * BOX_X1);
        int by0 = y + Math.round(h * BOX_Y0), by1 = y + Math.round(h * BOX_Y1);
        drawBust(g, face, bx0, by0, bx1 - bx0, by1 - by0);

        int textW = Math.round(w * (TEXT_X1 - TEXT_X0));
        int cx = x + w / 2;
        drawCentered(g, font, p.name().toUpperCase(), cx, y + Math.round(h * NAME_Y), textW, 1.0F, INK);

        // Bounty: ruby icon + amount
        String amount = String.format("%,d", p.amount());
        float scale = fitScale(font, amount, textW - 12, 1.0F);
        int amountW = Math.round(font.width(amount) * scale);
        int icon = Math.max(6, Math.round(9 * scale));
        int total = icon + 2 + amountW;
        int left = cx - total / 2;
        int ty = y + Math.round(h * BOUNTY_Y);
        g.pose().pushPose();
        g.pose().translate(left, ty - 1, 0);
        g.pose().scale(icon / 16F, icon / 16F, 1);
        g.renderItem(new ItemStack(ModItems.RUBY.get()), 0, 0);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(left + icon + 2, ty, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, amount, 0, 0, INK, false);
        g.pose().popPose();
        RenderSystem.disableBlend();
    }

    private static float fitScale(Font font, String text, int maxW, float preferred) {
        int w = font.width(text);
        return w * preferred <= maxW ? preferred : Math.max(0.45F, maxW / (float) w);
    }

    private static void drawCentered(GuiGraphics g, Font font, String text, int cx, int y, int maxW, float preferred, int color) {
        float scale = fitScale(font, text, maxW, preferred);
        g.pose().pushPose();
        g.pose().translate(cx - font.width(text) * scale / 2F, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    /** Head and shoulders, front view, cut off by the bottom of the box like a mugshot. */
    private static void drawBust(GuiGraphics g, Face face, int x, int y, int w, int h) {
        float s = Math.min(w / 18F, h / 15.5F);
        float ox = x + (w - 16 * s) / 2F;
        float oy = y + h - 15 * s;
        int aw = face.slim() ? 3 : 4;
        ResourceLocation skin = face.skin();

        g.enableScissor(x, y, x + w, y + h);
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        g.pose().scale(s, s, 1);
        // inner layer: arms, body, head
        g.blit(skin, 4 - aw, 8, aw, 12, 44, 20, aw, 12, 64, 64);
        g.blit(skin, 12, 8, aw, 12, 36, 52, aw, 12, 64, 64);
        g.blit(skin, 4, 8, 8, 12, 20, 20, 8, 12, 64, 64);
        g.blit(skin, 4, 0, 8, 8, 8, 8, 8, 8, 64, 64);
        // outer layer: sleeves, jacket, hat
        g.blit(skin, 4 - aw, 8, aw, 12, 44, 36, aw, 12, 64, 64);
        g.blit(skin, 12, 8, aw, 12, 52, 52, aw, 12, 64, 64);
        g.blit(skin, 4, 8, 8, 12, 20, 36, 8, 12, 64, 64);
        g.blit(skin, 4, 0, 8, 8, 40, 8, 8, 8, 64, 64);
        g.pose().popPose();
        g.disableScissor();
    }
}
