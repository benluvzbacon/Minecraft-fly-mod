package com.flywire.minecraft.brain;

public record MotorOutput(float forward, float strafe, float vertical, float turn,
                          float wingBeat, float takeoff, float landing, float feeding,
                          float escape, int activeNeurons, long updateNanos) {
    public static final MotorOutput SILENT = new MotorOutput(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
}
