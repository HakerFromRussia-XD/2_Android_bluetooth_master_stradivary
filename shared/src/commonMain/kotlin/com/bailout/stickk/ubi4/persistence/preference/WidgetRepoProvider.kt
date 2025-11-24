package com.bailout.stickk.ubi4.persistence.preference

import BaseSubDeviceInfoDao
import com.bailout.stickk.ubi4.data.local.db.BaseParameterInfoDao
import com.bailout.stickk.ubi4.data.local.db.ListWidgetsDao
import com.bailout.stickk.ubi4.data.local.db.WidgetStateDao
import com.bailout.stickk.ubi4.data.local.db.WidgetStateEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
        subDeviceDao: BaseSubDeviceInfoDao? = null
    ): WidgetRepository {
        return (repo ?: WidgetRepositoryImpl(
            dao = widgetStateDao,
            parameterInfoDao = parameterInfoDao,
            subDeviceDao = subDeviceDao,
            listWidgetsDao = listWidgetsDao
        )).also { repo = it }
    }

    fun get(): WidgetRepository = checkNotNull(repo) { "WidgetRepoProvider not init" }

}