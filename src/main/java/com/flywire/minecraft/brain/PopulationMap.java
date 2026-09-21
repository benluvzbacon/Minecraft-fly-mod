package com.flywire.minecraft.brain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Maps labels derived from the real v783 annotation table to real root IDs. */
public final class PopulationMap {
    private final Map<String, long[]> rootsByName;
    private final Map<String, int[]> indicesByName = new HashMap<>();

    public PopulationMap(Map<String, long[]> rootsByName) {
        this.rootsByName = Map.copyOf(rootsByName);
    }

    public static PopulationMap read(Reader reader) throws IOException {
        Map<String, ArrayList<Long>> values = new HashMap<>();
        try (BufferedReader in = new BufferedReader(reader)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split("\\t", -1);
                if (parts.length < 2) throw new IOException("Malformed population row");
                values.computeIfAbsent(parts[0], ignored -> new ArrayList<>()).add(Long.parseLong(parts[1]));
            }
        }
        Map<String, long[]> roots = new HashMap<>();
        for (Map.Entry<String, ArrayList<Long>> entry : values.entrySet()) {
            long[] ids = new long[entry.getValue().size()];
            for (int i = 0; i < ids.length; i++) ids[i] = entry.getValue().get(i);
            Arrays.sort(ids);
            roots.put(entry.getKey(), ids);
        }
        return new PopulationMap(roots);
    }

    public long[] roots(String name) { return rootsByName.getOrDefault(name, new long[0]); }
    public boolean contains(String name) { return rootsByName.containsKey(name); }
    public Map<String, long[]> rootsByName() { return rootsByName; }

    public int[] indices(String name, Connectome connectome) {
        int[] cached = indicesByName.get(name);
        if (cached != null) return cached;
        long[] roots = roots(name);
        int[] result = new int[roots.length];
        int used = 0;
        for (long root : roots) {
            int index = connectome.indexOf(root);
            if (index >= 0) result[used++] = index;
        }
        result = Arrays.copyOf(result, used);
        indicesByName.put(name, result);
        return result;
    }
}
