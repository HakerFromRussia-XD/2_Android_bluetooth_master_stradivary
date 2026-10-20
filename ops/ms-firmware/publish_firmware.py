#!/usr/bin/env python3
import argparse
import configparser
import fcntl
import hashlib
import json
import os
import re
import shutil
import stat
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath

FAMILIES = ("FAM", "GUI", "EMG", "BLDC_Driver", "FINGER")
MAX_FILE_SIZE = 25 * 1024 * 1024
MAX_RELEASE_SIZE = 200 * 1024 * 1024
MAX_ZIP_EXPANDED_SIZE = 64 * 1024 * 1024
RELEASE_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$")
SHA256_PATTERN = re.compile(r"^[0-9a-f]{64}$")
MOTORICA_CRC32_POLYNOMIAL = 0x04C11DB7


def now() -> str:
    return datetime.now(timezone.utc).isoformat()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def motorica_crc32(data: bytes) -> int:
    crc = 0xFFFFFFFF
    for value in data:
        crc ^= value
        for _ in range(8):
            crc = (
                (crc >> 1) ^ MOTORICA_CRC32_POLYNOMIAL
                if crc & 1
                else crc >> 1
            )
        crc &= 0xFFFFFFFF
    return crc ^ 0xFFFFFFFF


def safe_relative_path(value: str) -> PurePosixPath:
    path = PurePosixPath(value)
    if (
        not value
        or value.startswith("/")
        or "\\" in value
        or ":" in value
        or any(part in ("", ".", "..") for part in path.parts)
    ):
        raise ValueError(f"Invalid relative path: {value!r}")
    return path


def validate_zip(path: Path) -> str:
    try:
        with zipfile.ZipFile(path) as archive:
            expanded = 0
            names = set()
            for entry in archive.infolist():
                entry_path = PurePosixPath(entry.filename)
                if (
                    entry.filename.startswith("/")
                    or "\\" in entry.filename
                    or any(part in ("", ".", "..") for part in entry_path.parts)
                ):
                    raise ValueError(f"Unsafe ZIP member in {path.name}: {entry.filename}")
                mode = entry.external_attr >> 16
                if stat.S_IFMT(mode) == stat.S_IFLNK:
                    raise ValueError(f"ZIP symlink is not allowed: {entry.filename}")
                if entry.flag_bits & 0x1:
                    raise ValueError(f"Encrypted ZIP member is not allowed: {entry.filename}")
                if not entry.is_dir():
                    expanded += entry.file_size
                    names.add(entry.filename)
            if expanded > MAX_ZIP_EXPANDED_SIZE:
                raise ValueError(f"ZIP expands beyond 64 MiB: {path.name}")
            required = {"UserFW.bin", "FW_ini.ini"}
            if not required.issubset(names):
                raise ValueError(f"Firmware ZIP is missing {sorted(required - names)}: {path.name}")
            corrupt = archive.testzip()
            if corrupt:
                raise ValueError(f"Corrupt ZIP member in {path.name}: {corrupt}")

            image = archive.read("UserFW.bin")
            parser = configparser.ConfigParser(interpolation=None)
            parser.optionxform = str
            parser.read_string(archive.read("FW_ini.ini").decode("utf-8-sig"))
            size = int(parser["FW_INFO"]["FWSize"])
            checksum = int(parser["FW_INFO"]["FWCRC"])
            version_parts = [
                int(parser["FW_INFO"][key])
                for key in ("FwMajorVersion", "FwMinorVersion", "FwQuickFix")
            ]
            if any(part < 0 or part > 255 for part in version_parts):
                raise ValueError(f"Firmware version is out of range in {path.name}")
            if len(image) != size:
                raise ValueError(f"UserFW.bin size mismatch in {path.name}")
            if motorica_crc32(image) != checksum:
                raise ValueError(f"UserFW.bin CRC mismatch in {path.name}")
            return ".".join(str(part) for part in version_parts)
    except (configparser.Error, KeyError, UnicodeDecodeError, ValueError, zipfile.BadZipFile) as error:
        if isinstance(error, ValueError) and str(error).startswith(
            ("Unsafe ", "ZIP ", "Firmware ", "UserFW.bin ")
        ):
            raise
        raise ValueError(f"Invalid firmware ZIP {path.name}: {error}") from error


def validate_tree(source: Path) -> list[Path]:
    files = []
    total_size = 0
    for path in source.rglob("*"):
        if path.is_symlink():
            raise ValueError(f"Symlinks are not allowed: {path.relative_to(source)}")
        if path.is_dir():
            continue
        if not path.is_file():
            raise ValueError(f"Unsupported filesystem entry: {path.relative_to(source)}")
        size = path.stat().st_size
        if size > MAX_FILE_SIZE:
            raise ValueError(f"File exceeds 25 MiB: {path.relative_to(source)}")
        total_size += size
        if total_size > MAX_RELEASE_SIZE:
            raise ValueError("Release exceeds 200 MiB")
        files.append(path)
    return files


def validate_assembly(source: Path) -> dict:
    assembly_path = source / "assembly.json"
    if not assembly_path.is_file():
        raise ValueError("assembly.json is required")
    assembly = json.loads(assembly_path.read_text(encoding="utf-8"))
    if assembly.get("format") != 1 or assembly.get("kind") != "assembly":
        raise ValueError("Unsupported assembly format")
    for field in ("created_at", "created_by", "product"):
        if not isinstance(assembly.get(field), str) or not assembly[field]:
            raise ValueError(f"Missing assembly field: {field}")
    if not isinstance(assembly.get("config_version"), int) or assembly["config_version"] < 0:
        raise ValueError("Invalid config_version")

    checksum = assembly.get("checksum")
    unsigned = dict(assembly)
    unsigned.pop("checksum", None)
    canonical = json.dumps(
        unsigned, sort_keys=True, separators=(",", ":"), ensure_ascii=False
    ).encode("utf-8")
    if checksum != hashlib.sha256(canonical).hexdigest():
        raise ValueError("Assembly checksum mismatch")

    addresses = set()
    modules = assembly.get("modules")
    if not isinstance(modules, list):
        raise ValueError("Assembly modules must be a list")
    for module in modules:
        address = module.get("address")
        if not isinstance(address, int) or not 0 <= address <= 255:
            raise ValueError("Invalid module address")
        if address in addresses:
            raise ValueError(f"Duplicate module address: {address}")
        addresses.add(address)
        relative = safe_relative_path(module.get("file", ""))
        if relative.suffix.lower() != ".zip" or relative.parts[0] not in FAMILIES:
            raise ValueError(f"Invalid module path: {relative}")
        expected = module.get("sha256", "")
        if not SHA256_PATTERN.fullmatch(expected):
            raise ValueError(f"Invalid SHA-256 for {relative}")
        archive = source.joinpath(*relative.parts)
        if not archive.is_file():
            raise ValueError(f"Missing module archive: {relative}")
        validate_zip(archive)
        if sha256(archive) != expected:
            raise ValueError(f"SHA-256 mismatch for {relative}")
    return assembly


def build_catalog(source: Path) -> dict:
    entries = []
    for family in FAMILIES:
        directory = source / family
        if not directory.is_dir() or directory.is_symlink():
            raise ValueError(f"Missing firmware family directory: {family}")
        for path in sorted(directory.iterdir(), key=lambda value: value.name.lower()):
            if path.is_symlink() or not path.is_file():
                raise ValueError(f"Invalid catalog entry: {path.relative_to(source)}")
            if path.suffix.lower() != ".zip":
                raise ValueError(f"Unexpected non-ZIP file: {path.relative_to(source)}")
            version = validate_zip(path)
            entries.append(
                {
                    "family": family,
                    "name": path.name,
                    "path": path.relative_to(source).as_posix(),
                    "size": path.stat().st_size,
                    "sha256": sha256(path),
                    "version": version,
                }
            )
    return {"format": 1, "generated_at": now(), "files": entries}


def validate_immutable_archive_paths(root: Path, source: Path) -> None:
    releases = root / "releases"
    if not releases.exists():
        return
    for candidate in source.rglob("*.zip"):
        relative = candidate.relative_to(source)
        candidate_hash = sha256(candidate)
        for release in releases.iterdir():
            previous = release / relative
            if previous.is_file() and sha256(previous) != candidate_hash:
                raise ValueError(f"Archive path already has different content: {relative}")


def write_log(root: Path, payload: dict) -> None:
    log_dir = root / "publication-log"
    log_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    path = log_dir / f"{stamp}.json"
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    os.chmod(path, 0o640)


def switch_current(root: Path, release_id: str) -> str:
    current = root / "current"
    previous = os.readlink(current) if current.is_symlink() else "-"
    replacement = root / ".current.new"
    replacement.unlink(missing_ok=True)
    os.symlink(Path("releases") / release_id, replacement)
    os.replace(replacement, current)
    return previous


def publish_release(root: Path, release_id: str) -> dict:
    if not RELEASE_PATTERN.fullmatch(release_id):
        raise ValueError("Invalid release id")
    root.mkdir(parents=True, exist_ok=True)
    with (root / ".publish.lock").open("a+") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        source = root / "staging" / release_id
        if not source.is_dir() or source.is_symlink():
            raise ValueError(f"Staging release not found: {release_id}")
        releases = root / "releases"
        target = releases / release_id
        temporary = releases / f".{release_id}.tmp"
        if target.exists() or temporary.exists():
            raise ValueError(f"Release already exists: {release_id}")
        releases.mkdir(parents=True, exist_ok=True)
        try:
            shutil.copytree(source, temporary, symlinks=True)
            files = validate_tree(temporary)
            validate_assembly(temporary)
            catalog = build_catalog(temporary)
            validate_immutable_archive_paths(root, temporary)
            (temporary / "catalog.json").write_text(
                json.dumps(catalog, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
            )
            for directory, _, names in os.walk(temporary):
                os.chmod(directory, 0o755)
                for name in names:
                    os.chmod(Path(directory) / name, 0o644)
            os.rename(temporary, target)
        except Exception:
            if temporary.exists():
                shutil.rmtree(temporary)
            raise
        previous = switch_current(root, release_id)

        result = {
            "status": "published",
            "release": release_id,
            "previous": previous,
            "files": len(files),
            "catalog_files": len(catalog["files"]),
            "published_at": now(),
        }
        write_log(root, result)
        return result


def rollback_release(root: Path, release_id: str) -> dict:
    if not RELEASE_PATTERN.fullmatch(release_id):
        raise ValueError("Invalid release id")
    with (root / ".publish.lock").open("a+") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        target = root / "releases" / release_id
        if not target.is_dir() or target.is_symlink():
            raise ValueError(f"Published release not found: {release_id}")
        validate_assembly(target)
        build_catalog(target)
        previous = switch_current(root, release_id)
        result = {
            "status": "rolled_back",
            "release": release_id,
            "previous": previous,
            "rolled_back_at": now(),
        }
        write_log(root, result)
        return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("release_id")
    parser.add_argument("--root", default="/srv/ms-firmware")
    parser.add_argument("--rollback", action="store_true")
    args = parser.parse_args()
    try:
        action = rollback_release if args.rollback else publish_release
        result = action(Path(args.root), args.release_id)
    except Exception as error:
        payload = {
            "status": "rejected",
            "release": args.release_id,
            "error": str(error),
            "rejected_at": now(),
        }
        try:
            write_log(Path(args.root), payload)
        except Exception:
            pass
        print(json.dumps(payload, ensure_ascii=False), file=sys.stderr)
        return 1
    print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
