package com.flywire.minecraft.command;

import com.flywire.minecraft.brain.BrainManager;
import com.flywire.minecraft.brain.BrainStats;
import com.flywire.minecraft.entity.FlyEntity;
import com.flywire.minecraft.registry.FlyWireEntities;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public final class FlyWireCommands {
    private FlyWireCommands() { }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("flywire")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("spawn").executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayerOrThrow();
                    FlyEntity fly = FlyWireEntities.FLY.create(source.getWorld());
                    if (fly == null) return 0;
                    fly.refreshPositionAndAngles(player.getX(), player.getY() + 1.0, player.getZ(), player.getYaw(), 0);
                    source.getWorld().spawnEntity(fly);
                    source.sendFeedback(() -> Text.literal("Spawned a FlyWire fly."), false);
                    return 1;
                }))
                .then(literal("stats").executes(context -> {
                    ServerCommandSource source = context.getSource();
                    source.sendFeedback(() -> Text.literal("FlyWire brain: " + BrainManager.get().status()
                            + " | simulated flies: " + BrainManager.get().simulatedFlies()), false);
                    return 1;
                }))
                .then(literal("brain").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    FlyEntity nearest = null;
                    double distance = Double.MAX_VALUE;
                    for (FlyEntity entity : player.getWorld().getEntitiesByClass(FlyEntity.class,
                            player.getBoundingBox().expand(16), ignored -> true)) {
                        double d = player.squaredDistanceTo(entity);
                        if (d < distance) { distance = d; nearest = entity; }
                    }
                    if (nearest == null) {
                        context.getSource().sendFeedback(() -> Text.literal("No FlyWire fly nearby."), false);
                        return 0;
                    }
                    BrainStats stats = BrainManager.get().stats(nearest);
                    context.getSource().sendFeedback(() -> Text.literal("Fly brain: " + stats.behavior()
                            + ", active " + stats.activeNeurons() + "/" + stats.neurons()
                            + ", edges " + stats.edges() + ", update " + stats.updateNanos() + " ns"), false);
                    return 1;
                })));
    }
}
