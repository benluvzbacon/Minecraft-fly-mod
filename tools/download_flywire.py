#!/usr/bin/env python3
"""Download the unmodified public FlyWire FAFB v783 inputs.

The Codex UI endpoint is the authoritative dataset endpoint, but Codex may require an
interactive Google login. CI therefore uses the public Codex v783 export URL, which is
the same v783 release and does not need credentials. No file is overwritten unless
--force is provided, and SHA-256 is recorded for provenance.
"""
from __future__ import annotations
import argparse, hashlib, json, pathlib, shutil, sys, urllib.request

CODEX_UI = "https://codex.flywire.ai/api/download?dataset=fafb"
CONNECTIONS = "https://storage.googleapis.com/flywire-data/codex/data/fafb/783/connections.csv.gz"
ANNOTATIONS = "https://raw.githubusercontent.com/flyconnectome/flywire_annotations/v2.1.0/supplemental_files/Supplemental_file1_neuron_annotations.tsv"

def download(url: str, destination: pathlib.Path, force: bool) -> str:
    if destination.exists() and not force:
        print(f"exists (unchanged): {destination}")
        return hashlib.sha256(destination.read_bytes()).hexdigest()
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".partial")
    request = urllib.request.Request(url, headers={"User-Agent": "FlyWire-Minecraft/1.0 data-preprocessor"})
    print(f"downloading {url}\n       -> {destination}")
    with urllib.request.urlopen(request, timeout=120) as response, temporary.open("wb") as out:
        shutil.copyfileobj(response, out, length=1024 * 1024)
    temporary.replace(destination)
    digest = hashlib.sha256(destination.read_bytes()).hexdigest()
    print(f"sha256 {digest}  {destination.stat().st_size} bytes")
    return digest

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=pathlib.Path, default=pathlib.Path("data/flywire/raw"))
    parser.add_argument("--connections-url", default=CONNECTIONS)
    parser.add_argument("--annotations-url", default=ANNOTATIONS)
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    try:
        con_sha = download(args.connections_url, args.output / "connections_783.csv.gz", args.force)
        ann_sha = download(args.annotations_url, args.output / "Supplemental_file1_neuron_annotations.tsv", args.force)
    except Exception as exc:
        print(f"download failed: {exc}", file=sys.stderr)
        print("The official Codex UI may require Google login; use a public v783 export URL, never put credentials in source.", file=sys.stderr)
        return 2
    manifest = {
        "dataset": "FlyWire FAFB v783",
        "official_endpoint": CODEX_UI,
        "connections_url": args.connections_url,
        "annotations_url": args.annotations_url,
        "annotations_release": "flyconnectome/flywire_annotations v2.1.0",
        "connections_sha256": con_sha,
        "annotations_sha256": ann_sha,
        "raw_files_are_unmodified": True,
    }
    (args.output / "download-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
