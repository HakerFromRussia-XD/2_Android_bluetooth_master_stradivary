package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.data.blelog.V3BleLogRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.ManageBleLogFilterUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.ObserveBleLogUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.blelog.V3BleLogRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.blelog.V3BleLogViewModel

class V3BleLogViewModelFactory(private val repository: V3BleLogRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3BleLogViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3BleLogViewModel(ObserveBleLogUseCaseV3(repository), ManageBleLogFilterUseCaseV3(repository)) as T
    }

    companion object {
        fun from(context: Context) = V3BleLogViewModelFactory(createBleLogRepository(
            context.applicationContext.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE),
        ))
    }
}

internal fun createBleLogRepository(preferences: SharedPreferences) = V3BleLogRepositoryImpl(
    readGraphStreamHidden = { preferences.getBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, true) },
    saveGraphStreamHidden = { hidden ->
        preferences.edit().putBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, hidden).apply()
    },
)
