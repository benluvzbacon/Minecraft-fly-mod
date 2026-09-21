package com.flywire.minecraft.brain;

import java.util.Arrays;
import java.util.Map;

/**
 * Fixed-step leaky integrate-and-fire execution over the complete loaded CSR graph.
 * The graph is real FlyWire data; only the neuron dynamics and the embodiment
 * decoder are intentionally simplified for a Minecraft tick budget.
 */
public final class NeuralRuntime {
    private static final float DT = 0.001f;
    private static final float TAU = 0.010f;
    private static final float THRESHOLD = 0.85f;
    private static final float REFRACTORY = 0.002f;
    private static final float SENSORY_CURRENT = 3.0f;

    private final Connectome connectome;
    private final PopulationMap populations;
    private final NeuralConfig config;
    private final float[] membrane;
    private final float[] pending;
    private final float[] external;
    private final float[] refractory;
    private final int[] spikes;
    private final boolean[] activeThisTick;
    private final int[] fired;
    private int firedCount;
    private int lastActive;
    private float lastSensoryDrive;
    private long lastUpdateNanos;
    private long stepNumber;
    private int droppedSpikes;
    private MotorOutput lastOutput = MotorOutput.SILENT;

    public NeuralRuntime(Connectome connectome, PopulationMap populations, NeuralConfig config) {
        this.connectome = connectome;
        this.populations = populations;
        this.config = config;
        int n = connectome.neuronCount();
        this.membrane = new float[n];
        this.pending = new float[n];
        this.external = new float[n];
        this.refractory = new float[n];
        this.spikes = new int[n];
        this.activeThisTick = new boolean[n];
        this.fired = new int[n];
    }

    public Connectome connectome() { return connectome; }
    public PopulationMap populations() { return populations; }
    public int lastActiveNeurons() { return lastActive; }
    public long lastUpdateNanos() { return lastUpdateNanos; }
    public int droppedSpikes() { return droppedSpikes; }

    /**
     * Apply sensory rates to real annotated sensory populations.
     *
     * Large populations such as visual neurons are sampled deterministically under
     * the configured input budget. Sampling changes which real neurons receive the
     * Minecraft current; it never changes the immutable connectome or its metadata.
     */
    public void stimulate(Map<String, Float> signals) {
        Arrays.fill(external, 0f);
        float total = 0f;
        int count = 0;
        for (Map.Entry<String, Float> signal : signals.entrySet()) {
            float value = Math.max(0f, Math.min(1f, signal.getValue()));
            if (value == 0f) continue;
            int[] indices = populations.indices(signal.getKey(), connectome);
            if (indices.length == 0) continue;
            int limit = Math.min(indices.length, config.maxInputNeurons());
            int offset = (int) ((stepNumber * (long) Math.max(1, limit)) % indices.length);
            float current = value * SENSORY_CURRENT;
            for (int selected = 0; selected < limit; selected++) {
                int position = (int) (((long) selected * indices.length / limit + offset) % indices.length);
                int index = indices[position];
                external[index] = Math.min(6.0f, external[index] + current);
            }
            total += value;
            count++;
        }
        lastSensoryDrive = count == 0 ? 0f : Math.min(1f, total / count);
    }

    /** Run bounded 1 ms biological substeps and decode annotated motor populations. */
    public MotorOutput step() {
        long start = System.nanoTime();
        stepNumber++;
        Arrays.fill(spikes, 0);
        Arrays.fill(activeThisTick, false);
        firedCount = 0;
        droppedSpikes = 0;
        lastActive = 0;
        for (int substep = 0; substep < config.maxSubsteps(); substep++) integrateSubstep();

        float forward = signedRate("motor_forward") + signedRate("forward");
        float left = signedRate("motor_turn_left") + signedRate("turn_left");
        float right = signedRate("motor_turn_right") + signedRate("turn_right");
        float up = signedRate("motor_vertical_up") + signedRate("vertical_up");
        float down = signedRate("motor_vertical_down") + signedRate("vertical_down");
        float takeoff = rate("motor_takeoff") + rate("takeoff") + rate("motor_escape");
        float landing = rate("motor_landing") + rate("landing");
        float feeding = rate("motor_feed") + rate("feeding");
        float escape = rate("motor_escape") + rate("escape");
        float turn = right - left;
        float vertical = up - down;
        float wing = Math.min(1f, Math.max(0.05f, Math.abs(forward) + Math.abs(turn) + Math.abs(vertical) + escape));
        lastUpdateNanos = System.nanoTime() - start;
        lastOutput = new MotorOutput(clamp(forward), clamp(right - left), clamp(vertical), clamp(turn), wing,
                clamp(takeoff), clamp(landing), clamp(feeding), clamp(escape), lastActive, lastUpdateNanos);
        return lastOutput;
    }

    private void integrateSubstep() {
        firedCount = 0;
        for (int neuron = 0; neuron < membrane.length; neuron++) {
            // A bounded synaptic event is an instantaneous conductance/charge kick;
            // sensory drive remains a tonic current in the Euler LIF update below.
            float synapticKick = pending[neuron];
            pending[neuron] = 0f;
            if (refractory[neuron] > 0f) {
                refractory[neuron] -= DT;
                membrane[neuron] = 0f;
                continue;
            }
            if (synapticKick == 0f && external[neuron] == 0f && membrane[neuron] == 0f) continue;
            if (!activeThisTick[neuron]) {
                activeThisTick[neuron] = true;
                lastActive++;
            }
            // Euler LIF update. A per-edge synapse count is converted to a bounded
            // effective conductance; no connectivity is discarded by this scaling.
            membrane[neuron] += synapticKick;
            membrane[neuron] += ((-membrane[neuron] + external[neuron]) / TAU) * DT;
            if (membrane[neuron] >= THRESHOLD) {
                membrane[neuron] = 0f;
                refractory[neuron] = REFRACTORY;
                if (firedCount == fired.length) {
                    droppedSpikes++;
                    continue;
                }
                fired[firedCount++] = neuron;
                spikes[neuron]++;
            }
        }
        int[] targets = connectome.targets();
        int[] counts = connectome.synapseCounts();
        byte[] polarity = connectome.polarity();
        for (int i = 0; i < firedCount; i++) {
            int source = fired[i];
            for (int edge = connectome.edgeStart(source); edge < connectome.edgeEnd(source); edge++) {
                int target = targets[edge];
                float conductance = Math.min(1.0f, 0.35f + (float) Math.log1p(counts[edge]) * 0.25f);
                int sign = polarity[edge] == 0 ? 1 : polarity[edge];
                pending[target] += sign * conductance;
            }
        }
    }

    private float rate(String population) {
        int[] indices = populations.indices(population, connectome);
        if (indices.length == 0) return 0f;
        long count = 0;
        float voltage = 0f;
        for (int index : indices) {
            count += spikes[index];
            voltage += Math.max(0f, Math.min(1f, membrane[index] / THRESHOLD));
        }
        // A population fraction is a useful Minecraft actuator rate. Including
        // subthreshold voltage prevents a 1 ms boundary from hiding real activity.
        float spikeFraction = Math.min(1f, (float) count / indices.length);
        float voltageFraction = voltage / indices.length;
        return Math.min(1f, spikeFraction * 0.8f + voltageFraction * 0.2f);
    }

    private float signedRate(String population) { return rate(population); }
    private static float clamp(float value) { return Math.max(-1f, Math.min(1f, value)); }

    public BrainStats stats() { return stats(lastOutput); }

    public BrainStats stats(MotorOutput motor) {
        String behavior = motor.escape() > 0.2f ? "escape" : motor.feeding() > 0.2f ? "feeding"
                : motor.takeoff() > 0.2f ? "takeoff" : motor.landing() > 0.2f ? "landing"
                : motor.wingBeat() > 0.2f ? "flying" : "resting";
        return new BrainStats(connectome.neuronCount(), connectome.edgeCount(), lastActive,
                lastSensoryDrive, motor, behavior, lastUpdateNanos);
    }
}
