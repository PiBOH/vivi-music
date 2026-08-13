package com.vivimusic.de.data.update

import java.awt.Desktop
import java.io.File

actual fun updateAssetSuffixes(): List<String> {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        "win" in osName -> listOf(".msi", "-portable.exe", ".exe")
        "mac" in osName || "darwin" in osName -> listOf(".dmg")
        else -> listOf(".appimage", ".deb")
    }
}

actual fun saveAndLaunchUpdate(fileName: String, bytes: ByteArray): String {
    val updateDirectory = File(System.getProperty("user.home"), ".vivi-music-de/updates")
    updateDirectory.mkdirs()
    val destination = File(updateDirectory, File(fileName).name)
    destination.writeBytes(bytes)

    val osName = System.getProperty("os.name").orEmpty().lowercase()
    when {
        "win" in osName && destination.extension.equals("msi", ignoreCase = true) -> {
            ProcessBuilder("msiexec.exe", "/i", destination.absolutePath).start()
        }
        "win" in osName -> {
            ProcessBuilder(destination.absolutePath).start()
        }
        "mac" in osName || "darwin" in osName -> {
            ProcessBuilder("open", destination.absolutePath).start()
        }
        destination.extension.equals("appimage", ignoreCase = true) -> {
            destination.setExecutable(true)
            ProcessBuilder(destination.absolutePath).start()
        }
        destination.extension.equals("deb", ignoreCase = true) -> {
            ProcessBuilder("xdg-open", destination.absolutePath).start()
        }
        Desktop.isDesktopSupported() -> {
            Desktop.getDesktop().open(destination)
        }
        else -> error("The downloaded update cannot be launched on this desktop")
    }
    return destination.absolutePath
}
