# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-08-13

### Added
- Progetto Kotlin Multiplatform iniziale con target Android e Desktop (JVM).
- UI condivisa in Compose Multiplatform: Home con ricerca, Libreria (preferiti,
  cronologia, playlist), Impostazioni e barra "in riproduzione".
- Client InnerTube (YouTube Music) in `commonMain` per ricerca, home feed,
  album/playlist e risoluzione del flusso audio.
- Persistenza locale con Room KMP (entità per brani, playlist, preferiti,
  cronologia e stato di sincronizzazione).
- Sincronizzazione dati utente con Supabase (PostgREST + Realtime + Auth) e
  `SyncManager` con pull/push bidirezionale e mirroring in tempo reale.
- Supporto multilingua per 49 lingue con selezione manuale o lingua di sistema.
- Workflow GitHub Actions di CI (build su push/PR) e CD (artifact Android e
  desktop per Windows/macOS/Linux).
- Migrazione SQL Supabase iniziale con Row Level Security e Realtime.
- File `AGENTS.md` con convenzioni di progetto, regole SemVer e changelog.
