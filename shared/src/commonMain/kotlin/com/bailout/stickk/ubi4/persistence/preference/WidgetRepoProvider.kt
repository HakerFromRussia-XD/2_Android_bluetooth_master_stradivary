package com.bailout.stickk.ubi4.persistence.preference

import BaseSubDeviceInfoDao
import com.bailout.stickk.ubi4.data.local.db.BaseParameterInfoDao
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
    fun macFlow(): StateFlow<String> = _currentMac

    fun init(
        widgetStateDao: WidgetStateDao,
        parameterInfoDao: BaseParameterInfoDao,
        baseSubDeviceInfoDao: BaseSubDeviceInfoDao
    ): WidgetRepository {
        return (repo ?: WidgetRepositoryImpl(
            widgetStateDao,
            parameterInfoDao,
            baseSubDeviceInfoDao
        )).also { repo = it }
    }

    fun get(): WidgetRepository = checkNotNull(repo) { "WidgetRepoProvider not init" }

    fun observeAllForCurrentMac(): kotlinx.coroutines.flow.Flow<List<WidgetStateEntity>> =
        macFlow().flatMapLatest { m ->
            if (m.isBlank()) flowOf(emptyList()) else get().observeAll()
        }
}