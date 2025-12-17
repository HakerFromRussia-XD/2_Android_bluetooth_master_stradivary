package com.bailout.stickk.ubi4.data.local.db

import BaseSubDeviceInfoDao
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.bailout.stickk.ubi4.data.local.db.dao.BaseParameterInfoDao
import com.bailout.stickk.ubi4.data.local.db.dao.DeviceCrcDao
import com.bailout.stickk.ubi4.data.local.db.dao.ListWidgetsDao
import com.bailout.stickk.ubi4.data.local.db.dao.DataParameterDao
import com.bailout.stickk.ubi4.data.local.db.entity.BaseParameterInfoEntity
import com.bailout.stickk.ubi4.data.local.db.entity.BaseSubDeviceInfoEntity
import com.bailout.stickk.ubi4.data.local.db.entity.DeviceCrcEntity
import com.bailout.stickk.ubi4.data.local.db.entity.ListWidgetsEntity
import com.bailout.stickk.ubi4.data.local.db.entity.DataParameterEntity

@Database(
    entities = [DataParameterEntity::class, BaseParameterInfoEntity::class, BaseSubDeviceInfoEntity::class,
        ListWidgetsEntity::class, DeviceCrcEntity::class],
    version = 1,
    exportSchema = true // схемы уже настроены в Gradle
)
@ConstructedBy(Ubi4RoomDbConstructor::class)
abstract class Ubi4RoomDb : RoomDatabase() {
    abstract fun widgetStateDao(): DataParameterDao
    abstract fun baseParameterInfoDao(): BaseParameterInfoDao
    abstract fun baseSubDeviceInfoDao(): BaseSubDeviceInfoDao
    abstract fun listWidgetsDao(): ListWidgetsDao
    abstract fun deviceCrcDao(): DeviceCrcDao
}

expect object Ubi4RoomDbConstructor : RoomDatabaseConstructor<Ubi4RoomDb> {
    override fun initialize(): Ubi4RoomDb
}