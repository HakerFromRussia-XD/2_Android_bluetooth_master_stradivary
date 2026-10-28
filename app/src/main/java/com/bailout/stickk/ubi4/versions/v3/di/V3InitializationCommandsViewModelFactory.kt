package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceSessionRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.data.service.V3InitializationCommandsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.device.GetDeviceSessionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.ObserveDeviceSessionChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.GetInitializationCommandsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.ObserveInitializationCommandsAvailabilityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.SendInitializationCommandUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization.V3InitializationCommandsViewModel

class V3InitializationCommandsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3InitializationCommandsViewModel::class.java)
        val repository = V3InitializationCommandsRepositoryImpl({ BleDependencies.v3CommandTransport.enqueue(it) })
        val session = V3DeviceSessionRepositoryImpl()
        @Suppress("UNCHECKED_CAST")
        return V3InitializationCommandsViewModel(
            GetInitializationCommandsUseCaseV3(repository), ObserveInitializationCommandsAvailabilityUseCaseV3(repository),
            SendInitializationCommandUseCaseV3(repository), GetDeviceSessionUseCaseV3(session), ObserveDeviceSessionChangesUseCaseV3(session),
        ) as T
    }
}
