#!/usr/bin/env python3

from pathlib import Path
from typing import Iterable

from PIL import Image, ImageChops, ImageDraw, ImageFilter


WIDTH = 1774
HEIGHT = 887


def composite(base: Image.Image, layer: Image.Image) -> Image.Image:
    return Image.alpha_composite(base, layer)


def cubic_points(
    p0: tuple[float, float],
    p1: tuple[float, float],
    p2: tuple[float, float],
    p3: tuple[float, float],
    count: int,
) -> Iterable[tuple[int, int]]:
    for index in range(count):
        t = index / (count - 1)
        mt = 1.0 - t
        x = (
            mt**3 * p0[0]
            + 3 * mt**2 * t * p1[0]
            + 3 * mt * t**2 * p2[0]
            + t**3 * p3[0]
        )
        y = (
            mt**3 * p0[1]
            + 3 * mt**2 * t * p1[1]
            + 3 * mt * t**2 * p2[1]
            + t**3 * p3[1]
        )
        yield round(x), round(y)


def make_glass(screen_mask_path: Path, output_path: Path) -> None:
    screen_mask = Image.open(screen_mask_path).convert("L")
    if screen_mask.size != (WIDTH, HEIGHT):
        raise ValueError(
            f"Expected {WIDTH}x{HEIGHT} screen mask, got {screen_mask.size}"
        )

    glass = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))

    base_tint = Image.new("RGBA", glass.size, (218, 228, 246, 7))
    glass = composite(glass, base_tint)

    wide = Image.new("RGBA", glass.size, (0, 0, 0, 0))
    wide_draw = ImageDraw.Draw(wide)
    wide_draw.polygon(
        [(90, 20), (510, 20), (1090, 760), (620, 760)],
        fill=(247, 244, 255, 27),
    )
    wide = wide.filter(ImageFilter.GaussianBlur(28))
    glass = composite(glass, wide)

    narrow = Image.new("RGBA", glass.size, (0, 0, 0, 0))
    narrow_draw = ImageDraw.Draw(narrow)
    narrow_draw.polygon(
        [(280, 60), (430, 60), (940, 720), (755, 720)],
        fill=(255, 255, 255, 24),
    )
    narrow = narrow.filter(ImageFilter.GaussianBlur(10))
    glass = composite(glass, narrow)

    soft_patch = Image.new("RGBA", glass.size, (0, 0, 0, 0))
    patch_draw = ImageDraw.Draw(soft_patch)
    patch_draw.ellipse(
        (230, 70, 850, 330),
        fill=(226, 235, 255, 18),
    )
    soft_patch = soft_patch.filter(ImageFilter.GaussianBlur(42))
    glass = composite(glass, soft_patch)

    glint = Image.new("RGBA", glass.size, (0, 0, 0, 0))
    glint_draw = ImageDraw.Draw(glint)
    curve = list(
        cubic_points(
            (267, 286),
            (267, 178),
            (330, 130),
            (455, 124),
            80,
        )
    )
    curve.extend(
        cubic_points(
            (455, 124),
            (655, 116),
            (870, 120),
            (1110, 122),
            150,
        )
    )
    for index in range(len(curve) - 1):
        progress = index / max(1, len(curve) - 2)
        alpha = round(106 * (1.0 - progress) ** 1.35)
        glint_draw.line(
            [curve[index], curve[index + 1]],
            fill=(248, 243, 255, alpha),
            width=5,
        )
    glint = glint.filter(ImageFilter.GaussianBlur(1.4))
    glass = composite(glass, glint)

    alpha = glass.getchannel("A")
    clipped_alpha = ImageChops.multiply(alpha, screen_mask)
    glass.putalpha(clipped_alpha)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    glass.save(output_path)


if __name__ == "__main__":
    project_root = Path(__file__).resolve().parents[1]
    make_glass(
        project_root / "tmp/imagegen/front-up5-screen-mask.png",
        project_root
        / "design_assets/google_play/phone_glass_landscape_front_up5_final.png",
    )
