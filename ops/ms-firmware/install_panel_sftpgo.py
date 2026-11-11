#!/usr/bin/env python3
"""Install the same official SFTPGo version as FVDS into the private runtime."""

import argparse
import hashlib
import json
import platform
import shutil
import tarfile
from pathlib import Path
from urllib.request import urlopen

VERSION = "2.7.6"
RUNTIME = Path(__file__).resolve().parent / ".panel-runtime"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runtime", type=Path, default=RUNTIME)
    args = parser.parse_args()
    system = {"Darwin": "macOS", "Linux": "linux"}.get(platform.system())
    architecture = {"arm64": "arm64", "aarch64": "arm64", "x86_64": "x86_64", "AMD64": "x86_64"}.get(platform.machine())
    if system is None or architecture is None:
        parser.error("Use an official SFTPGo 2.7.6 package for your system and --binary/--resources")
    args.runtime.mkdir(parents=True, exist_ok=True, mode=0o700)
    args.runtime.chmod(0o700)
    with urlopen(f"https://api.github.com/repos/drakkan/sftpgo/releases/tags/v{VERSION}", timeout=60) as response:
        release = json.load(response)
    name = f"sftpgo_v{VERSION}_{system}_{architecture}.tar.xz"
    asset = next((a for a in release["assets"] if a["name"] == name), None)
    if asset is None or not str(asset.get("digest", "")).startswith("sha256:"):
        raise RuntimeError("Official release asset/checksum not found")
    archive = args.runtime / name
    with urlopen(asset["browser_download_url"], timeout=180) as response, archive.open("wb") as target:
        shutil.copyfileobj(response, target)
    if "sha256:" + hashlib.sha256(archive.read_bytes()).hexdigest() != asset["digest"]:
        raise RuntimeError("SFTPGo SHA-256 verification failed")
    with tarfile.open(archive) as source:
        source.extractall(args.runtime / "sftpgo", filter="data")
    print(f"Official SFTPGo {VERSION} installed; SHA-256 verified.")


if __name__ == "__main__":
    main()
