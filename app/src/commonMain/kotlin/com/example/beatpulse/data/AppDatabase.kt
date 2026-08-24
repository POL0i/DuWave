package com.example.beatpulse.data

// no android context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrackEntity::class, PlaylistEntity::class, PlaylistTrackCrossRef::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    // Note: Migrations might need to be abstracted if we want them on Desktop, but for now we'll handle them per platform.
}

// Room KMP expects a builder constructor per platform
interface AppDatabaseConstructor : androidx.room.RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
