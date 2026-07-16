#!/usr/bin/env python3
"""Convert V3 OBJ model parts into runtime-ready binary buffers."""

from __future__ import annotations

import argparse
import array
from dataclasses import dataclass
import heapq
import json
import math
import struct
import sys
from pathlib import Path


MAGIC = b"V3MB"
VERSION = 2
PARTS_MAGIC = b"V3PB"
PARTS_VERSION = 1
FLOATS_PER_VERTEX = 18
UV_EPSILON = 0.000001
COORDINATE_KEY_DECIMALS = 6
DEFAULT_CREASE_ANGLE_DEGREES = 45.0
DEFAULT_MIN_TRIANGLE_QUALITY = 0.0001
VOLUME_ROD_PROGRESS_GEODESIC_RATIO = "geodesic_ratio"
VOLUME_ROD_PROGRESS_HARMONIC_PATHS = "harmonic_paths"
HARMONIC_PATH_RELAXATION = 1.75
HARMONIC_PATH_TOLERANCE = 0.0000001
HARMONIC_PATH_MAX_ITERATIONS = 10000
VOLUME_ROD_REFERENCE_POSITION_TOLERANCE = 0.0001
VOLUME_ROD_REFERENCE_RELAXATION = 1.2
DEFORMATION_MAGIC = b"V3DF"
DEFORMATION_VERSION = 2
DEFORMATION_VOLUME_ROD_VERSION = 3
DEFORMATION_INFLUENCE_COUNT = 6
INFLUENCE_ORDER = ("palm", "index", "middle", "ring", "little", "thumb")
SUPPORTED_TRANSFORM_IDS = {
    "palm_base",
    "index_upper",
    "middle_upper",
    "ring_upper",
    "little_upper",
    "thumb_upper",
}


@dataclass(frozen=True)
class Face:
    refs: tuple[tuple[int, int, int], ...]
    labels: frozenset[str]
    line_number: int
    object_name: str


@dataclass
class IndexedVertex:
    coordinate: tuple[float, float, float]
    normal: tuple[float, float, float]
    texture: tuple[float, float]
    tangent_sum: list[float]
    bitangent_sum: list[float]


@dataclass(frozen=True)
class ObjGeometry:
    coordinates: list[tuple[float, float, float]]
    textures: list[tuple[float, float]]
    normals: list[tuple[float, float, float]]
    faces: list[Face]
    line_count: int


@dataclass(frozen=True)
class MeshProcessingSpec:
    recalculate_normals: bool
    crease_angle_degrees: float
    skip_degenerate_triangles: bool
    minimum_triangle_quality: float
    align_winding_to_normals: bool


@dataclass(frozen=True)
class TriangleRecord:
    refs: tuple[tuple[int, int, int], tuple[int, int, int], tuple[int, int, int]]
    face: Face
    normal: tuple[float, float, float]


@dataclass(frozen=True)
class ProcessedTriangle:
    refs: tuple[tuple[int, int, int], tuple[int, int, int], tuple[int, int, int]]
    face: Face
    corner_normals: tuple[
        tuple[float, float, float],
        tuple[float, float, float],
        tuple[float, float, float],
    ]
    smoothing_groups: tuple[int, int, int]


@dataclass(frozen=True)
class BinaryPart:
    part_id: str
    vertices: list[float]
    indices: list[int]
    stats: dict[str, int]


@dataclass(frozen=True)
class TopAnchorSpec:
    finger: str
    top_group: str
    soft_group: str
    transform_id: str
    influence_index: int


@dataclass(frozen=True)
class VolumeRodReferenceSpec:
    binary_asset: str
    deformation_asset: str
    reuse_centerline: bool
    reference_tether: float


@dataclass(frozen=True)
class DeformationSpec:
    type: str
    bottom_group: str
    bottom_transform_id: str
    tops: tuple[TopAnchorSpec, ...]
    falloff: str
    rod_sections: int
    rod_progress_mode: str
    rod_uniform_centerline_spacing: bool
    rod_centerline_projection_blend: float
    rod_centerline_projection_window_sections: float
    rod_maximum_triangle_section_span: float | None
    rod_reference: VolumeRodReferenceSpec | None

    @property
    def required_groups(self) -> tuple[str, ...]:
        groups = [self.bottom_group]
        for top in self.tops:
            groups.append(top.top_group)
            groups.append(top.soft_group)
        return tuple(groups)


@dataclass(frozen=True)
class VolumeRodData:
    centerline: tuple[tuple[float, float, float], ...]


@dataclass(frozen=True)
class VolumeRodReferenceData:
    progress_by_key: dict[tuple[float, float, float], float]
    centerline: tuple[tuple[float, float, float], ...] | None
    reference_tether: float


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--assets-dir",
        type=Path,
        default=Path("app/src/main/assets"),
        help="Android assets directory.",
    )
    parser.add_argument(
        "--manifest",
        default="STR2_V3/v3_model_parts_manifest.json",
        help="Manifest path relative to assets-dir.",
    )
    parser.add_argument(
        "--output-dir",
        default="STR2_V3_BIN",
        help="Output directory relative to assets-dir.",
    )
    parser.add_argument(
        "--split-objects-bundle",
        action="store_true",
        help="Write each OBJ as one multi-part bundle split by OBJ object declarations.",
    )
    return parser.parse_args()


def obj_to_bin_name(asset_path: str) -> str:
    source = Path(asset_path)
    return source.with_suffix(".v3bin").name


def obj_to_def_name(asset_path: str) -> str:
    source = Path(asset_path)
    return source.with_suffix(".v3def").name


def display_path(path: Path, base: Path) -> str:
    try:
        return str(path.relative_to(base))
    except ValueError:
        return str(path)


def parse_obj(
    path: Path,
    deformation: dict | None = None,
    object_filter: str | None = None,
    mesh_processing: dict | None = None,
    assets_dir: Path | None = None,
) -> tuple[
    list[float],
    list[int],
    dict[str, int],
    list[float] | None,
    list[int] | None,
    VolumeRodData | None,
]:
    vertices: list[float] = []
    indices: list[int] = []
    deformation_weights: list[float] | None = [] if deformation else None
    deformation_selection_influences: list[int] | None = [] if deformation else None
    geometry = read_obj_geometry(path)
    coordinates = geometry.coordinates
    textures = geometry.textures
    normals = geometry.normals
    faces = geometry.faces
    line_count = geometry.line_count
    if object_filter:
        faces = [face for face in faces if face.object_name == object_filter]
        if not faces:
            raise ValueError(f"{path}: object `{object_filter}` was not found")

    mesh_processing_spec = parse_mesh_processing_spec(mesh_processing)
    deformation_spec = parse_deformation_spec(deformation) if deformation else None
    weight_resolver = None
    selection_resolver = None
    volume_rod_data = None
    if deformation_spec:
        weight_resolver, selection_resolver, volume_rod_data = create_deformation_resolvers(
            path,
            coordinates,
            faces,
            deformation_spec,
            assets_dir,
        )

    processing_stats: dict[str, int] = {}
    if mesh_processing_spec is not None:
        processed_triangles, processing_stats = process_mesh_triangles(
            coordinates,
            normals,
            faces,
            mesh_processing_spec,
        )
        if deformation_spec:
            if weight_resolver is None or selection_resolver is None:
                raise ValueError("Missing deformation resolvers")
            vertices, indices, triangle_count, deformation_weights, deformation_selection_influences = build_processed_deformable_buffers(
                coordinates,
                textures,
                processed_triangles,
                weight_resolver,
                selection_resolver,
            )
        else:
            vertices, indices, triangle_count = build_processed_buffers(
                coordinates,
                textures,
                processed_triangles,
            )
    elif deformation_spec:
        if weight_resolver is None or selection_resolver is None:
            raise ValueError("Missing deformation resolvers")
        vertices, indices, triangle_count, deformation_weights, deformation_selection_influences = build_indexed_deformable_buffers(
            coordinates,
            textures,
            normals,
            faces,
            weight_resolver,
            selection_resolver,
        )
    else:
        vertices, indices, triangle_count = build_indexed_buffers(coordinates, textures, normals, faces)

    stats = build_stats(line_count, faces, triangle_count, coordinates, textures, normals, vertices, indices)
    stats.update(processing_stats)
    return vertices, indices, stats, deformation_weights, deformation_selection_influences, volume_rod_data


def read_obj_geometry(path: Path) -> ObjGeometry:
    coordinates: list[tuple[float, float, float]] = []
    textures: list[tuple[float, float]] = []
    normals: list[tuple[float, float, float]] = []
    faces: list[Face] = []
    line_count = 0
    current_groups: set[str] = set()
    current_object = path.stem
    current_material: str | None = None

    with path.open("r", encoding="utf-8") as source:
        for raw_line in source:
            line_count += 1
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            tokens = line.split()
            if tokens[0] == "v" and len(tokens) >= 4:
                coordinates.append((float(tokens[1]), float(tokens[2]), float(tokens[3])))
            elif tokens[0] == "vt" and len(tokens) >= 3:
                textures.append((float(tokens[1]), float(tokens[2])))
            elif tokens[0] == "vn" and len(tokens) >= 4:
                normals.append((float(tokens[1]), float(tokens[2]), float(tokens[3])))
            elif tokens[0] == "g":
                current_groups = set(tokens[1:])
            elif tokens[0] == "o":
                current_object = "_".join(tokens[1:]) if len(tokens) > 1 else path.stem
            elif tokens[0] == "usemtl":
                current_material = tokens[1] if len(tokens) > 1 else None
            elif tokens[0] == "f" and len(tokens) >= 4:
                labels = set(current_groups)
                if current_object:
                    labels.add(current_object)
                if current_material:
                    labels.add(current_material)
                refs = tuple(
                    parse_vertex_ref(token, len(coordinates), len(textures), len(normals))
                    for token in tokens[1:]
                )
                faces.append(
                    Face(
                        refs=refs,
                        labels=frozenset(labels),
                        line_number=line_count,
                        object_name=current_object,
                    )
                )

    return ObjGeometry(
        coordinates=coordinates,
        textures=textures,
        normals=normals,
        faces=faces,
        line_count=line_count,
    )


def build_stats(
    line_count: int,
    faces: list[Face],
    triangle_count: int,
    coordinates: list[tuple[float, float, float]],
    textures: list[tuple[float, float]],
    normals: list[tuple[float, float, float]],
    vertices: list[float],
    indices: list[int],
) -> dict[str, int]:
    return {
        "line_count": line_count,
        "face_count": len(faces),
        "triangle_count": triangle_count,
        "expanded_vertex_count": triangle_count * 3,
        "coordinate_count": len(coordinates),
        "texture_count": len(textures),
        "normal_count": len(normals),
        "vertex_count": len(vertices) // FLOATS_PER_VERTEX,
        "index_count": len(indices),
    }


def parse_mesh_processing_spec(source: dict | None) -> MeshProcessingSpec | None:
    if source is None:
        return None
    if not isinstance(source, dict):
        raise ValueError("`meshProcessing` must be an object")

    spec = MeshProcessingSpec(
        recalculate_normals=bool(source.get("recalculateNormals", False)),
        crease_angle_degrees=float(
            source.get("creaseAngleDegrees", DEFAULT_CREASE_ANGLE_DEGREES)
        ),
        skip_degenerate_triangles=bool(source.get("skipDegenerateTriangles", False)),
        minimum_triangle_quality=float(
            source.get("minimumTriangleQuality", DEFAULT_MIN_TRIANGLE_QUALITY)
        ),
        align_winding_to_normals=bool(source.get("alignWindingToNormals", False)),
    )
    if not 0.0 < spec.crease_angle_degrees < 180.0:
        raise ValueError("meshProcessing.creaseAngleDegrees must be between 0 and 180")
    if spec.minimum_triangle_quality < 0.0:
        raise ValueError("meshProcessing.minimumTriangleQuality must not be negative")
    if not (
        spec.recalculate_normals
        or spec.skip_degenerate_triangles
        or spec.align_winding_to_normals
    ):
        return None
    return spec


def process_mesh_triangles(
    coordinates: list[tuple[float, float, float]],
    normals: list[tuple[float, float, float]],
    faces: list[Face],
    spec: MeshProcessingSpec,
) -> tuple[list[ProcessedTriangle], dict[str, int]]:
    triangles: list[TriangleRecord] = []
    skipped_triangle_count = 0
    reoriented_triangle_count = 0

    for face in faces:
        first = face.refs[0]
        previous = face.refs[1]
        for current in face.refs[2:]:
            refs = (first, previous, current)
            face_cross, quality = triangle_cross_and_quality(coordinates, refs)
            if spec.skip_degenerate_triangles and quality <= spec.minimum_triangle_quality:
                skipped_triangle_count += 1
                previous = current
                continue

            face_normal = normalize_vector(face_cross, (0.0, 0.0, 1.0))
            source_normal = average_source_normal(normals, refs)
            if (
                spec.align_winding_to_normals
                and source_normal is not None
                and vector_dot(face_normal, source_normal) < 0.0
            ):
                refs = (first, current, previous)
                face_normal = tuple(-value for value in face_normal)
                reoriented_triangle_count += 1
            triangles.append(TriangleRecord(refs=refs, face=face, normal=face_normal))
            previous = current

    if not spec.recalculate_normals:
        processed = [
            ProcessedTriangle(
                refs=triangle.refs,
                face=triangle.face,
                corner_normals=tuple(
                    get_normal(normals, ref[2]) for ref in triangle.refs
                ),
                smoothing_groups=(index * 3, index * 3 + 1, index * 3 + 2),
            )
            for index, triangle in enumerate(triangles)
        ]
        return processed, {
            "skipped_triangle_count": skipped_triangle_count,
            "reoriented_triangle_count": reoriented_triangle_count,
            "crease_edge_count": 0,
        }

    node_count = len(triangles) * 3
    parents = list(range(node_count))

    def find(node: int) -> int:
        while parents[node] != node:
            parents[node] = parents[parents[node]]
            node = parents[node]
        return node

    def union(first_node: int, second_node: int) -> None:
        first_root = find(first_node)
        second_root = find(second_node)
        if first_root != second_root:
            parents[second_root] = first_root

    edge_corners: dict[
        tuple[tuple[float, float, float], tuple[float, float, float]],
        list[tuple[int, dict[tuple[float, float, float], int]]],
    ] = {}
    for triangle_index, triangle in enumerate(triangles):
        position_keys = [
            coordinate_key(coordinates[ref[0]]) for ref in triangle.refs
        ]
        for first_corner, second_corner in ((0, 1), (1, 2), (2, 0)):
            first_key = position_keys[first_corner]
            second_key = position_keys[second_corner]
            if first_key == second_key:
                continue
            edge_key = tuple(sorted((first_key, second_key)))
            edge_corners.setdefault(edge_key, []).append(
                (
                    triangle_index,
                    {
                        first_key: triangle_index * 3 + first_corner,
                        second_key: triangle_index * 3 + second_corner,
                    },
                )
            )

    crease_cosine = math.cos(math.radians(spec.crease_angle_degrees))
    crease_edge_count = 0
    non_manifold_edge_count = 0
    for edge_key, adjacent in edge_corners.items():
        if len(adjacent) != 2:
            if len(adjacent) > 2:
                non_manifold_edge_count += 1
            continue
        first_triangle = triangles[adjacent[0][0]]
        second_triangle = triangles[adjacent[1][0]]
        if vector_dot(first_triangle.normal, second_triangle.normal) < crease_cosine:
            crease_edge_count += 1
            continue
        for position_key in edge_key:
            union(adjacent[0][1][position_key], adjacent[1][1][position_key])

    normal_sums: dict[int, list[float]] = {}
    for triangle_index, triangle in enumerate(triangles):
        for corner_index in range(3):
            root = find(triangle_index * 3 + corner_index)
            corner_weight = triangle_corner_angle(coordinates, triangle.refs, corner_index)
            target = normal_sums.setdefault(root, [0.0, 0.0, 0.0])
            target[0] += triangle.normal[0] * corner_weight
            target[1] += triangle.normal[1] * corner_weight
            target[2] += triangle.normal[2] * corner_weight

    processed: list[ProcessedTriangle] = []
    smoothing_groups: set[int] = set()
    for triangle_index, triangle in enumerate(triangles):
        roots = tuple(find(triangle_index * 3 + corner_index) for corner_index in range(3))
        smoothing_groups.update(roots)
        processed.append(
            ProcessedTriangle(
                refs=triangle.refs,
                face=triangle.face,
                corner_normals=tuple(
                    normalize_vector(normal_sums[root], triangle.normal) for root in roots
                ),
                smoothing_groups=roots,
            )
        )

    return processed, {
        "skipped_triangle_count": skipped_triangle_count,
        "reoriented_triangle_count": reoriented_triangle_count,
        "crease_edge_count": crease_edge_count,
        "non_manifold_edge_count": non_manifold_edge_count,
        "normal_cluster_count": len(smoothing_groups),
    }


def triangle_cross_and_quality(
    coordinates: list[tuple[float, float, float]],
    refs: tuple[tuple[int, int, int], tuple[int, int, int], tuple[int, int, int]],
) -> tuple[tuple[float, float, float], float]:
    first, second, third = (coordinates[ref[0]] for ref in refs)
    first_edge = subtract_vector(second, first)
    second_edge = subtract_vector(third, first)
    face_cross = cross_vector(first_edge, second_edge)
    max_edge_length_squared = max(
        vector_length_squared(subtract_vector(second, first)),
        vector_length_squared(subtract_vector(third, second)),
        vector_length_squared(subtract_vector(first, third)),
    )
    if max_edge_length_squared <= UV_EPSILON * UV_EPSILON:
        return face_cross, 0.0
    return face_cross, math.sqrt(vector_length_squared(face_cross)) / max_edge_length_squared


def average_source_normal(
    normals: list[tuple[float, float, float]],
    refs: tuple[tuple[int, int, int], tuple[int, int, int], tuple[int, int, int]],
) -> tuple[float, float, float] | None:
    available = [normals[ref[2]] for ref in refs if 0 <= ref[2] < len(normals)]
    if not available:
        return None
    total = tuple(sum(normal[axis] for normal in available) for axis in range(3))
    if vector_length_squared(total) <= UV_EPSILON * UV_EPSILON:
        return None
    return normalize_vector(total, (0.0, 0.0, 1.0))


def triangle_corner_angle(
    coordinates: list[tuple[float, float, float]],
    refs: tuple[tuple[int, int, int], tuple[int, int, int], tuple[int, int, int]],
    corner_index: int,
) -> float:
    center = coordinates[refs[corner_index][0]]
    previous = coordinates[refs[(corner_index - 1) % 3][0]]
    following = coordinates[refs[(corner_index + 1) % 3][0]]
    first_direction = normalize_vector(subtract_vector(previous, center), (1.0, 0.0, 0.0))
    second_direction = normalize_vector(subtract_vector(following, center), (0.0, 1.0, 0.0))
    return math.acos(clamp(vector_dot(first_direction, second_direction), -1.0, 1.0))


def subtract_vector(
    first: tuple[float, float, float],
    second: tuple[float, float, float],
) -> tuple[float, float, float]:
    return first[0] - second[0], first[1] - second[1], first[2] - second[2]


def cross_vector(
    first: tuple[float, float, float],
    second: tuple[float, float, float],
) -> tuple[float, float, float]:
    return (
        first[1] * second[2] - first[2] * second[1],
        first[2] * second[0] - first[0] * second[2],
        first[0] * second[1] - first[1] * second[0],
    )


def vector_dot(
    first: tuple[float, float, float],
    second: tuple[float, float, float],
) -> float:
    return first[0] * second[0] + first[1] * second[1] + first[2] * second[2]


def vector_length_squared(vector: tuple[float, float, float]) -> float:
    return vector_dot(vector, vector)


def parse_obj_parts_by_object(
    path: Path,
    exclude_objects: set[str] | None = None,
    mesh_processing_by_object: dict | None = None,
) -> tuple[list[BinaryPart], dict[str, int]]:
    geometry = read_obj_geometry(path)
    exclude_objects = exclude_objects or set()
    if mesh_processing_by_object is None:
        mesh_processing_by_object = {}
    if not isinstance(mesh_processing_by_object, dict):
        raise ValueError("`meshProcessingByObject` must be an object")
    processing_specs = {
        str(object_name): parse_mesh_processing_spec(spec)
        for object_name, spec in mesh_processing_by_object.items()
    }
    faces_by_object: dict[str, list[Face]] = {}
    object_order: list[str] = []
    for face in geometry.faces:
        if face.object_name in exclude_objects:
            continue
        if face.object_name not in faces_by_object:
            faces_by_object[face.object_name] = []
            object_order.append(face.object_name)
        faces_by_object[face.object_name].append(face)

    missing_processing_objects = set(processing_specs).difference(faces_by_object)
    if missing_processing_objects:
        missing = ", ".join(sorted(missing_processing_objects))
        raise ValueError(f"{path}: meshProcessingByObject references missing objects: {missing}")

    parts: list[BinaryPart] = []
    total_vertices = 0
    total_indices = 0
    total_triangles = 0
    total_expanded_vertices = 0
    for object_name in object_order:
        object_faces = faces_by_object[object_name]
        processing_stats: dict[str, int] = {}
        processing_spec = processing_specs.get(object_name)
        if processing_spec is not None:
            processed_triangles, processing_stats = process_mesh_triangles(
                geometry.coordinates,
                geometry.normals,
                object_faces,
                processing_spec,
            )
            vertices, indices, triangle_count = build_processed_buffers(
                geometry.coordinates,
                geometry.textures,
                processed_triangles,
            )
        else:
            vertices, indices, triangle_count = build_indexed_buffers(
                geometry.coordinates,
                geometry.textures,
                geometry.normals,
                object_faces,
            )
        stats = build_stats(
            geometry.line_count,
            object_faces,
            triangle_count,
            geometry.coordinates,
            geometry.textures,
            geometry.normals,
            vertices,
            indices,
        )
        stats.update(processing_stats)
        parts.append(BinaryPart(object_name, vertices, indices, stats))
        total_vertices += stats["vertex_count"]
        total_indices += stats["index_count"]
        total_triangles += stats["triangle_count"]
        total_expanded_vertices += stats["expanded_vertex_count"]

    global_stats = {
        "line_count": geometry.line_count,
        "face_count": len(geometry.faces),
        "triangle_count": total_triangles,
        "expanded_vertex_count": total_expanded_vertices,
        "coordinate_count": len(geometry.coordinates),
        "texture_count": len(geometry.textures),
        "normal_count": len(geometry.normals),
        "vertex_count": total_vertices,
        "index_count": total_indices,
    }
    return parts, global_stats


def build_processed_buffers(
    coordinates: list[tuple[float, float, float]],
    textures: list[tuple[float, float]],
    triangles: list[ProcessedTriangle],
) -> tuple[list[float], list[int], int]:
    vertices, indices, weights, selection_influences = build_processed_buffer_data(
        coordinates,
        textures,
        triangles,
    )
    if weights is not None or selection_influences is not None:
        raise ValueError("Unexpected deformation data for a static mesh")
    return vertices, indices, len(triangles)


def build_processed_deformable_buffers(
    coordinates: list[tuple[float, float, float]],
    textures: list[tuple[float, float]],
    triangles: list[ProcessedTriangle],
    weight_resolver,
    selection_resolver,
) -> tuple[list[float], list[int], int, list[float], list[int]]:
    vertices, indices, weights, selection_influences = build_processed_buffer_data(
        coordinates,
        textures,
        triangles,
        weight_resolver,
        selection_resolver,
    )
    if weights is None or selection_influences is None:
        raise ValueError("Missing processed deformation data")
    return vertices, indices, len(triangles), weights, selection_influences


def build_processed_buffer_data(
    coordinates: list[tuple[float, float, float]],
    textures: list[tuple[float, float]],
    triangles: list[ProcessedTriangle],
    weight_resolver=None,
    selection_resolver=None,
) -> tuple[list[float], list[int], list[float] | None, list[int] | None]:
    records: list[IndexedVertex] = []
    weights_by_vertex: list[tuple[float, ...]] | None = [] if weight_resolver else None
    selection_by_vertex: list[int] | None = [] if selection_resolver else None
    indexes_by_key: dict[tuple, int] = {}
    indices: list[int] = []

    for triangle in triangles:
        refs = triangle.refs
        first, second, third = (coordinates[ref[0]] for ref in refs)
        first_uv, second_uv, third_uv = (get_texture(textures, ref[1]) for ref in refs)
        tangent, bitangent = calculate_tangent_space(
            first,
            second,
            third,
            first_uv,
            second_uv,
            third_uv,
        )
        selection_influence = selection_resolver(triangle.face) if selection_resolver else None
        for corner_index, ref in enumerate(refs):
            weights = weight_resolver(triangle.face, ref[0]) if weight_resolver else None
            key = (
                coordinate_key(coordinates[ref[0]]),
                ref[1],
                triangle.smoothing_groups[corner_index],
                weights,
                selection_influence,
            )
            vertex_index = indexes_by_key.get(key)
            if vertex_index is None:
                vertex_index = len(records)
                indexes_by_key[key] = vertex_index
                records.append(
                    IndexedVertex(
                        coordinate=coordinates[ref[0]],
                        normal=triangle.corner_normals[corner_index],
                        texture=get_texture(textures, ref[1]),
                        tangent_sum=[0.0, 0.0, 0.0],
                        bitangent_sum=[0.0, 0.0, 0.0],
                    )
                )
                if weights_by_vertex is not None:
                    if weights is None:
                        raise ValueError("Missing weights for a processed deformable vertex")
                    weights_by_vertex.append(weights)
                if selection_by_vertex is not None:
                    if selection_influence is None:
                        raise ValueError("Missing selection influence for a processed deformable vertex")
                    selection_by_vertex.append(selection_influence)
            add_vector(records[vertex_index].tangent_sum, tangent)
            add_vector(records[vertex_index].bitangent_sum, bitangent)
            indices.append(vertex_index)

    vertices: list[float] = []
    deformation_weights: list[float] | None = [] if weights_by_vertex is not None else None
    deformation_selection_influences: list[int] | None = [] if selection_by_vertex is not None else None
    for vertex_index, record in enumerate(records):
        tangent = normalize_vector(record.tangent_sum, (1.0, 0.0, 0.0))
        bitangent = normalize_vector(record.bitangent_sum, (0.0, 1.0, 0.0))
        vertices.extend(
            (
                record.coordinate[0],
                record.coordinate[1],
                record.coordinate[2],
                record.normal[0],
                record.normal[1],
                record.normal[2],
                1.0,
                1.0,
                0.0,
                0.0,
                record.texture[0],
                record.texture[1],
                tangent[0],
                tangent[1],
                tangent[2],
                bitangent[0],
                bitangent[1],
                bitangent[2],
            )
        )
        if deformation_weights is not None and weights_by_vertex is not None:
            deformation_weights.extend(weights_by_vertex[vertex_index])
        if deformation_selection_influences is not None and selection_by_vertex is not None:
            deformation_selection_influences.append(selection_by_vertex[vertex_index])

    return vertices, indices, deformation_weights, deformation_selection_influences


def build_indexed_buffers(
    coordinates: list[tuple[float, float, float]],
    textures: list[tuple[float, float]],
    normals: list[tuple[float, float, float]],
    faces: list[Face],
) -> tuple[list[float], list[int], int]:
    records: list[IndexedVertex] = []
    indexes_by_ref: dict[tuple[int, int, int], int] = {}
    indices: list[int] = []
    triangle_count = 0

    for face in faces:
        first = face.refs[0]
        previous = face.refs[1]
        for current in face.refs[2:]:
            triangle_count += 1
            triangle_refs = (first, previous, current)
            v1 = coordinates[first[0]]
            v2 = coordinates[previous[0]]
            v3 = coordinates[current[0]]
            uv1 = get_texture(textures, first[1])
            uv2 = get_texture(textures, previous[1])
            uv3 = get_texture(textures, current[1])
            tangent, bitangent = calculate_tangent_space(v1, v2, v3, uv1, uv2, uv3)
            for ref in triangle_refs:
                vertex_index = indexes_by_ref.get(ref)
                if vertex_index is None:
                    vertex_index = len(records)
                    indexes_by_ref[ref] = vertex_index
                    records.append(
                        IndexedVertex(
                            coordinate=coordinates[ref[0]],
                            normal=get_normal(normals, ref[2]),
                            texture=get_texture(textures, ref[1]),
                            tangent_sum=[0.0, 0.0, 0.0],
                            bitangent_sum=[0.0, 0.0, 0.0],
                        )
                    )
                add_vector(records[vertex_index].tangent_sum, tangent)
                add_vector(records[vertex_index].bitangent_sum, bitangent)
                indices.append(vertex_index)
            previous = current

    vertices: list[float] = []
    for record in records:
        tangent = normalize_vector(record.tangent_sum, (1.0, 0.0, 0.0))
        bitangent = normalize_vector(record.bitangent_sum, (0.0, 1.0, 0.0))
        vertices.extend(
            (
                record.coordinate[0],
                record.coordinate[1],
                record.coordinate[2],
                record.normal[0],
                record.normal[1],
                record.normal[2],
                1.0,
                1.0,
                0.0,
                0.0,
                record.texture[0],
                record.texture[1],
                tangent[0],
                tangent[1],
                tangent[2],
                bitangent[0],
                bitangent[1],
                bitangent[2],
            )
        )
    return vertices, indices, triangle_count


def build_indexed_deformable_buffers(
    coordinates: list[tuple[float, float, float]],
    textures: list[tuple[float, float]],
    normals: list[tuple[float, float, float]],
    faces: list[Face],
    weight_resolver,
    selection_resolver,
) -> tuple[list[float], list[int], int, list[float], list[int]]:
    records: list[IndexedVertex] = []
    deformation_weights_by_vertex: list[tuple[float, ...]] = []
    selection_influence_by_vertex: list[int] = []
    indexes_by_ref_and_weights: dict[
        tuple[tuple[int, int, int], tuple[float, ...], int],
        int,
    ] = {}
    indices: list[int] = []
    triangle_count = 0

    for face in faces:
        first = face.refs[0]
        previous = face.refs[1]
        for current in face.refs[2:]:
            triangle_count += 1
            triangle_refs = (first, previous, current)
            v1 = coordinates[first[0]]
            v2 = coordinates[previous[0]]
            v3 = coordinates[current[0]]
            uv1 = get_texture(textures, first[1])
            uv2 = get_texture(textures, previous[1])
            uv3 = get_texture(textures, current[1])
            tangent, bitangent = calculate_tangent_space(v1, v2, v3, uv1, uv2, uv3)
            for ref in triangle_refs:
                weights = weight_resolver(face, ref[0])
                selection_influence = selection_resolver(face)
                key = (ref, weights, selection_influence)
                vertex_index = indexes_by_ref_and_weights.get(key)
                if vertex_index is None:
                    vertex_index = len(records)
                    indexes_by_ref_and_weights[key] = vertex_index
                    records.append(
                        IndexedVertex(
                            coordinate=coordinates[ref[0]],
                            normal=get_normal(normals, ref[2]),
                            texture=get_texture(textures, ref[1]),
                            tangent_sum=[0.0, 0.0, 0.0],
                            bitangent_sum=[0.0, 0.0, 0.0],
                        )
                    )
                    deformation_weights_by_vertex.append(weights)
                    selection_influence_by_vertex.append(selection_influence)
                add_vector(records[vertex_index].tangent_sum, tangent)
                add_vector(records[vertex_index].bitangent_sum, bitangent)
                indices.append(vertex_index)
            previous = current

    vertices: list[float] = []
    deformation_weights: list[float] = []
    deformation_selection_influences: list[int] = []
    for vertex_index, record in enumerate(records):
        tangent = normalize_vector(record.tangent_sum, (1.0, 0.0, 0.0))
        bitangent = normalize_vector(record.bitangent_sum, (0.0, 1.0, 0.0))
        vertices.extend(
            (
                record.coordinate[0],
                record.coordinate[1],
                record.coordinate[2],
                record.normal[0],
                record.normal[1],
                record.normal[2],
                1.0,
                1.0,
                0.0,
                0.0,
                record.texture[0],
                record.texture[1],
                tangent[0],
                tangent[1],
                tangent[2],
                bitangent[0],
                bitangent[1],
                bitangent[2],
            )
        )
        deformation_weights.extend(deformation_weights_by_vertex[vertex_index])
        deformation_selection_influences.append(selection_influence_by_vertex[vertex_index])
    return vertices, indices, triangle_count, deformation_weights, deformation_selection_influences


def add_vector(target: list[float], source: tuple[float, float, float]) -> None:
    target[0] += source[0]
    target[1] += source[1]
    target[2] += source[2]


def normalize_vector(
    source: list[float] | tuple[float, float, float],
    fallback: tuple[float, float, float],
) -> tuple[float, float, float]:
    length = math.sqrt(source[0] * source[0] + source[1] * source[1] + source[2] * source[2])
    if length <= UV_EPSILON:
        return fallback
    return source[0] / length, source[1] / length, source[2] / length


def parse_vertex_ref(token: str, coordinate_count: int, texture_count: int, normal_count: int) -> tuple[int, int, int]:
    values = token.split("/")
    coordinate_index = parse_obj_index(values[0], coordinate_count)
    texture_index = parse_obj_index(values[1], texture_count) if len(values) > 1 and values[1] else -1
    normal_index = parse_obj_index(values[2], normal_count) if len(values) > 2 and values[2] else -1
    return coordinate_index, texture_index, normal_index


def parse_obj_index(raw_index: str, item_count: int) -> int:
    index = int(raw_index)
    if index > 0:
        return index - 1
    return item_count + index


def append_triangle(
    vertices: list[float],
    indices: list[int],
    coordinates: list[tuple[float, float, float]],
    textures: list[tuple[float, float]],
    normals: list[tuple[float, float, float]],
    ref1: tuple[int, int, int],
    ref2: tuple[int, int, int],
    ref3: tuple[int, int, int],
    face: Face,
    weight_resolver,
    deformation_weights: list[float] | None,
) -> None:
    v1 = coordinates[ref1[0]]
    v2 = coordinates[ref2[0]]
    v3 = coordinates[ref3[0]]
    uv1 = get_texture(textures, ref1[1])
    uv2 = get_texture(textures, ref2[1])
    uv3 = get_texture(textures, ref3[1])
    tangent, bitangent = calculate_tangent_space(v1, v2, v3, uv1, uv2, uv3)
    append_vertex(
        vertices,
        indices,
        deformation_weights,
        v1,
        get_normal(normals, ref1[2]),
        uv1,
        tangent,
        bitangent,
        weight_resolver(face, ref1[0]) if weight_resolver else None,
    )
    append_vertex(
        vertices,
        indices,
        deformation_weights,
        v2,
        get_normal(normals, ref2[2]),
        uv2,
        tangent,
        bitangent,
        weight_resolver(face, ref2[0]) if weight_resolver else None,
    )
    append_vertex(
        vertices,
        indices,
        deformation_weights,
        v3,
        get_normal(normals, ref3[2]),
        uv3,
        tangent,
        bitangent,
        weight_resolver(face, ref3[0]) if weight_resolver else None,
    )


def append_vertex(
    vertices: list[float],
    indices: list[int],
    deformation_weights: list[float] | None,
    coordinate: tuple[float, float, float],
    normal: tuple[float, float, float],
    texture: tuple[float, float],
    tangent: tuple[float, float, float],
    bitangent: tuple[float, float, float],
    weights: tuple[float, ...] | None,
) -> None:
    vertex_index = len(vertices) // FLOATS_PER_VERTEX
    vertices.extend(
        (
            coordinate[0],
            coordinate[1],
            coordinate[2],
            normal[0],
            normal[1],
            normal[2],
            1.0,
            1.0,
            0.0,
            0.0,
            texture[0],
            texture[1],
            tangent[0],
            tangent[1],
            tangent[2],
            bitangent[0],
            bitangent[1],
            bitangent[2],
        )
    )
    indices.append(vertex_index)
    if deformation_weights is not None:
        if weights is None:
            raise ValueError("Missing deformation weights for runtime vertex")
        deformation_weights.extend(weights)


def parse_deformation_spec(deformation: dict | None) -> DeformationSpec:
    if not deformation:
        raise ValueError("Missing deformation block")
    deformation_type = deformation.get("type")
    if deformation_type not in {"multi_top_one_bottom", "volume_invariant_rod"}:
        raise ValueError(f"Unsupported deformation type `{deformation_type}`")
    falloff = deformation.get("falloff", "smoothstep")
    if falloff not in {"smoothstep", "linear", "ease_in", "ease_out", "ease_out_cubic"}:
        raise ValueError(f"Unsupported deformation falloff `{falloff}`")

    bottom = deformation.get("bottom") or {}
    bottom_group = required_string(bottom, "faceGroup", "deformation.bottom")
    bottom_transform_id = required_string(bottom, "transformId", "deformation.bottom")
    validate_transform_id(bottom_transform_id)

    tops = deformation.get("tops")
    if not isinstance(tops, list):
        raise ValueError("deformation.tops must be an array")
    tops_by_finger = {}
    for top in tops:
        if not isinstance(top, dict):
            raise ValueError("Each deformation.tops item must be an object")
        finger = required_string(top, "finger", "deformation.tops")
        if finger not in INFLUENCE_ORDER or finger == "palm":
            raise ValueError(f"Unsupported deformation finger `{finger}`")
        if finger in tops_by_finger:
            raise ValueError(f"Duplicate deformation finger `{finger}`")
        transform_id = required_string(top, "transformId", f"deformation.tops.{finger}")
        validate_transform_id(transform_id)
        tops_by_finger[finger] = TopAnchorSpec(
            finger=finger,
            top_group=required_string(top, "topFaceGroup", f"deformation.tops.{finger}"),
            soft_group=required_string(top, "softFaceGroup", f"deformation.tops.{finger}"),
            transform_id=transform_id,
            influence_index=INFLUENCE_ORDER.index(finger),
        )
    if not tops_by_finger:
        raise ValueError("deformation.tops must define at least one top anchor")
    if deformation_type == "volume_invariant_rod" and len(tops_by_finger) != 1:
        raise ValueError("volume_invariant_rod must define exactly one top anchor")

    rod_sections = int(deformation.get("sections", 12))
    if deformation_type == "volume_invariant_rod" and not 4 <= rod_sections <= 32:
        raise ValueError("volume_invariant_rod sections must be between 4 and 32")
    rod_progress_mode = deformation.get(
        "progressMode",
        VOLUME_ROD_PROGRESS_GEODESIC_RATIO,
    )
    if rod_progress_mode not in {
        VOLUME_ROD_PROGRESS_GEODESIC_RATIO,
        VOLUME_ROD_PROGRESS_HARMONIC_PATHS,
    }:
        raise ValueError(f"Unsupported volume rod progressMode `{rod_progress_mode}`")
    rod_uniform_centerline_spacing = bool(
        deformation.get("uniformCenterlineSpacing", False)
    )
    rod_centerline_projection_blend = float(
        deformation.get("centerlineProjectionBlend", 0.0)
    )
    if not 0.0 <= rod_centerline_projection_blend <= 1.0:
        raise ValueError("deformation.centerlineProjectionBlend must be between 0 and 1")
    rod_centerline_projection_window_sections = float(
        deformation.get("centerlineProjectionWindowSections", 2.0)
    )
    if rod_centerline_projection_window_sections <= 0.0:
        raise ValueError("deformation.centerlineProjectionWindowSections must be positive")
    raw_maximum_triangle_section_span = deformation.get("maximumTriangleSectionSpan")
    rod_maximum_triangle_section_span = (
        float(raw_maximum_triangle_section_span)
        if raw_maximum_triangle_section_span is not None
        else None
    )
    if (
        rod_maximum_triangle_section_span is not None
        and rod_maximum_triangle_section_span <= 0.0
    ):
        raise ValueError("deformation.maximumTriangleSectionSpan must be positive")

    rod_reference = None
    raw_rod_reference = deformation.get("progressReference")
    if raw_rod_reference is not None:
        if deformation_type != "volume_invariant_rod":
            raise ValueError("deformation.progressReference requires volume_invariant_rod")
        if rod_progress_mode != VOLUME_ROD_PROGRESS_HARMONIC_PATHS:
            raise ValueError("deformation.progressReference requires harmonic_paths")
        if not isinstance(raw_rod_reference, dict):
            raise ValueError("deformation.progressReference must be an object")
        rod_reference = VolumeRodReferenceSpec(
            binary_asset=required_string(
                raw_rod_reference,
                "binaryAsset",
                "deformation.progressReference",
            ),
            deformation_asset=required_string(
                raw_rod_reference,
                "deformationAsset",
                "deformation.progressReference",
            ),
            reuse_centerline=bool(raw_rod_reference.get("reuseCenterline", False)),
            reference_tether=float(raw_rod_reference.get("referenceTether", 1.0)),
        )
        if rod_reference.reference_tether <= 0.0:
            raise ValueError("deformation.progressReference.referenceTether must be positive")

    return DeformationSpec(
        type=deformation_type,
        bottom_group=bottom_group,
        bottom_transform_id=bottom_transform_id,
        tops=tuple(tops_by_finger[finger] for finger in INFLUENCE_ORDER if finger in tops_by_finger),
        falloff=falloff,
        rod_sections=rod_sections,
        rod_progress_mode=rod_progress_mode,
        rod_uniform_centerline_spacing=rod_uniform_centerline_spacing,
        rod_centerline_projection_blend=rod_centerline_projection_blend,
        rod_centerline_projection_window_sections=rod_centerline_projection_window_sections,
        rod_maximum_triangle_section_span=rod_maximum_triangle_section_span,
        rod_reference=rod_reference,
    )


def required_string(source: dict, key: str, owner: str) -> str:
    value = source.get(key)
    if not isinstance(value, str) or not value:
        raise ValueError(f"Missing {owner}.{key}")
    return value


def validate_transform_id(transform_id: str) -> None:
    if transform_id not in SUPPORTED_TRANSFORM_IDS:
        raise ValueError(f"Unsupported deformation transformId `{transform_id}`")


def create_deformation_resolvers(
    path: Path,
    coordinates: list[tuple[float, float, float]],
    faces: list[Face],
    spec: DeformationSpec,
    assets_dir: Path | None,
):
    required_groups = set(spec.required_groups)
    group_coordinates: dict[str, set[int]] = {group: set() for group in required_groups}
    coordinate_by_key = {coordinate_key(coordinate): coordinate for coordinate in coordinates}
    resolved_groups: dict[Face, str] = {}

    for face in faces:
        group = resolve_deformation_group(path, face, required_groups)
        resolved_groups[face] = group
        for ref in face.refs:
            group_coordinates[group].add(ref[0])

    missing_groups = [group for group, indexes in group_coordinates.items() if not indexes]
    if missing_groups:
        raise ValueError(
            f"{path}: deformable OBJ is missing faces for groups: {', '.join(sorted(missing_groups))}"
        )

    group_coordinate_keys = {
        group: {coordinate_key(coordinates[index]) for index in indexes}
        for group, indexes in group_coordinates.items()
    }
    soft_axes = {}
    for top in spec.tops:
        bottom_locked_keys = group_coordinate_keys[top.soft_group].intersection(
            group_coordinate_keys[spec.bottom_group]
        )
        top_locked_keys = group_coordinate_keys[top.soft_group].intersection(
            group_coordinate_keys[top.top_group]
        )
        if not bottom_locked_keys:
            raise ValueError(f"{path}: deformation soft group `{top.soft_group}` has no shared bottom edge")
        if not top_locked_keys:
            raise ValueError(f"{path}: deformation soft group `{top.soft_group}` has no shared top edge")
        bottom_center = center_for_coordinate_keys(coordinate_by_key, bottom_locked_keys)
        top_center = center_for_coordinate_keys(coordinate_by_key, top_locked_keys)
        axis = (
            top_center[0] - bottom_center[0],
            top_center[1] - bottom_center[1],
            top_center[2] - bottom_center[2],
        )
        axis_length_sq = dot(axis, axis)
        if axis_length_sq < UV_EPSILON:
            raise ValueError(f"{path}: deformation axis for `{top.finger}` is too short")
        soft_axes[top.soft_group] = (
            top,
            bottom_center,
            axis,
            axis_length_sq,
            bottom_locked_keys,
            top_locked_keys,
        )

    top_by_group = {top.top_group: top for top in spec.tops}
    soft_groups_by_coordinate_key: dict[tuple[float, float, float], list[str]] = {}
    anchored_soft_keys: set[tuple[float, float, float]] = set()
    for soft_group, (_, _, _, _, bottom_locked_keys, top_locked_keys) in soft_axes.items():
        anchored_soft_keys.update(bottom_locked_keys)
        anchored_soft_keys.update(top_locked_keys)
        for coordinate_index in group_coordinates[soft_group]:
            soft_groups_by_coordinate_key.setdefault(coordinate_key(coordinates[coordinate_index]), []).append(soft_group)

    volume_rod_progress_by_key: dict[tuple[float, float, float], float] = {}
    volume_rod_data = None
    if spec.type == "volume_invariant_rod":
        rod_top = spec.tops[0]
        _, _, _, _, bottom_locked_keys, top_locked_keys = soft_axes[rod_top.soft_group]
        rod_faces = [face for face in faces if resolved_groups[face] == rod_top.soft_group]
        reference_data = load_volume_rod_reference(
            path,
            assets_dir,
            spec.rod_reference,
            rod_top.influence_index,
        )
        volume_rod_progress_by_key, volume_rod_data = build_volume_rod_data(
            path,
            coordinates,
            coordinate_by_key,
            rod_faces,
            bottom_locked_keys,
            top_locked_keys,
            spec.rod_sections,
            spec.rod_progress_mode,
            spec.rod_uniform_centerline_spacing,
            centerline_projection_blend=spec.rod_centerline_projection_blend,
            centerline_projection_window_sections=spec.rod_centerline_projection_window_sections,
            maximum_triangle_section_span=spec.rod_maximum_triangle_section_span,
            reference_data=reference_data,
        )

    def soft_weights_for_key(group: str, key: tuple[float, float, float]) -> tuple[float, ...]:
        top, bottom_center, axis, axis_length_sq, _, _ = soft_axes[group]
        if spec.type == "volume_invariant_rod":
            t = volume_rod_progress_by_key[key]
        else:
            coordinate = coordinate_by_key[key]
            projection = dot(
                (
                    coordinate[0] - bottom_center[0],
                    coordinate[1] - bottom_center[1],
                    coordinate[2] - bottom_center[2],
                ),
                axis,
            )
            raw_t = projection / axis_length_sq
            t = clamp(raw_t, 0.0, 1.0)
            if spec.falloff == "smoothstep":
                t = t * t * (3.0 - 2.0 * t)
            elif spec.falloff == "ease_in":
                t = t * t
            elif spec.falloff == "ease_out":
                t = 1.0 - ((1.0 - t) * (1.0 - t))
            elif spec.falloff == "ease_out_cubic":
                t = 1.0 - ((1.0 - t) * (1.0 - t) * (1.0 - t))
        weights = [0.0] * DEFORMATION_INFLUENCE_COUNT
        weights[0] = 1.0 - t
        weights[top.influence_index] = t
        return tuple(weights)  # type: ignore[return-value]

    shared_soft_weights: dict[tuple[float, float, float], tuple[float, ...]] = {}
    for key, soft_groups in soft_groups_by_coordinate_key.items():
        if len(soft_groups) < 2 or key in anchored_soft_keys:
            continue
        weights_by_group = [soft_weights_for_key(soft_group, key) for soft_group in soft_groups]
        shared_soft_weights[key] = tuple(
            sum(weights[influence] for weights in weights_by_group) / len(weights_by_group)
            for influence in range(DEFORMATION_INFLUENCE_COUNT)
        )  # type: ignore[assignment]

    palm_weights = tuple(1.0 if influence == 0 else 0.0 for influence in range(DEFORMATION_INFLUENCE_COUNT))

    def weights_for(face: Face, coordinate_index: int) -> tuple[float, ...]:
        group = resolved_groups[face]
        if group == spec.bottom_group:
            return palm_weights
        top = top_by_group.get(group)
        if top:
            weights = [0.0] * DEFORMATION_INFLUENCE_COUNT
            weights[top.influence_index] = 1.0
            return tuple(weights)  # type: ignore[return-value]
        top, bottom_center, axis, axis_length_sq, bottom_locked_keys, top_locked_keys = soft_axes[group]
        key = coordinate_key(coordinates[coordinate_index])
        if key in bottom_locked_keys:
            return palm_weights
        if key in top_locked_keys:
            weights = [0.0] * DEFORMATION_INFLUENCE_COUNT
            weights[top.influence_index] = 1.0
            return tuple(weights)  # type: ignore[return-value]
        shared_weights = shared_soft_weights.get(key)
        if shared_weights is not None:
            return shared_weights
        return soft_weights_for_key(group, key)

    selection_influence_by_group: dict[str, int] = {spec.bottom_group: 0}
    for top in spec.tops:
        selection_influence_by_group[top.top_group] = top.influence_index
        selection_influence_by_group[top.soft_group] = top.influence_index

    def selection_for(face: Face) -> int:
        return selection_influence_by_group[resolved_groups[face]]

    return weights_for, selection_for, volume_rod_data


def load_volume_rod_reference(
    source_path: Path,
    assets_dir: Path | None,
    spec: VolumeRodReferenceSpec | None,
    influence_index: int,
) -> VolumeRodReferenceData | None:
    if spec is None:
        return None
    if assets_dir is None:
        raise ValueError(f"{source_path}: progressReference requires an assets directory")

    binary_path = resolve_asset_path(assets_dir, spec.binary_asset)
    deformation_path = resolve_asset_path(assets_dir, spec.deformation_asset)
    binary_data = binary_path.read_bytes()
    deformation_data = deformation_path.read_bytes()

    binary_header_size = struct.calcsize("<4s10i")
    if len(binary_data) < binary_header_size:
        raise ValueError(f"{binary_path}: truncated V3 model header")
    binary_header = struct.unpack_from("<4s10i", binary_data)
    magic, _, floats_per_vertex, vertex_count = binary_header[:4]
    if magic != MAGIC:
        raise ValueError(f"{binary_path}: expected {MAGIC!r}, got {magic!r}")
    if floats_per_vertex != FLOATS_PER_VERTEX:
        raise ValueError(
            f"{binary_path}: expected {FLOATS_PER_VERTEX} floats per vertex, "
            f"got {floats_per_vertex}"
        )
    vertex_data_end = binary_header_size + vertex_count * floats_per_vertex * 4
    if len(binary_data) < vertex_data_end:
        raise ValueError(f"{binary_path}: truncated V3 vertex data")

    deformation_header_size = struct.calcsize("<4s3i")
    if len(deformation_data) < deformation_header_size:
        raise ValueError(f"{deformation_path}: truncated deformation header")
    deformation_magic, deformation_version, deformation_vertex_count, influence_count = (
        struct.unpack_from("<4s3i", deformation_data)
    )
    if deformation_magic != DEFORMATION_MAGIC:
        raise ValueError(
            f"{deformation_path}: expected {DEFORMATION_MAGIC!r}, got {deformation_magic!r}"
        )
    if deformation_vertex_count != vertex_count:
        raise ValueError(
            f"{deformation_path}: vertex count {deformation_vertex_count} does not match "
            f"{binary_path} vertex count {vertex_count}"
        )
    if not 0 <= influence_index < influence_count:
        raise ValueError(
            f"{deformation_path}: influence index {influence_index} is outside "
            f"the {influence_count} stored influences"
        )

    weight_data_end = deformation_header_size + vertex_count * influence_count * 4
    selection_data_end = weight_data_end + vertex_count * 4
    if len(deformation_data) < selection_data_end:
        raise ValueError(f"{deformation_path}: truncated deformation weights")

    progress_samples_by_key: dict[tuple[float, float, float], list[float]] = {}
    for vertex_index in range(vertex_count):
        vertex_offset = binary_header_size + vertex_index * floats_per_vertex * 4
        key = coordinate_key(struct.unpack_from("<3f", binary_data, vertex_offset))
        weight_offset = deformation_header_size + (
            vertex_index * influence_count + influence_index
        ) * 4
        progress = struct.unpack_from("<f", deformation_data, weight_offset)[0]
        progress_samples_by_key.setdefault(key, []).append(progress)

    progress_by_key = {}
    for key, samples in progress_samples_by_key.items():
        if max(samples) - min(samples) > HARMONIC_PATH_TOLERANCE:
            raise ValueError(
                f"{deformation_path}: position {key} has inconsistent reference progress"
            )
        progress_by_key[key] = clamp(sum(samples) / len(samples), 0.0, 1.0)

    centerline = None
    if spec.reuse_centerline:
        if deformation_version < DEFORMATION_VOLUME_ROD_VERSION:
            raise ValueError(f"{deformation_path}: reference has no volume rod centerline")
        if len(deformation_data) < selection_data_end + 4:
            raise ValueError(f"{deformation_path}: missing volume rod centerline")
        centerline_count = struct.unpack_from("<i", deformation_data, selection_data_end)[0]
        centerline_offset = selection_data_end + 4
        centerline_end = centerline_offset + centerline_count * 3 * 4
        if centerline_count < 2 or len(deformation_data) < centerline_end:
            raise ValueError(f"{deformation_path}: truncated volume rod centerline")
        centerline_values = struct.unpack_from(
            f"<{centerline_count * 3}f",
            deformation_data,
            centerline_offset,
        )
        centerline = tuple(
            (
                centerline_values[index * 3],
                centerline_values[index * 3 + 1],
                centerline_values[index * 3 + 2],
            )
            for index in range(centerline_count)
        )

    return VolumeRodReferenceData(
        progress_by_key=progress_by_key,
        centerline=centerline,
        reference_tether=spec.reference_tether,
    )


def resolve_asset_path(assets_dir: Path, asset: str) -> Path:
    path = Path(asset)
    return path if path.is_absolute() else assets_dir / path


def build_volume_rod_data(
    path: Path,
    coordinates: list[tuple[float, float, float]],
    coordinate_by_key: dict[tuple[float, float, float], tuple[float, float, float]],
    soft_faces: list[Face],
    bottom_locked_keys: set[tuple[float, float, float]],
    top_locked_keys: set[tuple[float, float, float]],
    section_count: int,
    progress_mode: str,
    uniform_centerline_spacing: bool = False,
    centerline_projection_blend: float = 0.0,
    centerline_projection_window_sections: float = 2.0,
    maximum_triangle_section_span: float | None = None,
    reference_data: VolumeRodReferenceData | None = None,
) -> tuple[dict[tuple[float, float, float], float], VolumeRodData]:
    adjacency: dict[
        tuple[float, float, float],
        dict[tuple[float, float, float], float],
    ] = {}
    for face in soft_faces:
        keys = [coordinate_key(coordinates[ref[0]]) for ref in face.refs]
        for key in keys:
            adjacency.setdefault(key, {})
        for index, key in enumerate(keys):
            neighbor = keys[(index + 1) % len(keys)]
            if key == neighbor:
                continue
            distance = vector_distance(coordinate_by_key[key], coordinate_by_key[neighbor])
            previous = adjacency[key].get(neighbor)
            if previous is None or distance < previous:
                adjacency[key][neighbor] = distance
                adjacency[neighbor][key] = distance

    bottom_sources = bottom_locked_keys.intersection(adjacency)
    top_sources = top_locked_keys.intersection(adjacency)
    if not bottom_sources or not top_sources:
        raise ValueError(f"{path}: volume rod boundary is disconnected from its soft mesh")

    distance_from_bottom = shortest_surface_distances(adjacency, bottom_sources)
    distance_from_top = shortest_surface_distances(adjacency, top_sources)
    missing_keys = set(adjacency).difference(distance_from_bottom).union(
        set(adjacency).difference(distance_from_top)
    )
    if missing_keys:
        raise ValueError(f"{path}: volume rod soft mesh contains disconnected vertices")

    progress_by_key: dict[tuple[float, float, float], float] = {}
    for key in adjacency:
        if key in bottom_locked_keys:
            progress_by_key[key] = 0.0
            continue
        if key in top_locked_keys:
            progress_by_key[key] = 1.0
            continue
        bottom_distance = distance_from_bottom[key]
        top_distance = distance_from_top[key]
        total_distance = bottom_distance + top_distance
        if total_distance <= UV_EPSILON:
            raise ValueError(f"{path}: volume rod has an invalid zero-length surface path")
        progress_by_key[key] = clamp(bottom_distance / total_distance, 0.0, 1.0)

    if reference_data is not None:
        reference_progress = match_volume_rod_reference_progress(
            coordinate_by_key,
            set(adjacency),
            reference_data.progress_by_key,
        )
        reference_match_ratio = len(reference_progress) / len(adjacency)
        if reference_match_ratio < 0.5:
            raise ValueError(
                f"{path}: volume rod reference matches only "
                f"{reference_match_ratio:.1%} of soft mesh positions"
            )
        progress_by_key = build_harmonic_surface_progress(
            adjacency,
            bottom_sources,
            top_sources,
            progress_by_key,
            reference_progress,
        )
        progress_by_key = regularize_volume_rod_reference_progress(
            adjacency,
            bottom_sources,
            top_sources,
            progress_by_key,
            reference_progress,
            reference_data.reference_tether,
        )
    elif progress_mode == VOLUME_ROD_PROGRESS_HARMONIC_PATHS:
        progress_by_key = build_harmonic_surface_progress(
            adjacency,
            bottom_sources,
            top_sources,
            progress_by_key,
        )

    if maximum_triangle_section_span is not None:
        progress_by_key = limit_volume_rod_triangle_progress_jumps(
            coordinates,
            soft_faces,
            bottom_sources,
            top_sources,
            progress_by_key,
            maximum_triangle_section_span / section_count,
        )

    bottom_center = center_for_coordinate_keys(coordinate_by_key, bottom_locked_keys)
    top_center = center_for_coordinate_keys(coordinate_by_key, top_locked_keys)
    if reference_data is not None and reference_data.centerline is not None:
        centerline = adapt_volume_rod_centerline(
            reference_data.centerline,
            bottom_center,
            top_center,
            section_count,
        )
    else:
        centerline = build_volume_rod_centerline(
            coordinate_by_key,
            progress_by_key,
            bottom_center,
            top_center,
            section_count,
        )
    if uniform_centerline_spacing:
        centerline = resample_volume_rod_centerline(centerline, section_count)
    if centerline_projection_blend > 0.0:
        progress_by_key = align_volume_rod_progress_to_centerline(
            coordinate_by_key,
            progress_by_key,
            bottom_sources,
            top_sources,
            centerline,
            centerline_projection_blend,
            centerline_projection_window_sections,
        )
        if maximum_triangle_section_span is not None:
            progress_by_key = limit_volume_rod_triangle_progress_jumps(
                coordinates,
                soft_faces,
                bottom_sources,
                top_sources,
                progress_by_key,
                maximum_triangle_section_span / section_count,
            )
    return progress_by_key, VolumeRodData(centerline=tuple(centerline))


def align_volume_rod_progress_to_centerline(
    coordinate_by_key: dict[tuple[float, float, float], tuple[float, float, float]],
    initial_progress: dict[tuple[float, float, float], float],
    bottom_sources: set[tuple[float, float, float]],
    top_sources: set[tuple[float, float, float]],
    centerline: list[tuple[float, float, float]],
    blend: float,
    window_sections: float,
) -> dict[tuple[float, float, float], float]:
    if len(centerline) < 2:
        raise ValueError("Volume rod centerline must contain at least two points")

    segment_lengths = [
        vector_distance(centerline[index], centerline[index + 1])
        for index in range(len(centerline) - 1)
    ]
    cumulative_lengths = [0.0]
    for segment_length in segment_lengths:
        cumulative_lengths.append(cumulative_lengths[-1] + segment_length)
    total_length = cumulative_lengths[-1]
    if total_length <= UV_EPSILON:
        raise ValueError("Volume rod centerline is too short for progress alignment")

    segment_count = len(segment_lengths)
    progress_window = window_sections / segment_count
    aligned = {}
    for key, progress in initial_progress.items():
        if key in bottom_sources:
            aligned[key] = 0.0
            continue
        if key in top_sources:
            aligned[key] = 1.0
            continue

        coordinate = coordinate_by_key[key]
        best_distance_sq = math.inf
        projected_progress = progress
        for segment_index, segment_length in enumerate(segment_lengths):
            segment_midpoint_progress = (segment_index + 0.5) / segment_count
            if abs(segment_midpoint_progress - progress) > progress_window:
                continue
            start = centerline[segment_index]
            end = centerline[segment_index + 1]
            segment = tuple(end[axis] - start[axis] for axis in range(3))
            segment_length_sq = dot(segment, segment)
            if segment_length_sq <= UV_EPSILON:
                continue
            amount = clamp(
                dot(
                    tuple(coordinate[axis] - start[axis] for axis in range(3)),
                    segment,
                ) / segment_length_sq,
                0.0,
                1.0,
            )
            projected = tuple(
                start[axis] + segment[axis] * amount
                for axis in range(3)
            )
            distance_sq = sum(
                (coordinate[axis] - projected[axis]) ** 2
                for axis in range(3)
            )
            if distance_sq >= best_distance_sq:
                continue
            best_distance_sq = distance_sq
            projected_progress = (
                cumulative_lengths[segment_index] + segment_length * amount
            ) / total_length

        aligned[key] = clamp(
            progress + (projected_progress - progress) * blend,
            0.0,
            1.0,
        )
    return aligned


def match_volume_rod_reference_progress(
    coordinate_by_key: dict[tuple[float, float, float], tuple[float, float, float]],
    current_keys: set[tuple[float, float, float]],
    reference_progress_by_key: dict[tuple[float, float, float], float],
) -> dict[tuple[float, float, float], float]:
    tolerance = VOLUME_ROD_REFERENCE_POSITION_TOLERANCE

    def cell_for(coordinate: tuple[float, float, float]) -> tuple[int, int, int]:
        return (
            math.floor(coordinate[0] / tolerance),
            math.floor(coordinate[1] / tolerance),
            math.floor(coordinate[2] / tolerance),
        )

    reference_keys_by_cell: dict[
        tuple[int, int, int],
        list[tuple[float, float, float]],
    ] = {}
    for reference_key in reference_progress_by_key:
        reference_keys_by_cell.setdefault(cell_for(reference_key), []).append(reference_key)

    matched_progress = {}
    for current_key in current_keys:
        coordinate = coordinate_by_key[current_key]
        cell = cell_for(coordinate)
        closest_reference = None
        closest_distance = math.inf
        for x_offset in (-1, 0, 1):
            for y_offset in (-1, 0, 1):
                for z_offset in (-1, 0, 1):
                    neighbor_cell = (
                        cell[0] + x_offset,
                        cell[1] + y_offset,
                        cell[2] + z_offset,
                    )
                    for reference_key in reference_keys_by_cell.get(neighbor_cell, ()):
                        distance = vector_distance(coordinate, reference_key)
                        if distance <= tolerance and distance < closest_distance:
                            closest_reference = reference_key
                            closest_distance = distance
        if closest_reference is not None:
            matched_progress[current_key] = reference_progress_by_key[closest_reference]
    return matched_progress


def limit_volume_rod_triangle_progress_jumps(
    coordinates: list[tuple[float, float, float]],
    soft_faces: list[Face],
    bottom_sources: set[tuple[float, float, float]],
    top_sources: set[tuple[float, float, float]],
    initial_progress: dict[tuple[float, float, float], float],
    maximum_jump: float,
) -> dict[tuple[float, float, float], float]:
    triangle_edges: set[
        tuple[tuple[float, float, float], tuple[float, float, float]]
    ] = set()
    for face in soft_faces:
        keys = [coordinate_key(coordinates[ref[0]]) for ref in face.refs]
        for index in range(1, len(keys) - 1):
            triangle = (keys[0], keys[index], keys[index + 1])
            for first_index, second_index in ((0, 1), (1, 2), (2, 0)):
                triangle_edges.add(tuple(sorted((triangle[first_index], triangle[second_index]))))

    constraint_adjacency: dict[
        tuple[float, float, float],
        set[tuple[float, float, float]],
    ] = {}
    for first, second in triangle_edges:
        constraint_adjacency.setdefault(first, set()).add(second)
        constraint_adjacency.setdefault(second, set()).add(first)

    edge_distance = {key: 0 for key in bottom_sources}
    pending = list(bottom_sources)
    pending_index = 0
    shortest_boundary_path = None
    while pending_index < len(pending):
        key = pending[pending_index]
        pending_index += 1
        distance = edge_distance[key]
        if key in top_sources:
            shortest_boundary_path = distance
            break
        for neighbor in constraint_adjacency.get(key, ()):
            if neighbor in edge_distance:
                continue
            edge_distance[neighbor] = distance + 1
            pending.append(neighbor)
    if shortest_boundary_path is None:
        raise ValueError("Volume rod triangle constraints do not connect both anchors")
    if maximum_jump * shortest_boundary_path < 1.0 - HARMONIC_PATH_TOLERANCE:
        raise ValueError(
            "Volume rod maximumTriangleSectionSpan is too small for the shortest anchor path"
        )

    progress = dict(initial_progress)
    fixed_sources = bottom_sources.union(top_sources)
    sorted_edges = sorted(triangle_edges)
    for _ in range(HARMONIC_PATH_MAX_ITERATIONS):
        maximum_excess = 0.0
        for first, second in sorted_edges:
            difference = progress[first] - progress[second]
            excess = abs(difference) - maximum_jump
            if excess <= 0.0:
                continue
            maximum_excess = max(maximum_excess, excess)
            direction = 1.0 if difference > 0.0 else -1.0
            first_fixed = first in fixed_sources
            second_fixed = second in fixed_sources
            if first_fixed and second_fixed:
                raise ValueError("Volume rod fixed triangle edge exceeds maximum progress jump")
            if first_fixed:
                progress[second] += direction * excess
            elif second_fixed:
                progress[first] -= direction * excess
            else:
                correction = direction * excess * 0.5
                progress[first] -= correction
                progress[second] += correction
        if maximum_excess < HARMONIC_PATH_TOLERANCE:
            break
    else:
        raise ValueError("Volume rod triangle progress limiter did not converge")

    for key in bottom_sources:
        progress[key] = 0.0
    for key in top_sources:
        progress[key] = 1.0
    return progress


def shortest_surface_distances(
    adjacency: dict[
        tuple[float, float, float],
        dict[tuple[float, float, float], float],
    ],
    sources: set[tuple[float, float, float]],
) -> dict[tuple[float, float, float], float]:
    distances = {source: 0.0 for source in sources}
    pending = [(0.0, source) for source in sources]
    heapq.heapify(pending)
    while pending:
        distance, key = heapq.heappop(pending)
        if distance > distances.get(key, math.inf):
            continue
        for neighbor, edge_length in adjacency[key].items():
            next_distance = distance + edge_length
            if next_distance >= distances.get(neighbor, math.inf):
                continue
            distances[neighbor] = next_distance
            heapq.heappush(pending, (next_distance, neighbor))
    return distances


def build_harmonic_surface_progress(
    adjacency: dict[
        tuple[float, float, float],
        dict[tuple[float, float, float], float],
    ],
    bottom_sources: set[tuple[float, float, float]],
    top_sources: set[tuple[float, float, float]],
    initial_progress: dict[tuple[float, float, float], float],
    fixed_progress: dict[tuple[float, float, float], float] | None = None,
) -> dict[tuple[float, float, float], float]:
    if bottom_sources.intersection(top_sources):
        raise ValueError("Volume rod top and bottom boundaries overlap")

    progress = dict(initial_progress)
    locked_progress = {
        key: clamp(value, 0.0, 1.0)
        for key, value in (fixed_progress or {}).items()
        if key in adjacency
    }
    locked_progress.update((key, 0.0) for key in bottom_sources)
    locked_progress.update((key, 1.0) for key in top_sources)
    progress.update(locked_progress)
    interior = [
        key
        for key in adjacency
        if key not in locked_progress
    ]
    conductance_by_key: dict[
        tuple[float, float, float],
        tuple[tuple[tuple[float, float, float], float], ...],
    ] = {}
    total_conductance_by_key: dict[tuple[float, float, float], float] = {}
    for key in interior:
        neighbors = tuple(
            (neighbor, 1.0 / max(edge_length, UV_EPSILON))
            for neighbor, edge_length in adjacency[key].items()
        )
        total_conductance = sum(conductance for _, conductance in neighbors)
        if total_conductance <= UV_EPSILON:
            raise ValueError("Volume rod surface path contains an isolated vertex")
        conductance_by_key[key] = neighbors
        total_conductance_by_key[key] = total_conductance

    for _ in range(HARMONIC_PATH_MAX_ITERATIONS):
        maximum_change = 0.0
        for key in interior:
            weighted_progress = sum(
                progress[neighbor] * conductance
                for neighbor, conductance in conductance_by_key[key]
            )
            average = weighted_progress / total_conductance_by_key[key]
            previous = progress[key]
            updated = previous + HARMONIC_PATH_RELAXATION * (average - previous)
            progress[key] = updated
            maximum_change = max(maximum_change, abs(updated - previous))
        if maximum_change < HARMONIC_PATH_TOLERANCE:
            break
    else:
        raise ValueError("Volume rod harmonic surface paths did not converge")

    progress.update(locked_progress)
    for key in interior:
        progress[key] = clamp(progress[key], 0.0, 1.0)
    return progress


def regularize_volume_rod_reference_progress(
    adjacency: dict[
        tuple[float, float, float],
        dict[tuple[float, float, float], float],
    ],
    bottom_sources: set[tuple[float, float, float]],
    top_sources: set[tuple[float, float, float]],
    initial_progress: dict[tuple[float, float, float], float],
    reference_progress: dict[tuple[float, float, float], float],
    reference_tether: float,
) -> dict[tuple[float, float, float], float]:
    progress = dict(initial_progress)
    interior = [
        key
        for key in adjacency
        if key not in bottom_sources and key not in top_sources
    ]

    for _ in range(HARMONIC_PATH_MAX_ITERATIONS):
        maximum_change = 0.0
        for key in interior:
            neighbor_count = len(adjacency[key])
            if neighbor_count == 0:
                raise ValueError("Volume rod reference regularization found an isolated vertex")
            reference_conductance = (
                reference_tether * neighbor_count
                if key in reference_progress
                else 0.0
            )
            weighted_progress = sum(progress[neighbor] for neighbor in adjacency[key])
            if reference_conductance > 0.0:
                weighted_progress += reference_progress[key] * reference_conductance
            target = weighted_progress / (neighbor_count + reference_conductance)
            previous = progress[key]
            updated = previous + VOLUME_ROD_REFERENCE_RELAXATION * (target - previous)
            progress[key] = updated
            maximum_change = max(maximum_change, abs(updated - previous))
        if maximum_change < HARMONIC_PATH_TOLERANCE:
            break
    else:
        raise ValueError("Volume rod reference regularization did not converge")

    for key in bottom_sources:
        progress[key] = 0.0
    for key in top_sources:
        progress[key] = 1.0
    for key in interior:
        progress[key] = clamp(progress[key], 0.0, 1.0)
    return progress


def adapt_volume_rod_centerline(
    reference_centerline: tuple[tuple[float, float, float], ...],
    bottom_center: tuple[float, float, float],
    top_center: tuple[float, float, float],
    section_count: int,
) -> list[tuple[float, float, float]]:
    if len(reference_centerline) < 2:
        raise ValueError("Volume rod reference centerline must contain at least two points")

    reference_bottom = reference_centerline[0]
    reference_top = reference_centerline[-1]
    bottom_correction = tuple(
        bottom_center[axis] - reference_bottom[axis]
        for axis in range(3)
    )
    top_correction = tuple(
        top_center[axis] - reference_top[axis]
        for axis in range(3)
    )
    centerline = []
    for index, reference_point in enumerate(reference_centerline):
        progress = index / (len(reference_centerline) - 1)
        correction = lerp_vector(bottom_correction, top_correction, progress)
        centerline.append(
            tuple(
                reference_point[axis] + correction[axis]
                for axis in range(3)
            )
        )
    centerline[0] = bottom_center
    centerline[-1] = top_center
    if len(centerline) != section_count + 1:
        centerline = resample_volume_rod_centerline(centerline, section_count)
    return centerline


def resample_volume_rod_centerline(
    centerline: list[tuple[float, float, float]],
    section_count: int,
) -> list[tuple[float, float, float]]:
    segment_lengths = [
        vector_distance(centerline[index], centerline[index + 1])
        for index in range(len(centerline) - 1)
    ]
    total_length = sum(segment_lengths)
    if total_length <= UV_EPSILON:
        raise ValueError("Volume rod centerline is too short to resample")

    resampled = [centerline[0]]
    segment_index = 0
    distance_before_segment = 0.0
    for node_index in range(1, section_count):
        target_distance = total_length * node_index / section_count
        while (
            segment_index < len(segment_lengths) - 1
            and distance_before_segment + segment_lengths[segment_index] < target_distance
        ):
            distance_before_segment += segment_lengths[segment_index]
            segment_index += 1
        segment_length = segment_lengths[segment_index]
        amount = (
            (target_distance - distance_before_segment) / segment_length
            if segment_length > UV_EPSILON
            else 0.0
        )
        resampled.append(
            lerp_vector(
                centerline[segment_index],
                centerline[segment_index + 1],
                clamp(amount, 0.0, 1.0),
            )
        )
    resampled.append(centerline[-1])
    return resampled


def build_volume_rod_centerline(
    coordinate_by_key: dict[tuple[float, float, float], tuple[float, float, float]],
    progress_by_key: dict[tuple[float, float, float], float],
    bottom_center: tuple[float, float, float],
    top_center: tuple[float, float, float],
    section_count: int,
) -> list[tuple[float, float, float]]:
    centerline = [bottom_center]
    sigma = 0.75 / section_count
    radius = sigma * 2.5
    for section_index in range(1, section_count):
        target_progress = section_index / section_count
        weighted_x = weighted_y = weighted_z = total_weight = 0.0
        for key, progress in progress_by_key.items():
            progress_delta = abs(progress - target_progress)
            if progress_delta > radius:
                continue
            normalized_delta = progress_delta / sigma
            weight = math.exp(-0.5 * normalized_delta * normalized_delta)
            coordinate = coordinate_by_key[key]
            weighted_x += coordinate[0] * weight
            weighted_y += coordinate[1] * weight
            weighted_z += coordinate[2] * weight
            total_weight += weight
        if total_weight <= UV_EPSILON:
            centerline.append(lerp_vector(bottom_center, top_center, target_progress))
        else:
            centerline.append(
                (weighted_x / total_weight, weighted_y / total_weight, weighted_z / total_weight)
            )
    centerline.append(top_center)

    for _ in range(2):
        smoothed = [centerline[0]]
        for index in range(1, len(centerline) - 1):
            previous = centerline[index - 1]
            current = centerline[index]
            following = centerline[index + 1]
            smoothed.append(
                (
                    previous[0] * 0.25 + current[0] * 0.5 + following[0] * 0.25,
                    previous[1] * 0.25 + current[1] * 0.5 + following[1] * 0.25,
                    previous[2] * 0.25 + current[2] * 0.5 + following[2] * 0.25,
                )
            )
        smoothed.append(centerline[-1])
        centerline = smoothed
    return centerline


def vector_distance(
    first: tuple[float, float, float],
    second: tuple[float, float, float],
) -> float:
    return math.sqrt(
        (first[0] - second[0]) ** 2
        + (first[1] - second[1]) ** 2
        + (first[2] - second[2]) ** 2
    )


def lerp_vector(
    first: tuple[float, float, float],
    second: tuple[float, float, float],
    amount: float,
) -> tuple[float, float, float]:
    return (
        first[0] + (second[0] - first[0]) * amount,
        first[1] + (second[1] - first[1]) * amount,
        first[2] + (second[2] - first[2]) * amount,
    )


def resolve_deformation_group(path: Path, face: Face, required_groups: set[str]) -> str:
    matches = sorted(required_groups.intersection(face.labels))
    if not matches:
        raise ValueError(
            f"{path}: face at line {face.line_number} is not assigned to a deformation group"
        )
    if len(matches) > 1:
        raise ValueError(
            f"{path}: face at line {face.line_number} matches multiple deformation groups: {', '.join(matches)}"
        )
    return matches[0]


def center_for_group(
    coordinates: list[tuple[float, float, float]],
    coordinate_indexes: set[int],
) -> tuple[float, float, float]:
    total_x = total_y = total_z = 0.0
    for index in coordinate_indexes:
        coordinate = coordinates[index]
        total_x += coordinate[0]
        total_y += coordinate[1]
        total_z += coordinate[2]
    count = float(len(coordinate_indexes))
    return total_x / count, total_y / count, total_z / count


def center_for_coordinate_keys(
    coordinates_by_key: dict[tuple[float, float, float], tuple[float, float, float]],
    coordinate_keys: set[tuple[float, float, float]],
) -> tuple[float, float, float]:
    total_x = total_y = total_z = 0.0
    for key in coordinate_keys:
        coordinate = coordinates_by_key[key]
        total_x += coordinate[0]
        total_y += coordinate[1]
        total_z += coordinate[2]
    count = float(len(coordinate_keys))
    return total_x / count, total_y / count, total_z / count


def coordinate_key(coordinate: tuple[float, float, float]) -> tuple[float, float, float]:
    return (
        round(coordinate[0], COORDINATE_KEY_DECIMALS),
        round(coordinate[1], COORDINATE_KEY_DECIMALS),
        round(coordinate[2], COORDINATE_KEY_DECIMALS),
    )


def dot(left: tuple[float, float, float], right: tuple[float, float, float]) -> float:
    return left[0] * right[0] + left[1] * right[1] + left[2] * right[2]


def clamp(value: float, minimum: float, maximum: float) -> float:
    return max(minimum, min(maximum, value))


def get_texture(textures: list[tuple[float, float]], index: int) -> tuple[float, float]:
    if index < 0 or index >= len(textures):
        return 0.0, 0.0
    return textures[index]


def get_normal(normals: list[tuple[float, float, float]], index: int) -> tuple[float, float, float]:
    if index < 0 or index >= len(normals):
        return 0.0, 0.0, 1.0
    return normals[index]


def calculate_tangent_space(
    v1: tuple[float, float, float],
    v2: tuple[float, float, float],
    v3: tuple[float, float, float],
    uv1: tuple[float, float],
    uv2: tuple[float, float],
    uv3: tuple[float, float],
) -> tuple[tuple[float, float, float], tuple[float, float, float]]:
    delta_pos1 = (v2[0] - v1[0], v2[1] - v1[1], v2[2] - v1[2])
    delta_pos2 = (v3[0] - v1[0], v3[1] - v1[1], v3[2] - v1[2])
    delta_uv1 = (uv2[0] - uv1[0], uv2[1] - uv1[1])
    delta_uv2 = (uv3[0] - uv1[0], uv3[1] - uv1[1])
    denominator = delta_uv1[0] * delta_uv2[1] - delta_uv1[1] * delta_uv2[0]
    if math.fabs(denominator) < UV_EPSILON:
        return (1.0, 0.0, 0.0), (0.0, 1.0, 0.0)

    r = 1.0 / denominator
    tangent = (
        (delta_pos1[0] * delta_uv2[1] - delta_pos2[0] * delta_uv1[1]) * r,
        (delta_pos1[1] * delta_uv2[1] - delta_pos2[1] * delta_uv1[1]) * r,
        (delta_pos1[2] * delta_uv2[1] - delta_pos2[2] * delta_uv1[1]) * r,
    )
    bitangent = (
        (delta_pos2[0] * delta_uv1[0] - delta_pos1[0] * delta_uv2[0]) * r,
        (delta_pos2[1] * delta_uv1[0] - delta_pos1[1] * delta_uv2[0]) * r,
        (delta_pos2[2] * delta_uv1[0] - delta_pos1[2] * delta_uv2[0]) * r,
    )
    return tangent, bitangent


def write_binary(path: Path, vertices: list[float], indices: list[int], stats: dict[str, int]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    vertex_count = len(vertices) // FLOATS_PER_VERTEX
    header = struct.pack(
        "<4s10i",
        MAGIC,
        VERSION,
        FLOATS_PER_VERTEX,
        vertex_count,
        len(indices),
        stats["line_count"],
        stats["face_count"],
        stats["triangle_count"],
        stats["coordinate_count"],
        stats["texture_count"],
        stats["normal_count"],
    )
    vertex_array = array.array("f", vertices)
    index_array = array.array("i", indices)
    if sys.byteorder != "little":
        vertex_array.byteswap()
        index_array.byteswap()
    with path.open("wb") as target:
        target.write(header)
        vertex_array.tofile(target)
        index_array.tofile(target)


def write_parts_bundle(path: Path, parts: list[BinaryPart], stats: dict[str, int]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    header = struct.pack(
        "<4s11i",
        PARTS_MAGIC,
        PARTS_VERSION,
        FLOATS_PER_VERTEX,
        len(parts),
        stats["vertex_count"],
        stats["index_count"],
        stats["line_count"],
        stats["face_count"],
        stats["triangle_count"],
        stats["coordinate_count"],
        stats["texture_count"],
        stats["normal_count"],
    )
    with path.open("wb") as target:
        target.write(header)
        for part in parts:
            name_bytes = part.part_id.encode("utf-8")
            target.write(
                struct.pack(
                    "<6i",
                    len(name_bytes),
                    part.stats["vertex_count"],
                    part.stats["index_count"],
                    part.stats["face_count"],
                    part.stats["triangle_count"],
                    part.stats["expanded_vertex_count"],
                )
            )
            target.write(name_bytes)
            vertex_array = array.array("f", part.vertices)
            index_array = array.array("i", part.indices)
            if sys.byteorder != "little":
                vertex_array.byteswap()
                index_array.byteswap()
            vertex_array.tofile(target)
            index_array.tofile(target)


def write_deformation(
    path: Path,
    deformation_weights: list[float],
    selection_influences: list[int] | None,
    vertex_count: int,
    volume_rod_data: VolumeRodData | None,
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    expected_weight_count = vertex_count * DEFORMATION_INFLUENCE_COUNT
    if len(deformation_weights) != expected_weight_count:
        raise ValueError(
            f"Unexpected deformation weight count: expected {expected_weight_count}, "
            f"got {len(deformation_weights)}"
        )
    if selection_influences is None:
        selection_influences = [0] * vertex_count
    if len(selection_influences) != vertex_count:
        raise ValueError(
            f"Unexpected deformation selection influence count: expected {vertex_count}, "
            f"got {len(selection_influences)}"
        )
    deformation_version = DEFORMATION_VOLUME_ROD_VERSION if volume_rod_data else DEFORMATION_VERSION
    header = struct.pack(
        "<4s3i",
        DEFORMATION_MAGIC,
        deformation_version,
        vertex_count,
        DEFORMATION_INFLUENCE_COUNT,
    )
    weights_array = array.array("f", deformation_weights)
    if sys.byteorder != "little":
        weights_array.byteswap()
    selection_array = array.array("i", selection_influences)
    if sys.byteorder != "little":
        selection_array.byteswap()
    with path.open("wb") as target:
        target.write(header)
        weights_array.tofile(target)
        selection_array.tofile(target)
        if volume_rod_data is not None:
            target.write(struct.pack("<i", len(volume_rod_data.centerline)))
            centerline_values = array.array(
                "f",
                (value for center in volume_rod_data.centerline for value in center),
            )
            if sys.byteorder != "little":
                centerline_values.byteswap()
            centerline_values.tofile(target)


def binary_output_path_for(part: dict, source_asset: str, assets_dir: Path, output_dir: Path) -> Path:
    explicit_asset = part.get("binaryAsset")
    if explicit_asset:
        return assets_dir / explicit_asset
    return output_dir / obj_to_bin_name(source_asset)


def deformation_output_path_for(part: dict, source_asset: str, assets_dir: Path, output_dir: Path) -> Path:
    deformation = part.get("deformation") or {}
    explicit_asset = deformation.get("asset") or part.get("deformationAsset")
    if explicit_asset:
        return assets_dir / explicit_asset
    return output_dir / obj_to_def_name(source_asset)


def convert_bundle(
    source_asset: str,
    output_asset: str | None,
    assets_dir: Path,
    output_dir: Path,
    exclude_objects: set[str] | None = None,
    mesh_processing_by_object: dict | None = None,
) -> tuple[int, int, int, int, int]:
    source_path = assets_dir / source_asset
    output_path = assets_dir / output_asset if output_asset else output_dir / obj_to_bin_name(source_asset)
    bundle_parts, stats = parse_obj_parts_by_object(
        source_path,
        exclude_objects,
        mesh_processing_by_object,
    )
    write_parts_bundle(output_path, bundle_parts, stats)
    file_size = output_path.stat().st_size
    print(
        f"{source_asset} -> {display_path(output_path, assets_dir)} "
        f"bundleParts={len(bundle_parts)} vertices={stats['vertex_count']} "
        f"indices={stats['index_count']} expandedVertices={stats['expanded_vertex_count']} "
        f"dedupSavedVertices={stats['expanded_vertex_count'] - stats['vertex_count']} "
        f"bytes={file_size}"
    )
    for bundle_part in bundle_parts:
        processing_info = ""
        if "skipped_triangle_count" in bundle_part.stats:
            processing_info = (
                f" skippedTriangles={bundle_part.stats['skipped_triangle_count']}"
                f" reorientedTriangles={bundle_part.stats['reoriented_triangle_count']}"
                f" creaseEdges={bundle_part.stats['crease_edge_count']}"
            )
        print(
            f"  part={bundle_part.part_id} faces={bundle_part.stats['face_count']} "
            f"triangles={bundle_part.stats['triangle_count']} "
            f"vertices={bundle_part.stats['vertex_count']} "
            f"indices={bundle_part.stats['index_count']}"
            f"{processing_info}"
        )
    return (
        len(bundle_parts),
        stats["vertex_count"],
        stats["index_count"],
        stats["expanded_vertex_count"],
        file_size,
    )


def object_filter_set(source: dict, key: str) -> set[str]:
    raw_value = source.get(key, [])
    if raw_value is None:
        return set()
    if not isinstance(raw_value, list):
        raise ValueError(f"`{key}` must be an array")
    return {str(item) for item in raw_value}


def convert_part(
    part: dict,
    assets_dir: Path,
    output_dir: Path,
) -> tuple[int, int, int, int]:
    source_asset = part.get("asset") or part.get("file")
    if not source_asset:
        raise ValueError(f"Missing asset for part {part}")
    source_path = assets_dir / source_asset
    output_path = binary_output_path_for(part, source_asset, assets_dir, output_dir)
    deformation = part.get("deformation")
    object_filter = part.get("object") or part.get("objectName")
    vertices, indices, stats, deformation_weights, selection_influences, volume_rod_data = parse_obj(
        source_path,
        deformation,
        object_filter,
        part.get("meshProcessing"),
        assets_dir,
    )
    write_binary(output_path, vertices, indices, stats)
    file_size = output_path.stat().st_size
    deformation_info = ""
    if deformation is not None:
        deformation_output_path = deformation_output_path_for(part, source_asset, assets_dir, output_dir)
        if deformation_weights is None:
            raise ValueError(f"Missing deformation weights for {source_asset}")
        write_deformation(
            deformation_output_path,
            deformation_weights,
            selection_influences,
            stats["vertex_count"],
            volume_rod_data,
        )
        deformation_info = f" deformation={display_path(deformation_output_path, assets_dir)}"
    object_info = f" object={object_filter}" if object_filter else ""
    processing_info = ""
    if "skipped_triangle_count" in stats:
        processing_info = (
            f" skippedTriangles={stats['skipped_triangle_count']}"
            f" reorientedTriangles={stats['reoriented_triangle_count']}"
            f" creaseEdges={stats['crease_edge_count']}"
        )
    print(
        f"{source_asset} -> {display_path(output_path, assets_dir)} "
        f"vertices={stats['vertex_count']} indices={stats['index_count']} "
        f"expandedVertices={stats['expanded_vertex_count']} "
        f"dedupSavedVertices={stats['expanded_vertex_count'] - stats['vertex_count']} "
        f"bytes={file_size}"
        f"{object_info}"
        f"{deformation_info}"
        f"{processing_info}"
    )
    return (
        stats["vertex_count"],
        stats["index_count"],
        stats["expanded_vertex_count"],
        file_size,
    )


def main() -> None:
    args = parse_args()
    assets_dir = args.assets_dir
    manifest_path = assets_dir / args.manifest
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    output_dir = assets_dir / args.output_dir

    total_vertices = 0
    total_indices = 0
    total_expanded_vertices = 0
    total_bytes = 0
    manifest_parts = manifest.get("parts")
    if manifest_parts is None and not (args.split_objects_bundle and "bundle" in manifest):
        raise ValueError("Manifest must contain `parts` or `bundle.source` with --split-objects-bundle")

    converted_part_count = 0
    if args.split_objects_bundle and "bundle" in manifest:
        bundle = manifest["bundle"]
        bundle_part_count, vertices, indices, expanded_vertices, file_size = convert_bundle(
            bundle["source"],
            bundle.get("asset"),
            assets_dir,
            output_dir,
            object_filter_set(bundle, "excludeObjects"),
            bundle.get("meshProcessingByObject"),
        )
        converted_part_count += bundle_part_count
        total_vertices += vertices
        total_indices += indices
        total_expanded_vertices += expanded_vertices
        total_bytes += file_size
        for part in manifest_parts or []:
            vertices, indices, expanded_vertices, file_size = convert_part(part, assets_dir, output_dir)
            converted_part_count += 1
            total_vertices += vertices
            total_indices += indices
            total_expanded_vertices += expanded_vertices
            total_bytes += file_size
    else:
        for part in manifest_parts:
            if args.split_objects_bundle:
                source_asset = part.get("asset") or part.get("file")
                if not source_asset:
                    raise ValueError(f"Missing asset for part {part}")
                if part.get("deformation") is not None:
                    raise ValueError("--split-objects-bundle does not support deformable parts without bundle")
                bundle_part_count, vertices, indices, expanded_vertices, file_size = convert_bundle(
                    source_asset,
                    part.get("binaryAsset"),
                    assets_dir,
                    output_dir,
                    object_filter_set(part, "excludeObjects"),
                    part.get("meshProcessingByObject"),
                )
                converted_part_count += bundle_part_count
            else:
                vertices, indices, expanded_vertices, file_size = convert_part(part, assets_dir, output_dir)
                converted_part_count += 1
            total_vertices += vertices
            total_indices += indices
            total_expanded_vertices += expanded_vertices
            total_bytes += file_size

    print(
        f"converted parts={converted_part_count} vertices={total_vertices} "
        f"indices={total_indices} expandedVertices={total_expanded_vertices} bytes={total_bytes}"
    )


if __name__ == "__main__":
    main()
