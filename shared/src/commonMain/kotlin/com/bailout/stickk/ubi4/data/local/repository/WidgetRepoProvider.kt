package com.bailout.stickk.ubi4.data.local.repository

import com.bailout.stickk.ubi4.data.local.db.dao.BaseSubDeviceInfoDao
import com.bailout.stickk.ubi4.data.local.db.dao.BaseParameterInfoDao
import com.bailout.stickk.ubi4.data.local.db.dao.DeviceCrcDao
import com.bailout.stickk.ubi4.data.local.db.dao.ListWidgetsDao
import com.bailout.stickk.ubi4.data.local.db.dao.DataParameterDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.concurrent.Volatile

object WidgetRepoProvider {
    @Volatile private var repo: WidgetRepository? = null

    private val _currentMac = MutableStateFlow("")
    fun setCurrentMac(mac: String) { _currentMac.value = mac }
    fun mac(): String = _currentMac.value

    fun init(
        dataParameterDao: DataParameterDao,
        parameterInfoDao: BaseParameterInfoDao,
        listWidgetsDao: ListWidgetsDao,
        subDeviceDao: BaseSubDeviceInfoDao? = null,
        deviceCrcDao: DeviceCrcDao? = null
    ): WidgetRepository {
        return (repo ?: WidgetRepositoryImpl(
            dao = dataParameterDao,
            parameterInfoDao = parameterInfoDao,
            subDeviceDao = subDeviceDao,
            listWidgetsDao = listWidgetsDao,
            deviceCrcDao = deviceCrcDao
        )).also { repo = it }
    }

    fun get(): WidgetRepository = checkNotNull(repo) { "WidgetRepoProvider not init" }

}