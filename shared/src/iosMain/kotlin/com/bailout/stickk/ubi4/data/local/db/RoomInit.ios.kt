package com.bailout.stickk.ubi4.data.local.db

import androidx.room.Room


actual object RoomInit {
    actual fun init(): Ubi4RoomDb =
        Room.databaseBuilder<Ubi4RoomDb>(
            name = "ubi4.db",
        ).fallbackToDestructiveMigration(true).build()
}