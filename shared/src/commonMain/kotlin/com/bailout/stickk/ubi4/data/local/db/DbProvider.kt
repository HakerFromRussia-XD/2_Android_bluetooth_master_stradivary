package com.bailout.stickk.ubi4.data.local.db

import kotlin.concurrent.Volatile

object DbProvider {
    @Volatile
    private var db: Ubi4RoomDb? = null

    fun instance(): Ubi4RoomDb =
        db ?: error("DbProvider is not initialized")

    fun setInstance(inst: Ubi4RoomDb) {
        db = inst
    }
}