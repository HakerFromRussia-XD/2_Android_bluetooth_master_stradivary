#!/usr/bin/env python3
"""Convert V3 OBJ model parts into runtime-ready binary buffers."""

from __future__ import annotations

import argparse
import array
from dataclasses import dataclass
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
DEFORMATION_MAGIC = b"V3DF"
DEFORMATION_VERSION = 1
DEFORMATION_INFLUENCE_COUNT = 5
INFLUENCE_ORDER = ("palm", "index", "middle", "ring", "little")
SUPPORTED_TRANSFORM_IDS = {
    "palm_base",
    "index_upper",
    "middle_upper",
    "ring_upper",
    "little_upper",
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
class DeformationSpec:
    bottom_group: str
    bottom_transform_id: str
    tops: tuple[TopAnchorSpec, ...]
    falloff: str

    @property
    def required_groups(self) -> tuple[str, ...]:
        groups = [self.bottom_group]
        for top in self.tops:
            groups.append(top.top_group)
            groups.append(top.soft_group)
        return tuple(groups)


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
) -> tuple[list[float], list[int], dict[str, int], list[float] | None]:
    vertices: list[float] = []
    indices: list[int] = []
    deformation_weights: list[float] | None = [] if deformation else None
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

    deformation_spec = parse_deformation_spec(deformation) if deformation else None
    weight_resolver = None
    if deformation_spec:
        weight_resolver = create_weight_resolver(path, coordinates, faces, deformation_spec)

    if deformation_spec:
        if weight_resolver is None:
            raise ValueError("Missing deformation weight resolver")
        vertices, indices, triangle_count, deformation_weights = build_indexed_deformable_buffers(
            coordinates,
            textures,
            normals,
            faces,
            weight_resolver,
        )
    else:
        vertices, indices, triangle_count = build_indexed_buffers(coordinates, textures, normals, faces)

    stats = build_stats(line_count, faces, triangle_count, coordinates, textures, normals, vertices, indices)
    return vertices, indices, stats, deformation_weights


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


def parse_obj_parts_by_object(
    path: Path,
    exclude_objects: set[str] | None = None,
) -> tuple[list[BinaryPart], dict[str, int]]:
    geometry = read_obj_geometry(path)
    exclude_objects = exclude_objects or set()
    faces_by_object: dict[str, list[Face]] = {}
    object_order: list[str] = []
    for face in geometry.faces:
        if face.object_name in exclude_objects:
            continue
        if face.object_name not in faces_by_object:
            faces_by_object[face.object_name] = []
            object_order.append(face.object_name)
        faces_by_object[face.object_name].append(face)

    parts: list[BinaryPart] = []
    total_vertices = 0
    total_indices = 0
    total_triangles = 0
    total_expanded_vertices = 0
    for object_name in object_order:
        object_faces = faces_by_object[object_name]
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
) -> tuple[list[float], list[int], int, list[float]]:
    records: list[IndexedVertex] = []
    deformation_weights_by_vertex: list[tuple[float, float, float, float, float]] = []
    indexes_by_ref_and_weights: dict[
        tuple[tuple[int, int, int], tuple[float, float, float, float, float]],
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
                key = (ref, weights)
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
                add_vector(records[vertex_index].tangent_sum, tangent)
                add_vector(records[vertex_index].bitangent_sum, bitangent)
                indices.append(vertex_index)
            previous = current

    vertices: list[float] = []
    deformation_weights: list[float] = []
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
    return vertices, indices, triangle_count, deformation_weights


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
    weights: tuple[float, float, float, float, float] | None,
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
    if deformation_type != "multi_top_one_bottom":
        raise ValueError(f"Unsupported deformation type `{deformation_type}`")
    falloff = deformation.get("falloff", "smoothstep")
    if falloff not in {"smoothstep", "linear", "ease_in"}:
        raise ValueError(f"Unsupported deformation falloff `{falloff}`")

    bottom = deformation.get("bottom") or {}
    bottom_group = required_string(bottom, "faceGroup", "deformation.bottom")
    bottom_transform_id = required_string(bottom, "transformId", "deformation.bottom")
    validate_transform_id(bottom_transform_id)

    tops = deformation.get("tops")
    if not isinstance(tops, list):
        raise ValueError("deformation.tops must be an array")
    expected_fingers = ("index", "middle", "ring", "little")
    tops_by_finger = {}
    for top in tops:
        if not isinstance(top, dict):
            raise ValueError("Each deformation.tops item must be an object")
        finger = required_string(top, "finger", "deformation.tops")
        if finger not in expected_fingers:
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
    missing = [finger for finger in expected_fingers if finger not in tops_by_finger]
    if missing:
        raise ValueError(f"Missing deformation top anchors for: {', '.join(missing)}")

    return DeformationSpec(
        bottom_group=bottom_group,
        bottom_transform_id=bottom_transform_id,
        tops=tuple(tops_by_finger[finger] for finger in expected_fingers),
        falloff=falloff,
    )


def required_string(source: dict, key: str, owner: str) -> str:
    value = source.get(key)
    if not isinstance(value, str) or not value:
        raise ValueError(f"Missing {owner}.{key}")
    return value


def validate_transform_id(transform_id: str) -> None:
    if transform_id not in SUPPORTED_TRANSFORM_IDS:
        raise ValueError(f"Unsupported deformation transformId `{transform_id}`")


def create_weight_resolver(
    path: Path,
    coordinates: list[tuple[float, float, float]],
    faces: list[Face],
    spec: DeformationSpec,
):
    required_groups = set(spec.required_groups)
    group_coordinates: dict[str, set[int]] = {group: set() for group in required_groups}
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

    soft_axes = {}
    for top in spec.tops:
        bottom_locked_indexes = group_coordinates[top.soft_group].intersection(group_coordinates[spec.bottom_group])
        top_locked_indexes = group_coordinates[top.soft_group].intersection(group_coordinates[top.top_group])
        if not bottom_locked_indexes:
            raise ValueError(f"{path}: deformation soft group `{top.soft_group}` has no shared bottom edge")
        if not top_locked_indexes:
            raise ValueError(f"{path}: deformation soft group `{top.soft_group}` has no shared top edge")
        bottom_center = center_for_group(coordinates, bottom_locked_indexes)
        top_center = center_for_group(coordinates, top_locked_indexes)
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
            bottom_locked_indexes,
            top_locked_indexes,
        )

    top_by_group = {top.top_group: top for top in spec.tops}

    def weights_for(face: Face, coordinate_index: int) -> tuple[float, float, float, float, float]:
        group = resolved_groups[face]
        if group == spec.bottom_group:
            return 1.0, 0.0, 0.0, 0.0, 0.0
        top = top_by_group.get(group)
        if top:
            weights = [0.0] * DEFORMATION_INFLUENCE_COUNT
            weights[top.influence_index] = 1.0
            return tuple(weights)  # type: ignore[return-value]
        top, bottom_center, axis, axis_length_sq, bottom_locked_indexes, top_locked_indexes = soft_axes[group]
        if coordinate_index in bottom_locked_indexes:
            return 1.0, 0.0, 0.0, 0.0, 0.0
        if coordinate_index in top_locked_indexes:
            weights = [0.0] * DEFORMATION_INFLUENCE_COUNT
            weights[top.influence_index] = 1.0
            return tuple(weights)  # type: ignore[return-value]
        coordinate = coordinates[coordinate_index]
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
        weights = [0.0] * DEFORMATION_INFLUENCE_COUNT
        weights[0] = 1.0 - t
        weights[top.influence_index] = t
        return tuple(weights)  # type: ignore[return-value]

    return weights_for


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


def write_deformation(path: Path, deformation_weights: list[float], vertex_count: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    expected_weight_count = vertex_count * DEFORMATION_INFLUENCE_COUNT
    if len(deformation_weights) != expected_weight_count:
        raise ValueError(
            f"Unexpected deformation weight count: expected {expected_weight_count}, "
            f"got {len(deformation_weights)}"
        )
    header = struct.pack(
        "<4s3i",
        DEFORMATION_MAGIC,
        DEFORMATION_VERSION,
        vertex_count,
        DEFORMATION_INFLUENCE_COUNT,
    )
    weights_array = array.array("f", deformation_weights)
    if sys.byteorder != "little":
        weights_array.byteswap()
    with path.open("wb") as target:
        target.write(header)
        weights_array.tofile(target)


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
) -> tuple[int, int, int, int, int]:
    source_path = assets_dir / source_asset
    output_path = assets_dir / output_asset if output_asset else output_dir / obj_to_bin_name(source_asset)
    bundle_parts, stats = parse_obj_parts_by_object(source_path, exclude_objects)
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
        print(
            f"  part={bundle_part.part_id} faces={bundle_part.stats['face_count']} "
            f"triangles={bundle_part.stats['triangle_count']} "
            f"vertices={bundle_part.stats['vertex_count']} "
            f"indices={bundle_part.stats['index_count']}"
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
    vertices, indices, stats, deformation_weights = parse_obj(source_path, deformation, object_filter)
    write_binary(output_path, vertices, indices, stats)
    file_size = output_path.stat().st_size
    deformation_info = ""
    if deformation is not None:
        deformation_output_path = deformation_output_path_for(part, source_asset, assets_dir, output_dir)
        if deformation_weights is None:
            raise ValueError(f"Missing deformation weights for {source_asset}")
        write_deformation(deformation_output_path, deformation_weights, stats["vertex_count"])
        deformation_info = f" deformation={display_path(deformation_output_path, assets_dir)}"
    object_info = f" object={object_filter}" if object_filter else ""
    print(
        f"{source_asset} -> {display_path(output_path, assets_dir)} "
        f"vertices={stats['vertex_count']} indices={stats['index_count']} "
        f"expandedVertices={stats['expanded_vertex_count']} "
        f"dedupSavedVertices={stats['expanded_vertex_count'] - stats['vertex_count']} "
        f"bytes={file_size}"
        f"{object_info}"
        f"{deformation_info}"
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
