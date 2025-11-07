package com.bailout.stickk.ubi4.persistence.preference

import com.bailout.stickk.ubi4.data.local.db.WidgetStateDao
import com.bailout.stickk.ubi4.data.local.db.WidgetStateEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.concurrent.Volatile



object WidgetRepoProvider {
    @Volatile private var repo: WidgetRepository? = null

    // реактивный MAC
    private val _currentMac = MutableStateFlow("")
    fun setCurrentMac(mac: String) { _currentMac.value = mac }
    fun mac(): String = _currentMac.value
    fun macFlow(): StateFlow<String> = _currentMac

    fun init(dao: WidgetStateDao): WidgetRepository {
        return (repo ?: WidgetRepositoryImpl(dao)).also { repo = it }
    }
    fun get(): WidgetRepository = checkNotNull(repo) { "WidgetRepoProvider not init" }

    /** ВАЖНО: поток «все записи для текущего MAC» */
    fun observeAllForCurrentMac(): kotlinx.coroutines.flow.Flow<List<WidgetStateEntity>> =
        macFlow().flatMapLatest { m ->
            if (m.isBlank()) flowOf(emptyList()) else get().observeAll() // внутри дао фильтрует по device_mac = mac()
        }
}