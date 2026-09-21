#!/usr/bin/env python3
"""Print SHA-256 checksums without modifying FlyWire files."""
import hashlib, pathlib, sys
for name in sys.argv[1:]:
    path = pathlib.Path(name); h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""): h.update(block)
    print(h.hexdigest(), path)
