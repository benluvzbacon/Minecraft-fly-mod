# FlyWire data in the Minecraft runtime

The jar's neural resource is generated as `flywire-v783.bin` plus `flywire-v783.populations.tsv`. The binary is a big-endian `FWB1` file:

```text
magic/version, neuron_count, edge_count
139,255 real root IDs
CSR offsets (neuron_count + 1)
CSR target indices (one for every retained real pair)
synapse counts (one for every edge)
polarity bytes (derived from released transmitter probabilities when available)
```

The runtime shares these immutable primitive arrays across every Fly. Each entity gets only membrane, pending-event, refractory, and spike-counter arrays. There is no Java object per neuron and no tiny substitute graph. `tools/build_connectome.py` retains every proofread endpoint pair in the chosen v783 pair table; `tools/validate_processed.py` rejects malformed output.

## Real biological data

- FlyWire FAFB v783 root/neuron identities.
- v783 neuron-to-neuron directed pair connectivity.
- Released synapse counts per pair.
- v783 annotation fields used to derive visual, olfactory, gustatory, mechanosensory, thermosensory, descending and named motor populations.
- Real named descending classes where the annotation table contains them, including DNa/DNb/DNg/DNp and MDN classes; the exact membership shipped in a build is printed in the generated metadata.

## Simulation approximations

- Membrane and refractory dynamics are a fixed-step leaky integrate-and-fire approximation, not a claim of exact fly biophysics.
- Synapse count becomes a bounded conductance for numerical stability. The count and edge remain present; this is a weight approximation.
- Minecraft brightness, local block silhouettes, movement/looming, odor-like flower/food fields, taste/contact, heat and gravity are an embodiment encoder, not a biological retina, antenna or proboscis.
- The motor decoder aggregates firing in annotation-derived groups into Minecraft thrust, turn, vertical, wing, takeoff, landing and feeding controls. Minecraft collision and movement are approximated by the entity physics.

The data path is therefore `Minecraft World -> sensory encoder -> real FlyWire sensory populations -> real v783 CSR connectome -> real annotated descending/motor populations -> motor decoder -> Fly movement`.
