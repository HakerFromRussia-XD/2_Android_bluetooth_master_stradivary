package com.bailout.stickk.ubi4.versions.v3.presentation.firmware

sealed interface V3UserFirmwareAction {
    data object ViewCreated : V3UserFirmwareAction
    data object ViewResumed : V3UserFirmwareAction
    data object ViewDestroyed : V3UserFirmwareAction
    data object InstallClicked : V3UserFirmwareAction
    data object RemindLaterClicked : V3UserFirmwareAction
    data object CompletionAcknowledged : V3UserFirmwareAction
}
