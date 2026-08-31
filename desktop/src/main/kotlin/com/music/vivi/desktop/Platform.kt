package com.music.vivi.desktop

import java.io.File

enum class DesktopOs {
    WINDOWS,
    MACOS,
    LINUX,
}

enum class DesktopArch {
    X64,
    ARM64,
}

/** Host platform detection used to pick the right update installer asset. */
object Platform {
    val os: DesktopOs = when {
        System.getProperty("os.name").lowercase().contains("win") -> DesktopOs.WINDOWS
        System.getProperty("os.name").lowercase().contains("mac") -> DesktopOs.MACOS
        else -> DesktopOs.LINUX
    }

    val arch: DesktopArch = when (System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> DesktopArch.ARM64
        else -> DesktopArch.X64
    }

    /** True when the host runs a Debian (or Debian-derived, e.g. Ubuntu) Linux
     *  distro — the only Linux family where a `.deb` is the native package. */
    val isDebianBased: Boolean = os == DesktopOs.LINUX && runCatching {
        File("/etc/os-release").readLines().any { line ->
            val trimmed = line.trim()
            val key = trimmed.substringBefore('=').uppercase()
            if (key == "ID" || key == "ID_LIKE") {
                trimmed.substringAfter('=', "")
                    .trim('"', '\'')
                    .split(' ', '\t')
                    .any { it.equals("debian", ignoreCase = true) || it.equals("ubuntu", ignoreCase = true) }
            } else {
                false
            }
        }
    }.getOrDefault(false)
}
