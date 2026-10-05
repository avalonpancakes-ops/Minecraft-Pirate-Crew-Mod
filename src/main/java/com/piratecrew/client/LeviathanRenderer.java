package com.piratecrew.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piratecrew.PirateCrew;
import com.piratecrew.client.model.LeviathanModel;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Guardian;

/**
 * The Leviathan: a guardian underneath (so its beam still draws) but shown as a great sea serpent. The
 * guardian body is hidden and the serpent model is drawn as a layer in its place.
 */
public class LeviathanRenderer extends GuardianRenderer {
    private static final ResourceLocation TEXTURE = PirateCrew.id("textures/entity/leviathan.png");
    private static final float SCALE = 5.5F;

    public LeviathanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, 3.5F, ModelLayers.ELDER_GUARDIAN);
        addLayer(new Serpent(this, new LeviathanModel(ctx.bakeLayer(LeviathanModel.LAYER))));
    }

    @Override
    public void render(Guardian entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        getModel().root().visible = false;
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    protected void scale(Guardian entity, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(Guardian entity) {
        return TEXTURE;
    }

    private static class Serpent extends RenderLayer<Guardian, GuardianModel> {
        private final LeviathanModel model;

        Serpent(RenderLayerParent<Guardian, GuardianModel> parent, LeviathanModel model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, Guardian entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float age, float headYaw, float headPitch) {
            model.setupAnim(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            model.renderToBuffer(pose, vc, light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
