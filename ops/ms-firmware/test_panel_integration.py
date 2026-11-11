"""End-to-end SFTPGo login/upload test against an isolated, fake Motorica API."""
import hashlib
import html
import io
import json
import re
import secrets
import socket
import subprocess
import tempfile
import threading
import time
import unittest
import zipfile
from http.cookiejar import CookieJar
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import HTTPCookieProcessor, Request, build_opener
from unittest.mock import patch

import firmware_panel as launcher
import motorica_httpfs as panel
from publish_firmware import motorica_crc32


@unittest.skipUnless((launcher.RUNTIME / "sftpgo" / "sftpgo").is_file(), "Install SFTPGo to run the browser-server integration test")
class SFTPGoIntegrationTest(unittest.TestCase):
    def test_password_login_and_upload_through_actual_webclient_server(self):
        with tempfile.TemporaryDirectory() as directory:
            runtime = Path(directory)
            password = secrets.token_urlsafe(32)
            (runtime / "password").write_text(password)
            fs = panel.FirmwareFS("fake-upstream-key", runtime / "state")
            image = b"isolated integration test"
            buffer = io.BytesIO()
            with zipfile.ZipFile(buffer, "w") as archive:
                archive.writestr("UserFW.bin", image)
                archive.writestr("FW_ini.ini", f"[FW_INFO]\nFWSize={len(image)}\nFWCRC={motorica_crc32(image)}\nFwMajorVersion=1\nFwMinorVersion=2\nFwQuickFix=3\n")
            data = buffer.getvalue()
            record = {"Id": "isolated-id", "FileName": "test_v1.2.3.zip", "BoardModel": "FAM",
                      "Version": "1.2.3", "Size": len(data), "Sha256": hashlib.sha256(data).hexdigest()}
            records, posts = [], []

            def upstream(url, *, key=None, body=None, content_type=None):
                self.assertEqual(key, "fake-upstream-key")
                if body is not None:
                    posts.append(body)
                    records.append(record)
                    return json.dumps(record).encode()
                self.assertTrue(url.endswith("/isolated-id/download"))
                return data

            with panel.HTTPFSServer(("127.0.0.1", 0), fs, "adapter-secret") as server:
                threading.Thread(target=server.serve_forever, daemon=True).start()
                with socket.socket() as reserve:
                    reserve.bind(("127.0.0.1", 0))
                    port = reserve.getsockname()[1]
                launcher.configure(runtime, launcher.RUNTIME / "sftpgo", port, server.server_port,
                                   "adapter-secret", runtime / "password")
                process = subprocess.Popen([str(launcher.RUNTIME / "sftpgo" / "sftpgo"), "serve",
                    "--config-dir", str(runtime), "--loaddata-from", str(runtime / "users.json"),
                    "--loaddata-clean", "--log-level", "error", "--log-file-path", str(runtime / "log")],
                    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                try:
                    browser = build_opener(HTTPCookieProcessor(CookieJar()))
                    base = f"http://127.0.0.1:{port}"
                    for attempt in range(50):
                        try:
                            with browser.open(base + "/web/client/login", timeout=1) as response:
                                login = response.read().decode()
                            break
                        except OSError:
                            if process.poll() is not None:
                                self.fail("SFTPGo failed to start")
                            time.sleep(0.1)
                    else:
                        self.fail("SFTPGo did not become ready")
                    csrf = html.unescape(re.search(r'name="_form_token" value="([^"]+)"', login)[1])
                    with patch.object(panel.api, "remote_records", side_effect=lambda *a, **kw: list(records)), patch.object(panel.api, "request", side_effect=upstream):
                        # Login via the same HTML form as the actual WebClient.
                        form = urlencode({"username": "ms-firmware", "password": password, "_form_token": csrf}).encode()
                        with browser.open(base + "/web/client/login", form, timeout=10) as response:
                            page = response.read().decode()
                        self.assertIn("Upload Files", page)
                        self.assertNotIn("adapter-secret", page)
                        self.assertNotIn("fake-upstream-key", page)
                        self.assertFalse((runtime / "users.json").exists())
                        csrf = re.search(r"'X-CSRF-TOKEN': '([^']+)'", page)[1]
                        upload_url = re.search(r"uploadPath = '([^'\n]+)\?path=", page)[1]
                        upload_url = json.loads('"' + upload_url + '"')
                        request = Request(base + upload_url + "?" + urlencode({"path": "/firmware/FAM/test_v1.2.3.zip"}),
                                          data=data, headers={"X-CSRF-TOKEN": csrf, "Content-Type": "application/zip"})
                        with browser.open(request, timeout=20) as response:
                            self.assertIn(response.status, (200, 201))
                            response.read()
                        self.assertEqual(len(posts), 1)
                        self.assertTrue(json.loads((runtime / "state" / "last-upload.json").read_text())["verified"])
                        self.assertEqual(fs.readdir("firmware/FAM")[0]["name"], "test_v1.2.3.zip")
                finally:
                    process.terminate()
                    process.wait(timeout=10)
                    server.shutdown()


if __name__ == "__main__":
    unittest.main()
