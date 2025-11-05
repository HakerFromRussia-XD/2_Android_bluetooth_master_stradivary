package com.bailout.stickk.ubi4.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [WidgetStateEntity::class],
    version = 1,
    exportSchema = true // схемы уже настроены в Gradle
)
@ConstructedBy(Ubi4RoomDbConstructor::class)
abstract class Ubi4RoomDb : RoomDatabase() {
    abstract fun widgetStateDao(): WidgetStateDao
}

expect object Ubi4RoomDbConstructor : RoomDatabaseConstructor<Ubi4RoomDb> {
    override fun initialize(): Ubi4RoomDb
}