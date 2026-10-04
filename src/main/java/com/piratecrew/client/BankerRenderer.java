package com.piratecrew.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piratecrew.PirateCrew;
import com.piratecrew.entity.BankerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The bank teller, drawn like a player with the banker skin. */
public class BankerRenderer extends HumanoidMobRenderer<BankerEntity, PlayerModel<BankerEntity>> {
    private static final ResourceLocation SKIN = PirateCrew.id("textures/entity/banker.png");

    public BankerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public void render(BankerEntity entity, float yaw, float partialTicks, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        PlayerModel<BankerEntity> m = this.getModel();
        m.setAllVisible(true);
        m.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        m.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        m.crouching = false;
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    protected void scale(BankerEntity entity, PoseStack pose, float partialTicks) {
        pose.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public ResourceLocation getTextureLocation(BankerEntity entity) {
        return SKIN;
    }
}
