#!/usr/bin/env python3
"""Build the runtime CSR from real FlyWire FAFB v783 tables.

Input is a Codex v783 pair table plus the systematic FlyWire annotation TSV. The
builder preserves every source pair whose endpoints are proofread neurons, including
its synapse count. It does not sample, generate, or collapse the graph to a toy model.
"""
from __future__ import annotations
import argparse, csv, gzip, hashlib, json, pathlib, struct, sys, tempfile
from collections import defaultdict

MAGIC, VERSION = 0x46574231, 1

def norm(row, *names, default=""):
    for name in names:
        if name in row and row[name] not in (None, ""):
            return row[name]
    return default

def read_annotations(path: pathlib.Path):
    roots = []
    rows = []
    with path.open(encoding="utf-8", newline="") as handle:
        for row in csv.DictReader(handle, delimiter="\t"):
            root = int(row["root_id"])
            roots.append(root)
            rows.append(row)
    roots = sorted(set(roots))
    index = {root: i for i, root in enumerate(roots)}
    return roots, index, rows

def polarity(row):
    # Codex CSV exports carry either a per-row nt_type or probability columns.
    # Use those released values only to assign a sign; the synapse count stays
    # lossless. Fast conductance sign is a documented dynamics approximation.
    nt = str(norm(row, "nt_type", "neurotransmitter", default="")).upper()
    if nt in {"GABA", "GLUT", "GLUTAMATE"}: return -1
    if nt in {"ACH", "ACETYLCHOLINE"}: return 1
    inhibitory = float(norm(row, "gaba_avg", "gaba", default="0") or 0)
    excitatory = sum(float(norm(row, key, default="0") or 0) for key in
                     ("ach_avg", "ach", "oct_avg", "oct", "ser_avg", "ser", "da_avg", "da"))
    glut = float(norm(row, "glut_avg", "glut", default="0") or 0)
    if inhibitory > max(excitatory, glut, 0.0): return -1
    return 1

def open_table(path: pathlib.Path):
    return gzip.open(path, "rt", encoding="utf-8", newline="") if path.suffix == ".gz" else path.open(encoding="utf-8", newline="")

def build_graph(connections: pathlib.Path, roots: list[int], index: dict[int, int]):
    edges = []
    unknown = 0
    with open_table(connections) as handle:
        reader = csv.DictReader(handle)
        required = {"pre_pt_root_id", "post_pt_root_id"}
        if not required.issubset(reader.fieldnames or set()):
            # Codex exports have also used pre_root_id/post_root_id.
            if not {"pre_root_id", "post_root_id"}.issubset(reader.fieldnames or set()):
                raise ValueError(f"unrecognised connection columns: {reader.fieldnames}")
        for row in reader:
            pre = int(norm(row, "pre_pt_root_id", "pre_root_id"))
            post = int(norm(row, "post_pt_root_id", "post_root_id"))
            if pre not in index or post not in index:
                unknown += 1
                continue
            count_text = norm(row, "syn_count", "synapse_count", "count")
            if not count_text:
                raise ValueError("connection table has no synapse-count column; refusing to invent weights")
            count = int(float(count_text))
            if count < 1: raise ValueError("source contains a non-positive synapse count")
            edges.append((index[pre], index[post], count, polarity(row)))
    edges.sort(key=lambda item: (item[0], item[1]))
    # Zenodo's proofread table can contain one row per neuropil. Collapse only
    # those duplicate rows: total synapse count is conserved exactly, and the
    # output is a unique neuron-pair graph; no minimum-synapse threshold is applied.
    merged = []
    for source, target, count, sign in edges:
        if merged and merged[-1][0] == source and merged[-1][1] == target:
            old_source, old_target, old_count, old_signed = merged[-1]
            merged[-1] = (old_source, old_target, old_count + count, old_signed + sign * count)
        else:
            merged.append((source, target, count, sign * count))
    merged = [(source, target, count, -1 if signed < 0 else 1) for source, target, count, signed in merged]
    return merged, unknown

def populations(rows):
    groups: dict[str, set[int]] = defaultdict(set)
    for row in rows:
        root = int(row["root_id"])
        text = " ".join(str(row.get(key, "")) for key in
                         ("flow", "super_class", "cell_class", "cell_sub_class", "cell_type", "supertype", "nerve")).lower()
        cell = " ".join(str(row.get(key, "")) for key in ("cell_type", "supertype", "hemibrain_type"))
        groups["all"].add(root)
        if "visual" in text or "optic" in text: groups["visual"].add(root)
        if "olfact" in text: groups["olfactory"].add(root)
        if any(word in text for word in ("gustatory", "taste", "sugar", "grn")): groups["gustatory"].add(root)
        if any(word in text for word in ("mechanosens", "bristle", "johnston", "auditory")): groups["mechanosensory"].add(root)
        if any(word in text for word in ("thermosens", "heating")): groups["thermosensory"].add(root)
        if "descending" in text or any(cell.startswith(prefix) for prefix in ("dna", "dnb", "dng", "dnp", "mdn")):
            groups["descending"].add(root)
        upper = cell.upper()
        if any(name in upper for name in ("DNP09", "DNP12", "DNP15", "DNG13")): groups["motor_forward"].add(root)
        if any(name in upper for name in ("DNA01", "DNA02", "DNA03")):
            groups["motor_turn_left" if row.get("side", "").lower() == "left" else "motor_turn_right"].add(root)
        if "MDN" in upper: groups["motor_vertical_up"].add(root)
        if "DNP13" in upper: groups["motor_landing"].add(root)
        if any(name in upper for name in ("DNB01", "DNB02", "DNG16", "DNG42")):
            groups["motor_escape"].add(root)
            groups["motor_takeoff"].add(root)
        for token in (row.get("cell_type", ""), row.get("supertype", ""), row.get("hemibrain_type", "")):
            token = token.strip()
            if token: groups["cell:" + token].add(root)
    return groups

def write_binary(path: pathlib.Path, roots, edges):
    offsets = [0] * (len(roots) + 1)
    for source, _, _, _ in edges: offsets[source + 1] += 1
    for i in range(len(roots)): offsets[i + 1] += offsets[i]
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("wb") as out:
        out.write(struct.pack(">iiii", MAGIC, VERSION, len(roots), len(edges)))
        out.write(struct.pack(">" + "q" * len(roots), *roots))
        out.write(struct.pack(">" + "i" * len(offsets), *offsets))
        out.write(struct.pack(">" + "i" * len(edges), *(e[1] for e in edges)))
        out.write(struct.pack(">" + "i" * len(edges), *(e[2] for e in edges)))
        out.write(bytes((e[3] & 0xff for e in edges)))

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--connections", type=pathlib.Path, default=pathlib.Path("data/flywire/raw/connections_783.csv.gz"))
    ap.add_argument("--annotations", type=pathlib.Path, default=pathlib.Path("data/flywire/raw/Supplemental_file1_neuron_annotations.tsv"))
    ap.add_argument("--output", type=pathlib.Path, default=pathlib.Path("data/flywire/processed"))
    args = ap.parse_args()
    roots, index, rows = read_annotations(args.annotations)
    edges, unknown = build_graph(args.connections, roots, index)
    write_binary(args.output / "flywire-v783.bin", roots, edges)
    groups = populations(rows)
    with (args.output / "flywire-v783.populations.tsv").open("w", encoding="utf-8") as out:
        out.write("# population\troot_id; labels come from FlyWire v783 systematic annotations\n")
        for name in sorted(groups):
            for root in sorted(groups[name]): out.write(f"{name}\t{root}\n")
    metadata = {
        "dataset": "FlyWire FAFB v783", "format": "FWB1 CSR", "neuron_count": len(roots),
        "edge_count": len(edges), "skipped_unknown_endpoint_rows": unknown,
        "source_connections_sha256": hashlib.sha256(args.connections.read_bytes()).hexdigest(),
        "source_annotations_sha256": hashlib.sha256(args.annotations.read_bytes()).hexdigest(),
        "populations": {name: len(ids) for name, ids in sorted(groups.items())},
        "synapse_counts_preserved": True,
        "dynamics_are_approximate": True,
    }
    (args.output / "flywire-v783.metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))

if __name__ == "__main__": main()
