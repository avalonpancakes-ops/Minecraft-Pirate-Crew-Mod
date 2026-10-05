package com.piratecrew.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piratecrew.PirateCrew;
import com.piratecrew.entity.boss.KrakenEntity;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** The Kraken: the squid model, recoloured and scaled up to the size of a ship, tentacles writhing. */
public class KrakenRenderer extends MobRenderer<KrakenEntity, SquidModel<KrakenEntity>> {
    private static final ResourceLocation TEXTURE = PirateCrew.id("textures/entity/kraken.png");
    private static final float SCALE = 3.2F;

    public KrakenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SquidModel<>(ctx.bakeLayer(ModelLayers.SQUID)), 2.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(KrakenEntity entity) {
        return TEXTURE;
    }

    /** SquidModel spreads its tentacles by this angle. */
    @Override
    protected float getBob(KrakenEntity entity, float partialTick) {
        float t = entity.tickCount + partialTick;
        return 0.25F + 0.55F * Math.abs(Mth.sin(t * 0.09F));
    }

    @Override
    protected void setupRotations(KrakenEntity entity, PoseStack pose, float bob, float bodyRot, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(0.0F, 0.45F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        float t = entity.tickCount + partialTick;
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 0.05F) * 6.0F));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.cos(t * 0.037F) * 5.0F));
        if (entity.deathTime > 0) {
            float f = Math.min(1.0F, Mth.sqrt((entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F));
            pose.mulPose(Axis.ZP.rotationDegrees(f * 90.0F));
        }
    }
}
