package com.flywire.minecraft.client.render;

import com.flywire.minecraft.client.model.FlyModel;
import com.flywire.minecraft.entity.FlyEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public final class FlyEntityRenderer extends MobEntityRenderer<FlyEntity, FlyModel> {
    private static final Identifier TEXTURE = Identifier.of("flywire", "textures/entity/fly.png");

    public FlyEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new FlyModel(context.getPart(FlyModel.LAYER)), 0.12f);
    }

    @Override
    public Identifier getTexture(FlyEntity entity) { return TEXTURE; }

    @Override
    protected void scale(FlyEntity fly, net.minecraft.client.util.math.MatrixStack matrices, float amount) {
        matrices.scale(0.55f, 0.55f, 0.55f);
    }
}
