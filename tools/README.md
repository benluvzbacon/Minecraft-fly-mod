# FlyWire data tools

These scripts never generate connectivity. They download the unmodified FlyWire FAFB v783 pair table and v2.1.0 systematic annotation table, inspect them, and deterministically serialize the complete proofread graph to `FWB1` CSR.

```bash
python tools/download_flywire.py
python tools/verify_checksums.py
python tools/inspect_dataset.py
python tools/build_connectome.py
python tools/validate_processed.py
```

The default connection source is the public v783 Codex export at `storage.googleapis.com/flywire-data/codex/data/fafb/783/connections.csv.gz`; it is the same release named by `https://codex.flywire.ai/api/download?dataset=fafb` and avoids interactive authentication. Override `--connections-url` if an official export is supplied by Codex. Raw files stay under `data/flywire/raw`, generated files under `data/flywire/processed`, and neither is committed.
