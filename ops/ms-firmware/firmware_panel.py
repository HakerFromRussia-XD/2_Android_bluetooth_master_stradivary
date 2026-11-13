#!/usr/bin/env python3
"""Run the original SFTPGo WebClient against Motorica firmware storage."""

import argparse
import json
import os
import secrets
import subprocess
import threading
from pathlib import Path

import migrate_to_motorica as api
from motorica_httpfs import FirmwareFS, HTTPFSServer

ROOT = Path(__file__).resolve().parent
RUNTIME = ROOT / ".panel-runtime"


def private_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n")
    path.chmod(0o600)


def configure(runtime, resources, port, adapter_port, token, password_file, base_config=None):
    config = json.loads((base_config or resources / "sftpgo.json").read_text())
    config["common"]["upload_mode"] = 0
    config["common"]["temp_path"] = str(runtime / "tmp")
    config["common"]["max_total_connections"] = 20
    config["common"]["max_per_host_connections"] = 10
    config["sftpd"]["bindings"] = [{"port": 0, "address": "127.0.0.1"}]
    config["ftpd"]["bindings"] = [{"port": 0, "address": "127.0.0.1"}]
    config["webdavd"]["bindings"] = [{"port": 0, "address": "127.0.0.1"}]
    config["data_provider"].update(driver="memory", name="", track_quota=0,
                                    create_default_admin=False, users_base_dir=str(runtime / "home"))
    binding = config["httpd"]["bindings"][0]
    binding.update(address="127.0.0.1", port=port, enable_web_admin=False,
                   enable_web_client=True, enable_rest_api=False)
    binding["branding"]["web_client"].update(name="SFTPGo WebClient", short_name="WebClient")
    config["httpd"]["bindings"] = [binding]
    config["httpd"]["templates_path"] = str(resources / "templates")
    config["httpd"]["static_files_path"] = str(resources / "static")
    config["httpd"]["openapi_path"] = str(resources / "openapi")
    config["smtp"]["templates_path"] = str(resources / "templates")
    config["httpd"]["max_upload_file_size"] = api.MAX_BYTES + 1024 * 1024
    config["httpd"]["hide_support_link"] = True
    config["httpd"]["cookie_lifetime"] = 20
    config["httpd"]["jwt_lifetime"] = 10
    config["httpd"]["signing_passphrase"] = secrets.token_urlsafe(48)
    config["telemetry"]["bind_port"] = 0
    for directory in (runtime / "tmp", runtime / "home"):
        directory.mkdir(parents=True, exist_ok=True)
    private_json(runtime / "sftpgo.json", config)
    password = password_file.read_text().rstrip("\r\n")
    if not password:
        raise ValueError("Файл пароля пуст")
    user = {
        "username": "ms-firmware", "password": password, "status": 1,
        "home_dir": str(runtime / "home"),
        "permissions": {"/": ["list", "download", "upload", "create_dirs"],
                        "/firmware": ["list", "download", "upload", "overwrite", "create_dirs"]},
        "filesystem": {"provider": 6, "httpconfig": {
            "endpoint": f"http://127.0.0.1:{adapter_port}/v1",
            "api_key": {"status": "Plain", "payload": token},
        }},
        "filters": {"denied_protocols": ["SSH", "FTP", "DAV"], "start_directory": "/firmware",
                    "web_client": ["shares-disabled", "mfa-disabled", "password-change-disabled", "publickey-change-disabled"]},
    }
    # SFTPGo requires an initialized administrator even when the admin UI/API are disabled.
    # No admin listener is exposed; this random credential is discarded on shutdown.
    admin = {"username": "panel-internal", "password": secrets.token_urlsafe(48),
             "status": 1, "permissions": ["*"]}
    private_json(runtime / "users.json", {"version": 16, "users": [user], "admins": [admin]})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--adapter-port", type=int, default=0)
    parser.add_argument("--runtime", type=Path, default=RUNTIME)
    parser.add_argument("--resources", type=Path, default=RUNTIME / "sftpgo")
    parser.add_argument("--binary", type=Path, default=RUNTIME / "sftpgo" / "sftpgo")
    parser.add_argument("--base-config", type=Path, help="Base sftpgo.json when resources are installed separately")
    parser.add_argument("--password-file", type=Path, default=RUNTIME / "webclient.password")
    args = parser.parse_args()
    key = os.environ.get("MOTORICA_WRITE_KEY") or api.WRITE_KEY
    if not key:
        parser.error("Set MOTORICA_WRITE_KEY or WRITE_KEY in migrate_to_motorica.py")
    runtime, resources = args.runtime.resolve(), args.resources.resolve()
    base_config = args.base_config.resolve() if args.base_config else resources / "sftpgo.json"
    if not args.binary.is_file() or not base_config.is_file():
        parser.error("SFTPGo 2.7.6 is required; see README.md for installation")
    if not args.password_file.is_file():
        parser.error("Provide the existing WebClient password in --password-file")
    runtime.mkdir(parents=True, exist_ok=True, mode=0o700)
    runtime.chmod(0o700)
    filesystem = FirmwareFS(key, runtime / "state")
    token = secrets.token_urlsafe(48)
    with HTTPFSServer(("127.0.0.1", args.adapter_port), filesystem, token) as adapter:
        configure(runtime, resources, args.port, adapter.server_port, token, args.password_file, base_config)
        thread = threading.Thread(target=adapter.serve_forever, daemon=True)
        thread.start()
        command = [str(args.binary.resolve()), "serve", "--config-dir", str(runtime),
                   "--loaddata-from", str(runtime / "users.json"), "--loaddata-clean",
                   "--log-file-path", str(runtime / "sftpgo.log"), "--log-level", "info"]
        child_env = {name: value for name, value in os.environ.items()
                     if name not in {"MOTORICA_WRITE_KEY", "MOTORICA_READ_KEY"}}
        child = subprocess.Popen(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, env=child_env)
        print(f"MS Firmware Files: http://127.0.0.1:{args.port}/web/client/", flush=True)
        print("Login: ms-firmware; password: same as FVDS. Storage: Motorica API.", flush=True)
        try:
            result = child.wait()
            if result:
                raise SystemExit(f"SFTPGo stopped ({result}); see {runtime / 'sftpgo.log'}")
        except KeyboardInterrupt:
            child.terminate()
            child.wait(timeout=20)
        finally:
            adapter.shutdown()
            # The password lives in its designated file; remove the transient import copy.
            (runtime / "users.json").unlink(missing_ok=True)


if __name__ == "__main__":
    main()
