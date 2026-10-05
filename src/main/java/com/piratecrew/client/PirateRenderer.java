package com.piratecrew.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piratecrew.PirateCrew;
import com.piratecrew.entity.PirateEntity;
import com.piratecrew.skin.PirateSkins;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * Renders pirates exactly like players. Skins with thin arms use the slim model,
 * so this hands each pirate to one of two body renderers.
 */
public class PirateRenderer extends EntityRenderer<PirateEntity> {
    private final Body wide;
    private final Body slim;

    public PirateRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.wide = new Body(ctx, false);
        this.slim = new Body(ctx, true);
        this.shadowRadius = 0.5F;
    }

    static boolean isSlim(PirateEntity pirate) {
        return isSlim(pirate.getSkinName(), pirate.getUUID());
    }

    static ResourceLocation texture(PirateEntity pirate) {
        return texture(pirate.getSkinName(), pirate.getUUID());
    }

    public static boolean isSlim(String skin, java.util.UUID id) {
        if (PirateSkins.isValid(skin)) return PirateSkins.isSlim(skin);
        return "slim".equals(DefaultPlayerSkin.getSkinModelName(id));
    }

    public static ResourceLocation texture(String skin, java.util.UUID id) {
        if (PirateSkins.isValid(skin)) return PirateCrew.id("textures/entity/pirate/" + skin + ".png");
        return DefaultPlayerSkin.getDefaultSkin(id);
    }

    private Body pick(PirateEntity pirate) {
        return isSlim(pirate) ? slim : wide;
    }

    @Override
    public void render(PirateEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        pick(entity).render(entity, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(PirateEntity entity) {
        return texture(entity);
    }

    public static class Body extends HumanoidMobRenderer<PirateEntity, PlayerModel<PirateEntity>> {
        public Body(EntityRendererProvider.Context ctx, boolean slim) {
            super(ctx, new PlayerModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5F);
            this.addLayer(new HumanoidArmorLayer<>(this,
                    new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                    ctx.getModelManager()));
        }

        @Override
        public void render(PirateEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
            PlayerModel<PirateEntity> m = this.getModel();
            m.setAllVisible(true);
            HumanoidModel.ArmPose main = armPose(entity, InteractionHand.MAIN_HAND);
            HumanoidModel.ArmPose off = armPose(entity, InteractionHand.OFF_HAND);
            if (main.isTwoHanded()) off = entity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
            boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
            m.rightArmPose = rightHanded ? main : off;
            m.leftArmPose = rightHanded ? off : main;
            m.crouching = entity.isCrouching();
            super.render(entity, yaw, partialTicks, pose, buffers, light);
        }

        /** Same poses players use: drawing a bow, loading/holding a crossbow, raising a trident. */
        private static HumanoidModel.ArmPose armPose(PirateEntity entity, InteractionHand hand) {
            ItemStack stack = entity.getItemInHand(hand);
            if (stack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
            if (entity.isUsingItem() && entity.getUsedItemHand() == hand) {
                UseAnim anim = stack.getUseAnimation();
                if (anim == UseAnim.BOW) return HumanoidModel.ArmPose.BOW_AND_ARROW;
                if (anim == UseAnim.SPEAR) return HumanoidModel.ArmPose.THROW_SPEAR;
                if (anim == UseAnim.CROSSBOW) return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                if (anim == UseAnim.BLOCK) return HumanoidModel.ArmPose.BLOCK;
            }
            if (hand == InteractionHand.MAIN_HAND && stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
            return HumanoidModel.ArmPose.ITEM;
        }

        @Override
        protected void scale(PirateEntity entity, PoseStack pose, float partialTicks) {
            float s = 0.9375F * entity.renderScale(); // players' scale, bigger for bosses
            pose.scale(s, s, s);
        }

        @Override
        public ResourceLocation getTextureLocation(PirateEntity entity) {
            return texture(entity);
        }
    }
}
