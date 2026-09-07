#!/usr/bin/env python3
"""Convert a binary STL into the compact V3 object format used by iOS cards."""

import argparse
import math
import struct
from pathlib import Path


def read_binary_stl(path: Path):
    data = path.read_bytes()
    count = struct.unpack_from("<I", data, 80)[0]
    if len(data) != 84 + count * 50:
        raise ValueError("Only binary STL input is supported")
    triangles = []
    offset = 84
    for _ in range(count):
        values = struct.unpack_from("<12fH", data, offset)
        triangles.append((values[3:6], values[6:9], values[9:12]))
        offset += 50
    return triangles


def clustered(triangles, cell):
    sums = {}
    for triangle in triangles:
        for vertex in triangle:
            key = tuple(round(value / cell) for value in vertex)
            total = sums.setdefault(key, [0.0, 0.0, 0.0, 0])
            total[0] += vertex[0]
            total[1] += vertex[1]
            total[2] += vertex[2]
            total[3] += 1
    centers = {key: tuple(value / total[3] for value in total[:3]) for key, total in sums.items()}
    result = []
    seen = set()
    for triangle in triangles:
        keys = tuple(tuple(round(value / cell) for value in vertex) for vertex in triangle)
        if len(set(keys)) != 3:
            continue
        face_key = tuple(sorted(keys))
        if face_key in seen:
            continue
        seen.add(face_key)
        result.append(tuple(centers[key] for key in keys))
    return result


def face_vertices(triangles):
    points = [vertex for triangle in triangles for vertex in triangle]
    minimum = [min(point[axis] for point in points) for axis in range(3)]
    maximum = [max(point[axis] for point in points) for axis in range(3)]
    center = [(minimum[axis] + maximum[axis]) * 0.5 for axis in range(3)]
    output = []
    for triangle in triangles:
        a, b, c = triangle
        ab = tuple(b[i] - a[i] for i in range(3))
        ac = tuple(c[i] - a[i] for i in range(3))
        normal = (ab[1] * ac[2] - ab[2] * ac[1], ab[2] * ac[0] - ab[0] * ac[2], ab[0] * ac[1] - ab[1] * ac[0])
        length = math.sqrt(sum(value * value for value in normal))
        if length < 1e-8:
            continue
        normal = tuple(value / length for value in normal)
        tangent = (1.0, 0.0, 0.0)
        bitangent = (0.0, 1.0, 0.0)
        for vertex in triangle:
            position = tuple(vertex[i] - center[i] for i in range(3))
            output.extend((*position, *normal, 0.75, 0.76, 0.78, 1.0, 0.0, 0.0, *tangent, *bitangent))
    return output


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--target", type=int, default=4500)
    args = parser.parse_args()
    source = read_binary_stl(args.input)
    low, high = 0.05, 5.0
    chosen = source
    for _ in range(28):
        cell = (low + high) * 0.5
        candidate = clustered(source, cell)
        chosen = candidate
        if len(candidate) > args.target:
            low = cell
        else:
            high = cell
    values = face_vertices(chosen)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(struct.pack("<4sII", b"V3OB", 1, len(values) // 18) + struct.pack(f"<{len(values)}f", *values))
    print(f"source_triangles={len(source)} output_triangles={len(values) // 54} vertices={len(values) // 18}")


if __name__ == "__main__":
    main()
