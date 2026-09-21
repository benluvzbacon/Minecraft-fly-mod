package com.flywire.minecraft.brain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {
    @Test
    void defaultsStayWithinSafeBudget() {
        NeuralConfig config = NeuralConfig.load();
        assertTrue(config.maxSubsteps() >= 1 && config.maxSubsteps() <= 32);
        assertTrue(config.maxFlies() >= 1 && config.maxFlies() <= 256);
    }
}
