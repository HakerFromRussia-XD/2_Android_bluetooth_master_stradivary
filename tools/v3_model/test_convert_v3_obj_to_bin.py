from __future__ import annotations

import json
import struct
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
TOOL_PATH = ROOT_DIR / "tools" / "v3_model" / "convert_v3_obj_to_bin.py"
APP_ASSETS_DIR = ROOT_DIR / "app" / "src" / "main" / "assets"
FIXTURES_DIR = ROOT_DIR / "tools" / "v3_model" / "test_fixtures"
FIXTURE_MANIFEST = FIXTURES_DIR / "v3_deformable_manifest.json"


class ConvertV3ObjToBinTest(unittest.TestCase):
    def run_converter(self, assets_dir: Path, manifest: str | Path, output_dir: Path) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [
                sys.executable,
                str(TOOL_PATH),
                "--assets-dir",
                str(assets_dir),
                "--manifest",
                str(manifest),
                "--output-dir",
                str(output_dir),
            ],
            cwd=ROOT_DIR,
            capture_output=True,
            text=True,
            check=False,
        )

    def test_current_v3_manifest_converts_without_deformation_sidecars(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            output_dir = Path(tmp)
            result = self.run_converter(
                APP_ASSETS_DIR,
                "STR2_V3/v3_model_parts_manifest.json",
                output_dir,
            )

            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("converted parts=19", result.stdout)
            self.assertEqual(19, len(list(output_dir.glob("*.v3bin"))))
            self.assertEqual([], list(output_dir.glob("*.v3def")))

    def test_deformable_fixture_writes_v3def_sidecar(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            output_dir = Path(tmp)
            result = self.run_converter(FIXTURES_DIR, "v3_deformable_manifest.json", output_dir)

            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("deformation=", result.stdout)
            self.assertTrue((output_dir / "v3_part_99_four_finger_corrugation_rubber.v3bin").is_file())

            deformation_path = output_dir / "v3_part_99_four_finger_corrugation_rubber.v3def"
            self.assertTrue(deformation_path.is_file())
            magic, version, vertex_count, influence_count = struct.unpack(
                "<4s3i",
                deformation_path.read_bytes()[:16],
            )
            self.assertEqual(b"V3DF", magic)
            self.assertEqual(1, version)
            self.assertEqual(60, vertex_count)
            self.assertEqual(5, influence_count)

    def test_deformable_fixture_requires_all_face_groups(self) -> None:
        manifest = json.loads(FIXTURE_MANIFEST.read_text(encoding="utf-8"))
        manifest["parts"][0]["deformation"]["tops"][3]["softFaceGroup"] = "soft_little_missing"
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            manifest_path = tmp_dir / "missing_group_manifest.json"
            manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
            result = self.run_converter(FIXTURES_DIR, manifest_path, tmp_dir / "out")

            self.assertNotEqual(result.returncode, 0)
            self.assertIn("not assigned to a deformation group", result.stderr)

    def test_deformable_fixture_rejects_unknown_transform_id(self) -> None:
        manifest = json.loads(FIXTURE_MANIFEST.read_text(encoding="utf-8"))
        manifest["parts"][0]["deformation"]["tops"][0]["transformId"] = "bad_transform"
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            manifest_path = tmp_dir / "bad_transform_manifest.json"
            manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
            result = self.run_converter(FIXTURES_DIR, manifest_path, tmp_dir / "out")

            self.assertNotEqual(result.returncode, 0)
            self.assertIn("Unsupported deformation transformId `bad_transform`", result.stderr)


if __name__ == "__main__":
    unittest.main()
