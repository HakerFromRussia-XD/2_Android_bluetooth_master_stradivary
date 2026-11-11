#!/usr/bin/env python3
"""SFTPGo HTTP filesystem adapter for the Motorica firmware API."""

import hashlib
import json
import secrets
import tempfile
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, quote, unquote, urlsplit

import migrate_to_motorica as api
from publish_firmware import FAMILIES, validate_zip

API_URL = api.DESTINATION_URL + "/api/firmware"
DIRECTORY = 2147483648 | 0o755  # Go fs.ModeDir, not POSIX S_IFDIR.
EPOCH = "1970-01-01T00:00:00Z"
METADATA_MODEL = "MS_FIRMWARE_METADATA"
MANIFESTS = ("assembly.json", "catalog.json")
HIDDEN_MODELS = {METADATA_MODEL, "TEST-DO-NOT-FLASH"}


def safe_name(name):
    return isinstance(name, str) and name not in ("", ".", "..") and not any(
        c in name for c in '/\\\r\n"\x00')


def record_value(record, name, default=None):
    return next((v for k, v in record.items() if k.lower() == name.lower()), default)


def file_info(name, *, size=0, directory=False, modified=EPOCH):
    return {"name": name, "size": size, "mode": DIRECTORY if directory else 0o644,
            "last_modified": modified or EPOCH}


class FirmwareFS:
    def __init__(self, key, state):
        self.key = key
        self.state = Path(state)
        self.state.mkdir(parents=True, exist_ok=True)
        self.lock = threading.RLock()
        self.cached_at = 0
        self.cached = None
        self.upload_lock = threading.Lock()

    def records(self, *, refresh=False):
        with self.lock:
            if refresh or self.cached is None or time.monotonic() - self.cached_at > 5:
                self.cached = api.remote_records(API_URL, self.key)
                self.cached_at = time.monotonic()
            return self.cached

    def folders(self, records):
        path = self.state / "folders.json"
        local = json.loads(path.read_text()) if path.exists() else []
        return sorted((set(FAMILIES) | set(local) | {
            str(api.field(r, "BoardModel")) for r in records
            if safe_name(api.field(r, "BoardModel")) and api.field(r, "BoardModel") not in HIDDEN_MODELS
        }) - HIDDEN_MODELS)

    def files(self, records):
        # The API permits repeated names. Expose all records without hiding older files.
        files = {}
        for record in records:
            model = str(api.field(record, "BoardModel"))
            name = record_value(record, "FileName", "")
            identifier = str(api.field(record, "Id"))
            if model == METADATA_MODEL:
                if name in MANIFESTS:
                    # API returns newest first. Earlier revisions remain stored, not in the file tree.
                    files.setdefault(f"firmware/{name}", record)
                continue
            if model in HIDDEN_MODELS:
                continue
            if not safe_name(model):
                raise ValueError("В API обнаружена модель с недопустимым именем папки")
            if not safe_name(name):
                name = identifier + ".zip"
            path = f"firmware/{model}/{name}"
            if path in files:
                stem, suffix = Path(name).stem, Path(name).suffix
                path = f"firmware/{model}/{stem}__{identifier}{suffix}"
            if path in files:
                raise ValueError("Неоднозначные имена файлов в API")
            files[path] = record
        return files

    def stat(self, path, records=None):
        records = self.records() if records is None else records
        parts = path.split("/") if path else []
        if not parts:
            return file_info("/", directory=True)
        if path == "firmware":
            return file_info("firmware", directory=True)
        if len(parts) == 2 and parts[0] == "firmware" and parts[1] in self.folders(records):
            return file_info(parts[1], directory=True)
        record = self.files(records).get(path)
        if record is None:
            raise FileNotFoundError(path)
        return file_info(parts[-1], size=int(record_value(record, "Size", 0)),
                         modified=record_value(record, "UploadedAt", EPOCH))

    def readdir(self, path):
        records = self.records()
        if not path:
            return [file_info("firmware", directory=True)]
        if path == "firmware":
            return ([file_info(model, directory=True) for model in self.folders(records)]
                    + [self.stat(f"firmware/{name}", records) for name in MANIFESTS
                       if f"firmware/{name}" in self.files(records)])
        self.stat(path, records)
        if len(path.split("/")) != 2 or path.split("/")[1] not in self.folders(records):
            raise ValueError("Это не папка")
        prefix = path + "/"
        return [self.stat(name, records) for name in self.files(records) if name.startswith(prefix)]

    def open(self, path, offset=0):
        record = self.files(self.records()).get(path)
        if record is None:
            raise FileNotFoundError(path)
        identifier = str(api.field(record, "Id"))
        data = api.request(API_URL + "/" + quote(identifier, safe="") + "/download", key=self.key)
        if hashlib.sha256(data).hexdigest() != str(api.field(record, "Sha256")).lower():
            raise RuntimeError("SHA-256 скачанного архива не совпадает с API")
        return data[offset:]

    def mkdir(self, path):
        parts = path.split("/")
        if len(parts) != 2 or parts[0] != "firmware" or not safe_name(parts[1]):
            raise ValueError("Создавайте папки моделей непосредственно внутри firmware")
        if len(parts[1]) > 100 or parts[1] in HIDDEN_MODELS:
            raise ValueError("Имя модели должно быть не длиннее 100 символов")
        with self.lock:
            folders = self.folders(self.records())
            if parts[1] not in folders:
                folders.append(parts[1])
                api.save_json(self.state / "folders.json", folders)

    def create(self, path, data):
        if path in {f"firmware/{name}" for name in MANIFESTS}:
            return self.create_manifest(path, data)
        parts = path.split("/")
        if len(parts) != 3 or parts[0] != "firmware" or not all(safe_name(p) for p in parts):
            raise ValueError("Загружайте ZIP в папку firmware/модель")
        model, name = parts[1:]
        if not name.lower().endswith(".zip"):
            raise ValueError("Можно загружать только ZIP с UserFW.bin и FW_ini.ini")
        if not data or len(data) > api.MAX_BYTES:
            raise ValueError("Пустой файл или размер больше 100 МиБ")
        with tempfile.TemporaryDirectory(prefix="motorica-firmware-") as directory:
            archive = Path(directory) / name
            archive.write_bytes(data)
            version = validate_zip(archive)
            checksum = hashlib.sha256(data).hexdigest()
            with self.upload_lock:
                records = self.records(refresh=True)
                if model not in self.folders(records):
                    raise FileNotFoundError("Сначала создайте папку модели")
                existing = self.files(records).get(path)
                if existing is not None:
                    if str(api.field(existing, "Sha256")).lower() == checksum:
                        return {"id": api.field(existing, "Id"), "reused": True}
                    raise PermissionError("Имя уже занято другим файлом. Используйте новое имя архива")
                body, content_type = api.multipart(archive, model, version, path,
                                                    description=f"Uploaded via MS Firmware Files: {path}")
                # A failed POST is not retried: the API creates a new record on every call.
                record = json.loads(api.request(API_URL, key=self.key, body=body, content_type=content_type))
                identifier = str(api.field(record, "Id"))
                # Invalidate even if verification fails; a record may already have been created.
                with self.lock:
                    self.cached = None
                api.save_json(self.state / "last-upload.json", {
                    "id": identifier, "path": path, "sha256": checksum, "verified": False,
                    "uploaded_at": datetime.now(timezone.utc).isoformat(),
                })
                if str(api.field(record, "Sha256")).lower() != checksum:
                    raise RuntimeError("SHA-256 ответа API не совпадает с файлом")
                downloaded = api.request(API_URL + "/" + quote(identifier, safe="") + "/download", key=self.key)
                if len(downloaded) != len(data) or hashlib.sha256(downloaded).hexdigest() != checksum:
                    raise RuntimeError("Не удалось проверить загруженную копию")
                api.save_json(self.state / "last-upload.json", {
                    "id": identifier, "path": path, "sha256": checksum, "verified": True,
                    "uploaded_at": datetime.now(timezone.utc).isoformat(),
                })
                return {"id": identifier, "verified": True}

    def create_manifest(self, path, data):
        name = path.split("/")[-1]
        if path != f"firmware/{name}" or name not in MANIFESTS:
            raise ValueError("Неизвестный JSON-файл")
        if not data or len(data) > 4 * 1024 * 1024:
            raise ValueError("JSON пуст или больше 4 МиБ")
        document = json.loads(data)
        if not isinstance(document, dict) or document.get("format") != 1:
            raise ValueError("Неподдерживаемый формат JSON")
        required = "modules" if name == "assembly.json" else "files"
        if not isinstance(document.get(required), list):
            raise ValueError("Некорректная структура JSON")
        checksum = hashlib.sha256(data).hexdigest()
        with self.upload_lock:
            records = self.records(refresh=True)
            existing = self.files(records).get(path)
            if existing is not None and str(api.field(existing, "Sha256")).lower() == checksum:
                self.open(path)  # Also verify the remote bytes before reporting success.
                return {"id": api.field(existing, "Id"), "reused": True, "verified": True}
            with tempfile.TemporaryDirectory(prefix="motorica-json-") as directory:
                archive = Path(directory) / name
                archive.write_bytes(data)
                body, content_type = api.multipart(archive, METADATA_MODEL, "1.0.0", path,
                    description=f"MS Firmware file: {path}; original bytes preserved")
                record = json.loads(api.request(API_URL, key=self.key, body=body, content_type=content_type))
            identifier = str(api.field(record, "Id"))
            with self.lock:
                self.cached = None
            if str(api.field(record, "Sha256")).lower() != checksum:
                raise RuntimeError("SHA-256 JSON не совпадает с API")
            downloaded = api.request(API_URL + "/" + quote(identifier, safe="") + "/download", key=self.key)
            if downloaded != data:
                raise RuntimeError("Загруженная копия JSON отличается от оригинала")
            return {"id": identifier, "sha256": checksum, "verified": True}


class HTTPFSServer(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, address, filesystem, token):
        super().__init__(address, HTTPFSHandler)
        self.filesystem = filesystem
        self.token = token


class HTTPFSHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass

    def reply(self, status, payload, *, binary=False):
        data = payload if binary else json.dumps(payload, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/octet-stream" if binary else "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def body(self):
        if self.headers.get("Transfer-Encoding", "").lower() == "chunked":
            data = bytearray()
            while True:
                line = self.rfile.readline(128)
                size = int(line.split(b";", 1)[0].strip(), 16)
                if size == 0:
                    # SFTPGo does not send trailers; reject unexpected trailer headers.
                    if self.rfile.readline(8192) != b"\r\n":
                        raise ValueError("Неподдерживаемый trailer")
                    return bytes(data)
                if size < 0 or len(data) + size > api.MAX_BYTES:
                    raise ValueError("Размер больше 100 МиБ")
                chunk = self.rfile.read(size)
                if len(chunk) != size or self.rfile.read(2) != b"\r\n":
                    raise ValueError("Файл получен не полностью")
                data.extend(chunk)
        length = int(self.headers.get("Content-Length", "0"))
        if not 0 < length <= api.MAX_BYTES:
            raise ValueError("Пустой файл или размер больше 100 МиБ")
        data = self.rfile.read(length)
        if len(data) != length:
            raise ValueError("Файл получен не полностью")
        return data

    def handle_operation(self):
        if not secrets.compare_digest(self.headers.get("X-API-KEY", ""), self.server.token):
            self.reply(401, {"error": "Unauthorized"})
            return
        url = urlsplit(self.path)
        parts = url.path.split("/", 3)
        if len(parts) < 3 or parts[1] != "v1":
            self.reply(404, {"error": "Not found"})
            return
        operation = parts[2]
        raw = unquote(parts[3]) if len(parts) == 4 else ""
        path = raw[1:] if raw.startswith("/") else raw
        if path.endswith("/"):
            path = path[:-1]
        if path and not all(safe_name(p) for p in path.split("/")):
            self.reply(400, {"error": "Некорректный путь"})
            return
        fs = self.server.filesystem
        try:
            if self.command == "GET" and operation == "stat":
                self.reply(200, fs.stat(path))
            elif self.command == "GET" and operation == "readdir":
                self.reply(200, fs.readdir(path))
            elif self.command == "GET" and operation == "open":
                offset = int(parse_qs(url.query).get("offset", ["0"])[0])
                if offset < 0:
                    raise ValueError("Некорректное смещение")
                self.reply(200, fs.open(path, offset), binary=True)
            elif self.command == "GET" and operation == "mimetype":
                self.reply(200, {"mime": "application/json" if path.endswith(".json") else "application/zip"})
            elif self.command == "GET" and operation == "dirsize":
                fs.stat(path)
                files = fs.files(fs.records())
                children = [r for name, r in files.items() if not path or name.startswith(path + "/")]
                self.reply(200, {"files": len(children), "size": sum(int(record_value(r, "Size", 0)) for r in children)})
            elif self.command == "POST" and operation == "mkdir":
                fs.mkdir(path)
                self.reply(200, {"message": "OK"})
            elif self.command == "POST" and operation == "create":
                self.reply(201, {"message": "OK", **fs.create(path, self.body())})
            else:
                self.reply(501, {"error": "Операция пока не поддерживается API Motorica"})
        except FileNotFoundError:
            self.reply(404, {"error": "Файл или папка не найдены"})
        except PermissionError as error:
            self.reply(403, {"error": str(error)})
        except (ValueError, KeyError, TypeError):
            self.reply(400, {"error": "Некорректный архив, путь или метаданные. Проверьте ZIP, размер, CRC и версию FW_ini.ini"})
        except Exception:
            # A POST may have completed remotely. Never expose credentials or upstream bodies.
            self.reply(502, {"error": "Не удалось подтвердить операцию Motorica. Обновите список перед повторной загрузкой"})

    do_GET = handle_operation
    do_POST = handle_operation
    do_PATCH = handle_operation
    do_DELETE = handle_operation
