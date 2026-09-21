#!/usr/bin/env python3
"""Strict validation for the Java FWB1 reader's inputs."""
from __future__ import annotations
import argparse, pathlib, struct, sys

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--directory", type=pathlib.Path, default=pathlib.Path("data/flywire/processed")); a = ap.parse_args()
    binary = a.directory / "flywire-v783.bin"; populations = a.directory / "flywire-v783.populations.tsv"
    with binary.open("rb") as f:
        magic, version, n, e = struct.unpack(">iiii", f.read(16))
        if magic != 0x46574231 or version != 1: raise ValueError("bad FWB1 header")
        roots = list(struct.unpack(">" + "q" * n, f.read(8*n)))
        offsets = list(struct.unpack(">" + "i" * (n+1), f.read(4*(n+1))))
        targets = list(struct.unpack(">" + "i" * e, f.read(4*e)))
        counts = list(struct.unpack(">" + "i" * e, f.read(4*e)))
        polarity = f.read(e)
    if roots != sorted(roots) or len(set(roots)) != n: raise ValueError("root IDs are not unique and sorted")
    if offsets[0] != 0 or offsets[-1] != e or any(a > b for a,b in zip(offsets, offsets[1:])): raise ValueError("bad CSR offsets")
    if any(t < 0 or t >= n for t in targets): raise ValueError("target out of range")
    if any(c < 1 for c in counts) or len(polarity) != e: raise ValueError("bad edge payload")
    rootset = set(roots)
    rows = 0
    with populations.open(encoding="utf-8") as f:
        for line in f:
            if line.strip() and not line.startswith("#"):
                name, root = line.rstrip("\n").split("\t")
                if int(root) not in rootset: raise ValueError(f"population root absent: {name} {root}")
                rows += 1
    print(f"valid FWB1: {n:,} neurons, {e:,} edges, {rows:,} population memberships")

if __name__ == "__main__":
    try: main()
    except (OSError, ValueError, struct.error) as exc: print(f"INVALID: {exc}", file=sys.stderr); raise SystemExit(1)
