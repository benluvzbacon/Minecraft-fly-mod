package com.flywire.minecraft.client;

import com.flywire.minecraft.client.model.FlyModel;
import com.flywire.minecraft.client.render.FlyEntityRenderer;
import com.flywire.minecraft.registry.FlyWireEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

public final class FlyWireClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(FlyModel.LAYER, FlyModel::getTexturedModelData);
        EntityRendererRegistry.register(FlyWireEntities.FLY, FlyEntityRenderer::new);
    }
}
