# Vivi Music DE

Client musicale desktop e mobile basato su **Kotlin Multiplatform** e **Compose
Multiplatform**, che ripropone l'esperienza di [ViVi Music](https://github.com/25huizengek1/ViMusic)
(client YouTube Music open source). La UI e la logica sono condivise tra
**Android** e **Desktop (Windows/macOS/Linux)** e i dati utente (playlist,
preferiti, cronologia) si sincronizzano in tempo reale tramite **Supabase**.

## Caratteristiche

- Target **Android** e **Desktop (JVM)** da un unico codebase.
- UI in **Compose Multiplatform** (Material 3): Home con ricerca, Libreria,
  Impostazioni e barra "in riproduzione".
- Catalogo **YouTube Music** via client InnerTube (ricerca, home feed,
  album/playlist, risoluzione flusso audio).
- Database locale **Room KMP** con driver SQLite bundled.
- Sincronizzazione **Supabase** (PostgREST + Realtime + Auth) con mirroring in
  tempo reale tra dispositivi.
- **49 lingue** supportate, con selezione manuale o lingua di sistema.
- **CI/CD** con GitHub Actions.

## Requisiti di build

- JDK 17+ (Gradle 8.x non supporta JDK 25; il packaging desktop con JDK 17
  abbassa il requisito runtime a macOS 10.15+ e Windows 10+).
- Android SDK con platform 36 (solo per il target Android).
- Un progetto Supabase (opzionale, per la sincronizzazione).

## Requisiti di sistema (runtime)

- **Windows**: Windows 10 o successivo (x86-64). Windows 7/8/8.1 non sono
  supportati perche il runtime JDK 17+ richiede Windows 10+.
- **macOS**: macOS 10.15 (Catalina) o successivo, sia Intel che Apple Silicon.
- **Linux**: Debian/Ubuntu tramite `.deb`; Arch Linux e altre distribuzioni
  (glibc) tramite `.AppImage`.
- **CPU**: processore x86-64 con istruzioni SSE2 (es. Intel Core i5-650 o
  superiore). Non e richiesto il supporto AVX.

## Build ed esecuzione

```bash
# Desktop: compila e avvia
./gradlew :composeApp:run

# Desktop: installer nativo per il sistema corrente
./gradlew :composeApp:packageDistributionForCurrentOS

# Android: APK di debug
./gradlew :composeApp:assembleDebug

# Android: installa su dispositivo/emulatore collegato
./gradlew :composeApp:installDebug

# Tutti i controlli
./gradlew build
```

## Release automatico

La versione canonica dell'app e in `version.txt` (formato SemVer, es.
`0.0.1-alpha`).

Per pubblicare una GitHub Release:
1. Aggiorna `version.txt` e `CHANGELOG.md`.
2. Commit e push con un messaggio che inizia con `v` (es. `v0.0.1-alpha: ...`),
   oppure esegui manualmente il workflow `auto-release.yml`.

Il workflow compila l'APK Android e gli installer desktop
(Windows/macOS/Linux) e crea la release con gli artifact allegati.

## Configurazione Supabase

1. Crea un progetto su [Supabase](https://supabase.com).
2. Esegui `supabase/migrations/0001_init.sql` nell'SQL editor (crea tabelle,
   RLS e abilita Realtime).
3. Fornisci le credenziali:
   - **Android**: aggiungi a `local.properties` (file git-ignorato):
     ```properties
     supabase.url=https://tuo-progetto.supabase.co
     supabase.anonKey=la-tua-anon-key
     ```
   - **Desktop**: imposta le variabili d'ambiente `SUPABASE_URL` e
     `SUPABASE_ANON_KEY`, oppure crea un file `supabase.env` con
     `SUPABASE_URL=...` e `SUPABASE_ANON_KEY=...`.

Senza credenziali l'app funziona in modalità solo-locale.

## Struttura del progetto

Vedi [`AGENTS.md`](AGENTS.md) per architettura, convenzioni, regole di
versionamento (SemVer) e istruzioni per aggiungere nuove lingue.

## Nota

Il motore di riproduzione audio (player) è il prossimo passo: l'app risolve già
il flusso audio dei brani ma non lo riproduce ancora. Il client InnerTube è un
sottoinsieme funzionante del modulo completo del progetto originale.
