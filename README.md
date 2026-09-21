# FlyWire Brain for Minecraft

A Fabric 1.21.1 mod that adds a small, blocky **Fly** mob whose server-side decisions are driven by a sparse runtime over the **real FlyWire FAFB v783 adult female Drosophila connectome**. This is not a fake/random neural network and it does not use normal Minecraft pathfinding as a stand-in for the brain.

> **Data note:** the repository intentionally does not contain the multi-gigabyte raw export or the generated whole-brain binary. GitHub Actions downloads the public v783 inputs, validates them, preprocesses the real graph, packages it into the jar, and uploads the runnable jar as `flywire-minecraft-mod`. A code-only local jar without the data is explicitly marked inactive; it never silently substitutes fake connectivity.

## Architecture

```text
Minecraft World
      ↓
Fly sensory encoder (local light, block silhouettes, motion/looming, food/odor, touch, heat)
      ↓
Real FlyWire annotated sensory populations
      ↓
REAL FLYWIRE FAFB v783 CONNECTOME (CSR; real root IDs, pairs and synapse counts)
      ↓
Real annotated descending / motor populations
      ↓
Motor decoder (thrust, turn, vertical, wing, takeoff, landing, feeding)
      ↓
Fly movement and Minecraft physics
      ↺
```

Each Minecraft tick runs a configurable number of 1 ms fixed substeps. The immutable CSR graph is shared; each fly owns primitive arrays for LIF state. No 139,255 Java `Neuron` objects are created. Multiple flies are supported with a configurable state budget.

## What is real and what is approximated

### Real biological data

- FlyWire FAFB v783 root IDs and the proofread neuron population.
- v783 directed neuron-pair connectivity and released synapse counts.
- Annotation-derived sensory, descending and motor populations, including named DNa/DNb/DNg/DNp/MDN classes when present in the release.
- The runtime graph is generated from the complete selected proofread pair table, not a sampled toy subgraph.

### Simulation approximations

- Membrane voltage, refractory periods and synaptic conductance use a documented leaky integrate-and-fire model.
- Synapse counts are converted to bounded conductances for numerical stability; counts and edges are preserved.
- Minecraft's discrete blocks are encoded into coarse local sensory signals rather than a biological retina/antenna/proboscis.
- A motor population decoder maps firing rates to Minecraft acceleration and animation. Minecraft collision, wing mechanics and block attachment are not biological muscle mechanics.

These distinctions are also recorded in [`data/flywire/README.md`](data/flywire/README.md), and the exact decoder contract is in [`data/flywire/MOTOR_MAPPING.md`](data/flywire/MOTOR_MAPPING.md).

## Features

- `flywire:fly`, a genuine living `PathAwareEntity` with health, damage, death, natural Overworld spawning, collisions and sounds.
- Minecraft-style pixel-art model: blocky body/head, compound-eye texture, six legs, antennae and two animated translucent-looking wings.
- Neural control rather than goals/pathfinding: hovering, acceleration/deceleration, turns, vertical motion, takeoff/landing, feeding and threat response are decoded from brain activity.
- Local sensing of brightness/obstacles, player/entity looming, flowers/honey/sweet food, contact and heat. The fly is not given world-wide target knowledge.
- Optional operator commands: `/flywire spawn`, `/flywire stats`, `/flywire brain` (permission level 2).
- Optional profiling/debug properties:
  - `-Dflywire.neural.enabled=true|false`
  - `-Dflywire.neural.maxSubsteps=4` (1–32 biological ms steps per tick)
  - `-Dflywire.neural.maxFlies=32` (1–256 independent state vectors)
  - `-Dflywire.neural.debug=true`
  - `-Dflywire.neural.profile=true`

## Download the runnable jar from GitHub Actions

Open the repository's **Actions** tab, select **Build FlyWire Minecraft mod**, open a successful run, and download the `flywire-minecraft-mod` artifact. Put its `.jar` in a Fabric 1.21.1 `mods` folder alongside Fabric API and launch Java 21. That artifact is built only after `tools/validate_processed.py` succeeds and contains `data/flywire/brain/flywire-v783.bin`; no manual compilation is needed.

## Reproduce locally

The raw files and generated binary are intentionally ignored because the upstream release is very large:

```bash
python3 tools/download_flywire.py
python3 tools/inspect_dataset.py
python3 tools/build_connectome.py
python3 tools/validate_processed.py
./gradlew test
./gradlew build -PrequireFlyWireData
```

The default downloader uses the non-interactive public v783 Codex export plus the systematic annotation table. The authoritative UI is `https://codex.flywire.ai/api/download?dataset=fafb`; the UI can require Google authentication, so the downloader never attempts interactive login or embeds credentials. To use a different official v783 export, pass `--connections-url`. Raw inputs remain byte-for-byte in `data/flywire/raw/`, and checksums are written to `download-manifest.json`.

A plain `./gradlew build` is useful for code/resource development when data is unavailable, but its log says it is a code-only development jar and the mod reports the missing brain at startup. `-PrequireFlyWireData` turns that into a hard failure, as used in CI.

## Tests and validation

- Java tests cover binary serialization/deserialization, CSR bounds and indexing, population mapping, LIF propagation and configuration.
- `tools/inspect_dataset.py` reports annotation/graph columns and counts without changing inputs.
- `tools/validate_processed.py` checks the Java binary format, sorted real root IDs, CSR bounds, positive synapse counts and population identities.
- GitHub Actions runs inspection, preprocessing, validation, tests, Gradle build, and verifies that the final jar actually contains the processed brain before uploading it.

## Provenance, licensing and citations

The official source, version, processing steps, checksum policy and licensing notes are in [`data/flywire/ORIGIN.md`](data/flywire/ORIGIN.md). The FlyWire connectivity release is CC BY 4.0; upstream annotation terms apply to annotations. This mod source is MIT licensed and does not relicense upstream data.

Please cite the FlyWire Consortium / Dorkenwald et al., *Neuronal wiring diagram of an adult brain*, Nature (2024), DOI `10.1038/s41586-024-07558-y`; Schlegel et al., *Whole-brain annotation and multi-connectome cell typing of Drosophila*, Nature (2024), DOI `10.1038/s41586-024-07686-5`; and Zenodo record [10676866](https://zenodo.org/records/10676866).
