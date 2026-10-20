#!/usr/bin/env python3
import argparse
import json
import os
import time
from datetime import datetime, timezone
from pathlib import Path

from publish_firmware import FAMILIES, build_catalog


def snapshot(root: Path) -> tuple[tuple[str, int, int], ...]:
    entries = []
    for path in root.rglob("*"):
        if not path.is_file() or path.name == "catalog.json":
            continue
        file_stat = path.stat()
        entries.append(
            (
                path.relative_to(root).as_posix(),
                file_stat.st_size,
                file_stat.st_mtime_ns,
            )
        )
    return tuple(sorted(entries))


def rebuild(root: Path) -> None:
    for family in FAMILIES:
        (root / family).mkdir(parents=True, exist_ok=True)
    catalog = build_catalog(root)
    temporary = root / ".catalog.json.tmp"
    temporary.write_text(
        json.dumps(catalog, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    os.chmod(temporary, 0o644)
    os.replace(temporary, root / "catalog.json")
    print(
        f"{datetime.now(timezone.utc).isoformat()} "
        f"catalog updated: {len(catalog['files'])} archives",
        flush=True,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default="/srv/ms-firmware/staging/firmware")
    parser.add_argument("--interval", type=float, default=1.0)
    args = parser.parse_args()

    root = Path(args.root)
    root.mkdir(parents=True, exist_ok=True)
    last_attempt = None
    while True:
        current = snapshot(root)
        if current != last_attempt:
            try:
                rebuild(root)
            except Exception as error:
                print(
                    f"{datetime.now(timezone.utc).isoformat()} "
                    f"catalog update deferred: {error}",
                    flush=True,
                )
            last_attempt = current
        time.sleep(args.interval)


if __name__ == "__main__":
    main()
