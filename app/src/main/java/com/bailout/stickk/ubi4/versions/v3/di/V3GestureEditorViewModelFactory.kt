package com.bailout.stickk.ubi4.versions.v3.di

import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.versions.v3.data.gestureeditor.V3GestureEditorRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.ObserveGestureSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.RequestGestureSettingsUseCaseV3
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.data.appsettings.V3AppSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.GetGestureEditorNamesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SaveGestureEditorNamesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.gestureeditor.V3GestureEditorViewModel

class V3GestureEditorViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE,
    )
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3GestureEditorViewModel::class.java)
        val repository = V3AppSettingsRepositoryImpl(preferences)
        val editorRepository = V3GestureEditorRepositoryImpl({ BleDependencies.v3CommandTransport.enqueue(it) })
        @Suppress("UNCHECKED_CAST")
        return V3GestureEditorViewModel(GetGestureEditorNamesUseCaseV3(repository), SaveGestureEditorNamesUseCaseV3(repository),
            ObserveGestureSettingsUseCaseV3(editorRepository), RequestGestureSettingsUseCaseV3(editorRepository)) as T
    }
}
