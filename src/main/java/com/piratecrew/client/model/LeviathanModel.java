package com.piratecrew.client.model;

import com.piratecrew.PirateCrew;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Guardian;

/**
 * The Leviathan as a great sea serpent: a horned head with a working jaw, and six tapering body
 * segments with dorsal fins ending in a tail fin, rippling side to side as it swims.
 */
public class LeviathanModel extends HierarchicalModel<Guardian> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(PirateCrew.id("leviathan"), "main");
    private static final int[] SIZE = {9, 8, 7, 6, 5, 4};
    private static final int[][] UV = {{0, 36}, {40, 21}, {40, 40}, {76, 0}, {76, 16}, {76, 32}};

    private final ModelPart root, head, jaw;
    private final ModelPart[] body = new ModelPart[SIZE.length];

    public LeviathanModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        ModelPart p = head;
        for (int i = 0; i < SIZE.length; i++) {
            p = p.getChild("body" + i);
            body[i] = p;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -5, -12, 10, 9, 12), PartPose.offset(0, 12, -6));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 21).addBox(-4, 0, -11, 8, 3, 11), PartPose.offset(0, 4, 0));
        head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -0.5F, 0, 1, 1, 6), PartPose.offsetAndRotation(3, -5, -3, 0.55F, 0.3F, 0));
        head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -0.5F, 0, 1, 1, 6), PartPose.offsetAndRotation(-3, -5, -3, 0.55F, -0.3F, 0));
        PartDefinition p = head;
        for (int i = 0; i < SIZE.length; i++) {
            float r = SIZE[i] / 2F;
            PartDefinition seg = p.addOrReplaceChild("body" + i, CubeListBuilder.create().texOffs(UV[i][0], UV[i][1]).addBox(-r, -r, 0, SIZE[i], SIZE[i], 10),
                    PartPose.offset(0, i == 0 ? -0.5F : 0, i == 0 ? 0 : 10));
            seg.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(110, 0).addBox(-0.5F, -3, 0, 1, 3, 6), PartPose.offset(0, -r, 2));
            if (i == SIZE.length - 1) {
                seg.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(110, 10).addBox(-0.5F, -4, 0, 1, 8, 6), PartPose.offset(0, 0, 9));
            }
            p = seg;
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Guardian entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        float speed = 0.12F + Mth.clamp(limbSwingAmount, 0, 1) * 0.15F;
        head.yRot = Mth.sin(age * speed) * 0.12F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;
        jaw.xRot = 0.15F + Mth.sin(age * 0.07F) * 0.12F + (entity.hasActiveAttackTarget() ? 0.35F : 0);
        for (int i = 0; i < body.length; i++) {
            body[i].yRot = Mth.sin(age * speed - (i + 1) * 0.8F) * 0.3F;
            body[i].xRot = Mth.sin(age * speed * 0.5F - i * 0.6F) * 0.06F;
        }
    }
}
