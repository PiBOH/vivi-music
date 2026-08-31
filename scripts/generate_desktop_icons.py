#!/usr/bin/env python3
"""Generate OS-specific app icons for the VIVI Music desktop edition.

Reads the official source logo (converted to PNG for platform packaging) and produces:
  - logo_vmde.ico  (Windows)
  - logo_vmde.icns (macOS)
  - logo_vmde.png  (Linux — used as-is, kept here as the source too)

Requires Pillow:  pip install pillow
"""

import io
import os
import struct

from PIL import Image, ImageDraw, ImageOps

SRC = os.path.join(os.path.dirname(__file__), "..", "desktop", "icons", "logo_vmde.png")  # circular crop of logo_vmde_official.jpg
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "desktop", "icons")


def square(img: Image.Image) -> Image.Image:
    """Center the image on a transparent square canvas (avoids distortion)."""
    w, h = img.size
    side = max(w, h)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(img, ((side - w) // 2, (side - h) // 2), img)
    return canvas


def circularize(img: Image.Image) -> Image.Image:
    """Clip the square artwork to a circle (the official DE icon shape).

    Applied to any square source that is not already round, so a plain
    square logo still produces the round official icon.
    """
    side = min(img.size)
    img = ImageOps.fit(img, (side, side), Image.Resampling.LANCZOS)
    if img.mode != "RGBA":
        img = img.convert("RGBA")
    alpha = img.getchannel("A")
    # Already round (corners transparent)? Then keep the existing mask.
    corners = [alpha.getpixel(p) for p in [(0, 0), (side - 1, 0), (0, side - 1), (side - 1, side - 1)]]
    if all(v < 8 for v in corners):
        return img
    # Anti-aliased mask drawn at 4x then downscaled
    mask = Image.new("L", (side * 4, side * 4), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((0, 0, side * 4 - 1, side * 4 - 1), fill=255)
    mask = mask.resize((side, side), Image.Resampling.LANCZOS)
    img.putalpha(mask)
    return img


def build_ico(img: Image.Image, path: str) -> None:
    sizes = [(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]
    img.save(path, format="ICO", sizes=sizes)


def build_bmp(img: Image.Image, path: str, size: tuple[int, int], bg=(255, 255, 255)) -> None:
    """Place the logo centered on a solid-color canvas (Inno Setup wizard images
    use BMP without alpha, so a white background blends into the modern wizard)."""
    thumb = img.copy()
    thumb.thumbnail((size[0] - 8, size[1] - 8), Image.Resampling.LANCZOS)
    canvas = Image.new("RGB", size, bg)
    thumb_rgba = thumb.convert("RGBA")
    canvas.paste(thumb_rgba, ((size[0] - thumb.width) // 2, (size[1] - thumb.height) // 2), thumb_rgba)
    canvas.save(path, format="BMP")


def build_icns(img: Image.Image, path: str) -> None:
    # Modern ICNS: PNG data stored in standard slot types. macOS 10.15+ reads
    # PNG-based icns fine, so we do not need legacy JPEG2000/PNG fallbacks.
    entries = [
        ("ic07", 128),
        ("ic08", 256),
        ("ic09", 512),
        ("ic10", 1024),
        ("ic11", 32),
        ("ic12", 64),
        ("ic13", 256),
        ("ic14", 512),
    ]
    chunks = b""
    for slot, size in entries:
        buf = io.BytesIO()
        img.resize((size, size), Image.Resampling.LANCZOS).save(buf, format="PNG")
        data = buf.getvalue()
        chunks += slot.encode("ascii") + struct.pack(">I", len(data) + 8) + data

    with open(path, "wb") as f:
        f.write(b"icns" + struct.pack(">I", 8 + len(chunks)) + chunks)


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    img = circularize(square(Image.open(SRC).convert("RGBA")))

    build_ico(img, os.path.join(OUT_DIR, "logo_vmde.ico"))
    build_icns(img, os.path.join(OUT_DIR, "logo_vmde.icns"))
    build_bmp(img, os.path.join(OUT_DIR, "logo_vmde_wizard.bmp"), (164, 314))
    build_bmp(img, os.path.join(OUT_DIR, "logo_vmde_small.bmp"), (55, 58))
    print(f"Wrote {OUT_DIR}/logo_vmde.ico, logo_vmde.icns, logo_vmde_wizard.bmp and logo_vmde_small.bmp")


if __name__ == "__main__":
    main()
