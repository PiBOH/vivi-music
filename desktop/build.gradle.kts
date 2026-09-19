import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// version.txt is the single source of truth for release metadata. Layout:
//   line 1 = mobile (Android) version, e.g. 6.4.21
//   line 2 = mobile version code (monotonic int)
//   line 3 = mobile release channel (stable / rc / beta / alpha / nightly)
//   line 4 = desktop ("DE") version, e.g. 1.33.52 (the program's own SemVer)
//   line 5 = desktop version code (monotonic counter)
//   line 6 = desktop release channel (stable / rc / beta / alpha / nightly)
//   (comment lines, prefixed with '#', may follow and are ignored)
// The human-readable desktop version is "<mobile>_DE-<de>", e.g. 6.4.21_DE-1.33.52.
val versionLines: List<String> = rootProject.file("version.txt")
    .takeIf { it.exists() }
    ?.readLines()
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() && !it.startsWith("#") }
    ?: emptyList()

val mobileVersion: String = versionLines.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: "0.0.0"
val deVersion: String = versionLines.getOrNull(3)?.takeIf { it.isNotEmpty() } ?: "0.0.0"
val releaseChannel: String = versionLines.getOrNull(5)?.takeIf { it.isNotEmpty() } ?: "stable"
val fullVersion: String = "${mobileVersion}_DE-${deVersion}"

// Installers/package managers require a purely numeric MAJOR.MINOR.PATCH on
// Windows and macOS (jpackage JDK-8283707, Inno Setup AppVersion). That numeric
// version is the DE version — the part after "DE-".
val numericPackageVersion: String = deVersion.substringBefore('+').substringBefore('-')

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// Ship version.txt + CHANGELOG.md + contributorsde.json as classpath resources
// so the About screen can read build metadata, the changelog and the dynamic
// contributor list at runtime (config-cache friendly).
tasks.processResources {
    from(rootProject.file("version.txt"))
    from(rootProject.file("CHANGELOG.md"))
    from(rootProject.file("contributorsde.json"))
}

kotlin {
    jvmToolchain(21)
}

// --- Keep every installer small: runtime icon minimization -----------------
// The extended Material icons artifact bundles ~10k vector icons (~36 MB) while
// the desktop app references fewer than a hundred. Compilation must keep using
// the full artifact (every reference resolves at compile time), but no packaged
// runtime ever needs the other icons: this task copies the full jar into a
// minimized jar that keeps every non-icon class plus the per-icon classes whose
// icon name is referenced in desktop/src (all styles). The minimized jar
// replaces the original on the RUNTIME classpath — the classpath that dev
// `run`, tests and every jpackage setup (hence every installer) consume.
//
// The extended jar holds ONLY per-icon `...Kt.class` files: the `Icons`
// accessors the sources use (`Icons.Filled.Home`, `Icons.Outlined.X`, ...) live
// in the separate material-icons-core artifact, which stays on the runtime
// classpath on purpose — see the runtimeClasspath block at the end of this
// file. Once minimized, this task can therefore never fix a missing `Icons$...`
// class by keeping more entries: that class was never in this jar.
abstract class MinimizeIconsJarTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    // Compile classpath: contains the full extended icons jar + core jar.
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val compileClasspath: ConfigurableFileCollection

    @get:OutputFile
    abstract val minimizedJar: RegularFileProperty

    @TaskAction
    fun minimize() {
        val styleDirs = setOf("filled", "outlined", "rounded", "sharp", "twotone", "automirrored")
        // Base accessor names kept unconditionally so `Icons.*` never breaks.
        val used = mutableSetOf("icons", "automirrored")
        fun normalize(name: String): String = name.lowercase().removePrefix("kt").trimStart('_')
        val iconRef = Regex(
            """Icons\.(?:Filled|Outlined|Rounded|Sharp|TwoTone|AutoMirrored)(?:\.(?:Filled|Outlined|Rounded|Sharp|TwoTone))?\.([A-Za-z_][A-Za-z0-9_]*)"""
        )
        sources.files.filter { it.isDirectory }.forEach { dir ->
            dir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                iconRef.findAll(file.readText()).forEach { used.add(normalize(it.groupValues[1])) }
            }
        }

        val fullJarFile = compileClasspath.files.first { it.name.startsWith("material-icons-extended-desktop") }
        val coreJarFile = compileClasspath.files.firstOrNull { it.name.startsWith("material-icons-core-desktop") }

        // Per-icon classes already provided by the core artifact must not be
        // duplicated on the runtime classpath.
        val coreClasses = HashSet<String>()
        coreJarFile?.let { core ->
            ZipInputStream(core.inputStream().buffered()).use { zin ->
                while (true) {
                    val entry = zin.nextEntry ?: break
                    if (!entry.isDirectory && entry.name.endsWith(".class")) coreClasses.add(entry.name)
                    zin.closeEntry()
                }
            }
        }

        val out = minimizedJar.get().asFile
        out.parentFile.mkdirs()
        var kept = 0
        var total = 0
        ZipInputStream(fullJarFile.inputStream().buffered()).use { zin ->
            ZipOutputStream(out.outputStream().buffered()).use { zout ->
                while (true) {
                    val entry = zin.nextEntry ?: break
                    total++
                    val keep = when {
                        entry.isDirectory -> true
                        entry.name.endsWith(".class") && coreClasses.contains(entry.name) -> false
                        !entry.name.endsWith(".class") -> true
                        !entry.name.startsWith("androidx/compose/material/icons/") -> true
                        else -> {
                            val simple = entry.name.substringAfterLast('/').removeSuffix(".class")
                            if (!simple.endsWith("Kt")) true
                            else {
                                val segments = entry.name.split('/')
                                val inStyleDir = segments.size >= 2 && segments[segments.size - 2] in styleDirs
                                !inStyleDir || normalize(simple.removeSuffix("Kt")) in used
                            }
                        }
                    }
                    if (keep) {
                        kept++
                        zout.putNextEntry(ZipEntry(entry.name))
                        zin.copyTo(zout)
                        zout.closeEntry()
                    }
                    zin.closeEntry()
                }
            }
        }
        logger.lifecycle("minimizeIconsJar: kept $kept/$total entries (referenced icon names: ${used.size})")
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Material 3 tonal color palette (same seed-based scheme as the Android app)
    implementation(libs.materialKolor)

    // Reused JVM-pure network/parsing modules (same code as the Android app)
    implementation(project(":innertube"))
    implementation(project(":spotify"))
    implementation(project(":lastfm"))
    implementation(project(":kizzy"))
    implementation(project(":shazamkit"))
    implementation(project(":jiosaavn"))
    implementation(project(":lyricsProvider"))
    implementation(project(":sync"))
    implementation(project(":canvas"))
    implementation(project(":applecanvas"))
    implementation(project(":vivimusiccanvas"))

    implementation(libs.kotlinx.coroutines.core)
    // Provides the Swing-based Main dispatcher backed by the AWT/Swing event
    // dispatch thread, required by Dispatchers.Main on desktop. Without it
    // running code that hops back to the Main dispatcher throws
    // "Module with the Main dispatcher is missing". Same version as core.
    implementation(libs.kotlinx.coroutines.swing)

    // Thumbnail / artwork loading (Coil 3, desktop JVM support)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // Pure-Java MP4 (incl. fragmented/DASH fMP4) demuxer + bundled JAAD AAC decoder
    // for self-contained desktop audio playback.
    implementation("org.jcodec:jcodec:0.2.5")

    // Native OS system-volume access (WinMM wave output on Windows) so the
    // Android system volume can be mirrored on the desktop and vice versa.
    implementation("net.java.dev.jna:jna:5.14.0")
    // Cross-platform global key hook (macOS/Linux media keys). The Windows
    // path keeps its own low-level hook (see MediaKeys.kt); JNativeHook is
    // only used on the other OSes, where it needs macOS Accessibility
    // permission (fails gracefully when not granted).
    implementation("com.github.kwhat:jnativehook:2.2.2")

    // JavaFX WebView for the embedded YouTube sign-in window. The platform
    // classifier is picked from the BUILDING machine: each OS CI job builds its
    // own package, so the packaged app always ships the matching natives.
    val buildOs = System.getProperty("os.name").lowercase()
    val buildArm = System.getProperty("os.arch").lowercase().let { it.contains("aarch64") || it.contains("arm") }
    val javafxClassifier = when {
        buildOs.contains("win") -> "win"
        buildOs.contains("mac") || buildOs.contains("darwin") -> if (buildArm) "mac-aarch64" else "mac"
        else -> if (buildArm) "linux-aarch64" else "linux"
    }
    val javafxVersion = "21.0.4"
    implementation("org.openjfx:javafx-base:$javafxVersion:$javafxClassifier")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$javafxClassifier")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$javafxClassifier")
    implementation("org.openjfx:javafx-media:$javafxVersion:$javafxClassifier")
    implementation("org.openjfx:javafx-web:$javafxVersion:$javafxClassifier")

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Local LAN relay (embedded WebSocket server) for offline device pairing
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)

    // QR code (ZXing) + mDNS service registration (JmDNS) for LAN discovery
    implementation(libs.zxing.core)
    implementation(libs.jmdns)

    // Drag-to-reorder for the Queue screen (same lib as the Android app)
    implementation(libs.compose.reorderable)
}

val minimizeIconsJar = tasks.register<MinimizeIconsJarTask>("minimizeIconsJar") {
    group = "build"
    description = "Ships only the Material icons the desktop app actually references"
    sources.from(files("src/main/kotlin", "src/main/java"))
    compileClasspath.from(configurations.compileClasspath)
    minimizedJar.set(layout.buildDirectory.file("libs/material-icons-extended-desktop-minimized.jar"))
}

// Compilation keeps the full icons artifact; the packaged runtime (dev run,
// tests and every jpackage/Inno Setup image) gets the minimized jar instead.
configurations.runtimeClasspath {
    exclude(group = "org.jetbrains.compose.material", module = "material-icons-extended-desktop")
}
dependencies {
    runtimeOnly(files(minimizeIconsJar))

    // The extended artifact is the ONLY route through which material-icons-core
    // reaches this project (extended -> core), and core is the artifact that
    // declares the `Icons` accessors the sources use (`Icons.class`,
    // `Icons$Filled`, `Icons$Outlined`, ...); the extended jar itself only holds
    // the per-icon `...Kt.class` files. Excluding the extended artifact above
    // therefore took core away with it, and every build from 1.50.72 to 1.50.74
    // crashed on the very first icon it drew — the crash dump reads
    // `NoClassDefFoundError: androidx/compose/material/icons/Icons$Outlined`
    // from `MainKt.Sidebar`, i.e. the app never got past its first frame.
    //
    // So the exclude above may only ever drop the ~36 MB extended jar, and core
    // is put back here. Its version is read from the already-resolved compile
    // classpath instead of being written out by hand, so it can never drift away
    // from the version the Compose plugin selects; the `check` turns a future
    // change that removes core from the graph into a loud packaging failure
    // instead of a release that crashes on startup for every user.
    runtimeOnly(
        files(
            configurations.compileClasspath.map { compileClasspath ->
                val iconsCore = compileClasspath.files.filter {
                    it.name.startsWith("material-icons-core-desktop")
                }
                check(iconsCore.isNotEmpty()) {
                    "material-icons-core-desktop is missing from the compile classpath: without it " +
                        "the runtime classpath has no `androidx.compose.material.icons.Icons` " +
                        "accessor classes and the packaged app crashes on its first icon " +
                        "(NoClassDefFoundError: Icons\$Outlined)."
                }
                iconsCore
            },
        ),
    )
}

compose.desktop {
    application {
        mainClass = "com.music.vivi.desktop.MainKt"

        // JVM tuning for glitch-free audio (issue #4).
        //
        // Audio is played by a Java thread that hands PCM to the sound card, and
        // a stop-the-world pause freezes that thread no matter how much audio
        // the app has buffered in its own queues: the device ring drains while
        // the JVM is frozen, and the user hears a gap (the same pause is what
        // makes the UI hitch at that moment).
        //
        // The default heap is 25% of the machine's RAM (a 6 GB max heap on a
        // 24 GB Mac, as reported in a user's system info) and G1 is then free to
        // grow the young generation up to 60% of it — i.e. collection sizes (and
        // pauses) far beyond what a music player needs. Capping the heap and the
        // young generation, and asking G1 for a 20 ms pause target, keeps the
        // stop-the-world part in the low-millisecond range. All flags are
        // product flags (no -XX:+UnlockExperimentalVMOptions needed) and were
        // verified to start on Temurin 21.
        jvmArgs += listOf(
            "-Xmx2g",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=20",
            "-XX:NewSize=128m",
            "-XX:MaxNewSize=384m",
            "-XX:MaxMetaspaceSize=256m",
            // Skiko (Compose Desktop's renderer) calls `System.gc()` every 30 s by
            // design, to trim memory on a parked window. Its `FrameWatcher`
            // coroutine reads `gcDelayMillis = 30000`, waits, and does
            // `if (frameCounter.get() < minFramesToRenderer /* 1000 */) System.gc()`,
            // i.e. whenever the UI is not animating at high frame rate. The
            // counter is reset each round, so a player that only redraws a seek
            // bar never reaches 1000 frames and gets the call every single time.
            // An explicit gc() is a **full, stop-the-world** collection: measured
            // on the packaged 1.50.76 image with -Xlog:gc it is `Pause Full
            // (System.gc())` every 30.05 s, 46-69 ms on a fresh session and up to
            // 1976 ms once the session has grown, which freezes the audio writer
            // thread and the UI together (the "pauses/skips + UI hitch" of #4).
            // This flag turns every explicit gc() into a concurrent G1 cycle
            // instead: same memory reclamation, no stop-the-world full GC. Same
            // run after the flag: zero `Pause Full`, the 30 s event becomes a
            // bounded 2.9-9.9 ms young pause that the 8 s PCM queue absorbs.
            "-XX:+ExplicitGCInvokesConcurrent",
        )

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Deb,
            )
            // jlink bundles only a minimal set of modules by default. The dev
            // tools (CPU/RAM/thread stats) use `java.lang.management` and the
            // richer `com.sun.management` bean; without these modules the
            // packaged launcher crashes on startup with "Failed to launch JVM".
            // jlink includes only JDK modules. JavaFX is shipped as regular runtime
            // dependencies in the application image and is launched via Stage,
            // so the packaged embedded WebView does not need Swing interop.
            // jdk.jsobject provides netscape.javascript.JSObject, which the
            // JavaFX WebView requires at runtime — without it WebView creation
            // throws NoClassDefFoundError in packaged builds.
            //
            // JavaFX is shipped as CLASSPATH jars, so jlink cannot see its
            // module requirements. Missing modules silently break the WebView:
            //  - java.net.http  → javafx.web requires it; without it the page
            //    load hangs at RUNNING forever (white window, no error dialog);
            //  - jdk.unsupported → sun.misc.Unsafe needed by the Marlin 2D
            //    rendering engine; without it QuantumRenderer aborts painting
            //    (white window).
            // Both verified with a limited-modules reproduction (DE 1.34.2).
            modules(
                "java.management", "jdk.management", "jdk.jsobject",
                "java.net.http", "jdk.unsupported",
            )
            packageName = "VIVIMusic"
            packageVersion = numericPackageVersion
            description = "VIVI Music Desktop Edition"
            vendor = "PiBOH"

            windows {
                menuGroup = "VIVI Music"
                // Machine-wide install into Program Files (requires admin/UAC).
                perUserInstall = false
                installationPath = "C:/Program Files/VIVIMusic"
                iconFile.set(project.file("icons/logo_vmde.ico"))
            }

            linux {
                debMaintainer = "PiBOH"
                appCategory = "Audio"
                iconFile.set(project.file("icons/logo_vmde.png"))
            }

            macOS {
                bundleID = "com.vivi.vivimusic.desktop"
                minimumSystemVersion = "10.15"
                iconFile.set(project.file("icons/logo_vmde.icns"))
            }
        }
    }
}
