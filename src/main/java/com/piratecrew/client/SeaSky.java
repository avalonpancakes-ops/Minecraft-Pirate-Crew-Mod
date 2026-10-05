package com.piratecrew.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.piratecrew.PirateCrew;
import com.piratecrew.sundered.SunderedSea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * The Sundered Sea's night sky: slow ribbons of sea-green and violet light ripple over the stars.
 * Drawn right after the vanilla sky (so stars, sun and moon stay), additive, with no depth writes.
 */
@Mod.EventBusSubscriber(modid = PirateCrew.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SeaSky {
    private static final float R = 100F;
    /** centre angle, arc width, base elevation (blocks at R), height, phase. */
    private static final float[][] RIBBONS = {
            {2.10F, 2.6F, 34F, 52F, 0.0F},
            {4.40F, 2.2F, 48F, 40F, 1.7F},
            {0.30F, 2.4F, 40F, 46F, 3.1F},
            {3.20F, 1.6F, 66F, 30F, 4.6F},
    };

    @SubscribeEvent
    public static void afterSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().equals(SunderedSea.LEVEL)) return;
        float pt = event.getPartialTick();
        float strength = Mth.clamp(mc.level.getStarBrightness(pt) * 2.2F, 0F, 1F) * (1F - mc.level.getRainLevel(pt));
        if (strength <= 0.01F) return;
        float time = (mc.level.getGameTime() + pt) / 20F;

        PoseStack ps = event.getPoseStack();
        Matrix4f m = ps.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (float[] r : RIBBONS) ribbon(bb, m, r, time, strength);
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void ribbon(BufferBuilder bb, Matrix4f m, float[] r, float time, float strength) {
        int segs = 128;
        float centre = r[0], arc = r[1], base = r[2], height = r[3], phase = r[4];
        for (int i = 0; i < segs; i++) {
            float u0 = i / (float) segs, u1 = (i + 1) / (float) segs;
            column(bb, m, centre, arc, base, height, phase, time, strength, u0, u1);
        }
    }

    private static void column(BufferBuilder bb, Matrix4f m, float centre, float arc, float base, float height, float phase,
                               float time, float strength, float u0, float u1) {
        float[] a = point(centre, arc, base, height, phase, time, u0);
        float[] b = point(centre, arc, base, height, phase, time, u1);
        float edge0 = Mth.sin(u0 * Mth.PI), edge1 = Mth.sin(u1 * Mth.PI);
        // flickering curtains: brightness rolls along the ribbon
        float f0 = streaks(u0, time, phase), f1 = streaks(u1, time, phase);
        float a0 = 0.62F * strength * edge0 * f0, a1 = 0.62F * strength * edge1 * f1;
        // bottom: sea green; middle: teal; top: violet fading out
        bb.vertex(m, a[0], a[1], a[2]).color(0.25F, 1.0F, 0.62F, a0).endVertex();
        bb.vertex(m, b[0], b[1], b[2]).color(0.25F, 1.0F, 0.62F, a1).endVertex();
        bb.vertex(m, b[0], b[3], b[2]).color(0.25F, 0.75F, 0.95F, a1 * 0.55F).endVertex();
        bb.vertex(m, a[0], a[3], a[2]).color(0.25F, 0.75F, 0.95F, a0 * 0.55F).endVertex();

        bb.vertex(m, a[0], a[3], a[2]).color(0.25F, 0.75F, 0.95F, a0 * 0.55F).endVertex();
        bb.vertex(m, b[0], b[3], b[2]).color(0.25F, 0.75F, 0.95F, a1 * 0.55F).endVertex();
        bb.vertex(m, b[0], b[4], b[2]).color(0.55F, 0.3F, 1.0F, 0F).endVertex();
        bb.vertex(m, a[0], a[4], a[2]).color(0.55F, 0.3F, 1.0F, 0F).endVertex();
    }

    /** Bright vertical rays that slide along the curtain, over a softer rolling glow. */
    private static float streaks(float u, float time, float phase) {
        float rays = Mth.sin(u * 71F + time * 0.9F + phase * 3F);
        rays = rays * rays * rays * rays;
        return 0.35F + 0.35F * (0.5F + 0.5F * Mth.sin(u * 17F + time * 1.3F + phase * 2F)) + 0.5F * rays;
    }

    /** x, bottom y, z, middle y, top y for a point along the ribbon. */
    private static float[] point(float centre, float arc, float base, float height, float phase, float time, float u) {
        float ang = centre + (u - 0.5F) * arc;
        // the curtain folds in and out as it drifts
        float fold = 9F * Mth.sin(u * 9F + time * 0.35F + phase) + 4F * Mth.sin(u * 21F - time * 0.6F + phase);
        float rad = R + fold;
        float x = Mth.cos(ang) * rad, z = Mth.sin(ang) * rad;
        float y = base + 5F * Mth.sin(u * 6F + time * 0.25F + phase);
        float h = height * (0.8F + 0.25F * Mth.sin(u * 13F + time * 0.5F + phase));
        return new float[]{x, y, z, y + h * 0.35F, y + h};
    }
}
