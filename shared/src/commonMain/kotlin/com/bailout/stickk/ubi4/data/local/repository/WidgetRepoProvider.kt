package com.bailout.stickk.ubi4.data.local.repository

import BaseSubDeviceInfoDao
import com.bailout.stickk.ubi4.data.local.db.dao.BaseParameterInfoDao
import com.bailout.stickk.ubi4.data.local.db.dao.DeviceCrcDao
import com.bailout.stickk.ubi4.data.local.db.dao.ListWidgetsDao
import com.bailout.stickk.ubi4.data.local.db.dao.WidgetStateDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.concurrent.Volatile

object WidgetRepoProvider {
    @Volatile private var repo: WidgetRepository? = null

    private val _currentMac = MutableStateFlow("")
    fun setCurrentMac(mac: String) { _currentMac.value = mac }
    fun mac(): String = _currentMac.value

    fun init(
        widgetStateDao: WidgetStateDao,
        parameterInfoDao: BaseParameterInfoDao,
        listWidgetsDao: ListWidgetsDao,
        subDeviceDao: BaseSubDeviceInfoDao? = null,
        deviceCrcDao: DeviceCrcDao? = null
    ): WidgetRepository {
        return (repo ?: WidgetRepositoryImpl(
            dao = widgetStateDao,
            parameterInfoDao = parameterInfoDao,
            subDeviceDao = subDeviceDao,
            listWidgetsDao = listWidgetsDao,
            deviceCrcDao = deviceCrcDao
        )).also { repo = it }
    }

    fun get(): WidgetRepository = checkNotNull(repo) { "WidgetRepoProvider not init" }

}