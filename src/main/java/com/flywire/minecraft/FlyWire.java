package com.flywire.minecraft;

import com.flywire.minecraft.brain.BrainManager;
import com.flywire.minecraft.command.FlyWireCommands;
import com.flywire.minecraft.entity.FlyEntity;
import com.flywire.minecraft.registry.FlyWireEntities;
import com.flywire.minecraft.registry.FlyWireItems;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.tag.BiomeTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlyWire implements net.fabricmc.api.ModInitializer {
    public static final String MOD_ID = "flywire";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        FlyWireEntities.register();
        FlyWireItems.register();
        FabricDefaultAttributeRegistry.register(FlyWireEntities.FLY, FlyEntity.createAttributes().build());
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD), SpawnGroup.CREATURE,
                FlyWireEntities.FLY, 18, 2, 5);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> FlyWireCommands.register(dispatcher));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof FlyEntity fly) BrainManager.get().forget(fly);
        });
        BrainManager.get().initialize();
        LOGGER.info("FlyWire Brain initialized. Dataset status: {}", BrainManager.get().status());
    }
}
