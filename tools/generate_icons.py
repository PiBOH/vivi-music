#!/usr/bin/env python3
"""Generate the Vivi Music DE desktop app icons from the shared pixel-art grid.

The grid mirrors AXOLOTL_PIXELS in
composeApp/src/commonMain/kotlin/com/vivimusic/de/ui/Axolotl.kt. Keep the two in
sync. Outputs composeApp/icons/icon.png, icon.ico and icon.icns.

Usage: python3 tools/generate_icons.py
"""

import io
import os
import struct

from PIL import Image

PIXELS = [
    ".PPP........PPP.",
    ".PPPP......PPPP.",
    ".PPPPBBBBBBPPPP.",
    ".PBBBBBBBBBBBBP.",
    ".PBBEEBBBBEEBBP.",
    ".PBBEEBBBBEEBBP.",
    ".PBBBBBBBBBBBBP.",
    "..BBBBBBBBBBBB..",
    "..BBBBBMMBBBBB..",
    "..BBBBBBBBBBBB..",
    "..BBBBBBBBBBBB..",
    "...BBBBBBBBBB...",
    "....BBBBBBBB....",
    ".....DBBBBD.....",
    "......DBBD......",
    ".......DD.......",
]

COLORS = {
    "B": (0x54, 0xC9, 0xF0),
    "D": (0x2E, 0x6E, 0x8E),
    "P": (0xFF, 0x8F, 0xB5),
    "E": (0x14, 0x20, 0x2E),
    "M": (0x2E, 0x6E, 0x8E),
}

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


def base_image():
    cols = len(PIXELS[0])
    rows = len(PIXELS)
    img = Image.new("RGBA", (cols, rows), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(PIXELS):
        for x, ch in enumerate(row):
            if ch != ".":
                r, g, b = COLORS[ch]
                px[x, y] = (r, g, b, 255)
    return img


def scaled(size):
    return base_image().resize((size, size), Image.NEAREST)


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

    scaled(256).save(os.path.join(out_dir, "icon.png"), format="PNG")

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
