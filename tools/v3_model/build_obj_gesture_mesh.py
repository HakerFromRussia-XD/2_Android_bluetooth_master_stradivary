#!/usr/bin/env python3
"""Convert a textured OBJ stored in a zip archive to the compact V3 card format."""

import argparse
import io
import math
import struct
import zipfile
from pathlib import Path

from PIL import Image


def normalized(vector):
    length = math.sqrt(sum(value * value for value in vector))
    return tuple(value / length for value in vector) if length > 1e-8 else (0.0, 1.0, 0.0)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("archive", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--obj", default="Paper_coffee_cup.obj")
    parser.add_argument("--texture", default="coffe_cup_col.png")
    args = parser.parse_args()

    with zipfile.ZipFile(args.archive) as archive:
        lines = archive.read(args.obj).decode("utf-8").splitlines()
        texture = Image.open(io.BytesIO(archive.read(args.texture))).convert("RGB")

    positions, texcoords, normals, triangles = [], [], [], []
    include_faces = True
    for line in lines:
        fields = line.split()
        if not fields:
            continue
        if fields[0] == "v":
            positions.append(tuple(map(float, fields[1:4])))
        elif fields[0] == "vt":
            texcoords.append(tuple(map(float, fields[1:3])))
        elif fields[0] == "vn":
            normals.append(tuple(map(float, fields[1:4])))
        elif fields[0] == "usemtl":
            # Cup Grip uses the open paper cup without the separate plastic lid.
            include_faces = len(fields) < 2 or fields[1] != "lid"
        elif fields[0] == "f" and include_faces:
            face = []
            for token in fields[1:]:
                indices = token.split("/")
                face.append(tuple(int(value) - 1 if value else -1 for value in indices + [""] * (3 - len(indices))))
            for index in range(1, len(face) - 1):
                triangles.append((face[0], face[index], face[index + 1]))

    minimum = [min(point[axis] for point in positions) for axis in range(3)]
    maximum = [max(point[axis] for point in positions) for axis in range(3)]
    center = [(minimum[axis] + maximum[axis]) * 0.5 for axis in range(3)]
    # Match the hand model's working units while preserving the OBJ proportions.
    source_height = max(maximum[axis] - minimum[axis] for axis in range(3))
    scale = 70.0 / source_height
    width, height = texture.size
    values = []
    for triangle in triangles:
        p = [positions[vertex[0]] for vertex in triangle]
        ab = tuple(p[1][i] - p[0][i] for i in range(3))
        ac = tuple(p[2][i] - p[0][i] for i in range(3))
        face_normal = normalized((ab[1] * ac[2] - ab[2] * ac[1],
                                  ab[2] * ac[0] - ab[0] * ac[2],
                                  ab[0] * ac[1] - ab[1] * ac[0]))
        for vertex in triangle:
            position_index, uv_index, normal_index = vertex
            position = tuple((positions[position_index][i] - center[i]) * scale for i in range(3))
            uv = texcoords[uv_index] if 0 <= uv_index < len(texcoords) else (0.0, 0.0)
            normal = normalized(normals[normal_index]) if 0 <= normal_index < len(normals) else face_normal
            x = min(width - 1, max(0, round((uv[0] % 1.0) * (width - 1))))
            y = min(height - 1, max(0, round((1.0 - (uv[1] % 1.0)) * (height - 1))))
            rgb = tuple(channel / 255.0 for channel in texture.getpixel((x, y)))
            tangent = normalized(ab)
            bitangent = normalized((normal[1] * tangent[2] - normal[2] * tangent[1],
                                    normal[2] * tangent[0] - normal[0] * tangent[2],
                                    normal[0] * tangent[1] - normal[1] * tangent[0]))
            values.extend((*position, *normal, *rgb, 1.0, *uv, *tangent, *bitangent))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(struct.pack("<4sII", b"V3OB", 1, len(values) // 18)
                            + struct.pack(f"<{len(values)}f", *values))
    print(f"triangles={len(triangles)} vertices={len(values) // 18}")


if __name__ == "__main__":
    main()
