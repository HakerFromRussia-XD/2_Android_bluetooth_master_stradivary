package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import androidx.lifecycle.ViewModel
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsCommandsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.StartProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.StopProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.RefreshSensorsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.EditPlotThresholdUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.SetPlotThresholdsUseCaseV3
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetsSource

class V3SensorsViewModelFactory(
    private val repository: V3DeviceSettingsRepository,
    private val widgetsSource: V3SensorsWidgetsSource,
    private val plotRepository: V3SensorsPlotRepository,
    private val commandsRepository: V3SensorsCommandsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3SensorsViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3SensorsViewModel(
            repository, SetSliderValueUseCaseV3(repository), widgetsSource,
            plotRepository, EditPlotThresholdUseCaseV3(), SetPlotThresholdsUseCaseV3(plotRepository),
            commandsRepository, StartProsthesisMovementUseCaseV3(commandsRepository),
            StopProsthesisMovementUseCaseV3(commandsRepository), RefreshSensorsUseCaseV3(commandsRepository),
        ) as T
    }
}
