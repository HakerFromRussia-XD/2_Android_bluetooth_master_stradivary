package com.bailout.stickk.ubi4.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WidgetStateEntity::class],
    version = 1,
    exportSchema = true // схемы уже настроены в Gradle
)
abstract class Ubi4RoomDb : RoomDatabase() {
    abstract fun widgetStateDao(): WidgetStateDao
}