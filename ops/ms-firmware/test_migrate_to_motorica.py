import hashlib
import io
import json
import tempfile
import unittest
import zipfile
from email import policy
from email.parser import BytesParser
from pathlib import Path
from unittest.mock import patch

import migrate_to_motorica as migration
from publish_firmware import motorica_crc32


class MigrationTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.work = Path(self.temporary.name)
        image = b"test firmware bytes"
        archive = io.BytesIO()
        with zipfile.ZipFile(archive, "w") as output:
            output.writestr("UserFW.bin", image)
            output.writestr("FW_ini.ini", f"[FW_INFO]\nFWSize={len(image)}\nFWCRC={motorica_crc32(image)}\nFwMajorVersion=1\nFwMinorVersion=2\nFwQuickFix=3\n")
        self.data = archive.getvalue()
        self.digest = hashlib.sha256(self.data).hexdigest()
        self.entry = {"source_path": "FAM/FAM_v1.2.3.zip", "board_model": "FAM",
                      "version": "1.2.3", "sha256": self.digest, "size": len(self.data)}
        self.catalog = {"format": 1, "files": [{"family": "FAM", "name": "FAM_v1.2.3.zip",
                         "path": self.entry["source_path"], "version": "1.2.3",
                         "size": len(self.data), "sha256": self.digest}]}
        self.assembly = {"modules": [{"file": self.entry["source_path"], "sha256": self.digest}]}

    def prepare(self):
        values = [json.dumps(self.catalog).encode(), json.dumps(self.assembly).encode(), self.data]
        with patch.object(migration, "request", side_effect=values):
            return migration.prepare("https://source.test", self.work, {"FAM": "FAM"})

    def test_preparation_backs_up_and_validates_source(self):
        entries, snapshot = self.prepare()
        self.assertEqual(entries, [self.entry])
        self.assertEqual(json.loads((Path(snapshot) / "assembly.json").read_text()), self.assembly)
        self.assertEqual((self.work / "archives" / self.entry["source_path"]).read_bytes(), self.data)

    def test_source_corruption_stops_before_upload(self):
        self.catalog["files"][0]["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "SHA-256 mismatch"):
            self.prepare()

    def test_upload_then_rerun_reuses_existing_record(self):
        entries, snapshot = self.prepare()
        records = []
        posts = []

        def server(url, *, key=None, body=None, content_type=None):
            self.assertEqual(key, "test-key")
            if body is not None:
                posts.append(body)
                message = BytesParser(policy=policy.default).parsebytes(
                    f"Content-Type: {content_type}\r\nMIME-Version: 1.0\r\n\r\n".encode() + body)
                parts = {part.get_param("name", header="content-disposition"): part for part in message.iter_parts()}
                self.assertEqual(parts["File"].get_filename(), "FAM_v1.2.3.zip")
                self.assertEqual(parts["File"].get_payload(decode=True), self.data)
                self.assertEqual(parts["BoardModel"].get_payload(decode=True), b"FAM")
                self.assertEqual(parts["Version"].get_payload(decode=True), b"1.2.3")
                record = {"id": "new-id", "boardModel": "FAM", "version": "1.2.3", "sha256": self.digest}
                records.append(record)
                return json.dumps(record).encode()
            return self.data if url.endswith("/download") else json.dumps(records).encode()

        with patch.object(migration, "request", side_effect=server):
            migration.migrate("https://destination.test/api/firmware", "test-key", self.work, entries, snapshot)
            migration.migrate("https://destination.test/api/firmware", "test-key", self.work, entries, snapshot)
        self.assertEqual(len(posts), 1)
        report = json.loads((self.work / "migration-report.json").read_text())
        self.assertEqual(report["files"][0]["status"], "verified")
        self.assertEqual(report["files"][0]["action"], "reused")
        self.assertNotIn("test-key", (self.work / "migration-report.json").read_text())

    def test_corrupt_destination_copy_is_not_marked_verified(self):
        entries, snapshot = self.prepare()
        record = {"Id": "id", "Sha256": self.digest}
        with patch.object(migration, "request", side_effect=[b"[]", json.dumps(record).encode(), b"corrupt"]):
            with self.assertRaisesRegex(ValueError, "differs from source"):
                migration.migrate("https://destination.test/api/firmware", "key", self.work, entries, snapshot)
        report = json.loads((self.work / "migration-report.json").read_text())
        self.assertEqual(report["files"][0]["id"], "id")
        self.assertNotEqual(report["files"][0]["status"], "verified")

    def test_lost_post_response_is_not_retried(self):
        entries, snapshot = self.prepare()
        with patch.object(migration, "request", side_effect=[b"[]", RuntimeError("lost response")]) as request:
            with self.assertRaisesRegex(RuntimeError, "lost response"):
                migration.migrate("https://destination.test/api/firmware", "key", self.work, entries, snapshot)
        self.assertEqual(request.call_count, 2)


if __name__ == "__main__":
    unittest.main()
