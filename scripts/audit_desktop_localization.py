#!/usr/bin/env python3
"""
Audit the generated desktop string table (`Localization.kt` + `LocalizationTablesN.kt`).

Run from the repo root:  python3 scripts/audit_desktop_localization.py

Why this exists — `Localization.get` resolves a key in three steps:

    1. the language's own map;
    2. the English map;
    3. "the first map that has the key", as a last resort so a raw snake_case
       key never reaches the screen.

Step 3 is where a *translation* bug hides: if a key is missing from English but
present in some other language, an English build shows that other language. That
is how the Alpha lyrics style came out in Arabic letters, and how a key that only
the extras batches defined (never the Android resources) could show up in any
language but English. Both classes are reported here as errors.

The audit fails (exit 1) on

  * a key the UI asks for that no map defines (shows the raw key), and
  * a key missing from a language map, or present in one but absent from
    English (the step-3 leak).

It only *reports*, without failing, the two remaining "not translated yet"
classes, because they are the mobile resources' own gaps rather than a bug:

  * English text left in a language map (the resource was never translated
    upstream, or the key is desktop-only and no batch has covered it yet);
  * brand/technical strings that are meant to read the same everywhere.

`BRAND_KEYS` and `TECHNICAL_KEYS` below are that documented exception list.
"""

import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DESKTOP = os.path.join(
    REPO, "desktop", "src", "main", "kotlin", "com", "music", "vivi", "desktop"
)

# Strings that stay in Latin / stay identical in every language on purpose.
BRAND_KEYS = {
    "lyrics_style_apple", "lyrics_style_apple_v2", "lyrics_style_vivimusic",
    "lyrics_style_metro", "lyrics_style_lyrics_v2", "lyrics_style_alpha",
    "lyrics_style_karaoke", "apple_mini_player", "mini_player_apple",
    "player_background_apple", "canvas_source_tidal", "canvas_source_vivimusic",
    "canvas_source_apple_music", "vivi_equalizer", "lastfm", "discord",
    "de", "mobile", "website", "telegram_channel", "wrapped_title",
    "lyrics_style_none",
    # Discord's own feature name, kept as-is upstream.
    "discord_presence",
    # The window/app title; "desktop" is the same loanword everywhere it is
    # not already translated, and the product name never changes.
    "header",
}
# Device/technical readouts, font names and unit labels: English by design.
TECHNICAL_KEYS = {
    "gpu", "cpu", "heap", "memory", "threads", "process", "uptime", "status",
    "radix", "arch", "distribution",
    "density_55", "density_65", "density_75", "density_85", "density_100",
    "font_google_sans", "font_sans_flex", "font_outfit", "font_plus_jakarta_sans",
    "sort_az", "sort_za", "ok", "preview_text_quote",
    "ai_api_key", "ai_base_url", "ai_deepl_formality_default",
}

# Which scripts a language may use. A value written in a script the language
# does not use means a translation was pasted under the wrong language tag.
SCRIPT_RANGES = [
    ("ARABIC", 0x0600, 0x06FF), ("ARABIC", 0x0750, 0x077F), ("ARABIC", 0xFB50, 0xFDFF),
    ("HEBREW", 0x0590, 0x05FF),
    ("CYRILLIC", 0x0400, 0x04FF),
    ("GREEK", 0x0370, 0x03FF),
    ("INDIC", 0x0900, 0x097F), ("INDIC", 0x0980, 0x09FF), ("INDIC", 0x0A00, 0x0A7F),
    ("INDIC", 0x0A80, 0x0AFF), ("INDIC", 0x0B00, 0x0B7F), ("INDIC", 0x0B80, 0x0BFF),
    ("INDIC", 0x0C00, 0x0C7F), ("INDIC", 0x0C80, 0x0CFF),
    ("KHMER", 0x1780, 0x17FF), ("THAI", 0x0E00, 0x0E7F),
    ("JA", 0x3040, 0x30FF), ("JA", 0x31F0, 0x31FF),
    ("KO", 0x1100, 0x11FF), ("KO", 0xAC00, 0xD7AF),
    ("HAN", 0x3400, 0x4DBF), ("HAN", 0x4E00, 0x9FFF), ("HAN", 0xF900, 0xFAFF),
]
ONLY_SCRIPT = {
    "ar": "ARABIC", "fa": "ARABIC", "iw": "HEBREW",
    "ru": "CYRILLIC", "uk": "CYRILLIC", "be": "CYRILLIC", "bg": "CYRILLIC", "sr": "CYRILLIC",
    "el": "GREEK",
    "hi": "INDIC", "bn": "INDIC", "as": "INDIC", "pa": "INDIC", "ml": "INDIC",
    "ta": "INDIC", "te": "INDIC", "km": "KHMER", "th": "THAI",
    "zh-rCN": "HAN", "zh-rTW": "HAN",
}
# Languages that legitimately mix scripts (Japanese mixes kana with kanji,
# Korean is heading to hangul-only but still uses hanja).
MIXED_SCRIPT = {"ja": {"JA", "HAN"}, "ko": {"KO", "HAN"}}


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def parse_tables():
    """{lang: {key: value}} from the generated table files."""
    funcs = {}
    for name in sorted(os.listdir(DESKTOP)):
        if not (name.startswith("LocalizationTables") and name.endswith(".kt")):
            continue
        text = read(os.path.join(DESKTOP, name))
        for fn, body in re.findall(
            r"internal fun (strings_\d+)\(\): Map<String, String> =\s*mapOf\((.*?)\n    \)",
            text,
            re.S,
        ):
            funcs[fn] = dict(re.findall(r'"([^"]+)" to "([^"]*)"', body))
    localization = read(os.path.join(DESKTOP, "Localization.kt"))
    lang_of = dict(re.findall(r'"([^"]+)" to (strings_\d+)\(\)', localization))
    missing = [fn for fn in lang_of.values() if fn not in funcs]
    if missing:
        raise SystemExit(
            "Localization.kt references table functions that no file defines: %s"
            % ", ".join(missing)
        )
    return {lang: funcs[fn] for lang, fn in lang_of.items()}


def requested_keys():
    """Every key a screen passes to Localization.get.

    Calls whose key comes from a `when` are skipped as a whole: the literals in
    them are the *values* of the branch (`"fast" -> `), not keys, and every key
    such a call can resolve to is a literal somewhere else in the call sites.
    """
    keys = set()
    for name in sorted(os.listdir(DESKTOP)):
        if not name.endswith(".kt") or name.startswith("Localization"):
            continue
        text = read(os.path.join(DESKTOP, name))
        for m in re.finditer(r"Localization\.get\s*\(", text):
            i = m.end() - 1
            depth = 0
            while i < len(text):
                c = text[i]
                if c == "(":
                    depth += 1
                elif c == ")":
                    depth -= 1
                    if depth == 0:
                        break
                i += 1
            call = text[m.end() - 1:i]
            if "when" in call:
                continue
            for match in re.finditer(r'"([^"]+)"', call):
                lit = match.group(1)
                before = call[:match.start()].rstrip()
                # `if (mode == "native") "key_a" else "key_b"`: the compared
                # literal is a stored setting value, not a key.
                if before.endswith("==") or before.endswith("!="):
                    continue
                if re.fullmatch(r"[a-z][a-z0-9_]*", lit):
                    keys.add(lit)
    return keys


def scripts_in(value):
    found = set()
    for ch in value:
        for family, lo, hi in SCRIPT_RANGES:
            if lo <= ord(ch) <= hi:
                found.add(family)
    return found


def main():
    tables = parse_tables()
    en = tables["en"]
    errors = []
    wrong_script = []
    english_left = []

    for key in sorted(requested_keys()):
        if key not in en:
            errors.append("key '%s' is asked for but no table defines it" % key)

    for lang, entries in sorted(tables.items()):
        if lang != "en":
            for key in sorted(set(en) - set(entries)):
                errors.append("%s: missing '%s' (would fall back)" % (lang, key))
            for key in sorted(set(entries) - set(en)):
                errors.append(
                    "%s: defines '%s' but English does not (English builds would show %s)"
                    % (lang, key, lang)
                )
        only = ONLY_SCRIPT.get(lang)
        mixed = MIXED_SCRIPT.get(lang)
        for key, value in entries.items():
            found = scripts_in(value)
            if not found:
                continue
            if only and found != {only}:
                wrong_script.append("%s: %s -> %s" % (lang, key, value))
            elif mixed and found - mixed:
                wrong_script.append("%s: %s -> %s" % (lang, key, value))
            elif not only and not mixed:
                wrong_script.append("%s: %s -> %s" % (lang, key, value))
        if lang == "en":
            continue
        for key, value in entries.items():
            if key in BRAND_KEYS or key in TECHNICAL_KEYS:
                continue
            if en.get(key) == value and len(value.split()) >= 2:
                english_left.append("%s: %s -> %s" % (lang, key, value))

    print("Languages: %d   English keys: %d" % (len(tables) - 1, len(en)))
    print("Missing / leaking keys: %d" % len(errors))
    for line in errors[:40]:
        print("  " + line)
    print("Values in a script the language does not use: %d" % len(wrong_script))
    for line in wrong_script[:40]:
        print("  " + line)
    print("English text left in a language map: %d" % len(english_left))
    print("  (brand and technical strings excluded; %d of them are brands)"% len(BRAND_KEYS))
    per_key = {}
    for line in english_left:
        per_key.setdefault(line.split(": ")[1], []).append(line.split(": ")[0])
    for key, langs in sorted(per_key.items(), key=lambda kv: -len(kv[1]))[:30]:
        print("    %-32s %2d language(s): %s" % (key, len(langs), ", ".join(langs[:6]) + ("…" if len(langs) > 6 else "")))
    return 1 if (errors or wrong_script) else 0


if __name__ == "__main__":
    sys.exit(main())
