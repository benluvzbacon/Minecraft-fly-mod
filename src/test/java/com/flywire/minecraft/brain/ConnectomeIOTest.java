package com.flywire.minecraft.brain;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class ConnectomeIOTest {
    @Test
    void roundTripsPrimitiveCsrAndRealRootIds() throws Exception {
        Connectome original = new Connectome(
                new long[]{100L, 200L, 300L},
                new int[]{0, 1, 2, 2},
                new int[]{1, 2},
                new int[]{4, 2},
                new byte[]{1, -1});
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConnectomeIO.write(bytes, original);
        Connectome loaded = ConnectomeIO.read(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(3, loaded.neuronCount());
        assertEquals(2, loaded.edgeCount());
        assertEquals(200L, loaded.rootId(1));
        assertArrayEquals(original.offsets(), loaded.offsets());
        assertArrayEquals(original.synapseCounts(), loaded.synapseCounts());
        assertArrayEquals(original.polarity(), loaded.polarity());
    }

    @Test
    void populationRootsBecomeGraphIndices() throws Exception {
        PopulationMap map = PopulationMap.read(new StringReader("visual\t300\nvisual\t100\n"));
        Connectome graph = new Connectome(new long[]{100, 200, 300}, new int[]{0, 0, 0, 0}, new int[0], new int[0], new byte[0]);
        assertArrayEquals(new int[]{0, 2}, map.indices("visual", graph));
    }
}
