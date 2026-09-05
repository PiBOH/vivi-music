#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Inject the VIVI Music DE uninstall cleanup into a .deb's postrm script.

jpackage generates a .deb whose postrm only removes the installed files under
/opt. When the user uninstalls (`dpkg -r` / `apt remove`), dpkg runs postrm as
root, so we append a hook that keeps exactly ONE final backup of every user's
`~/.vivimusic` data and deletes all the caches (updates, audio, video/canvas,
lyrics, logs, …) — the same behavior the Windows uninstaller has.

The hook sources the shared `scripts/uninstall-cleanup.sh` at uninstall time.
Because postrm runs as root and the script is kept in the repo (not installed
under /opt, which is being removed), the hook needs the script at a stable
location: we copy it into `/usr/share/vivi-music-de/uninstall-cleanup.sh`
during install (see the postinst hook below) so postrm can find it even after
the /opt files are gone.
"""

import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

CLEANUP_SOURCE = Path(__file__).resolve().parent / "uninstall-cleanup.sh"
INSTALL_TARGET = "/usr/share/vivi-music-de/uninstall-cleanup.sh"

POSTRM_HOOK = """\
# --- VIVI Music DE uninstall cleanup (keeps one final backup, wipes caches) ---
if [ "$1" = "remove" ] || [ "$1" = "purge" ]; then
    if [ -f "{target}" ]; then
        sh "{target}" || true
    fi
fi
"""

POSTINST_HOOK = """\
# --- VIVI Music DE uninstall cleanup support file ---
mkdir -p /usr/share/vivi-music-de
for src in /opt/vivi-music-de/bin/uninstall-cleanup.sh /opt/VIVIMusic/bin/uninstall-cleanup.sh; do
    if [ -f "$src" ]; then
        cp -f "$src" "{target}" 2>/dev/null || true
        break
    fi
done
"""


def read_script() -> str:
    if not CLEANUP_SOURCE.is_file():
        print(f"error: {CLEANUP_SOURCE} not found", file=sys.stderr)
        sys.exit(1)
    return CLEANUP_SOURCE.read_text(encoding="utf-8")


def ensure_script_in_package(workdir: Path) -> None:
    """Embed the cleanup script into the package's /opt tree so the postinst
    hook can copy it to /usr/share. jpackage installs to /opt/VIVIMusic or
    /opt/vivi-music-de depending on the package name; we write it to both
    candidate trees if present, and to opt root otherwise."""
    script = read_script()
    candidates = [
        workdir / "opt" / "VIVIMusic" / "bin" / "uninstall-cleanup.sh",
        workdir / "opt" / "vivi-music-de" / "bin" / "uninstall-cleanup.sh",
    ]
    written = False
    for path in candidates:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(script, encoding="utf-8")
        os.chmod(path, 0o755)
        written = True
    if not written:
        print("warning: no /opt app dir found; cleanup script not embedded", file=sys.stderr)


def append_hook(script_path: Path, hook: str) -> None:
    if script_path.exists():
        text = script_path.read_text(encoding="utf-8")
        if not text.endswith("\n"):
            text += "\n"
        script_path.write_text(text + hook, encoding="utf-8")
    else:
        script_path.write_text("#!/bin/sh\n" + hook, encoding="utf-8")
        os.chmod(script_path, 0o755)


def main() -> None:
    if len(sys.argv) != 2:
        print("usage: patch_deb_postrm.py <file.deb>", file=sys.stderr)
        sys.exit(2)

    deb = Path(sys.argv[1]).resolve()
    if not deb.is_file():
        print(f"not found: {deb}", file=sys.stderr)
        sys.exit(1)

    workdir = Path(tempfile.mkdtemp(prefix="vivideb-postrm-"))
    try:
        subprocess.run(["dpkg-deb", "-R", str(deb), str(workdir)], check=True)

        ensure_script_in_package(workdir)
        append_hook(workdir / "DEBIAN" / "postrm", POSTRM_HOOK.format(target=INSTALL_TARGET))
        append_hook(workdir / "DEBIAN" / "postinst", POSTINST_HOOK.format(target=INSTALL_TARGET))

        os.remove(deb)
        subprocess.run(
            ["dpkg-deb", "--build", "--root-owner-group", str(workdir), str(deb)],
            check=True,
        )
        print(f"Repacked with uninstall cleanup: {deb}")
    finally:
        shutil.rmtree(workdir, ignore_errors=True)


if __name__ == "__main__":
    main()