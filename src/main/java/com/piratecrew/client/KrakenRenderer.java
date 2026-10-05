package com.piratecrew.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piratecrew.PirateCrew;
import com.piratecrew.client.model.KrakenModel;
import com.piratecrew.entity.boss.KrakenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** The Kraken, drawn with its own many-armed model at the size of a ship. */
public class KrakenRenderer extends MobRenderer<KrakenEntity, KrakenModel> {
    private static final ResourceLocation TEXTURE = PirateCrew.id("textures/entity/kraken.png");
    private static final float SCALE = 2.8F;

    public KrakenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KrakenModel(ctx.bakeLayer(KrakenModel.LAYER)), 3.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(KrakenEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(KrakenEntity entity, PoseStack pose, float age, float bodyRot, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        float t = entity.tickCount + partialTick;
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 0.04F) * 3.0F));
        if (entity.deathTime > 0) {
            float f = Math.min(1.0F, Mth.sqrt((entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F));
            pose.mulPose(Axis.ZP.rotationDegrees(f * 90.0F));
        }
    }
}
