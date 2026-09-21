package com.flywire.minecraft.brain;

import java.util.Arrays;

/**
 * Immutable primitive-array representation of the FlyWire v783 pair-level graph.
 * There is deliberately no Neuron object: 139,255 roots and millions of edges are
 * indexed in compact CSR arrays shared by all flies.
 */
public final class Connectome {
    public static final int MAGIC = 0x46574231; // FWB1
    public static final int FORMAT_VERSION = 1;

    private final long[] rootIds;
    private final int[] offsets;
    private final int[] targets;
    private final int[] synapseCounts;
    private final byte[] polarity;

    public Connectome(long[] rootIds, int[] offsets, int[] targets, int[] synapseCounts, byte[] polarity) {
        if (rootIds.length == 0 || offsets.length != rootIds.length + 1
                || targets.length != synapseCounts.length || targets.length != polarity.length
                || offsets[0] != 0 || offsets[offsets.length - 1] != targets.length) {
            throw new IllegalArgumentException("Invalid FlyWire CSR dimensions");
        }
        for (int i = 1; i < offsets.length; i++) {
            if (offsets[i] < offsets[i - 1]) throw new IllegalArgumentException("CSR offsets are not monotonic");
        }
        for (int target : targets) {
            if (target < 0 || target >= rootIds.length) throw new IllegalArgumentException("CSR target out of range");
        }
        this.rootIds = rootIds;
        this.offsets = offsets;
        this.targets = targets;
        this.synapseCounts = synapseCounts;
        this.polarity = polarity;
    }

    public int neuronCount() { return rootIds.length; }
    public int edgeCount() { return targets.length; }
    public long rootId(int index) { return rootIds[index]; }
    public long[] rootIds() { return rootIds; }
    public int[] offsets() { return offsets; }
    public int[] targets() { return targets; }
    public int[] synapseCounts() { return synapseCounts; }
    public byte[] polarity() { return polarity; }

    public int indexOf(long rootId) {
        return Arrays.binarySearch(rootIds, rootId);
    }

    public int edgeStart(int neuron) { return offsets[neuron]; }
    public int edgeEnd(int neuron) { return offsets[neuron + 1]; }
}
