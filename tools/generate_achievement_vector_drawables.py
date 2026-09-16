#!/usr/bin/env python3
"""Trace the approved achievement artwork into monochrome vector resources."""

from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "design" / "achievement-icons" / "reference" / "achievement-icons-source.png"
REMAINING_SOURCE = (
    ROOT
    / "design"
    / "achievement-icons"
    / "reference"
    / "achievement-icons-remaining-source.png"
)
SVG_DIR = ROOT / "design" / "achievement-icons" / "vectors"
DRAWABLE_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable"

VIEWPORT = 192
CROP_SIZE = 270
REMAINING_CROP_SIZE = 560


@dataclass(frozen=True)
class PaletteEntry:
    value: int
    hex_color: str
    android_color: str


PALETTE = (
    PaletteEntry(0x21, "#212121", "@color/ubi4_dark_back"),
    PaletteEntry(0x2A, "#2A2A2A", "@color/ubi4_back"),
    PaletteEntry(0x37, "#373737", "@color/ubi4_gray"),
    PaletteEntry(0x44, "#444444", "@color/ubi4_gray_border"),
    PaletteEntry(0x83, "#838383", "@color/ubi4_deactivate_text"),
    PaletteEntry(0xB6, "#B6B6B6", "@color/rssi_gray"),
    PaletteEntry(0xD7, "#D7D7D8", "@color/number_picker_bg"),
    PaletteEntry(0xFC, "#FCFCFC", "@color/ubi4_white"),
)

ICONS = (
    ("bionic", 158, 164),
    ("cyborg", 429, 165),
    ("streak", 696, 165),
    ("long_haul", 964, 166),
    ("scientist", 157, 497),
    ("daily_challenge", 429, 497),
    ("precision", 696, 498),
    ("power", 966, 499),
    ("get_a_grip", 158, 829),
    ("alter_ego", 428, 828),
    ("ninja", 696, 829),
    ("anniversary", 966, 829),
    ("basic_training", 428, 1156),
    ("graduation", 696, 1156),
)

REMAINING_ICONS = (
    ("personalisation", 366, 413),
    ("always_connected", 955, 413),
    ("champion", 1542, 413),
)


Point = tuple[int, int]
Edge = tuple[Point, Point]


def crop_icon(
    image: Image.Image,
    center_x: int,
    center_y: int,
    crop_size: int = CROP_SIZE,
    use_circular_alpha: bool = False,
) -> Image.Image:
    half = crop_size // 2
    crop = Image.new("RGBA", (crop_size, crop_size))
    source_box = (
        max(0, center_x - half),
        max(0, center_y - half),
        min(image.width, center_x + half),
        min(image.height, center_y + half),
    )
    source_crop = image.crop(source_box)
    destination = (
        max(0, half - center_x),
        max(0, half - center_y),
    )
    crop.alpha_composite(source_crop, destination)
    resized = crop.resize((VIEWPORT, VIEWPORT), Image.Resampling.LANCZOS)
    if not use_circular_alpha:
        return resized

    rgba = np.asarray(resized, dtype=np.uint8).copy()
    coordinate = np.arange(VIEWPORT, dtype=np.float32)
    x_coordinates, y_coordinates = np.meshgrid(coordinate, coordinate)
    center = (VIEWPORT - 1) / 2.0
    circular_mask = (
        (x_coordinates - center) ** 2 + (y_coordinates - center) ** 2
        <= 94.0**2
    )
    rgba[:, :, 3] = np.where(circular_mask, rgba[:, :, 3], 0)
    return Image.fromarray(rgba, mode="RGBA")


def apply_common_badge_frame(
    icon: Image.Image,
    frame_source: Image.Image,
) -> Image.Image:
    """Keep a new symbol while reusing the approved badge frame pixel-for-pixel."""
    icon_rgba = np.asarray(icon, dtype=np.float32)
    frame_rgba = np.asarray(frame_source, dtype=np.float32)
    coordinate = np.arange(VIEWPORT, dtype=np.float32)
    x_coordinates, y_coordinates = np.meshgrid(coordinate, coordinate)
    center = (VIEWPORT - 1) / 2.0
    radius = np.sqrt(
        (x_coordinates - center) ** 2 + (y_coordinates - center) ** 2
    )
    frame_weight = np.clip((radius - 58.0) / 10.0, 0.0, 1.0)[:, :, None]
    composited = icon_rgba * (1.0 - frame_weight) + frame_rgba * frame_weight
    return Image.fromarray(composited.round().astype(np.uint8), mode="RGBA")


def monochrome_pixels(image: Image.Image) -> tuple[np.ndarray, np.ndarray]:
    rgba = np.asarray(image, dtype=np.float32)
    luminance = (
        rgba[:, :, 0] * 0.2126
        + rgba[:, :, 1] * 0.7152
        + rgba[:, :, 2] * 0.0722
    ) / 255.0
    tone_input = np.linspace(0.0, 1.0, 6)
    tone_output = np.array((0.13, 0.22, 0.31, 0.52, 0.75, 0.99))
    toned = np.interp(luminance, tone_input, tone_output) * 255.0

    palette_values = np.array([entry.value for entry in PALETTE], dtype=np.float32)
    palette_indices = np.abs(toned[:, :, None] - palette_values).argmin(axis=2)
    alpha_mask = largest_connected_component(rgba[:, :, 3] >= 24.0)
    return palette_indices, alpha_mask


def largest_connected_component(mask: np.ndarray) -> np.ndarray:
    height, width = mask.shape
    visited = np.zeros_like(mask, dtype=bool)
    largest: list[Point] = []

    for y in range(height):
        for x in range(width):
            if not mask[y, x] or visited[y, x]:
                continue
            stack = [(x, y)]
            visited[y, x] = True
            component: list[Point] = []
            while stack:
                current_x, current_y = stack.pop()
                component.append((current_x, current_y))
                for delta_x, delta_y in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    next_x = current_x + delta_x
                    next_y = current_y + delta_y
                    if (
                        0 <= next_x < width
                        and 0 <= next_y < height
                        and mask[next_y, next_x]
                        and not visited[next_y, next_x]
                    ):
                        visited[next_y, next_x] = True
                        stack.append((next_x, next_y))
            if len(component) > len(largest):
                largest = component

    result = np.zeros_like(mask, dtype=bool)
    for x, y in largest:
        result[y, x] = True
    return result


def boundary_edges(mask: np.ndarray) -> set[Edge]:
    height, width = mask.shape
    edges: set[Edge] = set()
    for y in range(height):
        for x in range(width):
            if not mask[y, x]:
                continue
            if y == 0 or not mask[y - 1, x]:
                edges.add(((x, y), (x + 1, y)))
            if x == width - 1 or not mask[y, x + 1]:
                edges.add(((x + 1, y), (x + 1, y + 1)))
            if y == height - 1 or not mask[y + 1, x]:
                edges.add(((x + 1, y + 1), (x, y + 1)))
            if x == 0 or not mask[y, x - 1]:
                edges.add(((x, y + 1), (x, y)))
    return edges


DIRECTION_INDEX = {
    (1, 0): 0,
    (0, 1): 1,
    (-1, 0): 2,
    (0, -1): 3,
}


def choose_next(previous: Point, current: Point, candidates: list[Point]) -> Point:
    incoming = (current[0] - previous[0], current[1] - previous[1])
    incoming_index = DIRECTION_INDEX[incoming]

    def priority(candidate: Point) -> int:
        direction = (candidate[0] - current[0], candidate[1] - current[1])
        turn = (DIRECTION_INDEX[direction] - incoming_index) % 4
        return {1: 0, 0: 1, 3: 2, 2: 3}[turn]

    return min(candidates, key=priority)


def trace_loops(mask: np.ndarray) -> list[list[Point]]:
    remaining = boundary_edges(mask)
    outgoing: dict[Point, set[Point]] = defaultdict(set)
    for start, end in remaining:
        outgoing[start].add(end)

    loops: list[list[Point]] = []
    while remaining:
        start_edge = min(remaining)
        start, current = start_edge
        previous = start
        loop = [start, current]
        remaining.remove(start_edge)
        outgoing[start].remove(current)

        while current != start:
            candidates = [end for end in outgoing[current] if (current, end) in remaining]
            if not candidates:
                break
            next_point = choose_next(previous, current, candidates)
            remaining.remove((current, next_point))
            outgoing[current].remove(next_point)
            loop.append(next_point)
            previous, current = current, next_point

        if len(loop) >= 4 and loop[-1] == start:
            loops.append(simplify_collinear(loop[:-1]))
    return loops


def simplify_collinear(points: list[Point]) -> list[Point]:
    if len(points) <= 3:
        return points
    simplified: list[Point] = []
    count = len(points)
    for index, current in enumerate(points):
        previous = points[(index - 1) % count]
        following = points[(index + 1) % count]
        vector_a = (current[0] - previous[0], current[1] - previous[1])
        vector_b = (following[0] - current[0], following[1] - current[1])
        if vector_a != vector_b:
            simplified.append(current)
    return simplified


def path_data(loops: list[list[Point]]) -> str:
    commands: list[str] = []
    for loop in loops:
        if len(loop) < 3:
            continue
        first = loop[0]
        commands.append(f"M{first[0]},{first[1]}")
        previous = first
        for point in loop[1:]:
            delta_x = point[0] - previous[0]
            delta_y = point[1] - previous[1]
            if delta_y == 0:
                commands.append(f"h{delta_x}")
            elif delta_x == 0:
                commands.append(f"v{delta_y}")
            else:
                commands.append(f"L{point[0]},{point[1]}")
            previous = point
        commands.append("Z")
    return "".join(commands)


def vector_layers(image: Image.Image) -> list[tuple[PaletteEntry, str]]:
    palette_indices, alpha_mask = monochrome_pixels(image)
    layers: list[tuple[PaletteEntry, str]] = []
    for index, entry in enumerate(PALETTE):
        mask = alpha_mask & (palette_indices == index)
        if not mask.any():
            continue
        data = path_data(trace_loops(mask))
        if data:
            layers.append((entry, data))
    return layers


def android_vector(layers: list[tuple[PaletteEntry, str]]) -> str:
    paths = "\n".join(
        "    <path\n"
        f"        android:fillColor=\"{entry.android_color}\"\n"
        "        android:fillType=\"evenOdd\"\n"
        f"        android:pathData=\"{data}\" />"
        for entry, data in layers
    )
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/generate_achievement_vector_drawables.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="96dp"
    android:height="96dp"
    android:viewportWidth="{VIEWPORT}"
    android:viewportHeight="{VIEWPORT}">
{paths}
</vector>
"""


def svg_vector(title: str, layers: list[tuple[PaletteEntry, str]]) -> str:
    paths = "\n".join(
        f'  <path fill="{entry.hex_color}" fill-rule="evenodd" d="{data}"/>'
        for entry, data in layers
    )
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{VIEWPORT}" height="{VIEWPORT}" viewBox="0 0 {VIEWPORT} {VIEWPORT}">
  <title>{title}</title>
{paths}
</svg>
"""


def main() -> None:
    SVG_DIR.mkdir(parents=True, exist_ok=True)
    DRAWABLE_DIR.mkdir(parents=True, exist_ok=True)
    approved_source = Image.open(SOURCE).convert("RGBA")
    common_badge_frame = crop_icon(approved_source, 696, 165)

    sources = (
        (SOURCE, ICONS, CROP_SIZE, False),
        (REMAINING_SOURCE, REMAINING_ICONS, REMAINING_CROP_SIZE, True),
    )
    for source_path, icons, crop_size, use_circular_alpha in sources:
        source = Image.open(source_path).convert("RGBA")
        for slug, center_x, center_y in icons:
            icon = crop_icon(
                source,
                center_x,
                center_y,
                crop_size=crop_size,
                use_circular_alpha=use_circular_alpha,
            )
            if use_circular_alpha:
                icon = apply_common_badge_frame(icon, common_badge_frame)
            layers = vector_layers(icon)
            (SVG_DIR / f"achievement_{slug}.svg").write_text(
                svg_vector(slug, layers), encoding="utf-8"
            )
            (DRAWABLE_DIR / f"ic_achievement_{slug}.xml").write_text(
                android_vector(layers), encoding="utf-8"
            )


if __name__ == "__main__":
    main()
