package com.flywire.minecraft.brain;

/** Runtime knobs intentionally live in system properties so a server needs no config library. */
public record NeuralConfig(boolean enabled, boolean debug, boolean profile, int maxSubsteps, int maxFlies) {
    public static NeuralConfig load() {
        return new NeuralConfig(
                Boolean.parseBoolean(System.getProperty("flywire.neural.enabled", "true")),
                Boolean.parseBoolean(System.getProperty("flywire.neural.debug", "false")),
                Boolean.parseBoolean(System.getProperty("flywire.neural.profile", "false")),
                clamp(Integer.getInteger("flywire.neural.maxSubsteps", 4), 1, 32),
                clamp(Integer.getInteger("flywire.neural.maxFlies", 32), 1, 256));
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
