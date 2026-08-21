#!/usr/bin/env python3
"""Convert a plain OBJ to the compact vertex-colored V3 card format."""

import argparse
import math
import struct
from pathlib import Path


def unit(vector):
    length = math.sqrt(sum(value * value for value in vector))
    return tuple(value / length for value in vector) if length > 1e-8 else (0.0, 1.0, 0.0)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--color", nargs=3, type=float, default=(0.52, 0.30, 0.12))
    parser.add_argument("--size", type=float, default=95.0)
    parser.add_argument("--short-side-scale", type=float, default=1.0,
                        help="Scale only the middle-sized axis, preserving length and thickness")
    parser.add_argument("--wood", action="store_true",
                        help="Bake natural wood grain into vertex colors")
    args = parser.parse_args()

    positions, texcoords, normals, triangles = [], [], [], []
    for line in args.input.read_text().splitlines():
        fields = line.split()
        if not fields:
            continue
        if fields[0] == "v":
            positions.append(tuple(map(float, fields[1:4])))
        elif fields[0] == "vt":
            texcoords.append(tuple(map(float, fields[1:3])))
        elif fields[0] == "vn":
            normals.append(tuple(map(float, fields[1:4])))
        elif fields[0] == "f":
            face = []
            for token in fields[1:]:
                raw = token.split("/")
                raw += [""] * (3 - len(raw))
                face.append(tuple(int(value) - 1 if value else -1 for value in raw[:3]))
            for index in range(1, len(face) - 1):
                triangles.append((face[0], face[index], face[index + 1]))

    minimum = [min(point[axis] for point in positions) for axis in range(3)]
    maximum = [max(point[axis] for point in positions) for axis in range(3)]
    center = [(minimum[axis] + maximum[axis]) * 0.5 for axis in range(3)]
    extents = [maximum[axis] - minimum[axis] for axis in range(3)]
    _, short_axis, long_axis = sorted(range(3), key=lambda axis: extents[axis])
    axis_scale = [1.0, 1.0, 1.0]
    axis_scale[short_axis] = args.short_side_scale
    scale = args.size / extents[long_axis]
    values = []
    for triangle in triangles:
        points = [positions[vertex[0]] for vertex in triangle]
        ab = tuple(points[1][i] - points[0][i] for i in range(3))
        ac = tuple(points[2][i] - points[0][i] for i in range(3))
        face_normal = unit((ab[1] * ac[2] - ab[2] * ac[1],
                            ab[2] * ac[0] - ab[0] * ac[2],
                            ab[0] * ac[1] - ab[1] * ac[0]))
        tangent = unit(ab)
        for position_index, uv_index, normal_index in triangle:
            centered = tuple(positions[position_index][i] - center[i] for i in range(3))
            position = tuple(centered[i] * axis_scale[i] * scale for i in range(3))
            uv = texcoords[uv_index] if 0 <= uv_index < len(texcoords) else (0.0, 0.0)
            normal = unit(normals[normal_index]) if 0 <= normal_index < len(normals) else face_normal
            bitangent = unit((normal[1] * tangent[2] - normal[2] * tangent[1],
                              normal[2] * tangent[0] - normal[0] * tangent[2],
                              normal[0] * tangent[1] - normal[1] * tangent[0]))
            color = args.color
            if args.wood:
                along = centered[long_axis] / max(extents[long_axis], 1e-8)
                across = centered[short_axis] / max(extents[short_axis], 1e-8)
                # Broad, low-contrast grain reads as matte wood at card size;
                # high-frequency stripes looked noisy and artificial.
                grain = (0.78 * math.sin(along * 31.0 + math.sin(across * 7.0) * 1.15)
                         + 0.22 * math.sin(along * 67.0 - across * 5.0))
                grain = 0.5 + 0.5 * grain
                dark = (0.34, 0.175, 0.065)
                light = (0.52, 0.305, 0.135)
                color = tuple(dark[i] + (light[i] - dark[i]) * grain for i in range(3))
            values.extend((*position, *normal, *color, 1.0, *uv, *tangent, *bitangent))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(struct.pack("<4sII", b"V3OB", 1, len(values) // 18)
                            + struct.pack(f"<{len(values)}f", *values))
    print(f"triangles={len(triangles)} vertices={len(values) // 18}")


if __name__ == "__main__":
    main()
