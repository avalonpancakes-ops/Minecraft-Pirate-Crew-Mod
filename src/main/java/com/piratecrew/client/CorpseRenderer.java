package com.piratecrew.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piratecrew.entity.CorpseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/** Draws a dead player's body, wearing their own skin, lying on its side like the vanilla death pose. */
public class CorpseRenderer extends EntityRenderer<CorpseEntity> {
    private final PlayerModel<LivingEntity> wide;
    private final PlayerModel<LivingEntity> slim;

    public CorpseRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.wide = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        this.slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.shadowRadius = 0.6F;
    }

    private static PlayerInfo info(CorpseEntity c) {
        UUID owner = c.getOwner();
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        return owner == null || conn == null ? null : conn.getPlayerInfo(owner);
    }

    private static boolean isSlim(CorpseEntity c) {
        PlayerInfo info = info(c);
        if (info != null) return "slim".equals(info.getModelName());
        UUID owner = c.getOwner();
        return owner != null && "slim".equals(DefaultPlayerSkin.getSkinModelName(owner));
    }

    @Override
    public ResourceLocation getTextureLocation(CorpseEntity c) {
        PlayerInfo info = info(c);
        if (info != null) return info.getSkinLocation();
        UUID owner = c.getOwner();
        return owner != null ? DefaultPlayerSkin.getDefaultSkin(owner) : DefaultPlayerSkin.getDefaultSkin();
    }

    @Override
    public void render(CorpseEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        PlayerModel<LivingEntity> model = isSlim(entity) ? slim : wide;
        model.setAllVisible(true);
        model.young = false;
        model.crouching = false;
        model.riding = false;
        pose.pushPose();
        pose.translate(0.0F, 0.47F, 0.0F); // resting on its side, not sunk into the ground
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        pose.translate(0.9F, 0.0F, 0.0F); // centre the body on the corpse's position
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F)); // fallen over, like the end of the death animation
        pose.scale(-0.9375F, -0.9375F, 0.9375F);
        pose.translate(0.0F, -1.501F, 0.0F);
        model.renderToBuffer(pose, buffers.getBuffer(model.renderType(getTextureLocation(entity))), light,
                OverlayTexture.NO_OVERLAY, 0.85F, 0.85F, 0.85F, 1.0F);
        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }
}
