package com.vivimusic.de.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dir = File(System.getProperty("user.home"), ".vivi-music-de")
    dir.mkdirs()
    val dbFile = File(dir, "vivi_music_de.db")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
    )
}
