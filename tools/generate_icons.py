#!/usr/bin/env python3
"""Generate the Vivi Music DE desktop app icons from the app logo.

Reads logo.png from the repository root and produces the platform icons used by
Compose/jpackage:

- composeApp/icons/icon.png  (Linux)
- composeApp/icons/icon.ico  (Windows)
- composeApp/icons/icon.icns (macOS)

Usage: python3 tools/generate_icons.py
"""

import io
import os
import struct

from PIL import Image

ICNS_TYPES = [
    ("icp4", 16),
    ("icp5", 32),
    ("icp6", 64),
    ("ic07", 128),
    ("ic08", 256),
    ("ic09", 512),
    ("ic10", 1024),
]

ICO_SIZES = [16, 24, 32, 48, 64, 128, 256]

PNG_SIZE = 512


def load_logo():
    here = os.path.dirname(os.path.abspath(__file__))
    logo_path = os.path.normpath(os.path.join(here, "..", "logo.png"))
    return Image.open(logo_path).convert("RGBA")


def to_square(img):
    """Pad the (non-square) logo onto a transparent square canvas, centered."""
    w, h = img.size
    if w == h:
        return img
    side = max(w, h)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(img, ((side - w) // 2, (side - h) // 2))
    return canvas


def scaled(size):
    return to_square(load_logo()).resize((size, size), Image.LANCZOS)


def build_icns():
    entries = []
    for os_type, size in ICNS_TYPES:
        buf = io.BytesIO()
        scaled(size).save(buf, format="PNG")
        data = buf.getvalue()
        entries.append((os_type.encode("ascii"), data))
    total = 8 + sum(8 + len(data) for _, data in entries)
    out = bytearray(b"icns" + struct.pack(">I", total))
    for os_type, data in entries:
        out += os_type + struct.pack(">I", 8 + len(data)) + data
    return bytes(out)


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    out_dir = os.path.normpath(os.path.join(here, "..", "composeApp", "icons"))
    os.makedirs(out_dir, exist_ok=True)

    scaled(PNG_SIZE).save(os.path.join(out_dir, "icon.png"), format="PNG")

    ico_images = [scaled(s) for s in ICO_SIZES]
    ico_images[-1].save(
        os.path.join(out_dir, "icon.ico"),
        format="ICO",
        append_images=ico_images[:-1],
    )

    with open(os.path.join(out_dir, "icon.icns"), "wb") as f:
        f.write(build_icns())

    print("Wrote icons to", out_dir)


if __name__ == "__main__":
    main()
