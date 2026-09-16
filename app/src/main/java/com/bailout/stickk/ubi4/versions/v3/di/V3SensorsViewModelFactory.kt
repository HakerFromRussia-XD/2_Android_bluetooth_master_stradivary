package com.bailout.stickk.ubi4.versions.v3.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.versions.v3.domain.device.GetDeviceSessionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.ObserveDeviceSessionChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.EditPlotThresholdUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.IsPlotPausedUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.GetPlotSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.ObservePlotAvailabilityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.ObservePlotSamplesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.ObservePlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.ObserveSensorsCommandsAvailabilityUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.RefreshSensorsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.SetPlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.StartProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.StopProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsCommandsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.GetSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.ObserveSliderSettingsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.V3SensorsViewModel
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidgetsSource

class V3SensorsViewModelFactory(
    private val repository: V3DeviceSettingsRepository,
    private val widgetsSource: V3SensorsWidgetsSource,
    private val plotRepository: V3SensorsPlotRepository,
    private val commandsRepository: V3SensorsCommandsRepository,
    private val sessionRepository: V3DeviceSessionRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == V3SensorsViewModel::class.java)
        @Suppress("UNCHECKED_CAST")
        return V3SensorsViewModel(
            getSliderSettings = GetSliderSettingsUseCaseV3(repository),
            observeSliderSettings = ObserveSliderSettingsUseCaseV3(repository),
            setSliderValue = SetSliderValueUseCaseV3(repository),
            widgetsSource = widgetsSource,
            getDeviceSession = GetDeviceSessionUseCaseV3(sessionRepository),
            observeDeviceSessionChanges = ObserveDeviceSessionChangesUseCaseV3(sessionRepository),
            getPlotSettings = GetPlotSettingsUseCaseV3(plotRepository),
            isPlotPaused = IsPlotPausedUseCaseV3(plotRepository),
            observePlotAvailability = ObservePlotAvailabilityUseCaseV3(plotRepository),
            observePlotSamples = ObservePlotSamplesUseCaseV3(plotRepository),
            observePlotThresholds = ObservePlotThresholdsUseCaseV3(plotRepository),
            editPlotThreshold = EditPlotThresholdUseCaseV3(),
            setPlotThresholds = SetPlotThresholdsUseCaseV3(plotRepository),
            observeCommandsAvailability = ObserveSensorsCommandsAvailabilityUseCaseV3(commandsRepository),
            startMovement = StartProsthesisMovementUseCaseV3(commandsRepository),
            stopMovement = StopProsthesisMovementUseCaseV3(commandsRepository),
            refreshSensors = RefreshSensorsUseCaseV3(commandsRepository),
        ) as T
    }
}
