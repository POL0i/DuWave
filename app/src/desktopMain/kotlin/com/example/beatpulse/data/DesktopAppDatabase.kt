package com.example.beatpulse.data

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getAppDatabase(): AppDatabase {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "beatpulse.db")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath,
    )
        .setDriver(DesktopJdbcSQLiteDriver())
        .build()
}
