package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.device.GetDeviceSessionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.ObserveDeviceSessionChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import com.bailout.stickk.ubi4.versions.v3.domain.service.ChangeDeviceRoleUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.EditDeviceInfoTextUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.GetDeviceInfoTextUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.GetDeviceRoleUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.ObserveDeviceInfoAvailabilityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.ObserveDeviceRoleAvailabilityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.ObserveProsthesisCalibrationAvailabilityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.ReleaseProsthesisCalibrationButtonUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.RestoreDeviceRoleUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.SetDeviceInfoTextUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.StartProsthesisCalibrationUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoRepository
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3ProsthesisCalibrationRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSpinnerSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSpinnerSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSpinnerValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceViewModel
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.V3ServiceWidgetsSource

class V3ServiceViewModelFactory(
    private val repository: V3DeviceSettingsRepository,
    private val widgetsSource: V3ServiceWidgetsSource,
    private val spinnerRepository: V3SpinnerSettingsRepository,
    private val roleRepository: V3DeviceRoleRepository,
    private val deviceInfoRepository: V3DeviceInfoRepository,
    private val calibrationRepository: V3ProsthesisCalibrationRepository,
    private val sessionRepository: V3DeviceSessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3ServiceViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3ServiceViewModel(
            getSliderSettings = GetSliderSettingsUseCaseV3(repository),
            observeSliderSettings = ObserveSliderSettingsUseCaseV3(repository),
            setSliderValue = SetSliderValueUseCaseV3(repository),
            widgetsSource = widgetsSource,
            getDeviceSession = GetDeviceSessionUseCaseV3(sessionRepository),
            observeDeviceSessionChanges = ObserveDeviceSessionChangesUseCaseV3(sessionRepository),
            getSpinnerSettings = GetSpinnerSettingsUseCaseV3(spinnerRepository),
            observeSpinnerSettings = ObserveSpinnerSettingsUseCaseV3(spinnerRepository),
            setSpinnerValue = SetSpinnerValueUseCaseV3(spinnerRepository),
            observeDeviceRoleAvailability = ObserveDeviceRoleAvailabilityUseCaseV3(roleRepository),
            getDeviceRole = GetDeviceRoleUseCaseV3(roleRepository),
            restoreDeviceRole = RestoreDeviceRoleUseCaseV3(roleRepository),
            changeDeviceRole = ChangeDeviceRoleUseCaseV3(roleRepository),
            observeDeviceInfoAvailability = ObserveDeviceInfoAvailabilityUseCaseV3(deviceInfoRepository),
            getDeviceInfoText = GetDeviceInfoTextUseCaseV3(deviceInfoRepository),
            editDeviceInfoText = EditDeviceInfoTextUseCaseV3(),
            setDeviceInfoText = SetDeviceInfoTextUseCaseV3(deviceInfoRepository),
            observeCalibrationAvailability = ObserveProsthesisCalibrationAvailabilityUseCaseV3(calibrationRepository),
            startCalibration = StartProsthesisCalibrationUseCaseV3(calibrationRepository),
            releaseCalibrationButton = ReleaseProsthesisCalibrationButtonUseCaseV3(calibrationRepository),
        ) as T
    }
}
