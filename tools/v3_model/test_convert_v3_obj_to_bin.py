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
    def run_converter(
        self,
        assets_dir: Path,
        manifest: str | Path,
        output_dir: Path,
        *extra_args: str,
    ) -> subprocess.CompletedProcess[str]:
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
                *extra_args,
            ],
            cwd=ROOT_DIR,
            capture_output=True,
            text=True,
            check=False,
        )

    def test_non_deformable_mesh_is_written_indexed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            assets_dir = tmp_dir / "assets"
            mesh_dir = assets_dir / "mesh"
            output_dir = tmp_dir / "out"
            mesh_dir.mkdir(parents=True)
            (mesh_dir / "square.obj").write_text(
                "\n".join(
                    [
                        "v 0 0 0",
                        "v 1 0 0",
                        "v 1 1 0",
                        "v 0 1 0",
                        "vt 0 0",
                        "vt 1 0",
                        "vt 1 1",
                        "vt 0 1",
                        "vn 0 0 1",
                        "f 1/1/1 2/2/1 3/3/1",
                        "f 1/1/1 3/3/1 4/4/1",
                    ]
                ),
                encoding="utf-8",
            )
            (assets_dir / "manifest.json").write_text(
                json.dumps({"parts": [{"partId": "square", "asset": "mesh/square.obj"}]}),
                encoding="utf-8",
            )

            result = self.run_converter(assets_dir, "manifest.json", output_dir)

            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("vertices=4", result.stdout)
            self.assertIn("expandedVertices=6", result.stdout)
            data = (output_dir / "square.v3bin").read_bytes()
            header = struct.unpack("<4s10i", data[:44])
            magic, version, floats_per_vertex, vertex_count, index_count = header[:5]
            self.assertEqual(b"V3MB", magic)
            self.assertEqual(2, version)
            self.assertEqual(18, floats_per_vertex)
            self.assertEqual(4, vertex_count)
            self.assertEqual(6, index_count)
            index_offset = 44 + vertex_count * floats_per_vertex * 4
            self.assertEqual((0, 1, 2, 0, 2, 3), struct.unpack("<6i", data[index_offset:index_offset + 24]))

    def test_object_split_bundle_writes_named_parts(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            assets_dir = tmp_dir / "assets"
            mesh_dir = assets_dir / "mesh"
            output_dir = tmp_dir / "out"
            mesh_dir.mkdir(parents=True)
            (mesh_dir / "two_parts.obj").write_text(
                "\n".join(
                    [
                        "o first",
                        "v 0 0 0",
                        "v 1 0 0",
                        "v 1 1 0",
                        "vt 0 0",
                        "vt 1 0",
                        "vt 1 1",
                        "vn 0 0 1",
                        "f 1/1/1 2/2/1 3/3/1",
                        "o second",
                        "v 0 0 1",
                        "v 1 0 1",
                        "v 1 1 1",
                        "vt 0 0",
                        "vt 1 0",
                        "vt 1 1",
                        "vn 0 0 1",
                        "f 4/4/2 5/5/2 6/6/2",
                    ]
                ),
                encoding="utf-8",
            )
            (assets_dir / "manifest.json").write_text(
                json.dumps({"parts": [{"partId": "two_parts", "asset": "mesh/two_parts.obj"}]}),
                encoding="utf-8",
            )

            result = self.run_converter(
                assets_dir,
                "manifest.json",
                output_dir,
                "--split-objects-bundle",
            )

            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("bundleParts=2", result.stdout)
            data = (output_dir / "two_parts.v3bin").read_bytes()
            header = struct.unpack("<4s11i", data[:48])
            magic, version, floats_per_vertex, part_count, vertex_count, index_count = header[:6]
            self.assertEqual(b"V3PB", magic)
            self.assertEqual(1, version)
            self.assertEqual(18, floats_per_vertex)
            self.assertEqual(2, part_count)
            self.assertEqual(6, vertex_count)
            self.assertEqual(6, index_count)
            offset = 48
            part_header = struct.unpack("<6i", data[offset:offset + 24])
            name_len, part_vertex_count, part_index_count, part_face_count, part_triangle_count, _ = part_header
            offset += 24
            self.assertEqual("first", data[offset:offset + name_len].decode("utf-8"))
            self.assertEqual((3, 3, 1, 1), (part_vertex_count, part_index_count, part_face_count, part_triangle_count))

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
            self.assertEqual(44, vertex_count)
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
