package com.bailout.stickk.ubi4.versions.v3.domain.vtosc

import kotlinx.coroutines.flow.Flow

interface V3VtoscRepository {
    fun observeConfig(): Flow<V3VtoscConfig>
    /** True while a READ of DTCE_VTFB_SETTINGS is in flight for address 0x30. */
    fun observeLoading(): Flow<Boolean>
    fun loadConfig()
    fun writeConfig(config: V3VtoscConfig)
    fun saveConfig()
}
