#!/usr/bin/env python3
"""Validate a manually uploaded game catalog and atomically expose its last valid version."""
import argparse
import fcntl
import hashlib
import json
import os
import re
import tempfile
import time
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse

PACKAGE_PATTERN = re.compile(r"[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+")
IOS_BUNDLE_PATTERN = re.compile(r"[A-Za-z0-9.-]+")
SCHEME_PATTERN = re.compile(r"[A-Za-z][A-Za-z0-9+.-]*")
MAX_VERSION_CODE = (1 << 63) - 1


def required_string(value: dict, key: str) -> str:
    result = value.get(key)
    if not isinstance(result, str) or not result.strip():
        raise ValueError(f"Missing or invalid {key}")
    return result


def validate_version(platform: dict) -> None:
    version = platform.get("versionCode")
    if type(version) is not int or not 0 <= version <= MAX_VERSION_CODE:
        raise ValueError("versionCode must be a non-negative signed 64-bit integer")
    name = platform.get("versionName", "")
    if not isinstance(name, str):
        raise ValueError("versionName must be a string")


def validate_manifest(body: bytes) -> dict:
    catalog = json.loads(body.decode("utf-8"))
    if not isinstance(catalog, dict) or not isinstance(catalog.get("games"), list):
        raise ValueError("The catalog must contain a games array")
    ids, android_packages, ios_bundles = set(), set(), set()
    for game in catalog["games"]:
        if not isinstance(game, dict):
            raise ValueError("Each game must be an object")
        game_id = required_string(game, "id")
        required_string(game, "title")
        if game_id in ids:
            raise ValueError("Duplicate game id")
        ids.add(game_id)
        android = game.get("android")
        if android is None and "packageName" in game:
            android = game  # Retain the Android catalog's original flat format.
        ios = game.get("ios")
        if android is None and ios is None:
            raise ValueError("Each game must describe at least one supported platform")
        if android is not None:
            if not isinstance(android, dict):
                raise ValueError("android must be an object")
            package = required_string(android, "packageName")
            if not PACKAGE_PATTERN.fullmatch(package):
                raise ValueError("Invalid Android packageName")
            if game_id == "stk" and package != "com.motorica.games.stk":
                raise ValueError("The reserved stk entry must use com.motorica.games.stk")
            if package in android_packages:
                raise ValueError("Duplicate Android packageName")
            android_packages.add(package)
            required_string(android, "launcherActivity")
            validate_version(android)
            if "usesMotoricaControl" in android and type(android["usesMotoricaControl"]) is not bool:
                raise ValueError("usesMotoricaControl must be a boolean")
        if ios is not None:
            if not isinstance(ios, dict):
                raise ValueError("ios must be an object")
            bundle = required_string(ios, "bundleId")
            if not IOS_BUNDLE_PATTERN.fullmatch(bundle):
                raise ValueError("Invalid iOS bundleId")
            bundle_key = bundle.lower()
            if bundle_key in ios_bundles:
                raise ValueError("Duplicate iOS bundleId")
            ios_bundles.add(bundle_key)
            if not SCHEME_PATTERN.fullmatch(required_string(ios, "urlScheme")):
                raise ValueError("Invalid iOS urlScheme")
            validate_version(ios)
            store = ios.get("appStoreUrl", "")
            if not isinstance(store, str):
                raise ValueError("appStoreUrl must be a string")
            if store and (urlparse(store).scheme != "https" or not urlparse(store).netloc):
                raise ValueError("appStoreUrl must be empty or an HTTPS URL")
    return catalog


def snapshot(path: Path):
    try:
        current = path.stat()
        return current.st_mtime_ns, current.st_size, current.st_ino
    except FileNotFoundError:
        return None


def publish_manifest(source: Path, destination: Path) -> dict:
    if source.is_symlink() or not source.is_file():
        raise ValueError("The uploaded manifest must be a regular file")
    before = snapshot(source)
    body = source.read_bytes()
    if snapshot(source) != before:
        raise ValueError("The upload is still changing")
    catalog = validate_manifest(body)
    digest = hashlib.sha256(body).hexdigest()
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.is_symlink():
        raise ValueError("The public manifest must not be a symlink")
    with (destination.parent / ".publish.lock").open("a+") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        previous = destination.read_bytes() if destination.exists() else None
        if previous == body:
            return {"status": "unchanged", "sha256": digest, "games": len(catalog["games"])}
        if previous is not None:
            history = destination.parent / "history"
            history.mkdir(mode=0o750, exist_ok=True)
            stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
            previous_digest = hashlib.sha256(previous).hexdigest()
            archive = history / f"{stamp}-{previous_digest}.json"
            with archive.open("xb") as output:
                output.write(previous)
                output.flush()
                os.fsync(output.fileno())
            os.chmod(archive, 0o640)
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(dir=destination.parent, prefix=".manifest-", delete=False) as output:
                temporary = Path(output.name)
                output.write(body)
                output.flush()
                os.fsync(output.fileno())
            os.chmod(temporary, 0o644)
            os.replace(temporary, destination)
            directory_fd = os.open(destination.parent, os.O_RDONLY)
            try:
                os.fsync(directory_fd)
            finally:
                os.close(directory_fd)
        finally:
            if temporary is not None:
                temporary.unlink(missing_ok=True)
    return {"status": "published", "sha256": digest, "games": len(catalog["games"])}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=Path("/srv/ms-firmware/staging/games/manifest.json"))
    parser.add_argument("--destination", type=Path, default=Path("/srv/ms-firmware/games/manifest.json"))
    parser.add_argument("--interval", type=float, default=1.0)
    parser.add_argument("--once", action="store_true")
    args = parser.parse_args()
    if args.interval <= 0:
        parser.error("--interval must be positive")
    last_attempt = None
    while True:
        current = snapshot(args.source)
        if args.once or current != last_attempt or (current is not None and not args.destination.exists()):
            try:
                result = publish_manifest(args.source, args.destination)
                if result["status"] == "published" or args.once:
                    print(json.dumps(result), flush=True)
            except Exception as error:
                print(json.dumps({"status": "deferred", "error": str(error)}), flush=True)
                if args.once:
                    return 1
            last_attempt = current
        if args.once:
            return 0
        time.sleep(args.interval)


if __name__ == "__main__":
    raise SystemExit(main())
