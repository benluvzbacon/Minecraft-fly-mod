package com.flywire.minecraft.brain;

/**
 * Runtime knobs intentionally live in system properties so a server needs no config library.
 * The input budget limits how many real annotated sensory neurons receive an embodiment
 * current on one tick; it does not remove any edges or neurons from the shared connectome.
 */
public record NeuralConfig(boolean enabled, boolean debug, boolean profile, int maxSubsteps,
                           int maxFlies, int maxInputNeurons) {
    /** Compatibility constructor used by small deterministic unit fixtures. */
    public NeuralConfig(boolean enabled, boolean debug, boolean profile, int maxSubsteps, int maxFlies) {
        this(enabled, debug, profile, maxSubsteps, maxFlies, 256);
    }

    public static NeuralConfig load() {
        return new NeuralConfig(
                Boolean.parseBoolean(System.getProperty("flywire.neural.enabled", "true")),
                Boolean.parseBoolean(System.getProperty("flywire.neural.debug", "false")),
                Boolean.parseBoolean(System.getProperty("flywire.neural.profile", "false")),
                clamp(Integer.getInteger("flywire.neural.maxSubsteps", 8), 1, 32),
                clamp(Integer.getInteger("flywire.neural.maxFlies", 8), 1, 256),
                clamp(Integer.getInteger("flywire.neural.maxInputNeurons", 256), 16, 2048));
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
