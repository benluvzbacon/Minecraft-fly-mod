#!/usr/bin/env python3
"""Read-only inspection of FlyWire inputs; useful before an expensive build."""
from __future__ import annotations
import argparse, csv, gzip, json, pathlib
from collections import Counter

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--connections", type=pathlib.Path, default=pathlib.Path("data/flywire/raw/connections_783.csv.gz"))
    ap.add_argument("--annotations", type=pathlib.Path, default=pathlib.Path("data/flywire/raw/Supplemental_file1_neuron_annotations.tsv"))
    args = ap.parse_args()
    with args.annotations.open(encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f, delimiter="\t"))
    print(json.dumps({"annotation_rows": len(rows), "unique_roots": len({r["root_id"] for r in rows}),
                      "flows": Counter(r.get("flow", "") for r in rows),
                      "super_classes": Counter(r.get("super_class", "") for r in rows)}, indent=2))
    opener = gzip.open if args.connections.suffix == ".gz" else open
    count = 0; synapses = 0; sources = set(); targets = set()
    with opener(args.connections, "rt", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        print("connection_columns:", reader.fieldnames)
        for row in reader:
            count += 1
            sources.add(row.get("pre_pt_root_id", row.get("pre_root_id", "")))
            targets.add(row.get("post_pt_root_id", row.get("post_root_id", "")))
            count = row.get("syn_count", row.get("synapse_count"))
            if count is None: raise ValueError("connection table has no synapse count")
            synapses += int(float(count))
    print(json.dumps({"connection_rows": count, "unique_sources": len(sources), "unique_targets": len(targets), "synapse_count_sum": synapses}, indent=2))

if __name__ == "__main__": main()
