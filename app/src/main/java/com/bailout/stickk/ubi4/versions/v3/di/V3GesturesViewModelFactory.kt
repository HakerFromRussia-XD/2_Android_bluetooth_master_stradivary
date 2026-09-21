package com.bailout.stickk.ubi4.versions.v3.di

import android.content.Context
import androidx.lifecycle.ViewModel
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.data.appsettings.V3AppSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceSessionRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.gestures.V3GesturesRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets.DataFactoryV3GesturesWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.domain.device.GetDeviceSessionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.ObserveDeviceSessionChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets.V3GesturesWidgetsSource
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3AppSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.GetGesturesPreferencesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.GetCustomGestureNamesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetGesturesSectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetFactoryGestureCollectionExpandedUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.GetRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.EditRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.SaveRotationGroupSelectionUseCaseV3
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.GetGesturesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.LoadRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.RemoveGestureFromRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.ObserveGesturesChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.RequestActiveGestureUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.SelectGestureUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.V3GesturesRepository
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.MoveGestureInRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.SaveGestureSettingsSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.V3GesturesViewModel

class V3GesturesViewModelFactory(
    private val repository: V3GesturesRepository,
    private val appSettingsRepository: V3AppSettingsRepository,
    private val widgetsSource: V3GesturesWidgetsSource,
    private val sessionRepository: V3DeviceSessionRepository,
) : ViewModelProvider.Factory {
    companion object {
        fun create(context: Context): V3GesturesViewModelFactory = create(context) { packet ->
            BleDependencies.v3CommandTransport.enqueue(packet)
        }

        fun create(context: Context, enqueuePacket: (ByteArray) -> Unit): V3GesturesViewModelFactory =
            V3GesturesViewModelFactory(
                repository = V3GesturesRepositoryImpl(enqueuePacket),
                appSettingsRepository = V3AppSettingsRepositoryImpl(context.applicationContext.getSharedPreferences(
                    PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE,
                )),
                widgetsSource = DataFactoryV3GesturesWidgetsSource(),
                sessionRepository = V3DeviceSessionRepositoryImpl(),
            )
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3GesturesViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3GesturesViewModel(
            getGestures = GetGesturesUseCaseV3(repository),
            observeGesturesChanges = ObserveGesturesChangesUseCaseV3(repository),
            selectGesture = SelectGestureUseCaseV3(repository),
            requestActiveGesture = RequestActiveGestureUseCaseV3(repository),
            loadRotationGroup = LoadRotationGroupUseCaseV3(repository),
            removeGestureFromRotationGroup = RemoveGestureFromRotationGroupUseCaseV3(repository),
            getRotationGroupSelection = GetRotationGroupSelectionUseCaseV3(repository),
            editRotationGroupSelection = EditRotationGroupSelectionUseCaseV3(),
            saveRotationGroupSelection = SaveRotationGroupSelectionUseCaseV3(repository),
            moveGestureInRotationGroup = MoveGestureInRotationGroupUseCaseV3(repository),
            getGesturesPreferences = GetGesturesPreferencesUseCaseV3(appSettingsRepository),
            getCustomGestureNames = GetCustomGestureNamesUseCaseV3(appSettingsRepository),
            setGesturesSection = SetGesturesSectionUseCaseV3(appSettingsRepository),
            setFactoryCollectionExpanded = SetFactoryGestureCollectionExpandedUseCaseV3(appSettingsRepository),
            saveGestureSettingsSelection = SaveGestureSettingsSelectionUseCaseV3(appSettingsRepository),
            widgetsSource = widgetsSource,
            getDeviceSession = GetDeviceSessionUseCaseV3(sessionRepository),
            observeDeviceSessionChanges = ObserveDeviceSessionChangesUseCaseV3(sessionRepository),
        ) as T
    }
}
