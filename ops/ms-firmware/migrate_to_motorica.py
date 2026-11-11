#!/usr/bin/env python3
"""Copy catalogued firmware ZIPs to Motorica; default mode only prepares a backup."""

import argparse
import fcntl
import hashlib
import json
import os
import re
import sys
import uuid
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener

from publish_firmware import FAMILIES, validate_zip

# ВСТАВЬТЕ WriteKey МЕЖДУ КАВЫЧКАМИ (не коммитьте файл с ключом).
# Безопаснее оставить пустым и задать переменную окружения MOTORICA_WRITE_KEY.
WRITE_KEY = "q6qb/S8i(4cTP,G(iAxy?kw1z:@b-oVP"

SOURCE_URL = "https://firmware-ms.157.22.203.6.nip.io"
DESTINATION_URL = "https://gateway.motorica.org"
MAX_BYTES = 100 * 1024 * 1024


class NoRedirect(HTTPRedirectHandler):
    # Never forward an API key to a redirect target or repeat a POST implicitly.
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def request(url, *, key=None, body=None, content_type=None):
    headers = {"User-Agent": "MotoricaFirmwareMigration/1", "Cache-Control": "no-cache"}
    if key:
        headers["X-Api-Key"] = key
    if content_type:
        headers["Content-Type"] = content_type
    req = Request(url, data=body, headers=headers)
    try:
        with build_opener(NoRedirect()).open(req, timeout=180) as response:
            data = response.read(MAX_BYTES + 1)
            if len(data) > MAX_BYTES:
                raise ValueError("Response exceeds 100 MiB")
            return data
    except HTTPError as error:
        # Do not print request headers, server response bodies or credentials.
        raise RuntimeError(f"HTTP {error.code}: {urlsplit(url).path}") from None
    except URLError:
        raise RuntimeError(f"Network request failed: {urlsplit(url).path}") from None


def save_json(path, value):
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def field(record, name):
    """Accept the documented PascalCase and common camelCase response keys."""
    fields = {k.lower(): v for k, v in record.items()}
    if name.lower() not in fields:
        raise ValueError(f"API response is missing {name}; check the actual JSON contract")
    return fields[name.lower()]


def identity(record):
    checksum = str(field(record, "Sha256")).lower()
    if not re.fullmatch(r"[0-9a-f]{64}", checksum):
        raise ValueError("Invalid Sha256 in API response")
    return str(field(record, "BoardModel")), str(field(record, "Version")), checksum


def remote_records(api, key):
    records = json.loads(request(api, key=key))
    if not isinstance(records, list):
        raise ValueError("Expected a JSON array from GET /api/firmware; check the actual JSON contract")
    for record in records:
        identity(record)
        field(record, "Id")
    return records


def multipart(path, model, version, source_path, *, description=None):
    boundary = "MotoricaMigration" + uuid.uuid4().hex
    chunks = []
    for name, value in {
        "BoardModel": model,
        "Version": version,
        "Description": description if description is not None else f"Migrated from FVDS: {source_path}",
    }.items():
        chunks.append(
            f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode("utf-8")
        )
    chunks.append(
        (f'--{boundary}\r\nContent-Disposition: form-data; name="File"; filename="{path.name}"\r\n'
         + ('Content-Type: application/json\r\n\r\n' if path.suffix.lower() == '.json'
            else 'Content-Type: application/zip\r\n\r\n')).encode("utf-8")
    )
    chunks.extend([path.read_bytes(), f"\r\n--{boundary}--\r\n".encode()])
    return b"".join(chunks), f"multipart/form-data; boundary={boundary}"


def prepare(source, work, models):
    catalog_bytes = request(source + "/catalog.json")
    catalog = json.loads(catalog_bytes)
    if catalog.get("format") != 1 or not isinstance(catalog.get("files"), list):
        raise ValueError("Unsupported source catalog")
    assembly_bytes = request(source + "/assembly.json")
    assembly = json.loads(assembly_bytes)
    result = []
    seen = set()
    for item in catalog["files"]:
        family, name, relative = item["family"], item["name"], item["path"]
        if (family not in FAMILIES or relative != f"{family}/{name}"
                or any(c in name for c in '/\\\r\n"') or name in ("", ".", "..")
                or not name.lower().endswith(".zip") or relative in seen):
            raise ValueError(f"Invalid or duplicate source path: {relative!r}")
        seen.add(relative)
        size = item["size"]
        if not isinstance(size, int) or not 0 < size <= MAX_BYTES:
            raise ValueError(f"Invalid source size: {relative}")
        data = request(source + "/" + quote(relative, safe="/"))
        checksum = hashlib.sha256(data).hexdigest()
        if len(data) != size or checksum != item["sha256"].lower():
            raise ValueError(f"Source size/SHA-256 mismatch: {relative}")
        target = work / "archives" / family / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        version = validate_zip(target)
        if version != item["version"]:
            raise ValueError(f"ZIP version differs from catalog: {relative}")
        result.append({"source_path": relative, "board_model": models[family],
                       "version": version, "sha256": checksum, "size": size})
        print(f"Prepared: {relative} -> BoardModel={models[family]}", flush=True)
    # Ensure the automatic-update manifest does not reference unbacked-up files.
    by_path = {item["source_path"]: item for item in result}
    for module in assembly["modules"]:
        archive = by_path.get(module["file"])
        if archive is None or archive["sha256"] != module["sha256"].lower():
            raise ValueError(f"assembly.json references a missing/different archive: {module['file']}")
    snapshot = work / "snapshots" / hashlib.sha256(catalog_bytes + assembly_bytes).hexdigest()
    snapshot.mkdir(parents=True, exist_ok=True)
    (snapshot / "catalog.json").write_bytes(catalog_bytes)
    (snapshot / "assembly.json").write_bytes(assembly_bytes)
    return result, str(snapshot)


def migrate(api, key, work, entries, snapshot):
    # Read destination before the first POST. Abort on an unknown response format.
    records = remote_records(api, key)
    index = {}
    for record in records:
        index.setdefault(identity(record), record)
    report = {"source_snapshot": snapshot, "destination": api, "files": []}
    report_path = work / "migration-report.json"
    for entry in entries:
        row = dict(entry, status="pending")
        report["files"].append(row)
        save_json(report_path, report)
        token = entry["board_model"], entry["version"], entry["sha256"]
        record = index.get(token)
        if record is None:
            # Never retry POST automatically: it may have succeeded despite a lost response.
            row["status"] = "posting; if interrupted, rerun to reconcile with server list"
            save_json(report_path, report)
            body, content_type = multipart(work / "archives" / entry["source_path"],
                                           entry["board_model"], entry["version"], entry["source_path"])
            record = json.loads(request(api, key=key, body=body, content_type=content_type))
            row["action"] = "uploaded"
        else:
            row["action"] = "reused"
        identifier = str(field(record, "Id"))
        row.update(id=identifier, status="awaiting_download_verification")
        save_json(report_path, report)
        if str(field(record, "Sha256")).lower() != entry["sha256"]:
            raise ValueError(f"Server SHA-256 mismatch: {entry['source_path']}")
        data = request(api + "/" + quote(identifier, safe="") + "/download", key=key)
        if len(data) != entry["size"] or hashlib.sha256(data).hexdigest() != entry["sha256"]:
            raise ValueError(f"Downloaded copy differs from source: {entry['source_path']}")
        index[token] = record
        row["status"] = "verified"
        save_json(report_path, report)
        print(f"Verified ({row['action']}): {entry['source_path']} -> {identifier}", flush=True)
    print(f"Migration complete: {len(entries)} archives. Report: {report_path}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--upload", action="store_true", help="Upload to Motorica; otherwise only download and validate source")
    parser.add_argument("--source-url", default=SOURCE_URL)
    parser.add_argument("--destination-url", default=DESTINATION_URL)
    parser.add_argument("--work-dir", type=Path, default=Path("/tmp/motorica-firmware-migration"))
    parser.add_argument("--board-model", action="append", default=[], metavar="FAMILY=MODEL", help="Override BoardModel; repeat for multiple families")
    args = parser.parse_args()
    for url in (args.source_url, args.destination_url):
        parsed = urlsplit(url)
        if parsed.scheme != "https" or not parsed.netloc or parsed.username or parsed.password or parsed.query or parsed.fragment:
            parser.error("Source and destination must be HTTPS URLs without credentials/query/fragment")
    models = {family: family for family in FAMILIES}
    for override in args.board_model:
        family, separator, model = override.partition("=")
        if not separator or family not in models or not model.strip() or any(c in model for c in "\r\n"):
            parser.error(f"Invalid --board-model: {override!r}")
        models[family] = model
    key = os.environ.get("MOTORICA_WRITE_KEY") or WRITE_KEY
    if args.upload and not key:
        parser.error("Set WRITE_KEY at the top of this script or MOTORICA_WRITE_KEY in the environment")
    args.work_dir.mkdir(parents=True, exist_ok=True)
    with (args.work_dir / ".migration.lock").open("w") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        entries, snapshot = prepare(args.source_url.rstrip("/"), args.work_dir, models)
        save_json(args.work_dir / "migration-plan.json", {"source_snapshot": snapshot, "files": entries})
        if args.upload:
            migrate(args.destination_url.rstrip("/") + "/api/firmware", key, args.work_dir, entries, snapshot)
        else:
            print(f"Preparation complete: {len(entries)} archives. Add --upload to transfer them.")
        print("assembly.json/catalog.json are backed up locally, not published to Motorica.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, TypeError, OSError, RuntimeError) as error:
        print(f"Stopped: {error}", file=sys.stderr)
        sys.exit(1)
