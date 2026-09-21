package com.flywire.minecraft.registry;

import com.flywire.minecraft.FlyWire;
import com.flywire.minecraft.entity.FlyEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.registry.Registries;
import net.minecraft.world.Heightmap;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class FlyWireEntities {
    public static final EntityType<FlyEntity> FLY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(FlyWire.MOD_ID, "fly"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, FlyEntity::new)
                    .dimensions(EntityDimensions.changing(0.30f, 0.20f))
                    .trackRangeBlocks(8)
                    .trackedUpdateRate(2)
                    .spawnRestriction(SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, FlyEntity::canSpawn)
                    .build());

    private FlyWireEntities() { }
    public static void register() { }
}
