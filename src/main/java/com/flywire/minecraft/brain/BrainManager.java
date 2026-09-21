package com.flywire.minecraft.brain;

import com.flywire.minecraft.FlyWire;
import com.flywire.minecraft.entity.FlyEntity;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared immutable connectome plus an independent primitive state vector per fly. */
public final class BrainManager {
    private static final Logger LOGGER = FlyWire.LOGGER;
    private static final BrainManager INSTANCE = new BrainManager();
    private final Map<UUID, NeuralRuntime> runtimes = new HashMap<>();
    private NeuralConfig config;
    private Connectome connectome;
    private PopulationMap populations;
    private String loadError;

    private BrainManager() { }
    public static BrainManager get() { return INSTANCE; }

    public synchronized void initialize() {
        if (config != null) return;
        config = NeuralConfig.load();
        if (!config.enabled()) {
            loadError = "disabled by flywire.neural.enabled";
            return;
        }
        try (InputStream graph = BrainManager.class.getClassLoader().getResourceAsStream("data/flywire/brain/flywire-v783.bin");
             InputStream pop = BrainManager.class.getClassLoader().getResourceAsStream("data/flywire/brain/flywire-v783.populations.tsv")) {
            if (graph == null || pop == null) {
                loadError = "FlyWire v783 processed resources are absent; run the documented data pipeline";
                LOGGER.warn(loadError);
                return;
            }
            connectome = ConnectomeIO.read(graph);
            populations = PopulationMap.read(new InputStreamReader(pop, StandardCharsets.UTF_8));
            LOGGER.info("Loaded real FlyWire FAFB v783: {} neurons, {} directed edges, {} populations",
                    connectome.neuronCount(), connectome.edgeCount(), populations.rootsByName().size());
        } catch (IOException | RuntimeException exception) {
            loadError = exception.getMessage();
            LOGGER.error("Could not load the FlyWire connectome; flies will remain inactive", exception);
        }
    }

    public synchronized MotorOutput tick(FlyEntity fly, Map<String, Float> signals) {
        if (connectome == null || populations == null || runtimes.size() >= config.maxFlies() && !runtimes.containsKey(fly.getUuid())) {
            return MotorOutput.SILENT;
        }
        NeuralRuntime runtime = runtimes.computeIfAbsent(fly.getUuid(), ignored -> new NeuralRuntime(connectome, populations, config));
        runtime.stimulate(signals);
        return runtime.step();
    }

    public synchronized BrainStats stats(FlyEntity fly) {
        NeuralRuntime runtime = runtimes.get(fly.getUuid());
        if (runtime == null || connectome == null) return new BrainStats(0, 0, 0, 0, MotorOutput.SILENT, "unavailable", 0);
        return runtime.stats();
    }

    public synchronized void forget(FlyEntity fly) { runtimes.remove(fly.getUuid()); }
    public synchronized int simulatedFlies() { return runtimes.size(); }
    public synchronized String status() {
        if (connectome != null) return "loaded " + connectome.neuronCount() + " neurons / " + connectome.edgeCount() + " edges";
        return loadError == null ? "not initialized" : loadError;
    }
    public NeuralConfig config() { return config == null ? NeuralConfig.load() : config; }
}
