#!/usr/bin/env python3
"""Convert V3 OBJ model parts into runtime-ready binary buffers."""

from __future__ import annotations

import argparse
import array
import json
import math
import struct
import sys
from pathlib import Path


MAGIC = b"V3MB"
VERSION = 1
FLOATS_PER_VERTEX = 18
UV_EPSILON = 0.000001


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
    return parser.parse_args()


def obj_to_bin_name(asset_path: str) -> str:
    source = Path(asset_path)
    return source.with_suffix(".v3bin").name


def parse_obj(path: Path) -> tuple[list[float], list[int], dict[str, int]]:
    coordinates: list[tuple[float, float, float]] = []
    textures: list[tuple[float, float]] = []
    normals: list[tuple[float, float, float]] = []
    vertices: list[float] = []
    indices: list[int] = []
    line_count = 0
    face_count = 0
    triangle_count = 0

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
            elif tokens[0] == "f" and len(tokens) >= 4:
                face_count += 1
                first = parse_vertex_ref(tokens[1], len(coordinates), len(textures), len(normals))
                previous = parse_vertex_ref(tokens[2], len(coordinates), len(textures), len(normals))
                for token in tokens[3:]:
                    current = parse_vertex_ref(token, len(coordinates), len(textures), len(normals))
                    triangle_count += 1
                    append_triangle(vertices, indices, coordinates, textures, normals, first, previous, current)
                    previous = current

    stats = {
        "line_count": line_count,
        "face_count": face_count,
        "triangle_count": triangle_count,
        "coordinate_count": len(coordinates),
        "texture_count": len(textures),
        "normal_count": len(normals),
        "vertex_count": len(vertices) // FLOATS_PER_VERTEX,
        "index_count": len(indices),
    }
    return vertices, indices, stats


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
) -> None:
    v1 = coordinates[ref1[0]]
    v2 = coordinates[ref2[0]]
    v3 = coordinates[ref3[0]]
    uv1 = get_texture(textures, ref1[1])
    uv2 = get_texture(textures, ref2[1])
    uv3 = get_texture(textures, ref3[1])
    tangent, bitangent = calculate_tangent_space(v1, v2, v3, uv1, uv2, uv3)
    append_vertex(vertices, indices, v1, get_normal(normals, ref1[2]), uv1, tangent, bitangent)
    append_vertex(vertices, indices, v2, get_normal(normals, ref2[2]), uv2, tangent, bitangent)
    append_vertex(vertices, indices, v3, get_normal(normals, ref3[2]), uv3, tangent, bitangent)


def append_vertex(
    vertices: list[float],
    indices: list[int],
    coordinate: tuple[float, float, float],
    normal: tuple[float, float, float],
    texture: tuple[float, float],
    tangent: tuple[float, float, float],
    bitangent: tuple[float, float, float],
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


def main() -> None:
    args = parse_args()
    assets_dir = args.assets_dir
    manifest_path = assets_dir / args.manifest
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    output_dir = assets_dir / args.output_dir

    total_vertices = 0
    total_indices = 0
    total_bytes = 0
    for part in manifest["parts"]:
        source_asset = part.get("asset") or part.get("file")
        if not source_asset:
            raise ValueError(f"Missing asset for part {part}")
        source_path = assets_dir / source_asset
        output_path = output_dir / obj_to_bin_name(source_asset)
        vertices, indices, stats = parse_obj(source_path)
        write_binary(output_path, vertices, indices, stats)
        file_size = output_path.stat().st_size
        total_vertices += stats["vertex_count"]
        total_indices += stats["index_count"]
        total_bytes += file_size
        print(
            f"{source_asset} -> {output_path.relative_to(assets_dir)} "
            f"vertices={stats['vertex_count']} indices={stats['index_count']} bytes={file_size}"
        )

    print(
        f"converted parts={len(manifest['parts'])} vertices={total_vertices} "
        f"indices={total_indices} bytes={total_bytes}"
    )


if __name__ == "__main__":
    main()
