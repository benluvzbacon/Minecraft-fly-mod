package com.flywire.minecraft.brain;

public record BrainStats(int neurons, int edges, int activeNeurons, float sensoryDrive,
                         MotorOutput motor, String behavior, long updateNanos) { }
