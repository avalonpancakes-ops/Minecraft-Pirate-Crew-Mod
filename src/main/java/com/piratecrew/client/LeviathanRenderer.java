package com.piratecrew.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piratecrew.PirateCrew;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Guardian;

/** The Leviathan: an elder guardian, recoloured and grown to the size of a ship. */
public class LeviathanRenderer extends GuardianRenderer {
    private static final ResourceLocation TEXTURE = PirateCrew.id("textures/entity/leviathan.png");
    private static final float SCALE = 5.9F;

    public LeviathanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, 3.5F, ModelLayers.ELDER_GUARDIAN);
    }

    @Override
    protected void scale(Guardian entity, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(Guardian entity) {
        return TEXTURE;
    }
}
