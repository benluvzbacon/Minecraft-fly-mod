package com.flywire.minecraft.brain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NeuralRuntimeTest {
    @Test
    void sensorySpikePropagatesThroughCsrToMotorPopulation() throws Exception {
        // Root 10 is annotated sensory, root 20 is annotated motor. This fixture is
        // only a test of the runtime; production resources come from v783 tooling.
        Connectome graph = new Connectome(new long[]{10, 20}, new int[]{0, 1, 1},
                new int[]{1}, new int[]{10}, new byte[]{1});
        PopulationMap populations = PopulationMap.read(new java.io.StringReader(
                "visual\t10\nmotor_forward\t20\n"));
        NeuralRuntime runtime = new NeuralRuntime(graph, populations, new NeuralConfig(true, false, false, 32, 1));
        runtime.stimulate(Map.of("visual", 1.0f));
        MotorOutput output = runtime.step();
        assertTrue(output.activeNeurons() > 0);
        assertTrue(output.forward() > 0, "the real CSR edge should reach the annotated motor population");
    }
}
