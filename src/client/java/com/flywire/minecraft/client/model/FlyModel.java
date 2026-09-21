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

public final class FlyModel extends EntityModel<FlyEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(Identifier.of("flywire", "fly"), "main");
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart[] legs = new ModelPart[6];
    private final ModelPart antennaLeft;
    private final ModelPart antennaRight;

    public FlyModel(ModelPart root) {
        super();
        body = root.getChild("body");
        head = root.getChild("head");
        wingLeft = root.getChild("wing_left");
        wingRight = root.getChild("wing_right");
        antennaLeft = root.getChild("antenna_left");
        antennaRight = root.getChild("antenna_right");
        for (int i = 0; i < legs.length; i++) legs[i] = root.getChild("leg_" + i);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-3, -2, -4, 6, 4, 8), ModelTransform.pivot(0, 18, 0));
        root.addChild("head", ModelPartBuilder.create().uv(0, 13).cuboid(-3, -2, -3, 6, 4, 4), ModelTransform.pivot(0, 17, -5));
        root.addChild("wing_left", ModelPartBuilder.create().uv(0, 18).cuboid(0, -1, -3, 7, 1, 6), ModelTransform.of(2, 15, -1, 0, 0, -0.18f));
        root.addChild("wing_right", ModelPartBuilder.create().uv(0, 25).cuboid(-7, -1, -3, 7, 1, 6), ModelTransform.of(-2, 15, -1, 0, 0, 0.18f));
        root.addChild("antenna_left", ModelPartBuilder.create().uv(20, 13).cuboid(0, -1, -5, 1, 1, 4), ModelTransform.pivot(1, 16, -5));
        root.addChild("antenna_right", ModelPartBuilder.create().uv(20, 18).cuboid(-1, -1, -5, 1, 1, 4), ModelTransform.pivot(-1, 16, -5));
        for (int i = 0; i < 6; i++) {
            int side = i % 2 == 0 ? 1 : -1;
            int z = -2 + (i / 2) * 2;
            root.addChild("leg_" + i, ModelPartBuilder.create().uv(20, 0).cuboid(side > 0 ? 0 : -3, 0, -1, 3, 1, 1),
                    ModelTransform.pivot(side * 2, 20, z));
        }
        return TexturedModelData.of(data, 32, 32);
    }

    @Override
    public void setAngles(FlyEntity fly, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        float flap = fly.isFlying() ? MathHelper.sin(fly.wingPhase()) * 0.8f : 0.08f;
        wingLeft.roll = -0.18f - flap;
        wingRight.roll = 0.18f + flap;
        for (int i = 0; i < legs.length; i++) {
            legs[i].yaw = fly.isFlying() ? 0 : MathHelper.sin(animationProgress * 0.8f + i) * 0.25f;
        }
        head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.5f;
        head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE * 0.5f;
        antennaLeft.pitch = 0.15f + MathHelper.sin(animationProgress * 0.12f) * 0.05f;
        antennaRight.pitch = 0.15f + MathHelper.sin(animationProgress * 0.12f + 1) * 0.05f;
        body.pitch = fly.isFeeding() ? 0.2f : 0;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        body.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        head.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        wingLeft.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        wingRight.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        antennaLeft.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        antennaRight.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        for (ModelPart leg : legs) leg.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
