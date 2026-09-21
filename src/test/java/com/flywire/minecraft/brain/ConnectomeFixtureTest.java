package com.flywire.minecraft.brain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectomeFixtureTest {
    @Test
    void rootIndexLookupIsBinarySearchable() {
        Connectome graph = new Connectome(new long[]{720575940626838909L, 720575940628857210L},
                new int[]{0, 0, 0}, new int[0], new int[0], new byte[0]);
        assertEquals(1, graph.indexOf(720575940628857210L));
        assertEquals(-1, graph.indexOf(42L));
    }
}
