package com.vivimusic.de.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        SyncStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistSongDao(): PlaylistSongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun syncStateDao(): SyncStateDao
}

/**
 * The Room compiler generates the `actual` implementation for each target.
 */
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * Returns a platform specific builder pointing at the database file location.
 * Android and desktop use different filesystem APIs, hence the expect/actual.
 */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

/**
 * Builds the database using the bundled SQLite driver, which guarantees a
 * consistent SQLite version across Android and desktop.
 */
fun getRoomDatabase(): AppDatabase =
    getDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .build()
