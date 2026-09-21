package com.flywire.minecraft.client.model;

import com.flywire.minecraft.entity.FlyEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** A deliberately blocky, readable adult fly silhouette with animated flight anatomy. */
public final class FlyModel extends EntityModel<FlyEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(Identifier.of("flywire", "fly"), "main");
    private final ModelPart thorax;
    private final ModelPart abdomen;
    private final ModelPart abdomenTip;
    private final ModelPart head;
    private final ModelPart eyeLeft;
    private final ModelPart eyeRight;
    private final ModelPart proboscis;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart wingVeinLeft;
    private final ModelPart wingVeinRight;
    private final ModelPart[] legs = new ModelPart[6];
    private final ModelPart antennaLeft;
    private final ModelPart antennaRight;

    public FlyModel(ModelPart root) {
        super();
        thorax = root.getChild("thorax");
        abdomen = root.getChild("abdomen");
        abdomenTip = root.getChild("abdomen_tip");
        head = root.getChild("head");
        eyeLeft = root.getChild("eye_left");
        eyeRight = root.getChild("eye_right");
        proboscis = root.getChild("proboscis");
        wingLeft = root.getChild("wing_left");
        wingRight = root.getChild("wing_right");
        wingVeinLeft = root.getChild("wing_vein_left");
        wingVeinRight = root.getChild("wing_vein_right");
        antennaLeft = root.getChild("antenna_left");
        antennaRight = root.getChild("antenna_right");
        for (int i = 0; i < legs.length; i++) legs[i] = root.getChild("leg_" + i);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        // The model is intentionally made from cuboids so it remains legible at fly scale.
        root.addChild("thorax", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4, -3, -3, 8, 5, 6), ModelTransform.pivot(0, 18, 0));
        root.addChild("abdomen", ModelPartBuilder.create().uv(0, 12)
                .cuboid(-3, -2, 2, 6, 4, 7).uv(0, 24)
                .cuboid(-2.5f, -1.5f, 8, 5, 3, 3), ModelTransform.pivot(0, 18, 0));
        root.addChild("abdomen_tip", ModelPartBuilder.create().uv(18, 24)
                .cuboid(-1.5f, -1, 10, 3, 2, 2), ModelTransform.pivot(0, 18, 0));
        root.addChild("head", ModelPartBuilder.create().uv(28, 0)
                .cuboid(-3, -2, -5, 6, 4, 4), ModelTransform.pivot(0, 17, 0));
        root.addChild("eye_left", ModelPartBuilder.create().uv(28, 8)
                .cuboid(2.5f, -1.5f, -5.5f, 2, 3, 2), ModelTransform.pivot(0, 17, 0));
        root.addChild("eye_right", ModelPartBuilder.create().uv(36, 8)
                .cuboid(-4.5f, -1.5f, -5.5f, 2, 3, 2), ModelTransform.pivot(0, 17, 0));
        root.addChild("proboscis", ModelPartBuilder.create().uv(28, 14)
                .cuboid(-1, -1, -8, 2, 2, 3), ModelTransform.pivot(0, 17, 0));

        root.addChild("wing_left", ModelPartBuilder.create().uv(0, 30)
                .cuboid(0, -1, -4, 10, 1, 7), ModelTransform.of(3, 15, 0, 0, 0, -0.18f));
        root.addChild("wing_right", ModelPartBuilder.create().uv(0, 38)
                .cuboid(-10, -1, -4, 10, 1, 7), ModelTransform.of(-3, 15, 0, 0, 0, 0.18f));
        root.addChild("wing_vein_left", ModelPartBuilder.create().uv(22, 30)
                .cuboid(1, -1.2f, -3, 8, 1, 1), ModelTransform.of(3, 15, 0, 0, 0, -0.18f));
        root.addChild("wing_vein_right", ModelPartBuilder.create().uv(22, 38)
                .cuboid(-9, -1.2f, -3, 8, 1, 1), ModelTransform.of(-3, 15, 0, 0, 0, 0.18f));

        root.addChild("antenna_left", ModelPartBuilder.create().uv(40, 0)
                .cuboid(0, -1, -5, 1, 1, 5), ModelTransform.pivot(1, 15, 0));
        root.addChild("antenna_right", ModelPartBuilder.create().uv(40, 6)
                .cuboid(-1, -1, -5, 1, 1, 5), ModelTransform.pivot(-1, 15, 0));

        for (int i = 0; i < 6; i++) {
            int side = i % 2 == 0 ? 1 : -1;
            int z = -2 + (i / 2) * 2;
            int u = 44 + (i / 2) * 6;
            ModelPartBuilder leg = ModelPartBuilder.create().uv(u, 14)
                    .cuboid(side > 0 ? 0 : -4, 0, -1, 4, 1, 1)
                    .uv(u, 18).cuboid(side > 0 ? 3 : -5, 0, -1, 2, 1, 3);
            root.addChild("leg_" + i, leg, ModelTransform.pivot(side * 3, 20, z));
        }
        return TexturedModelData.of(data, 64, 64);
    }

    @Override
    public void setAngles(FlyEntity fly, float limbAngle, float limbDistance, float animationProgress,
                          float headYaw, float headPitch) {
        float flap = fly.isFlying() ? MathHelper.sin(fly.wingPhase()) * 0.95f : 0.04f;
        wingLeft.roll = -0.18f - flap;
        wingRight.roll = 0.18f + flap;
        wingVeinLeft.roll = wingLeft.roll;
        wingVeinRight.roll = wingRight.roll;

        float walk = fly.isFlying() ? 0 : MathHelper.sin(animationProgress * 0.8f) * 0.35f;
        for (int i = 0; i < legs.length; i++) {
            legs[i].yaw = fly.isFlying() ? 0 : walk * (i % 2 == 0 ? 1 : -1);
            legs[i].pitch = fly.isFlying() ? 0.18f : 0;
        }
        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.5f;
        head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE * 0.5f;
        eyeLeft.yaw = head.yaw;
        eyeRight.yaw = head.yaw;
        antennaLeft.pitch = 0.15f + MathHelper.sin(animationProgress * 0.12f) * 0.05f;
        antennaRight.pitch = 0.15f + MathHelper.sin(animationProgress * 0.12f + 1) * 0.05f;
        abdomen.pitch = fly.isFlying() ? MathHelper.sin(fly.wingPhase() * 0.5f) * 0.04f : 0;
        abdomenTip.pitch = abdomen.pitch;
        proboscis.pitch = fly.isFeeding() ? 0.35f : 0;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        thorax.render(matrices, vertices, light, overlay, color);
        abdomen.render(matrices, vertices, light, overlay, color);
        abdomenTip.render(matrices, vertices, light, overlay, color);
        head.render(matrices, vertices, light, overlay, color);
        eyeLeft.render(matrices, vertices, light, overlay, color);
        eyeRight.render(matrices, vertices, light, overlay, color);
        proboscis.render(matrices, vertices, light, overlay, color);
        wingLeft.render(matrices, vertices, light, overlay, color);
        wingRight.render(matrices, vertices, light, overlay, color);
        wingVeinLeft.render(matrices, vertices, light, overlay, color);
        wingVeinRight.render(matrices, vertices, light, overlay, color);
        antennaLeft.render(matrices, vertices, light, overlay, color);
        antennaRight.render(matrices, vertices, light, overlay, color);
        for (ModelPart leg : legs) leg.render(matrices, vertices, light, overlay, color);
    }
}
