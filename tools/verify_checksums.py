#!/usr/bin/env python3
"""Verify raw files against the downloader's immutable manifest."""
from __future__ import annotations
import argparse, hashlib, json, pathlib, sys

def digest(path: pathlib.Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""): h.update(block)
    return h.hexdigest()

def main() -> int:
    ap = argparse.ArgumentParser(); ap.add_argument("--raw", type=pathlib.Path, default=pathlib.Path("data/flywire/raw")); args = ap.parse_args()
    manifest = json.loads((args.raw / "download-manifest.json").read_text(encoding="utf-8"))
    expected = {"connections_783.csv.gz": manifest["connections_sha256"],
                "Supplemental_file1_neuron_annotations.tsv": manifest["annotations_sha256"]}
    for filename, checksum in expected.items():
        actual = digest(args.raw / filename)
        if actual != checksum: raise ValueError(f"checksum mismatch for {filename}: {actual} != {checksum}")
        print(f"verified {filename}: {actual}")
    return 0

if __name__ == "__main__":
    try: raise SystemExit(main())
    except (OSError, KeyError, ValueError) as exc: print(f"INVALID CHECKSUM: {exc}", file=sys.stderr); raise SystemExit(1)
