#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Verify desktop localization completeness.

1. Extract every literal key used via Localization.get(...) in the desktop
   sources and check it exists in the English table (missing -> raw key shown
   in the UI, the worst failure).
2. For each of the 48 language tables in Localization.kt, report keys that are
   missing (they silently fall back to English at runtime).

Run from the repo root:  python3 scripts/check_localization.py
"""
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOC = os.path.join(
    REPO, "desktop", "src", "main", "kotlin", "com", "music", "vivi", "desktop", "Localization.kt"
)
SRC = os.path.join(REPO, "desktop", "src", "main", "kotlin")

text = open(LOC, encoding="utf-8").read()

# ---- parse the language tables: strings_N() -> {key: value} ----
lang_tables = {}
for m in re.finditer(r"private fun strings_(\d+)\(\): Map<String, String> =\s*mapOf\((.*?)\n\s*\)", text, re.S):
    idx = int(m.group(1))
    body = m.group(2)
    pairs = re.findall(r'"([^"]+)"\s+to\s+"((?:[^"\\]|\\.)*)"', body)
    lang_tables[idx] = {k: v for k, v in pairs}

if not lang_tables:
    print("ERROR: could not parse Localization.kt language tables")
    sys.exit(1)

# ---- map table index -> language code from the strings map ----
lang_of = {}
for m in re.finditer(r'"([A-Za-z-]+)"\s+to\s+strings_(\d+)\(\)', text):
    lang_of[int(m.group(2))] = m.group(1)

en_idx = next((k for k, v in lang_of.items() if v == "en"), None)
if en_idx is None:
    print("ERROR: English table not found")
    sys.exit(1)
english = lang_tables[en_idx]

# language code -> set of keys
by_lang = {}
for idx, table in lang_tables.items():
    code = lang_of.get(idx, f"strings_{idx}")
    by_lang[code] = set(table.keys())

langs = sorted(k for k in by_lang if k != "en")

# ---- 1. keys used in the desktop sources ----
used = set()
for root, _dirs, files in os.walk(SRC):
    for fn in files:
        if not fn.endswith(".kt"):
            continue
        path = os.path.join(root, fn)
        src_text = open(path, encoding="utf-8").read()
        # Localization.get(language, "key") / Localization.get("key") etc.
        for mm in re.finditer(r'Localization\.get\(\s*(?:language|[A-Za-z_][A-Za-z0-9_]*)\s*,\s*"([^"]+)"', src_text):
            used.add(mm.group(1))
        # any other string-literal key passed to get( as second arg
        for mm in re.finditer(r"\.get\(\s*\"([^\"]+)\"\s*\)", src_text):
            if mm.group(1) in english or mm.group(1) in used:
                used.add(mm.group(1))

missing_in_en = sorted(k for k in used if k not in english)
print(f"== used keys: {len(used)}, English table: {len(english)} ==")
print()
if missing_in_en:
    print(f"!!! {len(missing_in_en)} USED KEYS MISSING FROM ENGLISH TABLE (raw key shown in UI):")
    for k in missing_in_en:
        print(f"    {k}")
else:
    print("OK: every used key exists in the English table.")

print()

# ---- 2. per-language coverage vs English ----
print(f"== per-language coverage (keys falling back to English) ==")
print(f"languages: {len(langs)}")
print()
missing_by_lang = {}
for code in langs:
    missing = sorted(k for k in english if k not in by_lang[code])
    if missing:
        missing_by_lang[code] = missing

if missing_by_lang:
    total = sum(len(v) for v in missing_by_lang.values())
    print(f"!! {total} missing translations across {len(missing_by_lang)} languages "
          f"(keys silently show English):")
    for code in langs:
        if code in missing_by_lang:
            miss = missing_by_lang[code]
            print(f"   {code}: {len(miss)} missing -> {miss[:12]}{'...' if len(miss) > 12 else ''}")
else:
    print("OK: every language table contains every English key.")

print()
# sanity: english-only keys that no language uses
print(f"English table has {len(english)} keys total.")

# --dump-missing: print every key missing in every language (key lang text)
if "--dump-missing" in sys.argv:
    for code in langs:
        miss = sorted(k for k in english if k not in by_lang[code])
        for k in miss:
            print(f"MISS\t{code}\t{k}\t{english[k]}")
