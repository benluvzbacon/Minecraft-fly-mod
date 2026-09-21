# FlyWire FAFB v783 provenance

## Source

This project uses the **FlyWire FAFB v783 adult female Drosophila brain connectome**. The authoritative public dataset endpoint named by FlyWire/Codex is:

- `https://codex.flywire.ai/api/download?dataset=fafb`
- Dataset page: `https://codex.flywire.ai/`
- Public connectivity release: Zenodo record `10676866`, version `783.0`, https://doi.org/10.5281/zenodo.10676866
- Systematic annotations: https://github.com/flyconnectome/flywire_annotations tag `v2.1.0`, `Supplemental_file1_neuron_annotations.tsv`

The default downloader uses the public v783 Codex CSV export at `https://storage.googleapis.com/flywire-data/codex/data/fafb/783/connections.csv.gz`, because the Codex UI endpoint may require an interactive Google login. This is not a substitute dataset: it is the public export of the same FAFB v783 release (139,255 proofread neurons and the pair-level connectivity used by Codex; the exact output row count is recorded after duplicate-per-neuropil aggregation). Pass `--connections-url` to use an official export supplied directly by Codex.

The Zenodo proofread pair table is an alternative public source when a CSV export is unavailable. It is the release described by the FlyWire Consortium and contains the proofread pair synapse counts; the current automated pipeline intentionally defaults to the streamable Codex CSV to keep CI non-interactive.

## Checksums

Checksums are not guessed or hard-coded. `tools/download_flywire.py` writes SHA-256 values for the exact downloaded files to `data/flywire/raw/download-manifest.json`. Run `tools/checksum.py` to independently verify them. The generated `flywire-v783.metadata.json` records the raw-input hashes used to create a binary.

Because raw connectivity is approximately gigabytes in public release form, it is not committed to this repository and is excluded by `.gitignore`. A build that claims to contain the brain must run preprocessing and `./gradlew build -PrequireFlyWireData`; the Gradle task refuses a required build if the verified binary is missing.

## Licensing and attribution

FlyWire connectivity release material is distributed by the FlyWire Consortium under **CC BY 4.0** according to Zenodo record 10676866. The systematic annotations repository and its paper-specific release terms must be followed when redistributing annotation-derived files. See the upstream repositories and papers for complete notices. This mod's source code is MIT licensed; the dataset is not relicensed by this repository.

Please cite the FlyWire Consortium/Dorkenwald et al., *Neuronal wiring diagram of an adult brain*, Nature (2024), DOI `10.1038/s41586-024-07558-y`, Schlegel et al., *Whole-brain annotation and multi-connectome cell typing of Drosophila*, Nature (2024), DOI `10.1038/s41586-024-07686-5`, and the v783 Zenodo record.

## Processing

1. Download raw files into `data/flywire/raw/` without editing them.
2. Read the proofread root IDs from the annotation table and sort the real root IDs.
3. Stream the real pair table, map each endpoint to its real root index, retain its positive synapse count, and retain an excitatory/inhibitory sign derived from the supplied neurotransmitter probabilities when present.
4. Sort edges by source/target and write deterministic `FWB1` CSR arrays to `data/flywire/processed/`.
5. Derive sensory and descending/motor populations from annotation fields (`flow`, `super_class`, `cell_type`, `supertype`, `nerve`, and `side`) and write their real root IDs to the population table.
6. Validate dimensions, monotonic CSR offsets, endpoint bounds, positive synapse counts, root identity, and population membership before packaging.

No connectivity, neuron identity, synapse count, or population row is generated as a replacement for missing data.
