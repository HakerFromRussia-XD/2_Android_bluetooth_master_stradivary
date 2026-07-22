from __future__ import annotations

import json
import struct
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from tools.v3_model.convert_v3_obj_to_bin import adapt_volume_rod_centerline
from tools.v3_model.convert_v3_obj_to_bin import align_volume_rod_progress_to_centerline
from tools.v3_model.convert_v3_obj_to_bin import build_harmonic_surface_progress
from tools.v3_model.convert_v3_obj_to_bin import Face
from tools.v3_model.convert_v3_obj_to_bin import flatten_volume_rod_progress_across_creases
from tools.v3_model.convert_v3_obj_to_bin import limit_volume_rod_triangle_progress_jumps
from tools.v3_model.convert_v3_obj_to_bin import limit_volume_rod_surface_progress_gradient
from tools.v3_model.convert_v3_obj_to_bin import match_volume_rod_reference_progress
from tools.v3_model.convert_v3_obj_to_bin import preserve_volume_rod_progress_order
from tools.v3_model.convert_v3_obj_to_bin import regularize_volume_rod_reference_progress
from tools.v3_model.convert_v3_obj_to_bin import resample_volume_rod_centerline
from tools.v3_model.convert_v3_obj_to_bin import smooth_volume_rod_progress_along_crease_chains
from tools.v3_model.convert_v3_obj_to_bin import smooth_volume_rod_surface_progress


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

    def test_object_split_bundle_applies_per_object_mesh_processing(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            assets_dir = tmp_dir / "assets"
            mesh_dir = assets_dir / "mesh"
            output_dir = tmp_dir / "out"
            mesh_dir.mkdir(parents=True)
            (mesh_dir / "processed_bundle.obj").write_text(
                "\n".join(
                    [
                        "o processed",
                        "v 0 0 0",
                        "v 1 0 0",
                        "v 0 1 0",
                        "v 0 0 1",
                        "vn 0 0 1",
                        "vn 0 1 0",
                        "f 1//1 2//1 3//1",
                        "f 1//2 2//2 4//2",
                        "f 1//1 2//1 2//1",
                        "o untouched",
                        "v 0 0 2",
                        "v 1 0 2",
                        "v 0 1 2",
                        "vn 0 0 1",
                        "f 5//3 6//3 7//3",
                    ]
                ),
                encoding="utf-8",
            )
            (assets_dir / "manifest.json").write_text(
                json.dumps(
                    {
                        "bundle": {
                            "source": "mesh/processed_bundle.obj",
                            "meshProcessingByObject": {
                                "processed": {
                                    "recalculateNormals": True,
                                    "creaseAngleDegrees": 45.0,
                                    "skipDegenerateTriangles": True,
                                    "minimumTriangleQuality": 0.0001,
                                    "alignWindingToNormals": True,
                                }
                            },
                        },
                        "parts": [],
                    }
                ),
                encoding="utf-8",
            )

            result = self.run_converter(
                assets_dir,
                "manifest.json",
                output_dir,
                "--split-objects-bundle",
            )

            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("part=processed", result.stdout)
            self.assertIn("skippedTriangles=1", result.stdout)
            self.assertIn("reorientedTriangles=1", result.stdout)
            self.assertIn("creaseEdges=1", result.stdout)
            data = (output_dir / "processed_bundle.v3bin").read_bytes()
            header = struct.unpack("<4s11i", data[:48])
            self.assertEqual((b"V3PB", 2, 9, 9), (header[0], header[3], header[4], header[5]))
            name_len, vertices, indices, faces, triangles, _ = struct.unpack("<6i", data[48:72])
            self.assertEqual("processed", data[72:72 + name_len].decode("utf-8"))
            self.assertEqual((6, 6, 3, 2), (vertices, indices, faces, triangles))

    def test_mesh_processing_splits_crease_and_removes_degenerate_triangle(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            assets_dir = tmp_dir / "assets"
            mesh_dir = assets_dir / "mesh"
            output_dir = tmp_dir / "out"
            mesh_dir.mkdir(parents=True)
            (mesh_dir / "crease.obj").write_text(
                "\n".join(
                    [
                        "v 0 0 0",
                        "v 1 0 0",
                        "v 0 1 0",
                        "v 0 0 1",
                        "vn 0 0 1",
                        "vn 0 1 0",
                        "f 1//1 2//1 3//1",
                        "f 1//2 2//2 4//2",
                        "f 1//1 2//1 2//1",
                    ]
                ),
                encoding="utf-8",
            )
            (assets_dir / "manifest.json").write_text(
                json.dumps(
                    {
                        "parts": [
                            {
                                "partId": "crease",
                                "asset": "mesh/crease.obj",
                                "meshProcessing": {
                                    "recalculateNormals": True,
                                    "creaseAngleDegrees": 45.0,
                                    "skipDegenerateTriangles": True,
                                    "minimumTriangleQuality": 0.0001,
                                    "alignWindingToNormals": True,
                                },
                            }
                        ]
                    }
                ),
                encoding="utf-8",
            )

            result = self.run_converter(assets_dir, "manifest.json", output_dir)

            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("skippedTriangles=1", result.stdout)
            self.assertIn("reorientedTriangles=1", result.stdout)
            self.assertIn("creaseEdges=1", result.stdout)
            data = (output_dir / "crease.v3bin").read_bytes()
            header = struct.unpack("<4s10i", data[:44])
            _, _, floats_per_vertex, vertex_count, index_count = header[:5]
            self.assertEqual(6, vertex_count)
            self.assertEqual(6, index_count)
            self.assertEqual(2, header[7])
            vertex_values = struct.unpack(
                f"<{vertex_count * floats_per_vertex}f",
                data[44:44 + vertex_count * floats_per_vertex * 4],
            )
            origin_normals = set()
            for vertex_index in range(vertex_count):
                offset = vertex_index * floats_per_vertex
                if vertex_values[offset:offset + 3] == (0.0, 0.0, 0.0):
                    origin_normals.add(tuple(round(value, 6) for value in vertex_values[offset + 3:offset + 6]))
            self.assertEqual({(0.0, 0.0, 1.0), (0.0, 1.0, 0.0)}, origin_normals)

    def test_harmonic_surface_progress_normalizes_each_path_by_its_length(self) -> None:
        short_bottom = (0.0, 0.0, 0.0)
        short_middle = (1.0, 0.0, 0.0)
        short_top = (2.0, 0.0, 0.0)
        long_bottom = (0.0, 1.0, 0.0)
        long_middle = (1.0, 1.0, 0.0)
        long_top = (4.0, 1.0, 0.0)
        adjacency = {
            short_bottom: {short_middle: 1.0},
            short_middle: {short_bottom: 1.0, short_top: 1.0},
            short_top: {short_middle: 1.0},
            long_bottom: {long_middle: 1.0},
            long_middle: {long_bottom: 1.0, long_top: 3.0},
            long_top: {long_middle: 3.0},
        }
        bottom = {short_bottom, long_bottom}
        top = {short_top, long_top}
        initial = {
            short_bottom: 0.0,
            short_middle: 0.5,
            short_top: 1.0,
            long_bottom: 0.0,
            long_middle: 0.5,
            long_top: 1.0,
        }

        progress = build_harmonic_surface_progress(adjacency, bottom, top, initial)

        self.assertAlmostEqual(0.5, progress[short_middle], places=6)
        self.assertAlmostEqual(0.25, progress[long_middle], places=6)
        self.assertEqual(0.0, progress[short_bottom])
        self.assertEqual(1.0, progress[long_top])

    def test_harmonic_surface_progress_keeps_reference_values_fixed(self) -> None:
        bottom = (0.0, 0.0, 0.0)
        reference = (1.0, 0.0, 0.0)
        unknown = (2.0, 0.0, 0.0)
        top = (3.0, 0.0, 0.0)
        adjacency = {
            bottom: {reference: 1.0},
            reference: {bottom: 1.0, unknown: 1.0},
            unknown: {reference: 1.0, top: 1.0},
            top: {unknown: 1.0},
        }
        initial = {bottom: 0.0, reference: 0.3, unknown: 0.6, top: 1.0}

        progress = build_harmonic_surface_progress(
            adjacency,
            {bottom},
            {top},
            initial,
            {reference: 0.25},
        )

        self.assertEqual(0.25, progress[reference])
        self.assertAlmostEqual(0.625, progress[unknown], places=6)

    def test_reference_progress_matches_float32_positions_by_tolerance(self) -> None:
        current_key = (1.234567, 2.345678, 3.456789)
        unmatched_key = (5.0, 5.0, 5.0)
        coordinate_by_key = {
            current_key: current_key,
            unmatched_key: unmatched_key,
        }
        reference_progress = {
            (1.234568, 2.345677, 3.456788): 0.42,
        }

        matched = match_volume_rod_reference_progress(
            coordinate_by_key,
            set(coordinate_by_key),
            reference_progress,
        )

        self.assertAlmostEqual(0.42, matched[current_key])
        self.assertNotIn(unmatched_key, matched)

    def test_reference_regularization_reduces_neighbor_weight_jump(self) -> None:
        bottom = (0.0, 0.0, 0.0)
        first = (1.0, 0.0, 0.0)
        second = (2.0, 0.0, 0.0)
        top = (3.0, 0.0, 0.0)
        adjacency = {
            bottom: {first: 1.0},
            first: {bottom: 1.0, second: 1.0},
            second: {first: 1.0, top: 1.0},
            top: {second: 1.0},
        }
        initial = {bottom: 0.0, first: 0.8, second: 0.9, top: 1.0}

        progress = regularize_volume_rod_reference_progress(
            adjacency,
            {bottom},
            {top},
            initial,
            {first: 0.8, second: 0.9},
            0.02,
        )

        initial_maximum_jump = max(
            abs(initial[key] - initial[neighbor])
            for key, neighbors in adjacency.items()
            for neighbor in neighbors
        )
        regularized_maximum_jump = max(
            abs(progress[key] - progress[neighbor])
            for key, neighbors in adjacency.items()
            for neighbor in neighbors
        )
        self.assertLess(regularized_maximum_jump, initial_maximum_jump)
        self.assertEqual(0.0, progress[bottom])
        self.assertEqual(1.0, progress[top])

    def test_triangle_progress_limiter_preserves_anchors_and_caps_jump(self) -> None:
        coordinates = [
            (0.0, 0.0, 0.0),
            (1.0, 0.0, 0.0),
            (2.0, 0.0, 0.0),
            (3.0, 0.0, 0.0),
            (0.5, 1.0, 0.0),
            (1.5, 1.0, 0.0),
            (2.5, 1.0, 0.0),
        ]
        triangle_indexes = (
            (0, 1, 4),
            (1, 5, 4),
            (1, 2, 5),
            (2, 6, 5),
            (2, 3, 6),
        )
        faces = [
            Face(
                refs=tuple((index, -1, -1) for index in triangle),
                labels=frozenset({"soft"}),
                line_number=line_number,
                object_name="soft",
            )
            for line_number, triangle in enumerate(triangle_indexes, 1)
        ]
        keys = [tuple(coordinate) for coordinate in coordinates]
        initial = {
            keys[0]: 0.0,
            keys[1]: 0.8,
            keys[2]: 0.9,
            keys[3]: 1.0,
            keys[4]: 0.4,
            keys[5]: 0.85,
            keys[6]: 0.95,
        }

        progress = limit_volume_rod_triangle_progress_jumps(
            coordinates,
            faces,
            {keys[0]},
            {keys[3]},
            initial,
            0.4,
        )

        self.assertEqual(0.0, progress[keys[0]])
        self.assertEqual(1.0, progress[keys[3]])
        for triangle in triangle_indexes:
            triangle_progress = [progress[keys[index]] for index in triangle]
            self.assertLessEqual(max(triangle_progress) - min(triangle_progress), 0.4000001)

    def test_surface_progress_gradient_limiter_scales_jump_by_edge_length(self) -> None:
        bottom = (0.0, 0.0, 0.0)
        middle = (0.25, 0.0, 0.0)
        upper_middle = (1.0, 0.0, 0.0)
        top = (2.0, 0.0, 0.0)
        adjacency = {
            bottom: {middle: 0.25},
            middle: {bottom: 0.25, upper_middle: 0.75},
            upper_middle: {middle: 0.75, top: 1.0},
            top: {upper_middle: 1.0},
        }

        progress = limit_volume_rod_surface_progress_gradient(
            adjacency,
            {bottom},
            {top},
            {
                bottom: 0.0,
                middle: 0.8,
                upper_middle: 0.9,
                top: 1.0,
            },
            0.5,
        )

        self.assertEqual(0.0, progress[bottom])
        self.assertEqual(1.0, progress[top])
        for first, neighbors in adjacency.items():
            for second, edge_length in neighbors.items():
                self.assertLessEqual(
                    abs(progress[first] - progress[second]),
                    0.5 * edge_length + 0.0000001,
                )

    def test_surface_progress_smoothing_preserves_gradient_and_anchors(self) -> None:
        bottom = (0.0, 0.0, 0.0)
        first = (1.0, 0.0, 0.0)
        second = (2.0, 0.0, 0.0)
        top = (3.0, 0.0, 0.0)
        adjacency = {
            bottom: {first: 1.0},
            first: {bottom: 1.0, second: 1.0},
            second: {first: 1.0, top: 1.0},
            top: {second: 1.0},
        }
        initial = {bottom: 0.0, first: 0.5, second: 0.8, top: 1.0}

        progress = smooth_volume_rod_surface_progress(
            adjacency,
            {bottom},
            {top},
            initial,
            0.5,
            20,
        )

        initial_energy = sum(
            (initial[first_key] - initial[second_key]) ** 2 / edge_length
            for first_key, neighbors in adjacency.items()
            for second_key, edge_length in neighbors.items()
            if first_key < second_key
        )
        smoothed_energy = sum(
            (progress[first_key] - progress[second_key]) ** 2 / edge_length
            for first_key, neighbors in adjacency.items()
            for second_key, edge_length in neighbors.items()
            if first_key < second_key
        )
        self.assertEqual(0.0, progress[bottom])
        self.assertEqual(1.0, progress[top])
        self.assertLess(smoothed_energy, initial_energy)
        for first_key, neighbors in adjacency.items():
            for second_key, edge_length in neighbors.items():
                self.assertLessEqual(
                    abs(progress[first_key] - progress[second_key]),
                    0.5 * edge_length + 0.0000001,
                )

    def test_reference_centerline_is_adapted_to_new_anchors(self) -> None:
        centerline = adapt_volume_rod_centerline(
            (
                (0.0, 0.0, 0.0),
                (1.0, 0.0, 0.0),
                (2.0, 0.0, 0.0),
            ),
            (0.0, 1.0, 0.0),
            (4.0, 3.0, 0.0),
            2,
        )

        self.assertEqual((0.0, 1.0, 0.0), centerline[0])
        self.assertEqual((2.0, 2.0, 0.0), centerline[1])
        self.assertEqual((4.0, 3.0, 0.0), centerline[2])

    def test_reference_centerline_is_resampled_for_new_section_count(self) -> None:
        centerline = adapt_volume_rod_centerline(
            (
                (0.0, 0.0, 0.0),
                (2.0, 0.0, 0.0),
                (4.0, 0.0, 0.0),
            ),
            (0.0, 1.0, 0.0),
            (4.0, 1.0, 0.0),
            4,
        )

        self.assertEqual(5, len(centerline))
        self.assertEqual((0.0, 1.0, 0.0), centerline[0])
        self.assertEqual((4.0, 1.0, 0.0), centerline[-1])

    def test_centerline_is_resampled_to_uniform_segment_lengths(self) -> None:
        centerline = resample_volume_rod_centerline(
            [
                (0.0, 0.0, 0.0),
                (4.0, 0.0, 0.0),
                (4.0, 2.0, 0.0),
            ],
            3,
        )

        self.assertEqual(
            [
                (0.0, 0.0, 0.0),
                (2.0, 0.0, 0.0),
                (4.0, 0.0, 0.0),
                (4.0, 2.0, 0.0),
            ],
            centerline,
        )

    def test_centerline_projection_aligns_interior_progress_and_preserves_anchors(self) -> None:
        bottom = (0.0, 0.0, 0.0)
        interior = (1.0, 2.0, 0.0)
        top = (4.0, 0.0, 0.0)
        coordinates = {
            bottom: bottom,
            interior: interior,
            top: top,
        }

        progress = align_volume_rod_progress_to_centerline(
            coordinates,
            {bottom: 0.2, interior: 0.5, top: 0.8},
            {bottom},
            {top},
            [
                (0.0, 0.0, 0.0),
                (1.0, 0.0, 0.0),
                (2.0, 0.0, 0.0),
                (3.0, 0.0, 0.0),
                (4.0, 0.0, 0.0),
            ],
            0.5,
            2.0,
        )

        self.assertEqual(0.0, progress[bottom])
        self.assertAlmostEqual(0.375, progress[interior])
        self.assertEqual(1.0, progress[top])

    def test_progress_order_preservation_prevents_longitudinal_edge_inversion(self) -> None:
        bottom = (0.0, 0.0, 0.0)
        first = (1.0, 0.0, 0.0)
        second = (2.0, 0.0, 0.0)
        top = (3.0, 0.0, 0.0)
        adjacency = {
            bottom: {first: 1.0},
            first: {bottom: 1.0, second: 1.0},
            second: {first: 1.0, top: 1.0},
            top: {second: 1.0},
        }

        progress = preserve_volume_rod_progress_order(
            adjacency,
            {bottom: 0.0, first: 0.3, second: 0.7, top: 1.0},
            {bottom: 0.0, first: 0.6, second: 0.4, top: 1.0},
            {bottom, top},
            0.25,
            0.01,
        )

        self.assertEqual(0.0, progress[bottom])
        self.assertGreaterEqual(progress[second] - progress[first], 0.1 - 0.0000001)
        self.assertEqual(1.0, progress[top])

    def test_crease_chain_smoothing_removes_zigzag_without_moving_endpoints(self) -> None:
        keys = [(float(index), 0.0, 0.0) for index in range(5)]
        progress = smooth_volume_rod_progress_along_crease_chains(
            dict(zip(keys, (0.0, 1.0, 0.0, 1.0, 0.0))),
            [(tuple(keys), False)],
            set(),
            1,
        )

        self.assertEqual(0.0, progress[keys[0]])
        self.assertEqual(0.5, progress[keys[1]])
        self.assertEqual(0.5, progress[keys[2]])
        self.assertEqual(0.5, progress[keys[3]])
        self.assertEqual(0.0, progress[keys[4]])

    def test_crease_flattening_uses_one_progress_across_a_fold(self) -> None:
        bottom = (0.0, 0.0, 0.0)
        first = (1.0, 0.0, 0.0)
        second = (1.0, 1.0, 0.0)
        top = (2.0, 0.0, 0.0)
        adjacency = {
            bottom: {first: 1.0, second: 1.0},
            first: {bottom: 1.0, second: 1.0, top: 1.0},
            second: {bottom: 1.0, first: 1.0, top: 1.0},
            top: {first: 1.0, second: 1.0},
        }

        progress = flatten_volume_rod_progress_across_creases(
            adjacency,
            {bottom: 0.0, first: 0.2, second: 0.6, top: 1.0},
            [((first, second), False)],
            {bottom, top},
            1.0,
            0.5,
        )

        self.assertEqual(0.0, progress[bottom])
        self.assertAlmostEqual(0.4, progress[first])
        self.assertAlmostEqual(0.4, progress[second])
        self.assertEqual(1.0, progress[top])

    def test_crease_flattening_skips_longitudinal_chain(self) -> None:
        bottom = (0.0, 0.0, 0.0)
        first = (1.0, 0.0, 0.0)
        second = (2.0, 0.0, 0.0)
        top = (3.0, 0.0, 0.0)
        initial = {bottom: 0.0, first: 0.2, second: 0.8, top: 1.0}
        adjacency = {
            bottom: {first: 1.0},
            first: {bottom: 1.0, second: 1.0},
            second: {first: 1.0, top: 1.0},
            top: {second: 1.0},
        }

        progress = flatten_volume_rod_progress_across_creases(
            adjacency,
            initial,
            [((first, second), False)],
            {bottom, top},
            1.0,
            0.3,
        )

        self.assertEqual(initial, progress)

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
            self.assertEqual(2, version)
            self.assertEqual(44, vertex_count)
            self.assertEqual(6, influence_count)

    def test_deformable_shared_soft_boundary_uses_one_blended_weight(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            assets_dir = tmp_dir / "assets"
            mesh_dir = assets_dir / "mesh"
            output_dir = tmp_dir / "out"
            mesh_dir.mkdir(parents=True)
            (mesh_dir / "shared_soft.obj").write_text(
                "\n".join(
                    [
                        "v 0 0 0",
                        "v 1 0 0",
                        "v 2 0 0",
                        "v 0 1 0",
                        "v 1 1 0",
                        "v 2 1 0",
                        "v 0 2 0",
                        "v 1 2 0",
                        "v 2 2 0",
                        "v 3 0 0",
                        "v 4 0 0",
                        "v 3 1 0",
                        "v 4 1 0",
                        "v 3 2 0",
                        "v 4 2 0",
                        "v 5 0 0",
                        "v 6 0 0",
                        "v 5 1 0",
                        "v 6 1 0",
                        "v 5 2 0",
                        "v 6 2 0",
                        "vt 0 0",
                        "vt 1 0",
                        "vt 1 1",
                        "vt 0 1",
                        "vn 0 0 1",
                        "g bottom_fixed_palm",
                        "f 1/1/1 2/2/1 3/3/1",
                        "f 10/1/1 11/2/1 17/3/1",
                        "f 16/1/1 17/2/1 11/3/1",
                        "g soft_index",
                        "f 1/1/1 2/2/1 5/3/1",
                        "f 1/1/1 5/3/1 4/4/1",
                        "f 4/1/1 5/2/1 8/3/1",
                        "f 4/1/1 8/3/1 7/4/1",
                        "g soft_middle",
                        "f 2/1/1 3/2/1 6/3/1",
                        "f 2/1/1 6/3/1 5/4/1",
                        "f 5/1/1 6/2/1 9/3/1",
                        "f 5/1/1 9/3/1 8/4/1",
                        "g soft_ring",
                        "f 10/1/1 11/2/1 13/3/1",
                        "f 10/1/1 13/3/1 12/4/1",
                        "f 12/1/1 13/2/1 15/3/1",
                        "f 12/1/1 15/3/1 14/4/1",
                        "g soft_little",
                        "f 16/1/1 17/2/1 19/3/1",
                        "f 16/1/1 19/3/1 18/4/1",
                        "f 18/1/1 19/2/1 21/3/1",
                        "f 18/1/1 21/3/1 20/4/1",
                        "g top_fixed_index",
                        "f 7/1/1 8/2/1 9/3/1",
                        "g top_fixed_middle",
                        "f 7/1/1 8/2/1 9/3/1",
                        "g top_fixed_ring",
                        "f 14/1/1 15/2/1 21/3/1",
                        "g top_fixed_little",
                        "f 20/1/1 21/2/1 15/3/1",
                    ]
                ),
                encoding="utf-8",
            )
            (assets_dir / "manifest.json").write_text(
                json.dumps(
                    {
                        "parts": [
                            {
                                "partId": "shared_soft",
                                "asset": "mesh/shared_soft.obj",
                                "deformation": {
                                    "type": "multi_top_one_bottom",
                                    "falloff": "linear",
                                    "bottom": {
                                        "faceGroup": "bottom_fixed_palm",
                                        "transformId": "palm_base",
                                    },
                                    "tops": [
                                        {
                                            "finger": "index",
                                            "topFaceGroup": "top_fixed_index",
                                            "softFaceGroup": "soft_index",
                                            "transformId": "index_upper",
                                        },
                                        {
                                            "finger": "middle",
                                            "topFaceGroup": "top_fixed_middle",
                                            "softFaceGroup": "soft_middle",
                                            "transformId": "middle_upper",
                                        },
                                        {
                                            "finger": "ring",
                                            "topFaceGroup": "top_fixed_ring",
                                            "softFaceGroup": "soft_ring",
                                            "transformId": "ring_upper",
                                        },
                                        {
                                            "finger": "little",
                                            "topFaceGroup": "top_fixed_little",
                                            "softFaceGroup": "soft_little",
                                            "transformId": "little_upper",
                                        },
                                    ],
                                },
                            }
                        ]
                    }
                ),
                encoding="utf-8",
            )

            result = self.run_converter(assets_dir, "manifest.json", output_dir)

            self.assertEqual(result.returncode, 0, result.stderr)
            binary_data = (output_dir / "shared_soft.v3bin").read_bytes()
            _, _, floats_per_vertex, vertex_count, _ = struct.unpack("<4s10i", binary_data[:44])[:5]
            vertex_values = struct.unpack(
                f"<{vertex_count * floats_per_vertex}f",
                binary_data[44:44 + vertex_count * floats_per_vertex * 4],
            )
            shared_vertex_indexes = []
            for vertex_index in range(vertex_count):
                offset = vertex_index * floats_per_vertex
                position = vertex_values[offset:offset + 3]
                if all(abs(position[axis] - expected) < 0.000001 for axis, expected in enumerate((1.0, 1.0, 0.0))):
                    shared_vertex_indexes.append(vertex_index)
            self.assertGreater(len(shared_vertex_indexes), 1)

            deformation_data = (output_dir / "shared_soft.v3def").read_bytes()
            _, version, def_vertex_count, influence_count = struct.unpack("<4s3i", deformation_data[:16])
            self.assertEqual(2, version)
            self.assertEqual(vertex_count, def_vertex_count)
            self.assertEqual(6, influence_count)
            weight_end = 16 + def_vertex_count * influence_count * 4
            weights = struct.unpack(
                f"<{def_vertex_count * influence_count}f",
                deformation_data[16:weight_end],
            )
            selection_influences = struct.unpack(
                f"<{def_vertex_count}i",
                deformation_data[weight_end:weight_end + def_vertex_count * 4],
            )
            for vertex_index in shared_vertex_indexes:
                offset = vertex_index * influence_count
                self.assertAlmostEqual(0.5, weights[offset], places=6)
                self.assertAlmostEqual(0.25, weights[offset + 1], places=6)
                self.assertAlmostEqual(0.25, weights[offset + 2], places=6)
                self.assertAlmostEqual(0.0, weights[offset + 3], places=6)
                self.assertAlmostEqual(0.0, weights[offset + 4], places=6)
                self.assertAlmostEqual(0.0, weights[offset + 5], places=6)
            self.assertEqual({1, 2}, {selection_influences[index] for index in shared_vertex_indexes})

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
