#!/usr/bin/env python3
"""Copy the original assembly.json and catalog.json to Motorica without rewriting."""
import argparse
import hashlib
import os
from pathlib import Path

import migrate_to_motorica as api
from motorica_httpfs import FirmwareFS, MANIFESTS


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--upload", action="store_true")
    parser.add_argument("--work-dir", type=Path, default=Path("/tmp/motorica-firmware-manifests"))
    args = parser.parse_args()
    args.work_dir.mkdir(parents=True, exist_ok=True)
    key = os.environ.get("MOTORICA_WRITE_KEY") or api.WRITE_KEY
    if args.upload and not key:
        parser.error("Set MOTORICA_WRITE_KEY or WRITE_KEY")
    fs = FirmwareFS(key, args.work_dir / "state")
    report = []
    for name in MANIFESTS:
        data = api.request(api.SOURCE_URL + "/" + name)
        (args.work_dir / name).write_bytes(data)
        row = {"path": "firmware/" + name, "size": len(data), "sha256": hashlib.sha256(data).hexdigest()}
        if args.upload:
            row.update(fs.create_manifest(row["path"], data))
            assert fs.open(row["path"]) == data, f"JSON differs from FVDS: {name}"
        report.append(row)
        api.save_json(args.work_dir / "manifest-migration-report.json", report)
        print(f"{'Verified' if args.upload else 'Prepared'}: {name} ({len(data)} bytes)", flush=True)


if __name__ == "__main__":
    main()
