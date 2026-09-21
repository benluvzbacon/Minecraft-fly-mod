package com.flywire.minecraft.registry;

import com.flywire.minecraft.FlyWire;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Registry for the survival-facing FlyWire experiment item. */
public final class FlyWireItems {
    public static final Item FLY_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            Identifier.of(FlyWire.MOD_ID, "fly_spawn_egg"),
            new SpawnEggItem(FlyWireEntities.FLY, 0x30242A, 0xC65B35, new Item.Settings()));

    private FlyWireItems() { }

    public static void register() {
        // Referencing the class completes static registration before the item group hook.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS)
                .register(entries -> entries.add(FLY_SPAWN_EGG));
    }
}
