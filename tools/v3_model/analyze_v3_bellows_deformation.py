#!/usr/bin/env python3
"""Numerically reproduce and inspect the Android V3 volume-rod deformation."""

from __future__ import annotations

import argparse
from collections import Counter
import json
import math
import struct
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import numpy as np


FLOATS_PER_VERTEX = 18
INFLUENCE_COUNT = 6
THUMB_INFLUENCE = 5

FIRST_AXIS_MIN = -35.0
FIRST_AXIS_MAX = 49.0
SECOND_AXIS_MIN = -68.0
SECOND_AXIS_MAX = 22.0
SECOND_AXIS_BIND = -34.0
TOUCH_X_CORRECTION_DEGREES = 34.0

DELTA_X_PIVOT = np.array((-65.678083, -18.191633, -28.560333), dtype=np.float64)
DELTA_Y_PIVOT = np.array((-40.648183, -27.336317, -31.565383), dtype=np.float64)

PALM_ANCHOR_BLEND = 0.24
FINGER_ANCHOR_BLEND = 0.55
MIN_RADIAL_SCALE = 1.0
MIN_AXIAL_SCALE = 0.35
MAX_AXIAL_SCALE = 2.5
AXIAL_SCALE_STRENGTH = 1.0
PALM_HANDLE_RATIO = 0.275
FINGER_HANDLE_RATIO = 0.50
MIN_HANDLE_SCALE = 0.65
MAX_HANDLE_SCALE = 1.20
BENDING_STRAIN_GAIN = 0.0
PALM_STRAIN_BLEND = 0.35
FINGER_STRAIN_BLEND = 0.32
MAX_COMBINED_RADIAL_SCALE = 1.85
STRETCH_SMOOTHING_PASSES = 16
COMPRESSION_SMOOTHING_PASSES = 40
COMPRESSION_RADIAL_GAIN = 0.0
MAX_COMPRESSION_RADIAL_SCALE = 1.04
MAX_COMPRESSION_COMBINED_RADIAL_SCALE = 1.15
INNER_EXPANSION_LIMIT_FACTOR = 1.0
USE_SHORTEST_ARC_FRAME_ALIGNMENT = True
BLEND_ROD_WITH_RIGID_ANCHORS = True
USE_SMOOTHSTEP_ANCHOR_BLEND = True
WEIGHT_EPSILON = 0.0001


@dataclass(frozen=True)
class Mesh:
    vertices: np.ndarray
    indices: np.ndarray

    @property
    def positions(self) -> np.ndarray:
        return self.vertices[:, :3]

    @property
    def normals(self) -> np.ndarray:
        return self.vertices[:, 3:6]


@dataclass(frozen=True)
class DeformationData:
    weights: np.ndarray
    selection_influences: np.ndarray
    centerline: np.ndarray


@dataclass(frozen=True)
class CoordinateTopology:
    coordinate_ids: np.ndarray
    coordinate_members: tuple[np.ndarray, ...]
    bind_coordinates: np.ndarray
    crease_chains: tuple[tuple[np.ndarray, bool], ...]


def read_mesh(path: Path) -> Mesh:
    data = path.read_bytes()
    header = struct.unpack_from("<4s10i", data)
    magic, version, floats_per_vertex, vertex_count, index_count, *_ = header
    if magic != b"V3MB":
        raise ValueError(f"{path}: invalid V3 mesh magic {magic!r}")
    if version not in {1, 2} or floats_per_vertex != FLOATS_PER_VERTEX:
        raise ValueError(
            f"{path}: unsupported version/layout {version}/{floats_per_vertex}"
        )
    vertex_offset = struct.calcsize("<4s10i")
    vertex_float_count = vertex_count * floats_per_vertex
    vertices = np.frombuffer(
        data,
        dtype="<f4",
        count=vertex_float_count,
        offset=vertex_offset,
    ).astype(np.float64).reshape(vertex_count, floats_per_vertex)
    index_offset = vertex_offset + vertex_float_count * 4
    indices = np.frombuffer(
        data,
        dtype="<i4",
        count=index_count,
        offset=index_offset,
    ).astype(np.int64)
    if index_offset + index_count * 4 != len(data):
        raise ValueError(f"{path}: unexpected binary size")
    return Mesh(vertices=vertices, indices=indices)


def read_deformation(path: Path, expected_vertices: int) -> DeformationData:
    data = path.read_bytes()
    magic, version, vertex_count, influence_count = struct.unpack_from("<4s3i", data)
    if magic != b"V3DF" or version != 3:
        raise ValueError(f"{path}: expected V3DF version 3, got {magic!r}/{version}")
    if vertex_count != expected_vertices or influence_count != INFLUENCE_COUNT:
        raise ValueError(
            f"{path}: expected {expected_vertices}x{INFLUENCE_COUNT}, "
            f"got {vertex_count}x{influence_count}"
        )
    header_size = struct.calcsize("<4s3i")
    weight_count = vertex_count * influence_count
    weights = np.frombuffer(
        data,
        dtype="<f4",
        count=weight_count,
        offset=header_size,
    ).astype(np.float64).reshape(vertex_count, influence_count)
    selection_offset = header_size + weight_count * 4
    selection = np.frombuffer(
        data,
        dtype="<i4",
        count=vertex_count,
        offset=selection_offset,
    ).astype(np.int64)
    node_count_offset = selection_offset + vertex_count * 4
    node_count = struct.unpack_from("<i", data, node_count_offset)[0]
    centerline = np.frombuffer(
        data,
        dtype="<f4",
        count=node_count * 3,
        offset=node_count_offset + 4,
    ).astype(np.float64).reshape(node_count, 3)
    expected_size = node_count_offset + 4 + node_count * 3 * 4
    if expected_size != len(data):
        raise ValueError(f"{path}: unexpected deformation binary size")
    return DeformationData(weights=weights, selection_influences=selection, centerline=centerline)


def normalize(vectors: np.ndarray, fallback: tuple[float, ...] | None = None) -> np.ndarray:
    vectors = np.asarray(vectors, dtype=np.float64)
    lengths = np.linalg.norm(vectors, axis=-1, keepdims=True)
    result = np.divide(vectors, lengths, out=np.zeros_like(vectors), where=lengths > 1.0e-6)
    if fallback is not None:
        result = np.where(lengths > 1.0e-6, result, np.asarray(fallback, dtype=np.float64))
    return result


def rotation_matrix(angle_degrees: float, axis: Iterable[float]) -> np.ndarray:
    axis_array = normalize(np.asarray(tuple(axis), dtype=np.float64), (1.0, 0.0, 0.0))
    x, y, z = axis_array
    angle = math.radians(angle_degrees)
    cosine = math.cos(angle)
    sine = math.sin(angle)
    one_minus = 1.0 - cosine
    matrix = np.eye(4, dtype=np.float64)
    matrix[:3, :3] = np.array(
        (
            (cosine + x * x * one_minus, x * y * one_minus - z * sine, x * z * one_minus + y * sine),
            (y * x * one_minus + z * sine, cosine + y * y * one_minus, y * z * one_minus - x * sine),
            (z * x * one_minus - y * sine, z * y * one_minus + x * sine, cosine + z * z * one_minus),
        ),
        dtype=np.float64,
    )
    return matrix


def translation_matrix(offset: np.ndarray) -> np.ndarray:
    matrix = np.eye(4, dtype=np.float64)
    matrix[:3, 3] = offset
    return matrix


def around_pivot(rotation: np.ndarray, pivot: np.ndarray) -> np.ndarray:
    return translation_matrix(pivot) @ rotation @ translation_matrix(-pivot)


def rotate_big_finger_first_axis(angle: float) -> np.ndarray:
    return (
        rotation_matrix(TOUCH_X_CORRECTION_DEGREES, (1.0, 0.0, 0.0))
        @ rotation_matrix(angle, (0.0, 0.0, -1.0))
        @ rotation_matrix(-TOUCH_X_CORRECTION_DEGREES, (1.0, 0.0, 0.0))
    )


def rotate_big_finger_second_axis(angle: float) -> np.ndarray:
    return (
        rotation_matrix(TOUCH_X_CORRECTION_DEGREES, (1.0, 0.0, 0.0))
        @ rotation_matrix(angle, (1.0, 0.0, 0.0))
        @ rotation_matrix(-TOUCH_X_CORRECTION_DEGREES, (1.0, 0.0, 0.0))
    )


def thumb_anchor_relative_matrix(
    first_axis_angle: float,
    second_axis_angle: float,
    mirrored: bool,
) -> np.ndarray:
    mirror = np.diag((1.0, -1.0, 1.0, 1.0)) if mirrored else np.eye(4)
    first_accumulated_angle = -first_axis_angle if mirrored else first_axis_angle
    second_accumulated_angle = second_axis_angle - SECOND_AXIS_BIND
    if mirrored:
        second_accumulated_angle = -second_accumulated_angle
    first_pivot = DELTA_Y_PIVOT.copy()
    second_pivot = DELTA_X_PIVOT.copy()
    if mirrored:
        first_pivot[1] = -first_pivot[1]
        second_pivot[1] = -second_pivot[1]
    model = mirror.copy()
    model = around_pivot(
        rotate_big_finger_first_axis(first_accumulated_angle), first_pivot
    ) @ model
    model = around_pivot(
        rotate_big_finger_second_axis(second_accumulated_angle), second_pivot
    ) @ model
    return np.linalg.inv(mirror) @ model


def thumb_skin_matrix(
    first_axis_angle: float,
    second_axis_angle: float,
    mirrored: bool,
) -> np.ndarray:
    current = thumb_anchor_relative_matrix(first_axis_angle, second_axis_angle, mirrored)
    bind = thumb_anchor_relative_matrix(0.0, SECOND_AXIS_BIND, mirrored)
    return current @ np.linalg.inv(bind)


def matrix_to_quaternion(matrix: np.ndarray) -> np.ndarray:
    x_axis = normalize(matrix[:3, 0], (1.0, 0.0, 0.0))
    y_axis = matrix[:3, 1] - x_axis * np.dot(x_axis, matrix[:3, 1])
    y_axis = normalize(y_axis, (0.0, 1.0, 0.0))
    z_axis = np.cross(x_axis, y_axis)
    x0, x1, x2 = x_axis
    y0, y1, y2 = y_axis
    z0, z1, z2 = z_axis
    trace = x0 + y1 + z2
    if trace > 0.0:
        scale = math.sqrt(trace + 1.0) * 2.0
        quaternion = np.array(
            ((y2 - z1) / scale, (z0 - x2) / scale, (x1 - y0) / scale, 0.25 * scale)
        )
    elif x0 > y1 and x0 > z2:
        scale = math.sqrt(1.0 + x0 - y1 - z2) * 2.0
        quaternion = np.array(
            (0.25 * scale, (y0 + x1) / scale, (z0 + x2) / scale, (y2 - z1) / scale)
        )
    elif y1 > z2:
        scale = math.sqrt(1.0 + y1 - x0 - z2) * 2.0
        quaternion = np.array(
            ((y0 + x1) / scale, 0.25 * scale, (z1 + y2) / scale, (z0 - x2) / scale)
        )
    else:
        scale = math.sqrt(1.0 + z2 - x0 - y1) * 2.0
        quaternion = np.array(
            ((z0 + x2) / scale, (z1 + y2) / scale, 0.25 * scale, (x1 - y0) / scale)
        )
    return normalize(quaternion, (0.0, 0.0, 0.0, 1.0))


def quaternion_rotate(quaternions: np.ndarray, vectors: np.ndarray) -> np.ndarray:
    quaternions = np.asarray(quaternions, dtype=np.float64)
    vectors = np.asarray(vectors, dtype=np.float64)
    xyz = quaternions[..., :3]
    scalar = quaternions[..., 3:4]
    tangent = 2.0 * np.cross(xyz, vectors)
    return vectors + scalar * tangent + np.cross(xyz, tangent)


def nlerp(first: np.ndarray, second: np.ndarray, amount: np.ndarray) -> np.ndarray:
    first = np.asarray(first, dtype=np.float64)
    second = np.asarray(second, dtype=np.float64)
    amount = np.asarray(amount, dtype=np.float64)[..., None]
    sign = np.where(np.sum(first * second, axis=-1, keepdims=True) < 0.0, -1.0, 1.0)
    return normalize(first + (second * sign - first) * amount, (0.0, 0.0, 0.0, 1.0))


def shortest_arc_quaternion(first: np.ndarray, second: np.ndarray) -> np.ndarray:
    first = normalize(first, (1.0, 0.0, 0.0))
    second = normalize(second, (1.0, 0.0, 0.0))
    dot = np.clip(np.sum(first * second, axis=-1), -1.0, 1.0)
    cross = np.cross(first, second)
    quaternion = np.concatenate((cross, (1.0 + dot)[..., None]), axis=-1)
    opposite = dot < -0.999999
    if np.any(opposite):
        source = first[opposite]
        candidate = np.cross(source, np.array((1.0, 0.0, 0.0)))
        use_y = np.linalg.norm(candidate, axis=1) <= 1.0e-6
        candidate[use_y] = np.cross(source[use_y], np.array((0.0, 1.0, 0.0)))
        quaternion[opposite, :3] = normalize(candidate, (0.0, 0.0, 1.0))
        quaternion[opposite, 3] = 0.0
    return normalize(quaternion, (0.0, 0.0, 0.0, 1.0))


def transform_points(matrix: np.ndarray, points: np.ndarray) -> np.ndarray:
    return points @ matrix[:3, :3].T + matrix[:3, 3]


def transform_directions(matrix: np.ndarray, directions: np.ndarray) -> np.ndarray:
    return normalize(directions @ matrix[:3, :3].T, (1.0, 0.0, 0.0))


def compute_polyline_tangents(centers: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    segment_vectors = centers[1:] - centers[:-1]
    segment_lengths = np.linalg.norm(segment_vectors, axis=1)
    tangents = np.empty_like(centers)
    tangents[0] = segment_vectors[0]
    tangents[-1] = segment_vectors[-1]
    previous = normalize(segment_vectors[:-1], (1.0, 0.0, 0.0))
    following = normalize(segment_vectors[1:], (1.0, 0.0, 0.0))
    tangents[1:-1] = previous + following
    return normalize(tangents, (1.0, 0.0, 0.0)), segment_lengths


def evaluate_guide(
    start: np.ndarray,
    start_tangent: np.ndarray,
    start_handle: float,
    end: np.ndarray,
    end_tangent: np.ndarray,
    end_handle: float,
    progress: np.ndarray,
) -> np.ndarray:
    progress = np.clip(np.asarray(progress, dtype=np.float64), 0.0, 1.0)
    remaining = 1.0 - progress
    start_control = start + start_tangent * start_handle
    end_control = end - end_tangent * end_handle
    return (
        start * (remaining**3)[..., None]
        + start_control * (3.0 * remaining**2 * progress)[..., None]
        + end_control * (3.0 * remaining * progress**2)[..., None]
        + end * (progress**3)[..., None]
    )


def smooth_segment_scales(scales: np.ndarray, passes: int) -> np.ndarray:
    result = scales.copy()
    for _ in range(passes):
        previous = np.concatenate((result[:1], result[:-1]))
        following = np.concatenate((result[1:], result[-1:]))
        result = previous * 0.25 + result * 0.5 + following * 0.25
    return result


def smoothstep(value: np.ndarray) -> np.ndarray:
    clamped = np.clip(value, 0.0, 1.0)
    return clamped * clamped * (3.0 - 2.0 * clamped)


def anchor_step(value: np.ndarray) -> np.ndarray:
    clamped = np.clip(value, 0.0, 1.0)
    return smoothstep(clamped) if USE_SMOOTHSTEP_ANCHOR_BLEND else clamped


def node_scale(segment_scales: np.ndarray, node_indexes: np.ndarray) -> np.ndarray:
    node_indexes = np.asarray(node_indexes, dtype=np.int64)
    result = np.empty(node_indexes.shape, dtype=np.float64)
    lower = node_indexes <= 0
    upper = node_indexes >= len(segment_scales)
    middle = ~(lower | upper)
    result[lower] = segment_scales[0]
    result[upper] = segment_scales[-1]
    result[middle] = (
        segment_scales[node_indexes[middle] - 1] + segment_scales[node_indexes[middle]]
    ) * 0.5
    return result


class VolumeRodDeformer:
    def __init__(self, deformation: DeformationData):
        self.deformation = deformation
        self.centerline = deformation.centerline
        self.rest_tangents, self.rest_segment_lengths = compute_polyline_tangents(self.centerline)
        self.total_rest_length = float(np.sum(self.rest_segment_lengths))
        self.rest_chord_length = float(np.linalg.norm(self.centerline[-1] - self.centerline[0]))

    def prepare(self, top_matrix: np.ndarray) -> dict[str, np.ndarray | float]:
        bottom_matrix = np.eye(4, dtype=np.float64)
        bottom_quaternion = matrix_to_quaternion(bottom_matrix)
        top_quaternion = matrix_to_quaternion(top_matrix)
        if np.dot(bottom_quaternion, top_quaternion) < 0.0:
            top_quaternion = -top_quaternion

        guide_start = transform_points(bottom_matrix, self.centerline[:1])[0]
        guide_end = transform_points(top_matrix, self.centerline[-1:])[0]
        guide_start_tangent = normalize(
            quaternion_rotate(bottom_quaternion, self.rest_tangents[0]),
            (1.0, 0.0, 0.0),
        )
        guide_end_tangent = normalize(
            quaternion_rotate(top_quaternion, self.rest_tangents[-1]),
            (1.0, 0.0, 0.0),
        )
        current_chord_length = float(np.linalg.norm(guide_end - guide_start))
        handle_scale = (
            float(np.clip(current_chord_length / self.rest_chord_length, MIN_HANDLE_SCALE, MAX_HANDLE_SCALE))
            if self.rest_chord_length > 1.0e-6
            else 1.0
        )
        rest_palm_handle = self.total_rest_length * PALM_HANDLE_RATIO
        rest_finger_handle = self.total_rest_length * FINGER_HANDLE_RATIO
        current_palm_handle = rest_palm_handle * handle_scale
        current_finger_handle = rest_finger_handle * handle_scale

        node_progress = np.linspace(0.0, 1.0, len(self.centerline))
        bottom_quaternions = np.broadcast_to(bottom_quaternion, (len(node_progress), 4))
        top_quaternions = np.broadcast_to(top_quaternion, (len(node_progress), 4))
        node_rotations = nlerp(bottom_quaternions, top_quaternions, node_progress)
        rest_guide = evaluate_guide(
            self.centerline[0],
            self.rest_tangents[0],
            rest_palm_handle,
            self.centerline[-1],
            self.rest_tangents[-1],
            rest_finger_handle,
            node_progress,
        )
        current_guide = evaluate_guide(
            guide_start,
            guide_start_tangent,
            current_palm_handle,
            guide_end,
            guide_end_tangent,
            current_finger_handle,
            node_progress,
        )
        current_centers = current_guide + quaternion_rotate(
            node_rotations, self.centerline - rest_guide
        )
        current_tangents, current_segment_lengths = compute_polyline_tangents(current_centers)
        axial_scales = np.clip(
            np.divide(
                current_segment_lengths,
                self.rest_segment_lengths,
                out=np.ones_like(current_segment_lengths),
                where=self.rest_segment_lengths > 1.0e-6,
            ),
            MIN_AXIAL_SCALE,
            MAX_AXIAL_SCALE,
        )
        passes = (
            COMPRESSION_SMOOTHING_PASSES
            if current_chord_length < self.rest_chord_length
            else STRETCH_SMOOTHING_PASSES
        )
        axial_scales = smooth_segment_scales(axial_scales, passes)
        volume_scales = np.sqrt(1.0 / axial_scales)
        compressed = axial_scales < 1.0
        volume_scales[compressed] = np.minimum(
            1.0 + (volume_scales[compressed] - 1.0) * COMPRESSION_RADIAL_GAIN,
            MAX_COMPRESSION_RADIAL_SCALE,
        )
        radial_scales = np.maximum(MIN_RADIAL_SCALE, volume_scales)
        return {
            "top_matrix": top_matrix,
            "node_rotations": node_rotations,
            "current_centers": current_centers,
            "current_tangents": current_tangents,
            "axial_scales": axial_scales,
            "radial_scales": radial_scales,
            "current_chord_length": current_chord_length,
        }

    def frames(self, progress: np.ndarray, runtime: dict[str, np.ndarray | float]) -> dict[str, np.ndarray]:
        progress = np.asarray(progress, dtype=np.float64)
        node_position = progress * (len(self.centerline) - 1)
        segment = np.minimum(np.floor(node_position).astype(np.int64), len(self.centerline) - 2)
        amount = node_position - segment
        following = segment + 1
        current_centers = runtime["current_centers"]
        current_tangents = runtime["current_tangents"]
        node_rotations = runtime["node_rotations"]
        assert isinstance(current_centers, np.ndarray)
        assert isinstance(current_tangents, np.ndarray)
        assert isinstance(node_rotations, np.ndarray)
        rest_center = self.centerline[segment] + (
            self.centerline[following] - self.centerline[segment]
        ) * amount[:, None]
        current_center = current_centers[segment] + (
            current_centers[following] - current_centers[segment]
        ) * amount[:, None]
        rest_tangent = normalize(
            self.rest_tangents[segment]
            + (self.rest_tangents[following] - self.rest_tangents[segment]) * amount[:, None],
            (1.0, 0.0, 0.0),
        )
        current_tangent = normalize(
            current_tangents[segment]
            + (current_tangents[following] - current_tangents[segment]) * amount[:, None],
            (1.0, 0.0, 0.0),
        )
        curvature_normal = current_tangents[following] - current_tangents[segment]
        curvature_normal -= current_tangent * np.sum(curvature_normal * current_tangent, axis=1)[:, None]
        curvature_normal = normalize(curvature_normal)

        vertex_rotation = nlerp(
            node_rotations[segment], node_rotations[following], amount
        )
        reference_tangent = normalize(
            quaternion_rotate(vertex_rotation, rest_tangent), (1.0, 0.0, 0.0)
        )
        frame_alignment = shortest_arc_quaternion(reference_tangent, current_tangent)
        tangent_dot = np.clip(np.sum(reference_tangent * current_tangent, axis=1), -1.0, 1.0)
        bend_normal_raw = reference_tangent - current_tangent * tangent_dot[:, None]
        bend_amount = np.linalg.norm(bend_normal_raw, axis=1)
        bend_normal = normalize(bend_normal_raw)

        axial_scales = runtime["axial_scales"]
        radial_scales = runtime["radial_scales"]
        assert isinstance(axial_scales, np.ndarray)
        assert isinstance(radial_scales, np.ndarray)
        start_axial = node_scale(axial_scales, segment)
        end_axial = node_scale(axial_scales, following)
        start_radial = node_scale(radial_scales, segment)
        end_radial = node_scale(radial_scales, following)
        anchor_blend = np.minimum(
            anchor_step(progress / PALM_ANCHOR_BLEND),
            anchor_step((1.0 - progress) / FINGER_ANCHOR_BLEND),
        )
        axial = start_axial + (end_axial - start_axial) * amount
        radial = start_radial + (end_radial - start_radial) * amount
        frame_axial = 1.0 + (axial - 1.0) * anchor_blend * AXIAL_SCALE_STRENGTH
        frame_radial = 1.0 + (radial - 1.0) * anchor_blend
        strain_blend = np.minimum(
            smoothstep(progress / PALM_STRAIN_BLEND),
            smoothstep((1.0 - progress) / FINGER_STRAIN_BLEND),
        )
        requested_strain = 1.0 + BENDING_STRAIN_GAIN * bend_amount * strain_blend
        maximum_combined = np.where(
            axial < 1.0,
            MAX_COMPRESSION_COMBINED_RADIAL_SCALE,
            MAX_COMBINED_RADIAL_SCALE,
        )
        maximum_strain = np.maximum(1.0, maximum_combined / frame_radial)
        frame_strain = np.minimum(requested_strain, maximum_strain)
        return {
            "progress": progress,
            "rest_center": rest_center,
            "current_center": current_center,
            "rest_tangent": rest_tangent,
            "current_tangent": current_tangent,
            "curvature_normal": curvature_normal,
            "vertex_rotation": vertex_rotation,
            "frame_alignment": frame_alignment,
            "bend_normal": bend_normal,
            "frame_axial": frame_axial,
            "frame_radial": frame_radial,
            "anchor_blend": anchor_blend,
            "frame_strain": frame_strain,
        }

    def deform_positions_with_progress(
        self,
        positions: np.ndarray,
        progress: np.ndarray,
        top_matrix: np.ndarray,
        runtime: dict[str, np.ndarray | float] | None = None,
    ) -> np.ndarray:
        progress = np.clip(np.asarray(progress, dtype=np.float64), 0.0, 1.0)
        output_positions = np.empty_like(positions)
        bottom = progress <= WEIGHT_EPSILON
        top = progress >= 1.0 - WEIGHT_EPSILON
        middle = ~(bottom | top)
        output_positions[bottom] = positions[bottom]
        output_positions[top] = transform_points(top_matrix, positions[top])
        if not np.any(middle):
            return output_positions

        if runtime is None:
            runtime = self.prepare(top_matrix)
        frames = self.frames(progress[middle], runtime)
        source_positions = positions[middle]
        rest_center = frames["rest_center"]
        rest_tangent = frames["rest_tangent"]
        current_center = frames["current_center"]
        current_tangent = frames["current_tangent"]
        vertex_rotation = frames["vertex_rotation"]
        frame_alignment = frames["frame_alignment"]
        bend_normal = frames["bend_normal"]
        curvature_normal = frames["curvature_normal"]

        offsets = source_positions - rest_center
        axial_offset = np.sum(offsets * rest_tangent, axis=1)
        radial = offsets - rest_tangent * axial_offset[:, None]
        radial_length = np.linalg.norm(radial, axis=1)
        rotated_radial = quaternion_rotate(vertex_rotation, radial)
        if USE_SHORTEST_ARC_FRAME_ALIGNMENT:
            rotated_radial = quaternion_rotate(frame_alignment, rotated_radial)
            rotated_radial = np.where(
                (radial_length > 1.0e-6)[:, None], rotated_radial, 0.0
            )
        else:
            rotated_radial -= current_tangent * np.sum(
                rotated_radial * current_tangent, axis=1
            )[:, None]
            rotated_length = np.linalg.norm(rotated_radial, axis=1)
            valid_radial = (radial_length > 1.0e-6) & (rotated_length > 1.0e-6)
            rotated_radial = np.divide(
                rotated_radial * radial_length[:, None],
                rotated_length[:, None],
                out=np.zeros_like(rotated_radial),
                where=valid_radial[:, None],
            )
        base_inner_offset = np.sum(rotated_radial * curvature_normal, axis=1)
        rotated_radial *= frames["frame_radial"][:, None]
        bend_component = np.sum(rotated_radial * bend_normal, axis=1)
        rotated_radial += bend_normal * (
            bend_component * (frames["frame_strain"] - 1.0)
        )[:, None]
        inner_offset = np.sum(rotated_radial * curvature_normal, axis=1)
        maximum_inner_offset = base_inner_offset * INNER_EXPANSION_LIMIT_FACTOR
        inner_correction = np.where(
            (base_inner_offset > 0.0) & (inner_offset > maximum_inner_offset),
            inner_offset - maximum_inner_offset,
            0.0,
        )
        rotated_radial -= curvature_normal * inner_correction[:, None]
        rod_positions = (
            current_center
            + current_tangent
            * (axial_offset * frames["frame_axial"])[:, None]
            + rotated_radial
        )

        anchor_blend = (
            frames["anchor_blend"]
            if BLEND_ROD_WITH_RIGID_ANCHORS
            else np.ones_like(progress[middle])
        )
        anchor_is_top = progress[middle] >= 0.5
        anchor_positions = source_positions.copy()
        if np.any(anchor_is_top):
            anchor_positions[anchor_is_top] = transform_points(
                top_matrix, source_positions[anchor_is_top]
            )
        output_positions[middle] = anchor_positions + (
            rod_positions - anchor_positions
        ) * anchor_blend[:, None]
        return output_positions

    def deform(self, positions: np.ndarray, normals: np.ndarray, top_matrix: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
        progress = np.clip(self.deformation.weights[:, THUMB_INFLUENCE], 0.0, 1.0)
        output_positions = np.empty_like(positions)
        output_normals = np.empty_like(normals)
        bottom = progress <= WEIGHT_EPSILON
        top = progress >= 1.0 - WEIGHT_EPSILON
        middle = ~(bottom | top)
        output_positions[bottom] = positions[bottom]
        output_normals[bottom] = normalize(normals[bottom], (1.0, 0.0, 0.0))
        output_positions[top] = transform_points(top_matrix, positions[top])
        output_normals[top] = transform_directions(top_matrix, normals[top])
        if not np.any(middle):
            return output_positions, output_normals

        runtime = self.prepare(top_matrix)
        frames = self.frames(progress[middle], runtime)
        source_positions = positions[middle]
        source_normals = normals[middle]
        rest_center = frames["rest_center"]
        rest_tangent = frames["rest_tangent"]
        current_center = frames["current_center"]
        current_tangent = frames["current_tangent"]
        vertex_rotation = frames["vertex_rotation"]
        frame_alignment = frames["frame_alignment"]
        bend_normal = frames["bend_normal"]
        curvature_normal = frames["curvature_normal"]

        offsets = source_positions - rest_center
        axial_offset = np.sum(offsets * rest_tangent, axis=1)
        radial = offsets - rest_tangent * axial_offset[:, None]
        radial_length = np.linalg.norm(radial, axis=1)
        rotated_radial = quaternion_rotate(vertex_rotation, radial)
        if USE_SHORTEST_ARC_FRAME_ALIGNMENT:
            rotated_radial = quaternion_rotate(frame_alignment, rotated_radial)
            rotated_radial = np.where(
                (radial_length > 1.0e-6)[:, None], rotated_radial, 0.0
            )
        else:
            rotated_radial -= current_tangent * np.sum(rotated_radial * current_tangent, axis=1)[:, None]
            rotated_length = np.linalg.norm(rotated_radial, axis=1)
            valid_radial = (radial_length > 1.0e-6) & (rotated_length > 1.0e-6)
            rotated_radial = np.divide(
                rotated_radial * radial_length[:, None],
                rotated_length[:, None],
                out=np.zeros_like(rotated_radial),
                where=valid_radial[:, None],
            )
        base_inner_offset = np.sum(rotated_radial * curvature_normal, axis=1)
        rotated_radial *= frames["frame_radial"][:, None]
        bend_component = np.sum(rotated_radial * bend_normal, axis=1)
        rotated_radial += bend_normal * (
            bend_component * (frames["frame_strain"] - 1.0)
        )[:, None]
        inner_offset = np.sum(rotated_radial * curvature_normal, axis=1)
        maximum_inner_offset = base_inner_offset * INNER_EXPANSION_LIMIT_FACTOR
        inner_correction = np.where(
            (base_inner_offset > 0.0) & (inner_offset > maximum_inner_offset),
            inner_offset - maximum_inner_offset,
            0.0,
        )
        rotated_radial -= curvature_normal * inner_correction[:, None]
        rod_positions = (
            current_center
            + current_tangent * (axial_offset * frames["frame_axial"])[:, None]
            + rotated_radial
        )

        normal_axial = np.sum(source_normals * rest_tangent, axis=1)
        normal_radial = source_normals - rest_tangent * normal_axial[:, None]
        normal_radial_length = np.linalg.norm(normal_radial, axis=1)
        transformed_normal_radial = quaternion_rotate(vertex_rotation, normal_radial)
        if USE_SHORTEST_ARC_FRAME_ALIGNMENT:
            transformed_normal_radial = quaternion_rotate(
                frame_alignment, transformed_normal_radial
            )
            transformed_normal_length = normal_radial_length
        else:
            transformed_normal_radial -= current_tangent * np.sum(
                transformed_normal_radial * current_tangent, axis=1
            )[:, None]
            transformed_normal_length = np.linalg.norm(transformed_normal_radial, axis=1)
        valid_normal = (normal_radial_length > 1.0e-6) & (transformed_normal_length > 1.0e-6)
        inverse_radial = 1.0 / frames["frame_radial"]
        transformed_normal_radial = np.divide(
            transformed_normal_radial
            * (normal_radial_length * inverse_radial)[:, None],
            transformed_normal_length[:, None],
            out=np.zeros_like(transformed_normal_radial),
            where=valid_normal[:, None],
        )
        normal_bend_component = np.sum(transformed_normal_radial * bend_normal, axis=1)
        transformed_normal_radial += bend_normal * (
            normal_bend_component * (1.0 / frames["frame_strain"] - 1.0)
        )[:, None]
        rod_normals = normalize(
            current_tangent * (normal_axial / frames["frame_axial"])[:, None]
            + transformed_normal_radial,
            (1.0, 0.0, 0.0),
        )

        anchor_blend = (
            frames["anchor_blend"]
            if BLEND_ROD_WITH_RIGID_ANCHORS
            else np.ones_like(progress[middle])
        )
        anchor_is_top = progress[middle] >= 0.5
        anchor_positions = source_positions.copy()
        anchor_normals = normalize(source_normals, (1.0, 0.0, 0.0))
        if np.any(anchor_is_top):
            anchor_positions[anchor_is_top] = transform_points(
                top_matrix, source_positions[anchor_is_top]
            )
            anchor_normals[anchor_is_top] = transform_directions(
                top_matrix, source_normals[anchor_is_top]
            )
        blended_positions = anchor_positions + (
            rod_positions - anchor_positions
        ) * anchor_blend[:, None]
        blended_normals = normalize(
            anchor_normals + (rod_normals - anchor_normals) * anchor_blend[:, None],
            (1.0, 0.0, 0.0),
        )
        output_positions[middle] = blended_positions
        output_normals[middle] = blended_normals
        return output_positions, output_normals


def triangle_geometry(positions: np.ndarray, triangles: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    points = positions[triangles]
    first = points[:, 1] - points[:, 0]
    second = points[:, 2] - points[:, 0]
    cross = np.cross(first, second)
    area = np.linalg.norm(cross, axis=1) * 0.5
    edges = np.stack(
        (
            np.linalg.norm(points[:, 1] - points[:, 0], axis=1),
            np.linalg.norm(points[:, 2] - points[:, 1], axis=1),
            np.linalg.norm(points[:, 0] - points[:, 2], axis=1),
        ),
        axis=1,
    )
    return cross, area, edges


def recalculate_surface_normals(
    positions: np.ndarray,
    triangles: np.ndarray,
) -> np.ndarray:
    points = positions[triangles]
    face_normals = normalize(
        np.cross(points[:, 1] - points[:, 0], points[:, 2] - points[:, 0])
    )
    normal_sums = np.zeros_like(positions)
    for corner in range(3):
        np.add.at(normal_sums, triangles[:, corner], face_normals)
    return normalize(normal_sums, (1.0, 0.0, 0.0))


def local_surface_jacobian_normals(
    mesh: Mesh,
    deformer: VolumeRodDeformer,
    triangles: np.ndarray,
    top_matrix: np.ndarray,
) -> tuple[np.ndarray, np.ndarray]:
    points = mesh.positions[triangles]
    progress = np.clip(
        deformer.deformation.weights[:, THUMB_INFLUENCE], 0.0, 1.0
    )[triangles]
    centroids = np.mean(points, axis=1)
    centroid_progress = np.mean(progress, axis=1)
    first_direction = points[:, 1] - points[:, 0]
    second_direction = points[:, 2] - points[:, 0]
    first_progress_direction = progress[:, 1] - progress[:, 0]
    second_progress_direction = progress[:, 2] - progress[:, 0]
    epsilon = 1.0e-4
    sample_positions = np.concatenate(
        (
            centroids + first_direction * epsilon,
            centroids - first_direction * epsilon,
            centroids + second_direction * epsilon,
            centroids - second_direction * epsilon,
        ),
        axis=0,
    )
    sample_progress = np.concatenate(
        (
            centroid_progress + first_progress_direction * epsilon,
            centroid_progress - first_progress_direction * epsilon,
            centroid_progress + second_progress_direction * epsilon,
            centroid_progress - second_progress_direction * epsilon,
        )
    )
    runtime = deformer.prepare(top_matrix)
    samples = deformer.deform_positions_with_progress(
        sample_positions, sample_progress, top_matrix, runtime
    ).reshape(4, len(triangles), 3)
    first_derivative = (samples[0] - samples[1]) / (2.0 * epsilon)
    second_derivative = (samples[2] - samples[3]) / (2.0 * epsilon)
    jacobian_cross = np.cross(first_derivative, second_derivative)
    bind_cross = np.cross(first_direction, second_direction)
    jacobian_area_ratio = np.divide(
        np.linalg.norm(jacobian_cross, axis=1),
        np.linalg.norm(bind_cross, axis=1),
        out=np.ones(len(triangles), dtype=np.float64),
        where=np.linalg.norm(bind_cross, axis=1) > 1.0e-12,
    )
    return normalize(jacobian_cross), jacobian_area_ratio


def build_coordinate_topology(mesh: Mesh, crease_angle_degrees: float = 45.0) -> CoordinateTopology:
    rounded = np.round(mesh.positions, 6)
    bind_coordinates, coordinate_ids = np.unique(rounded, axis=0, return_inverse=True)
    coordinate_members = tuple(
        np.flatnonzero(coordinate_ids == coordinate_id)
        for coordinate_id in range(len(bind_coordinates))
    )
    triangles = mesh.indices.reshape(-1, 3)
    coordinate_triangles = coordinate_ids[triangles]
    cross, area, _ = triangle_geometry(mesh.positions, triangles)
    triangle_normals = normalize(cross)
    edge_triangles: dict[tuple[int, int], list[int]] = {}
    for triangle_index, triangle in enumerate(coordinate_triangles):
        for first, second in ((triangle[0], triangle[1]), (triangle[1], triangle[2]), (triangle[2], triangle[0])):
            if first == second:
                continue
            edge = (int(min(first, second)), int(max(first, second)))
            edge_triangles.setdefault(edge, []).append(triangle_index)
    threshold = math.cos(math.radians(crease_angle_degrees))
    crease_edges: set[tuple[int, int]] = set()
    for edge, adjacent in edge_triangles.items():
        valid = [index for index in adjacent if area[index] > 1.0e-12]
        if len(valid) != 2:
            continue
        if float(np.dot(triangle_normals[valid[0]], triangle_normals[valid[1]])) <= threshold:
            crease_edges.add(edge)
    adjacency: dict[int, set[int]] = {}
    for first, second in crease_edges:
        adjacency.setdefault(first, set()).add(second)
        adjacency.setdefault(second, set()).add(first)
    remaining = set(crease_edges)
    chains: list[tuple[np.ndarray, bool]] = []

    def consume(start: int, following: int) -> list[int]:
        chain = [start, following]
        remaining.discard((min(start, following), max(start, following)))
        previous, current = start, following
        while len(adjacency[current]) == 2:
            candidates = [
                neighbor
                for neighbor in adjacency[current]
                if neighbor != previous
                and (min(current, neighbor), max(current, neighbor)) in remaining
            ]
            if not candidates:
                break
            following_vertex = min(candidates)
            remaining.discard((min(current, following_vertex), max(current, following_vertex)))
            chain.append(following_vertex)
            previous, current = current, following_vertex
        return chain

    for start in sorted(adjacency):
        if len(adjacency[start]) == 2:
            continue
        for following in sorted(adjacency[start]):
            edge = (min(start, following), max(start, following))
            if edge not in remaining:
                continue
            chain = consume(start, following)
            if len(chain) >= 20:
                chains.append((np.asarray(chain, dtype=np.int64), False))
    while remaining:
        start, following = min(remaining)
        chain = consume(start, following)
        is_loop = len(chain) > 2 and chain[-1] == chain[0]
        if is_loop:
            chain = chain[:-1]
        if len(chain) >= 20:
            chains.append((np.asarray(chain, dtype=np.int64), is_loop))
    return CoordinateTopology(
        coordinate_ids=coordinate_ids,
        coordinate_members=coordinate_members,
        bind_coordinates=bind_coordinates,
        crease_chains=tuple(chains),
    )


def collapsed_coordinates(positions: np.ndarray, topology: CoordinateTopology) -> np.ndarray:
    result = np.empty_like(topology.bind_coordinates)
    for coordinate_id, members in enumerate(topology.coordinate_members):
        result[coordinate_id] = np.mean(positions[members], axis=0)
    return result


def duplicate_seam_metrics(positions: np.ndarray, topology: CoordinateTopology) -> tuple[float, int]:
    maximum = 0.0
    worst_coordinate = -1
    for coordinate_id, members in enumerate(topology.coordinate_members):
        if len(members) < 2:
            continue
        points = positions[members]
        spread = float(np.max(np.linalg.norm(points[:, None] - points[None, :], axis=2)))
        if spread > maximum:
            maximum = spread
            worst_coordinate = coordinate_id
    return maximum, worst_coordinate


def chain_metrics(
    deformed_coordinates: np.ndarray,
    topology: CoordinateTopology,
) -> dict[str, float | int]:
    worst_scale_jump = 0.0
    worst_turn_delta = 0.0
    worst_laplacian_delta = 0.0
    worst_chain = -1
    for chain_index, (chain, is_loop) in enumerate(topology.crease_chains):
        bind = topology.bind_coordinates[chain]
        current = deformed_coordinates[chain]
        if is_loop:
            bind_edges = np.roll(bind, -1, axis=0) - bind
            current_edges = np.roll(current, -1, axis=0) - current
        else:
            bind_edges = bind[1:] - bind[:-1]
            current_edges = current[1:] - current[:-1]
        bind_lengths = np.linalg.norm(bind_edges, axis=1)
        current_lengths = np.linalg.norm(current_edges, axis=1)
        scales = np.divide(
            current_lengths,
            bind_lengths,
            out=np.ones_like(current_lengths),
            where=bind_lengths > 1.0e-9,
        )
        if len(scales) > 1:
            jumps = np.abs(np.diff(np.log(np.maximum(scales, 1.0e-9))))
            if is_loop:
                jumps = np.append(jumps, abs(math.log(max(scales[0], 1.0e-9) / max(scales[-1], 1.0e-9))))
            scale_jump = float(np.max(jumps))
        else:
            scale_jump = 0.0
        if is_loop:
            bind_prev = bind - np.roll(bind, 1, axis=0)
            bind_next = np.roll(bind, -1, axis=0) - bind
            current_prev = current - np.roll(current, 1, axis=0)
            current_next = np.roll(current, -1, axis=0) - current
            bind_midpoint = (np.roll(bind, 1, axis=0) + np.roll(bind, -1, axis=0)) * 0.5
            current_midpoint = (np.roll(current, 1, axis=0) + np.roll(current, -1, axis=0)) * 0.5
            bind_local = (np.linalg.norm(bind_prev, axis=1) + np.linalg.norm(bind_next, axis=1)) * 0.5
            current_local = (np.linalg.norm(current_prev, axis=1) + np.linalg.norm(current_next, axis=1)) * 0.5
            bind_laplacian = np.linalg.norm(bind - bind_midpoint, axis=1) / np.maximum(bind_local, 1.0e-9)
            current_laplacian = np.linalg.norm(current - current_midpoint, axis=1) / np.maximum(current_local, 1.0e-9)
        else:
            if len(chain) < 3:
                continue
            bind_prev = bind[1:-1] - bind[:-2]
            bind_next = bind[2:] - bind[1:-1]
            current_prev = current[1:-1] - current[:-2]
            current_next = current[2:] - current[1:-1]
            bind_midpoint = (bind[:-2] + bind[2:]) * 0.5
            current_midpoint = (current[:-2] + current[2:]) * 0.5
            bind_local = (np.linalg.norm(bind_prev, axis=1) + np.linalg.norm(bind_next, axis=1)) * 0.5
            current_local = (np.linalg.norm(current_prev, axis=1) + np.linalg.norm(current_next, axis=1)) * 0.5
            bind_laplacian = np.linalg.norm(bind[1:-1] - bind_midpoint, axis=1) / np.maximum(bind_local, 1.0e-9)
            current_laplacian = np.linalg.norm(current[1:-1] - current_midpoint, axis=1) / np.maximum(current_local, 1.0e-9)
        bind_turn = np.arccos(
            np.clip(np.sum(normalize(bind_prev) * normalize(bind_next), axis=1), -1.0, 1.0)
        )
        current_turn = np.arccos(
            np.clip(np.sum(normalize(current_prev) * normalize(current_next), axis=1), -1.0, 1.0)
        )
        turn_delta = float(np.max(np.abs(current_turn - bind_turn)))
        laplacian_delta = float(np.max(np.abs(current_laplacian - bind_laplacian)))
        chain_worst = max(scale_jump, turn_delta, laplacian_delta)
        if chain_worst > max(worst_scale_jump, worst_turn_delta, worst_laplacian_delta):
            worst_chain = chain_index
        worst_scale_jump = max(worst_scale_jump, scale_jump)
        worst_turn_delta = max(worst_turn_delta, turn_delta)
        worst_laplacian_delta = max(worst_laplacian_delta, laplacian_delta)
    return {
        "crease_edge_scale_log_jump_max": worst_scale_jump,
        "crease_turn_delta_degrees_max": math.degrees(worst_turn_delta),
        "crease_laplacian_delta_max": worst_laplacian_delta,
        "worst_crease_chain": worst_chain,
    }


def analyze_pose(
    mesh: Mesh,
    deformer: VolumeRodDeformer,
    topology: CoordinateTopology,
    bind_area: np.ndarray,
    bind_edges: np.ndarray,
    first_axis: float,
    second_axis: float,
    mirrored: bool,
) -> tuple[dict[str, float | int | bool | list[float]], np.ndarray]:
    top_matrix = thumb_skin_matrix(first_axis, second_axis, mirrored)
    positions, normals = deformer.deform(mesh.positions, mesh.normals, top_matrix)
    triangles = mesh.indices.reshape(-1, 3)
    cross, area, edges = triangle_geometry(positions, triangles)
    face_normals = normalize(cross)
    expected_normals = normalize(np.sum(normals[triangles], axis=1))
    renderer_alignment = np.sum(face_normals * expected_normals, axis=1)
    recalculated_normals = recalculate_surface_normals(positions, triangles)
    recalculated_alignment = np.sum(
        face_normals * normalize(np.sum(recalculated_normals[triangles], axis=1)),
        axis=1,
    )
    jacobian_normals, jacobian_area_ratio = local_surface_jacobian_normals(
        mesh, deformer, triangles, top_matrix
    )
    geometric_alignment = np.sum(face_normals * jacobian_normals, axis=1)
    renderer_inverted = renderer_alignment <= 0.0
    recalculated_inverted = recalculated_alignment <= 0.0
    geometric_inverted = geometric_alignment <= 0.0
    shading_only = renderer_inverted & ~geometric_inverted
    area_ratio = np.divide(area, bind_area, out=np.ones_like(area), where=bind_area > 1.0e-12)
    edge_ratio = np.divide(edges, bind_edges, out=np.ones_like(edges), where=bind_edges > 1.0e-12)
    seam_spread, seam_coordinate = duplicate_seam_metrics(positions, topology)
    collapsed = collapsed_coordinates(positions, topology)
    crease = chain_metrics(collapsed, topology)
    minimum_alignment_triangle = int(np.argmin(renderer_alignment))
    minimum_recalculated_alignment_triangle = int(np.argmin(recalculated_alignment))
    minimum_geometric_alignment_triangle = int(np.argmin(geometric_alignment))
    minimum_jacobian_area_triangle = int(np.argmin(jacobian_area_ratio))
    minimum_area_triangle = int(np.argmin(area_ratio))
    maximum_area_triangle = int(np.argmax(area_ratio))
    minimum_edge = np.unravel_index(np.argmin(edge_ratio), edge_ratio.shape)
    maximum_edge = np.unravel_index(np.argmax(edge_ratio), edge_ratio.shape)
    metrics: dict[str, float | int | bool | list[float]] = {
        "first_axis": float(first_axis),
        "second_axis": float(second_axis),
        "mirrored": mirrored,
        "inverted_triangles": int(np.count_nonzero(renderer_inverted)),
        "inverted_triangle_ids": np.flatnonzero(renderer_inverted).tolist(),
        "normal_alignment_min": float(renderer_alignment[minimum_alignment_triangle]),
        "normal_alignment_p01": float(np.quantile(renderer_alignment, 0.01)),
        "minimum_alignment_triangle": minimum_alignment_triangle,
        "minimum_alignment_triangle_vertices": positions[triangles[minimum_alignment_triangle]].reshape(-1).tolist(),
        "recalculated_normal_inverted_triangles": int(
            np.count_nonzero(recalculated_inverted)
        ),
        "recalculated_normal_inverted_triangle_ids": np.flatnonzero(
            recalculated_inverted
        ).tolist(),
        "recalculated_normal_alignment_min": float(
            recalculated_alignment[minimum_recalculated_alignment_triangle]
        ),
        "minimum_recalculated_alignment_triangle": minimum_recalculated_alignment_triangle,
        "geometric_inverted_triangles": int(np.count_nonzero(geometric_inverted)),
        "geometric_inverted_triangle_ids": np.flatnonzero(geometric_inverted).tolist(),
        "geometric_alignment_min": float(
            geometric_alignment[minimum_geometric_alignment_triangle]
        ),
        "geometric_alignment_p01": float(np.quantile(geometric_alignment, 0.01)),
        "minimum_geometric_alignment_triangle": minimum_geometric_alignment_triangle,
        "shading_only_inverted_triangles": int(np.count_nonzero(shading_only)),
        "shading_only_inverted_triangle_ids": np.flatnonzero(shading_only).tolist(),
        "jacobian_area_ratio_min": float(
            jacobian_area_ratio[minimum_jacobian_area_triangle]
        ),
        "jacobian_area_ratio_p01": float(np.quantile(jacobian_area_ratio, 0.01)),
        "minimum_jacobian_area_triangle": minimum_jacobian_area_triangle,
        "area_ratio_min": float(area_ratio[minimum_area_triangle]),
        "area_ratio_p01": float(np.quantile(area_ratio, 0.01)),
        "area_ratio_p99": float(np.quantile(area_ratio, 0.99)),
        "area_ratio_max": float(area_ratio[maximum_area_triangle]),
        "minimum_area_triangle": minimum_area_triangle,
        "maximum_area_triangle": maximum_area_triangle,
        "edge_ratio_min": float(edge_ratio[minimum_edge]),
        "edge_ratio_max": float(edge_ratio[maximum_edge]),
        "minimum_edge_triangle": int(minimum_edge[0]),
        "maximum_edge_triangle": int(maximum_edge[0]),
        "duplicate_seam_spread_max": seam_spread,
        "duplicate_seam_coordinate": seam_coordinate,
        "displacement_max": float(np.max(np.linalg.norm(positions - mesh.positions, axis=1))),
    }
    metrics.update(crease)
    return metrics, positions


def pose_values(minimum: float, maximum: float, count: int, extras: Iterable[float]) -> list[float]:
    values = {round(float(value), 6) for value in np.linspace(minimum, maximum, count)}
    values.update(round(float(value), 6) for value in extras)
    return sorted(value for value in values if minimum <= value <= maximum)


def metric_worst(poses: list[dict], key: str, minimum: bool = False) -> dict:
    return min(poses, key=lambda pose: pose[key]) if minimum else max(poses, key=lambda pose: pose[key])


def recurring_triangles(poses: list[dict], key: str, limit: int = 20) -> list[dict[str, int]]:
    counts: Counter[int] = Counter()
    for pose in poses:
        counts.update(int(index) for index in pose[key])
    return [
        {"triangle": triangle, "pose_count": pose_count}
        for triangle, pose_count in counts.most_common(limit)
    ]


def summarize(poses: list[dict], topology: CoordinateTopology, vertex_count: int, triangle_count: int) -> dict:
    return {
        "vertex_count": vertex_count,
        "triangle_count": triangle_count,
        "coordinate_count": len(topology.bind_coordinates),
        "crease_chain_count": len(topology.crease_chains),
        "pose_count": len(poses),
        "poses_with_inversions": sum(pose["inverted_triangles"] > 0 for pose in poses),
        "maximum_inverted_triangles": metric_worst(poses, "inverted_triangles"),
        "poses_with_geometric_inversions": sum(
            pose["geometric_inverted_triangles"] > 0 for pose in poses
        ),
        "maximum_geometric_inverted_triangles": metric_worst(
            poses, "geometric_inverted_triangles"
        ),
        "maximum_shading_only_inverted_triangles": metric_worst(
            poses, "shading_only_inverted_triangles"
        ),
        "poses_with_recalculated_normal_inversions": sum(
            pose["recalculated_normal_inverted_triangles"] > 0 for pose in poses
        ),
        "maximum_recalculated_normal_inverted_triangles": metric_worst(
            poses, "recalculated_normal_inverted_triangles"
        ),
        "recurring_geometric_inverted_triangles": recurring_triangles(
            poses, "geometric_inverted_triangle_ids"
        ),
        "recurring_shading_only_inverted_triangles": recurring_triangles(
            poses, "shading_only_inverted_triangle_ids"
        ),
        "recurring_recalculated_normal_inverted_triangles": recurring_triangles(
            poses, "recalculated_normal_inverted_triangle_ids"
        ),
        "minimum_normal_alignment": metric_worst(poses, "normal_alignment_min", minimum=True),
        "minimum_recalculated_normal_alignment": metric_worst(
            poses, "recalculated_normal_alignment_min", minimum=True
        ),
        "minimum_geometric_alignment": metric_worst(
            poses, "geometric_alignment_min", minimum=True
        ),
        "minimum_jacobian_area_ratio": metric_worst(
            poses, "jacobian_area_ratio_min", minimum=True
        ),
        "minimum_area_ratio": metric_worst(poses, "area_ratio_min", minimum=True),
        "maximum_area_ratio": metric_worst(poses, "area_ratio_max"),
        "minimum_edge_ratio": metric_worst(poses, "edge_ratio_min", minimum=True),
        "maximum_edge_ratio": metric_worst(poses, "edge_ratio_max"),
        "maximum_duplicate_seam_spread": metric_worst(poses, "duplicate_seam_spread_max"),
        "maximum_crease_scale_jump": metric_worst(poses, "crease_edge_scale_log_jump_max"),
        "maximum_crease_turn_delta": metric_worst(poses, "crease_turn_delta_degrees_max"),
        "maximum_crease_laplacian_delta": metric_worst(poses, "crease_laplacian_delta_max"),
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mesh", type=Path, required=True)
    parser.add_argument("--deformation", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--coordinates", type=Path)
    parser.add_argument("--first-samples", type=int, default=9)
    parser.add_argument("--second-samples", type=int, default=10)
    parser.add_argument(
        "--side",
        choices=("right", "mirrored", "both"),
        default="both",
        help="Analyze the non-mirrored hand, mirrored hand, or both.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    mesh = read_mesh(args.mesh)
    deformation = read_deformation(args.deformation, len(mesh.vertices))
    deformer = VolumeRodDeformer(deformation)
    topology = build_coordinate_topology(mesh)
    triangles = mesh.indices.reshape(-1, 3)
    _, bind_area, bind_edges = triangle_geometry(mesh.positions, triangles)
    first_values = pose_values(
        FIRST_AXIS_MIN,
        FIRST_AXIS_MAX,
        args.first_samples,
        (FIRST_AXIS_MIN, 0.0, FIRST_AXIS_MAX),
    )
    second_values = pose_values(
        SECOND_AXIS_MIN,
        SECOND_AXIS_MAX,
        args.second_samples,
        (SECOND_AXIS_MIN, SECOND_AXIS_BIND, 0.0, SECOND_AXIS_MAX),
    )
    mirrored_values = {
        "right": (False,),
        "mirrored": (True,),
        "both": (False, True),
    }[args.side]
    poses: list[dict] = []
    coordinate_dump: dict[str, np.ndarray] = {}
    for mirrored in mirrored_values:
        for first_axis in first_values:
            for second_axis in second_values:
                metrics, positions = analyze_pose(
                    mesh,
                    deformer,
                    topology,
                    bind_area,
                    bind_edges,
                    first_axis,
                    second_axis,
                    mirrored,
                )
                poses.append(metrics)
                if args.coordinates is not None:
                    key = f"{'mirrored' if mirrored else 'right'}_a1_{first_axis:g}_a2_{second_axis:g}"
                    coordinate_dump[key] = positions.astype(np.float32)

    baseline_matrix = thumb_skin_matrix(0.0, SECOND_AXIS_BIND, False)
    baseline_positions, _ = deformer.deform(mesh.positions, mesh.normals, baseline_matrix)
    baseline_error = float(np.max(np.linalg.norm(baseline_positions - mesh.positions, axis=1)))
    report = {
        "mesh": str(args.mesh),
        "deformation": str(args.deformation),
        "baseline_max_coordinate_error": baseline_error,
        "first_axis_values": first_values,
        "second_axis_values": second_values,
        "summary": summarize(poses, topology, len(mesh.vertices), len(triangles)),
        "poses": poses,
    }
    output_text = json.dumps(report, indent=2, ensure_ascii=True)
    if args.output is not None:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(output_text + "\n", encoding="utf-8")
    if args.coordinates is not None:
        args.coordinates.parent.mkdir(parents=True, exist_ok=True)
        np.savez_compressed(
            args.coordinates,
            bind=mesh.positions.astype(np.float32),
            triangles=triangles.astype(np.int32),
            progress=deformation.weights[:, THUMB_INFLUENCE].astype(np.float32),
            centerline=deformation.centerline.astype(np.float32),
            **coordinate_dump,
        )
    print(json.dumps(report["summary"], indent=2, ensure_ascii=True))
    print(f"baseline_max_coordinate_error={baseline_error:.9g}")
    if args.output is not None:
        print(f"report={args.output}")
    if args.coordinates is not None:
        print(f"coordinates={args.coordinates}")


if __name__ == "__main__":
    main()
