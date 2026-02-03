package com.bailout.stickk.ubi4.data.local.db

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual object RoomInit {

    actual fun init(): Ubi4RoomDb {
        val dbPath = documentsPath("ubi4.db")

        val builder: RoomDatabase.Builder<Ubi4RoomDb> =
            Room.databaseBuilder(
                name = dbPath
            )

        return getRoomDatabase(builder)
    }

    private fun documentsPath(fileName: String): String {
        val urls = NSFileManager.defaultManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask
        )

        val dirUrl = urls.firstOrNull() as? NSURL
            ?: error("Documents directory not found")

        val dirPath = dirUrl.path
            ?: error("Documents path is null")

        return "$dirPath/$fileName"
    }
}