package com.bailout.stickk.ubi4.versions.v3.presentation.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRoleRepository
import com.bailout.stickk.ubi4.versions.v3.domain.service.RestoreDeviceRoleUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.ChangeDeviceRoleUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoRepository
import com.bailout.stickk.ubi4.versions.v3.domain.service.SetDeviceInfoTextUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.EditDeviceInfoTextUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSpinnerValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.V3ServiceWidgetsSource

class V3ServiceViewModelFactory(
    private val repository: V3DeviceSettingsRepository,
    private val widgetsSource: V3ServiceWidgetsSource,
    private val spinnerRepository: V3SpinnerSettingsRepository,
    private val roleRepository: V3DeviceRoleRepository,
    private val deviceInfoRepository: V3DeviceInfoRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3ServiceViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3ServiceViewModel(repository, SetSliderValueUseCaseV3(repository), widgetsSource,
            spinnerRepository, SetSpinnerValueUseCaseV3(spinnerRepository), roleRepository,
            RestoreDeviceRoleUseCaseV3(roleRepository), ChangeDeviceRoleUseCaseV3(roleRepository),
            deviceInfoRepository, EditDeviceInfoTextUseCaseV3(), SetDeviceInfoTextUseCaseV3(deviceInfoRepository)) as T
    }
}
