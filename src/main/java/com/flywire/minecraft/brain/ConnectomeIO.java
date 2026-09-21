package com.flywire.minecraft.brain;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;

/** Binary reader/writer for the deterministic big-endian FWB1 CSR format. */
public final class ConnectomeIO {
    private ConnectomeIO() { }

    public static Connectome read(InputStream input) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(input))) {
            if (in.readInt() != Connectome.MAGIC) throw new IOException("Not an FWB1 FlyWire file");
            if (in.readInt() != Connectome.FORMAT_VERSION) throw new IOException("Unsupported FWB1 version");
            int neurons = in.readInt();
            int edges = in.readInt();
            if (neurons <= 0 || neurons > 2_000_000 || edges < 0 || edges > 100_000_000) {
                throw new IOException("Unsafe FlyWire dimensions: " + neurons + " neurons, " + edges + " edges");
            }
            long[] roots = new long[neurons];
            int[] offsets = new int[neurons + 1];
            int[] targets = new int[edges];
            int[] counts = new int[edges];
            byte[] polarity = new byte[edges];
            for (int i = 0; i < neurons; i++) roots[i] = in.readLong();
            for (int i = 0; i <= neurons; i++) offsets[i] = in.readInt();
            for (int i = 0; i < edges; i++) targets[i] = in.readInt();
            for (int i = 0; i < edges; i++) counts[i] = in.readInt();
            in.readFully(polarity);
            return new Connectome(roots, offsets, targets, counts, polarity);
        }
    }

    public static void write(OutputStream output, Connectome graph) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(output))) {
            out.writeInt(Connectome.MAGIC);
            out.writeInt(Connectome.FORMAT_VERSION);
            out.writeInt(graph.neuronCount());
            out.writeInt(graph.edgeCount());
            for (long root : graph.rootIds()) out.writeLong(root);
            for (int offset : graph.offsets()) out.writeInt(offset);
            for (int target : graph.targets()) out.writeInt(target);
            for (int count : graph.synapseCounts()) out.writeInt(count);
            out.write(graph.polarity());
        }
    }
}
