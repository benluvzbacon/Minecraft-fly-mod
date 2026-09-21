package com.flywire.minecraft.brain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataValidationTest {
    @Test
    void rejectsNonMonotonicCsr() {
        assertThrows(IllegalArgumentException.class, () -> new Connectome(
                new long[]{1, 2}, new int[]{0, 2, 1}, new int[]{0}, new int[]{1}, new byte[]{1}));
    }

    @Test
    void rejectsDanglingTarget() {
        assertThrows(IllegalArgumentException.class, () -> new Connectome(
                new long[]{1, 2}, new int[]{0, 1, 1}, new int[]{2}, new int[]{1}, new byte[]{1}));
    }
}
