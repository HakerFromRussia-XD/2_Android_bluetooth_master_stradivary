package com.bailout.stickk.ubi4.data.local.db

// shared/src/androidMain/kotlin/.../db/RoomInit.android.kt
import android.content.Context
import androidx.room.Room

object AndroidCtx {
    // где-то один раз положи applicationContext (в твоём WDApplication.onCreate)
    lateinit var appContext: Context
}

actual object RoomInit {
    actual fun init(): Ubi4RoomDb =
        Room.databaseBuilder(
            AndroidCtx.appContext,
            Ubi4RoomDb::class.java,
            "ubi4.db"
        )
            .fallbackToDestructiveMigration(false) // пока без миграций
            .build()
}