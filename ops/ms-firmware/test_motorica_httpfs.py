import hashlib
import http.client
import io
import json
import tempfile
import threading
import unittest
import zipfile
from email import policy
from email.parser import BytesParser
from pathlib import Path
from unittest.mock import patch

import motorica_httpfs as panel
from publish_firmware import motorica_crc32


class FirmwareFSTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.fs = panel.FirmwareFS("test-key", self.directory.name)
        image = b"firmware integration test"
        memory = io.BytesIO()
        with zipfile.ZipFile(memory, "w") as archive:
            archive.writestr("UserFW.bin", image)
            archive.writestr("FW_ini.ini", f"[FW_INFO]\nFWSize={len(image)}\nFWCRC={motorica_crc32(image)}\nFwMajorVersion=1\nFwMinorVersion=2\nFwQuickFix=3\n")
        self.data = memory.getvalue()
        self.record = {"Id": "one", "BoardModel": "FAM", "FileName": "test_v1.2.3.zip",
                       "Version": "1.2.3", "Sha256": hashlib.sha256(self.data).hexdigest(),
                       "Size": len(self.data), "UploadedAt": "2026-11-11T10:00:00Z"}

    def test_groups_and_duplicate_names_preserve_every_record(self):
        duplicate = dict(self.record, Id="two")
        with patch.object(panel.api, "remote_records", return_value=[self.record, duplicate]):
            self.assertEqual([f["name"] for f in self.fs.readdir("")], ["firmware"])
            files = self.fs.readdir("firmware/FAM")
            self.assertEqual(len(files), 2)
            self.assertEqual(len({f["name"] for f in files}), 2)
            self.assertEqual(files[0]["size"], len(self.data))
            self.assertTrue(self.fs.stat("firmware/FAM")["mode"] & 2147483648)

    def test_valid_upload_uses_ini_version_and_verifies_download(self):
        calls = []

        def upstream(url, *, key=None, body=None, content_type=None):
            self.assertEqual(key, "test-key")
            calls.append(url)
            if body is not None:
                form = BytesParser(policy=policy.default).parsebytes(
                    f"Content-Type: {content_type}\r\n\r\n".encode() + body)
                parts = {p.get_param("name", header="content-disposition"): p for p in form.iter_parts()}
                self.assertEqual(parts["File"].get_payload(decode=True), self.data)
                self.assertEqual(parts["Version"].get_payload(decode=True), b"1.2.3")
                self.assertNotIn(b"Migrated from FVDS", parts["Description"].get_payload(decode=True))
                return json.dumps(self.record).encode()
            return self.data

        with patch.object(panel.api, "remote_records", return_value=[]), patch.object(panel.api, "request", side_effect=upstream):
            result = self.fs.create("firmware/FAM/test_v1.2.3.zip", self.data)
        self.assertTrue(result["verified"])
        self.assertEqual(len(calls), 2)
        self.assertTrue(json.loads((Path(self.directory.name) / "last-upload.json").read_text())["verified"])

    def test_invalid_archive_never_reaches_api(self):
        with patch.object(panel.api, "request") as upstream:
            with self.assertRaises(ValueError):
                self.fs.create("firmware/FAM/invalid.zip", b"invalid")
        upstream.assert_not_called()

    def test_identical_file_does_not_create_duplicate(self):
        with patch.object(panel.api, "remote_records", return_value=[self.record]), patch.object(panel.api, "request") as upstream:
            result = self.fs.create("firmware/FAM/test_v1.2.3.zip", self.data)
        self.assertTrue(result["reused"])
        upstream.assert_not_called()

    def test_existing_name_with_different_hash_is_rejected(self):
        with patch.object(panel.api, "remote_records", return_value=[dict(self.record, Sha256="0" * 64)]), patch.object(panel.api, "request") as upstream:
            with self.assertRaises(PermissionError):
                self.fs.create("firmware/FAM/test_v1.2.3.zip", self.data)
        upstream.assert_not_called()

    def test_corrupt_download_is_not_marked_verified(self):
        with patch.object(panel.api, "remote_records", return_value=[]), patch.object(panel.api, "request", side_effect=[json.dumps(self.record).encode(), b"bad"]):
            with self.assertRaises(RuntimeError):
                self.fs.create("firmware/FAM/test_v1.2.3.zip", self.data)
        self.assertFalse(json.loads((Path(self.directory.name) / "last-upload.json").read_text())["verified"])

    def test_folder_creation_persists_model_without_upload(self):
        with patch.object(panel.api, "remote_records", return_value=[]):
            self.fs.mkdir("firmware/NEW_BOARD")
            second = panel.FirmwareFS("key", self.directory.name)
            self.assertIn("NEW_BOARD", [f["name"] for f in second.readdir("firmware")])
            with self.assertRaises(ValueError):
                self.fs.mkdir("firmware/FAM/nested")

    def test_original_root_json_visible_and_company_tests_hidden(self):
        manifests = [{**self.record, "Id": name, "BoardModel": panel.METADATA_MODEL,
                      "FileName": name} for name in panel.MANIFESTS]
        records = manifests + [dict(self.record, Id="company-test", BoardModel="TEST-DO-NOT-FLASH")]
        with patch.object(panel.api, "remote_records", return_value=records):
            names = {item["name"] for item in self.fs.readdir("firmware")}
            self.assertEqual(names, set(panel.FAMILIES) | set(panel.MANIFESTS))
            with self.assertRaises(FileNotFoundError):
                self.fs.stat("firmware/TEST-DO-NOT-FLASH")

    def test_manifest_upload_preserves_exact_bytes(self):
        data = b'{ "format": 1, "modules": [] }\n'
        record = dict(self.record, BoardModel=panel.METADATA_MODEL,
                      FileName="assembly.json", Sha256=hashlib.sha256(data).hexdigest())
        with patch.object(panel.api, "remote_records", return_value=[]), patch.object(panel.api, "request", side_effect=[json.dumps(record).encode(), data]) as upstream:
            result = self.fs.create("firmware/assembly.json", data)
        self.assertTrue(result["verified"])
        request = upstream.call_args_list[0].kwargs
        form = BytesParser(policy=policy.default).parsebytes(f"Content-Type: {request['content_type']}\r\n\r\n".encode() + request["body"])
        file_part = next(p for p in form.iter_parts() if p.get_filename())
        self.assertEqual(file_part.get_payload(decode=True), data)
        self.assertEqual(file_part.get_content_type(), "application/json")

    def test_manifest_revisions_show_only_newest_and_download_as_json(self):
        data = b'{"format":1,"files":[]}\n'
        record = dict(self.record, BoardModel=panel.METADATA_MODEL, FileName="catalog.json",
                      Sha256=hashlib.sha256(data).hexdigest())
        with patch.object(panel.api, "remote_records", return_value=[record, dict(record, Id="old")]), patch.object(panel.api, "request", return_value=data):
            self.assertEqual(len(self.fs.files(self.fs.records())), 1)
            self.assertEqual(self.fs.open("firmware/catalog.json"), data)

    def test_http_auth_chunked_upload_and_disabled_deletion(self):
        server = panel.HTTPFSServer(("127.0.0.1", 0), self.fs, "adapter-token")
        threading.Thread(target=server.serve_forever, daemon=True).start()
        self.addCleanup(server.server_close)
        self.addCleanup(server.shutdown)
        client = http.client.HTTPConnection("127.0.0.1", server.server_port, timeout=10)
        self.addCleanup(client.close)
        client.request("GET", "/v1/readdir/")
        response = client.getresponse()
        self.assertEqual(response.status, 401)
        response.read()
        headers = {"X-API-KEY": "adapter-token"}
        with patch.object(self.fs, "create", return_value={"verified": True}) as create:
            client.request("POST", "/v1/create/firmware/FAM/test.zip", body=iter([b"chunk1", b"chunk2"]), headers=headers, encode_chunked=True)
            response = client.getresponse()
            self.assertEqual(response.status, 201)
            response.read()
            create.assert_called_once_with("firmware/FAM/test.zip", b"chunk1chunk2")
        client.request("DELETE", "/v1/remove/firmware/FAM/test.zip", headers=headers)
        response = client.getresponse()
        self.assertEqual(response.status, 501)
        response.read()


if __name__ == "__main__":
    unittest.main()
