#!/bin/sh
# VIVI Music DE — uninstall cleanup for Linux/macOS.
#
# On uninstall, the app's user data directory (~/.vivimusic) is a mix of real
# user data (settings, playlists, imported fonts) and disposable caches
# (downloaded updates, audio/video/canvas/lyrics caches, logs, extracted helper
# libraries, artwork). This script keeps exactly ONE final backup of the user
# data and deletes everything else, so a reinstall starts from a clean slate
# without losing settings/playlists.
#
# Usage:
#   uninstall-cleanup.sh                 # uses $HOME/.vivimusic
#   uninstall-cleanup.sh /home/alice     # uses /home/alice/.vivimusic
#
# The backup is stored at:
#   <home>/.vivimusic/backups/uninstall-<timestamp>/
# containing device-sync.json, playlists.json and fonts/ (when present).
# Only the most recent backup survives; all older backups and every cache are
# removed.

set -u

cleanup_dir() {
  HOME_DIR="$1"
  VIVI_DIR="$HOME_DIR/.vivimusic"

  if [ ! -d "$VIVI_DIR" ]; then
    echo "VIVI Music DE: no user data at $VIVI_DIR — nothing to clean."
    return 0
  fi

  TS="$(date +%Y%m%d_%H%M%S 2>/dev/null || date +%Y%m%d)"
  BACKUPS_DIR="$VIVI_DIR/backups"
  BACKUP_DIR="$BACKUPS_DIR/uninstall-$TS"

  echo "VIVI Music DE: creating final backup at $BACKUP_DIR"
  mkdir -p "$BACKUP_DIR" || { echo "error: cannot create $BACKUP_DIR" >&2; return 1; }

  # Copy the real user data (best effort per file).
  [ -f "$VIVI_DIR/device-sync.json" ] && cp -f "$VIVI_DIR/device-sync.json" "$BACKUP_DIR/" 2>/dev/null
  [ -f "$VIVI_DIR/playlists.json" ] && cp -f "$VIVI_DIR/playlists.json" "$BACKUP_DIR/" 2>/dev/null
  if [ -d "$VIVI_DIR/fonts" ]; then
    mkdir -p "$BACKUP_DIR/fonts"
    cp -rf "$VIVI_DIR"/fonts/* "$BACKUP_DIR/fonts/" 2>/dev/null
  fi

  # Delete everything under ~/.vivimusic except the backups folder.
  for entry in "$VIVI_DIR"/* "$VIVI_DIR"/.[!.]*; do
    [ -e "$entry" ] || continue
    case "$(basename "$entry")" in
      backups) continue ;;
    esac
    rm -rf "$entry"
    echo "VIVI Music DE: removed $entry"
  done

  # Inside backups/, keep only the backup we just created.
  if [ -d "$BACKUPS_DIR" ]; then
    for entry in "$BACKUPS_DIR"/* "$BACKUPS_DIR"/.[!.]*; do
      [ -e "$entry" ] || continue
      case "$(basename "$entry")" in
        "uninstall-$TS") continue ;;
      esac
      rm -rf "$entry"
    done
  fi

  echo "VIVI Music DE: uninstall cleanup complete. Only the final backup remains."
}

# Clean the invoking user, plus every other home directory when running as
# root (dpkg postrm / AUR post_remove run as root on multi-user systems).
if [ "$(id -u 2>/dev/null)" = "0" ]; then
  for h in /home/*; do
    [ -d "$h" ] && cleanup_dir "$h"
  done
else
  cleanup_dir "${1:-$HOME}"
fi