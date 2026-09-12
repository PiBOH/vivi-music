#!/usr/bin/env python3
"""Subset the website's Material Symbols icon font to the icons actually used.

The full Material Symbols variable font is ~4 MB and every page of `.websitede`
loaded it, which alone fails the PageSpeed "avoid enormous network payloads"
audit. Only a handful of icon names are referenced anywhere, so this script
shrinks the font to those icons (a few KB) without changing a single outline.

Why three steps (a plain `pyftsubset --text=...` does NOT work here):

1. The icon names are rendered as **ligatures**: the HTML contains the literal
   text `download`, and the font's `rlig` feature turns that sequence into the
   icon glyph.
2. `pyftsubset --text="download"` keeps the ASCII glyphs `d`,`o`,... and then
   follows the ligature closure from *every* one of them - which reaches almost
   every icon in the font (~1600 glyphs survive, ~160 KB).
   So the ligature table is pruned FIRST: every ligature whose output glyph is
   not one of the wanted icons is deleted, and only then is the font subsetted.
3. The CSS pins `FILL 0 / GRAD 0 / opsz 24 / wght 400` with
   `font-variation-settings`, so instancing those axes (a static font) is
   faithful to what the browser renders and drops all the variation deltas.

Requires:  python3 -m pip install fonttools brotli

Usage:
    python3 scripts/subset_material_symbols.py \
        .websitede/assets/fonts/material-symbols-outlined.woff2 \
        block desktop_windows download expand_more laptop_mac menu_book \
        open_in_new smartphone star terminal

Note: `smartphone` is stored under the glyph name `mobile`; the script resolves
ligature targets by reading the font, so pass the icon names as written in the
HTML (the ligature text), not the glyph names.
"""

import os
import subprocess
import sys
import tempfile

from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

# Axes the website pins via `font-variation-settings` (see site-chrome.css).
PINNED_AXES = {"wght": 400, "FILL": 0, "GRAD": 0, "opsz": 24}


def ligature_name(first, components):
    """Rebuild the icon name from a ligature's component glyph names."""
    return (first + "".join(components)).replace("underscore", "_")


def collect_targets(font, wanted):
    """Map every wanted icon name to its ligature output glyph name."""
    targets = {}
    for lookup in font["GSUB"].table.LookupList.Lookup:
        for subtable in lookup.SubTable:
            sub = getattr(subtable, "ExtSubTable", subtable)
            for first, ligatures in (getattr(sub, "ligatures", {}) or {}).items():
                for lig in ligatures:
                    name = ligature_name(first, lig.Component)
                    if name in wanted:
                        targets[name] = lig.LigGlyph
    return targets


def prune_ligatures(font, keep_glyphs):
    """Delete every ligature whose output glyph is not in `keep_glyphs`."""
    removed = 0
    for lookup in font["GSUB"].table.LookupList.Lookup:
        for subtable in lookup.SubTable:
            sub = getattr(subtable, "ExtSubTable", subtable)
            ligatures = getattr(sub, "ligatures", None)
            if not ligatures:
                continue
            for first in list(ligatures.keys()):
                kept = [l for l in ligatures[first] if l.LigGlyph in keep_glyphs]
                removed += len(ligatures[first]) - len(kept)
                if kept:
                    ligatures[first] = kept
                else:
                    del ligatures[first]
    return removed


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    font_path = sys.argv[1]
    wanted = list(dict.fromkeys(sys.argv[2:]))
    original = os.path.getsize(font_path)

    with tempfile.TemporaryDirectory() as tmp:
        # 1. Pin the axes the CSS pins anyway -> static font, no variation data.
        static = os.path.join(tmp, "static.woff2")
        instancer.instantiateVariableFont(
            TTFont(font_path), PINNED_AXES, inplace=False
        ).save(static)

        # 2. Keep only the ligatures whose output glyph is one of our icons.
        font = TTFont(static)
        targets = collect_targets(font, set(wanted))
        missing = sorted(set(wanted) - set(targets))
        if missing:
            print("ERROR: no ligature found for: " + ", ".join(missing))
            return 1
        removed = prune_ligatures(font, set(targets.values()))
        pruned = os.path.join(tmp, "pruned.ttf")
        font.save(pruned)
        font.close()

        # 3. Subset to the icon names (input letters) + their glyphs.
        subprocess.run(
            [
                sys.executable, "-m", "fontTools.subset", pruned,
                "--text=" + " ".join(wanted),
                "--glyphs=" + ",".join(sorted(set(targets.values()))) + ",underscore",
                "--layout-features=rlig",
                "--no-hinting",
                "--flavor=woff2",
                "--output-file=" + font_path,
            ],
            check=True,
        )

    final = os.path.getsize(font_path)
    print("ligatures pruned : %d" % removed)
    print("size             : %d -> %d bytes (%.2f%% smaller)"
          % (original, final, 100.0 * (1 - final / original)))
    print("icons kept       : " + ", ".join(sorted(targets)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
