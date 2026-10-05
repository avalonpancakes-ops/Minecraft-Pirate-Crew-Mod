package com.piratecrew.client.model;

import com.piratecrew.PirateCrew;
import com.piratecrew.entity.boss.KrakenEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Kraken: a tall mantle with a crest, eight jointed tentacles splayed in a ring that ripple in
 * waves, and two long feeding arms reaching forward.
 */
public class KrakenModel extends HierarchicalModel<KrakenEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(PirateCrew.id("kraken"), "main");
    private static final int TENTACLES = 8, SEGMENTS = 3, ARMS = 2, ARM_SEGMENTS = 4;

    private final ModelPart root, mantle;
    private final ModelPart[][] tentacles = new ModelPart[TENTACLES][SEGMENTS];
    private final ModelPart[][] arms = new ModelPart[ARMS][ARM_SEGMENTS];

    public KrakenModel(ModelPart root) {
        this.root = root;
        this.mantle = root.getChild("mantle");
        for (int i = 0; i < TENTACLES; i++) {
            ModelPart p = root.getChild("tentacle" + i);
            for (int j = 0; j < SEGMENTS; j++) {
                p = p.getChild("s" + j);
                tentacles[i][j] = p;
            }
        }
        for (int i = 0; i < ARMS; i++) {
            ModelPart p = root.getChild("arm" + i);
            for (int j = 0; j < ARM_SEGMENTS; j++) {
                p = p.getChild("s" + j);
                arms[i][j] = p;
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition mantle = root.addOrReplaceChild("mantle", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -20, -6, 12, 20, 12), PartPose.ZERO);
        mantle.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 32).addBox(-4, -6, -4, 8, 6, 8), PartPose.offset(0, -20, 0));
        for (int i = 0; i < TENTACLES; i++) {
            float a = (float) (i * Math.PI * 2 / TENTACLES + Math.PI / TENTACLES);
            PartDefinition t = root.addOrReplaceChild("tentacle" + i, CubeListBuilder.create(),
                    PartPose.offsetAndRotation(Mth.sin(a) * 4.5F, -1, Mth.cos(a) * 4.5F, 0, a, 0));
            PartDefinition s0 = t.addOrReplaceChild("s0", CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, 0, -1.5F, 3, 8, 3), PartPose.rotation(0.4F, 0, 0));
            PartDefinition s1 = s0.addOrReplaceChild("s1", CubeListBuilder.create().texOffs(48, 11).addBox(-1, 0, -1, 2, 8, 2), PartPose.offset(0, 7.5F, 0));
            s1.addOrReplaceChild("s2", CubeListBuilder.create().texOffs(56, 11).addBox(-0.5F, 0, -0.5F, 1, 8, 1), PartPose.offset(0, 7.5F, 0));
        }
        for (int i = 0; i < ARMS; i++) {
            float a = (float) Math.PI + (i == 0 ? -0.45F : 0.45F);
            PartDefinition arm = root.addOrReplaceChild("arm" + i, CubeListBuilder.create(),
                    PartPose.offsetAndRotation(Mth.sin(a) * 3.5F, -3, Mth.cos(a) * 3.5F, 0, a, 0));
            PartDefinition p = arm;
            for (int j = 0; j < ARM_SEGMENTS; j++) {
                p = p.addOrReplaceChild("s" + j, CubeListBuilder.create().texOffs(48, 21).addBox(-1, 0, -1, 2, 9, 2),
                        j == 0 ? PartPose.rotation(0.9F, 0, 0) : PartPose.offset(0, 8.5F, 0));
            }
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(KrakenEntity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        float swim = Mth.clamp(limbSwingAmount * 2F, 0, 1);
        mantle.xRot = Mth.sin(age * 0.05F) * 0.05F - swim * 0.15F;
        for (int i = 0; i < TENTACLES; i++) {
            float phase = age * 0.12F + i * 0.8F;
            tentacles[i][0].xRot = 0.45F + Mth.sin(phase) * 0.15F + swim * 0.35F;
            tentacles[i][0].zRot = Mth.sin(phase * 0.7F) * 0.08F;
            tentacles[i][1].xRot = Mth.sin(phase - 0.7F) * 0.35F - 0.1F;
            tentacles[i][2].xRot = Mth.sin(phase - 1.4F) * 0.55F - 0.15F;
        }
        for (int i = 0; i < ARMS; i++) {
            for (int j = 0; j < ARM_SEGMENTS; j++) {
                float phase = age * 0.09F + i * 1.7F - j * 0.6F;
                arms[i][j].xRot = (j == 0 ? 0.9F : 0.05F) + Mth.sin(phase) * (0.18F + j * 0.08F);
                arms[i][j].zRot = Mth.cos(phase * 0.8F) * 0.06F * (j + 1);
            }
        }
    }
}
