#!/usr/bin/env python3
"""Generate ASTC texture assets for the V3 renderer."""

from __future__ import annotations

import argparse
import subprocess
from pathlib import Path


TEXTURES = [
    "str2_srednii_part8_new",
    "str2_ukazatelnii_part15_new",
    "gray",
    "str2_bezimiannii_part10_new",
    "str2_mizinec_part12_new",
    "str2_big_finger_part18_new",
    "str2_part9_new",
    "str2_part9_new_material_normal",
    "str2_ukazatelnii_part15_new_material_normal",
    "str2_srednii_part8_new_material_normal",
    "metal_color2",
    "str2_bezimiannii_part10_new_material_normal",
    "str2_mizinec_part12_new_material_normal",
    "str2_big_finger_part18_new_material_normal",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--astcenc",
        default="astcenc",
        help="Path to the astcenc executable.",
    )
    parser.add_argument(
        "--res-dir",
        type=Path,
        default=Path("app/src/main/res/drawable"),
        help="Drawable resource directory with source PNG files.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("app/src/main/assets/STR2_TEXTURE_ASTC"),
        help="Output directory for generated ASTC files.",
    )
    parser.add_argument(
        "--block",
        default="6x6",
        help="ASTC 2D block size.",
    )
    parser.add_argument(
        "--quality",
        default="-medium",
        help="astcenc quality preset or numeric quality.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    total_bytes = 0

    for texture_name in TEXTURES:
        source = args.res_dir / f"{texture_name}.png"
        target = args.output_dir / f"{texture_name}.astc"
        if not source.is_file():
            raise FileNotFoundError(source)

        subprocess.run(
            [
                args.astcenc,
                "-cl",
                str(source),
                str(target),
                args.block,
                args.quality,
            ],
            check=True,
        )
        size = target.stat().st_size
        total_bytes += size
        print(f"{source} -> {target} bytes={size}")

    print(
        f"converted textures={len(TEXTURES)} block={args.block} "
        f"quality={args.quality} bytes={total_bytes}"
    )


if __name__ == "__main__":
    main()
