package com.vivimusic.de.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.vivimusic.de.data.AppContextHolder

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appContext = AppContextHolder.context.applicationContext
    val dbFile = appContext.getDatabasePath("vivi_music_de.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
