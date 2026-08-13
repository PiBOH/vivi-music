# AGENTS.md

Istruzioni operative per gli agenti AI (e per gli sviluppatori) che lavorano su
questo repository. Leggere integralmente questo file prima di modificare il
codice.

## 1. Panoramica del progetto

**Vivi Music DE** è un client musicale desktop e mobile basato su
**Kotlin Multiplatform (KMP)** e **Compose Multiplatform**, che ripropone
l'esperienza di ViVi Music (client YouTube Music open source). Il codice e la UI
sono condivisi tra i target **Android** e **Desktop (JVM: Windows/macOS/Linux)**
e i dati utente (playlist, preferiti, cronologia) si sincronizzano in tempo
reale tramite **Supabase**.

### Stack tecnologico

| Area          | Tecnologia                                                              |
|---------------|-------------------------------------------------------------------------|
| Linguaggio    | Kotlin (2.4.x)                                                          |
| UI            | Compose Multiplatform + Material 3                                      |
| Build         | Gradle (Kotlin DSL), version catalog in `gradle/libs.versions.toml`     |
| Rete          | Ktor Client (OkHttp su Android, CIO su Desktop)                         |
| Database      | Room KMP (`androidx.room3`), driver SQLite bundled                       |
| Sincronizzaz. | Supabase (supabase-kt: PostgREST + Realtime + Auth)                     |
| i18n          | Compose Multiplatform resources (`composeResources/**`)                 |
| CI/CD         | GitHub Actions (`.github/workflows/ci.yml` e `auto-release.yml`)         |

### Struttura a moduli

```
vivi-music-de/
├── .github/workflows/          # ci.yml (CI) e auto-release.yml (release)
├── gradle/
│   ├── libs.versions.toml      # version catalog
│   └── wrapper/                # Gradle wrapper
├── composeApp/                 # unico modulo KMP (app Android + Desktop)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/         # codice condiviso
│       │   ├── composeResources/   # risorse stringa (values/ e values-XX/)
│       │   └── kotlin/com/vivimusic/de/
│       │       ├── data/           # db, network, sync, repository, container
│       │       ├── domain/         # modelli di dominio (Song, Playlist, ...)
│       │       ├── i18n/           # lingue e gestione locale
│       │       └── ui/             # schermate Compose condivise
│       ├── androidMain/        # MainActivity, manifest, actual Android
│       └── desktopMain/        # main.kt, actual Desktop
├── supabase/migrations/        # schema SQL + RLS + Realtime
├── AGENTS.md
└── CHANGELOG.md
```

Il progetto usa un **singolo modulo Gradle** (`composeApp`) con i tre source
set `commonMain`, `androidMain` e `desktopMain`. Il pattern `expect`/`actual` è
usato per le parti platform-specific (HTTP engine, builder del database,
persistenza impostazioni, gestione del locale).

### Flusso dei dati

- **UI** (`ui/`) -> **AppViewModel** -> **MusicRepository** -> database locale /
  **InnerTubeClient** (catalogo YouTube Music).
- **SyncManager** orchestra la sincronizzazione tra il database Room locale e
  **SupabaseSyncClient** (PostgREST per pull/push, Realtime per il mirroring).

## 2. Convenzioni di codice

- **Linguaggio**: codice, commenti e messaggi di commit in inglese. (Questo
  file e la documentazione rivolta all'utente possono essere in italiano.)
- **Package**: `com.vivimusic.de`.
- **Stile**: seguire `kotlin.code.style=official` (già configurato).
- **Nomi**: data class e classi in PascalCase, funzioni/proprietà in camelCase,
  costanti in UPPER_SNAKE_CASE.
- **Coroutine**: usare `suspend` per le operazioni I/O e `Flow`/`StateFlow` per
  lo stato reattivo. Non bloccare mai il thread UI.
- **Risorse**: ogni stringa visibile all'utente deve essere una risorsa
  (`Res.string.*`), mai una stringa hardcoded.
- **Dipendenze**: aggiungere le versioni solo in `gradle/libs.versions.toml`.
  Non introdurre nuove librerie senza necessità e senza verificarne la presenza
  nel progetto.
- **Errori**: non ingoiare le eccezioni; gestirle o propagarle in modo esplicito.
- **Niente emoji** in codice, commenti, stringhe, log, risorse e workflow.

## 3. Regola d'oro: "Quello che funziona non si tocca"

> **Non rifattorizzare o modificare moduli, classi o funzioni già funzionanti e
> stabili, a meno che non sia strettamente necessario per la nuova feature
> richiesta o su richiesta esplicita dell'utente.**

- Prima di toccare codice esistente, verificare che serva davvero al task.
- Preferire aggiunte non invasive (nuovi file, estensioni, parametri con
  default) rispetto a riscritture.
- Se una modifica a codice stabile è inevitabile, motivarla nel commit e nel
  changelog.
- Dopo ogni modifica, eseguire build e test per confermare che nulla si è rotto.

## 4. Versionamento (SemVer)

Ogni incremento di versione deve seguire rigorosamente il **Semantic Versioning**
(`MAJOR.MINOR.PATCH`), definito in https://semver.org:

- **MAJOR**: cambiamenti incompatibili con le versioni precedenti.
- **MINOR**: nuove funzionalità retrocompatibili.
- **PATCH**: correzioni di bug retrocompatibili.

La versione canonica dell'app è dichiarata in `version.txt` alla radice del
repository (formato SemVer, es. `0.0.1-alpha`). Il build la legge e la usa come
`defaultConfig.versionName` su Android. Il `packageVersion` degli installer
desktop è un valore numerico separato (jpackage richiede `MAJOR >= 1` e non
accetta suffissi di prerelease).

## 5. Changelog (Keep a Changelog)

Il file `CHANGELOG.md` segue lo standard **Keep a Changelog**
(https://keepachangelog.com). **Va aggiornato obbligatoriamente a ogni modifica
importante**, usando le sezioni:

- `Added` — nuove funzionalità.
- `Changed` — modifiche a funzionalità esistenti.
- `Deprecated` — funzionalità deprecate.
- `Removed` — funzionalità rimosse.
- `Fixed` — correzioni di bug.
- `Security` — correzioni di sicurezza.

Ogni entry è sotto una sezione `## [VERSIONE] - YYYY-MM-DD`. Non cancellare le
entry passate.

## 6. Localizzazione (i18n)

- Le stringhe vivono in `composeApp/src/commonMain/composeResources/`.
- `values/strings.xml` è il **default inglese** e contiene l'elenco canonico
  delle chiavi.
- Ogni lingua ha `values-<qualifier>/strings.xml` (es. `values-it`, `values-de`,
  `values-zh-rCN`, `values-zh-rTW`). Le chiavi assenti ricadono sull'inglese.
- L'elenco delle lingue supportate è in
  `i18n/AppLanguage.kt` (`supportedLanguages`), con codice BCP-47 e nome nativo.
- La selezione manuale usa il pattern `expect/actual LocalAppLocale`
  (`i18n/Locale.kt` + actual per piattaforma); la scelta è persistita tramite
  `SettingsStore` (`data/SettingsStore.kt`).

### Aggiungere una nuova lingua

1. Aggiungere una voce a `supportedLanguages` in `i18n/AppLanguage.kt`.
2. Creare `composeApp/src/commonMain/composeResources/values-XX/strings.xml`
   (sostituire `XX` con il qualifier della lingua) traducendo le chiavi del
   default `values/strings.xml`.
3. Non è richiesta la traduzione di tutte le chiavi: quelle mancanti ricadono
   sull'inglese.
4. Aggiornare `CHANGELOG.md` (sezione `Added`).

## 7. Sincronizzazione Supabase

- La configurazione è letta a runtime in `data/AppConfig.kt`:
  - **Android**: da `local.properties` (`supabase.url`, `supabase.anonKey`)
    iniettate in `BuildConfig` (vedi `composeApp/build.gradle.kts`).
  - **Desktop**: dalle variabili d'ambiente `SUPABASE_URL`/`SUPABASE_ANON_KEY`
    o dal file `supabase.env`.
- Lo schema (tabelle, RLS, Realtime) è in `supabase/migrations/0001_init.sql`.
- Se le credenziali mancano, l'app gira in modalità solo-locale
  (`SyncStatus.Disabled`).
- Il sync attuale fa push dell'intero dataset locale (niente dirty flag): è
  corretto e semplice per librerie piccole; per librerie grandi va introdotto
  un tracking delle modifiche per riga.

## 8. Build, test ed esecuzione

Requisiti: JDK 17+ (consigliato 17 per il packaging con massima compatibilità;
Gradle 8.x non supporta JDK 25), Android SDK con platform 36 (per il target
Android), connessione di rete per scaricare le dipendenze.

```bash
# Desktop: compila e avvia l'app
./gradlew :composeApp:run

# Desktop: genera l'installer nativo per il sistema corrente (.msi/.dmg/.deb/...)
./gradlew :composeApp:packageDistributionForCurrentOS

# Android: compila l'APK di debug
./gradlew :composeApp:assembleDebug

# Android: installa ed esegue su un dispositivo/emulatore collegato
./gradlew :composeApp:installDebug

# Tutti i controlli (test + build)
./gradlew build

# Pulizia
./gradlew clean
```

Nota su Windows: JDK 25 (o superiori) può non essere supportato da Gradle 8.x;
usare JDK 21 (es. `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot`).

## 9. Workflow GitHub Actions

- **CI** (`.github/workflows/ci.yml`): su ogni push a `main` e pull request
  compila il target Android (`assembleDebug`) e quello Desktop
  (`compileKotlinDesktop`) e carica l'APK di debug come artifact.
- **Auto Release** (`.github/workflows/auto-release.yml`): crea una GitHub
  Release automatica. Si attiva su:
  - un push a `main` il cui messaggio di commit inizia con `v` (es.
    `v0.0.1-alpha: ...`), oppure
  - `workflow_dispatch` manuale (con versione opzionale).
  La versione è letta da `version.txt`; la sezione di `CHANGELOG.md`
  corrispondente è usata come note di rilascio. Il workflow compila:
  - Android `assembleRelease` (APK) su `ubuntu-latest` (JDK 21);
  - gli installer desktop con JDK 17 in matrice `ubuntu-latest`,
    `windows-latest`, `macos-15-intel` e `macos-15` (dual-arch macOS).
  Gli artifact sono allegati alla release (tag = versione senza prefisso `v`).
- Per il rilascio firmato dell'APK e per il packaging MSI su Windows (che
  richiede WiX) consultare la documentazione e aggiungere i segreti/strumenti
  necessari.

### Rilasciare una nuova versione

1. Aggiornare `version.txt` con la nuova versione SemVer.
2. Aggiornare `CHANGELOG.md` con la sezione `## [VERSIONE] - YYYY-MM-DD`.
3. Commit con messaggio che inizia con `v` ed eseguire push:
   `git commit -m "v$(cat version.txt): descrizione" && git push`.

## 10. Definizione di "Done"

Un task è completo solo quando:

1. il codice compila (`./gradlew build`) e i test passano;
2. non sono state introdotte emoji nel codice/risorse/workflow;
3. non è stato rifattorizzato codice stabile senza necessità;
4. `CHANGELOG.md` è aggiornato (sezione corretta) quando la modifica è rilevante;
5. `version.txt` e `CHANGELOG.md` rispettano SemVer/Keep a Changelog se è
   cambiato il comportamento dell'app.
