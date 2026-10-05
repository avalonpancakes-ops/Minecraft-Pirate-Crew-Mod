package com.piratecrew.client.model;

import com.piratecrew.PirateCrew;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Captain's Tricorn: a felt crown with three upturned brim walls that meet in points, flaring
 * outward, gold-trimmed, with a skull badge on the front wall. Worn on the head part of a humanoid.
 */
public class TricornModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(PirateCrew.id("tricorn"), "main");

    public TricornModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        // crown, sitting just over the top of the head
        head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.5F, -12.0F, -4.5F, 9, 4, 9, new CubeDeformation(0.05F)), PartPose.ZERO);
        // three brim walls, 120 degrees apart; each tilts outward before it is turned into place
        float apothem = 4.9F;
        for (int i = 0; i < 3; i++) {
            float yaw = (float) (i * Math.PI * 2 / 3);
            head.addOrReplaceChild("wall" + i, CubeListBuilder.create().texOffs(0, 14 + i * 6)
                            .addBox(-8.5F, -4.5F, -apothem - 0.5F, 17, 4, 1),
                    PartPose.offsetAndRotation(0.0F, -7.6F, 0.0F, 0.42F, yaw, 0.0F));
        }
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
