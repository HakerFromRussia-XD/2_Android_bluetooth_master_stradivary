package com.bailout.stickk.ubi4.versions.v3.presentation.service.initialization

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommand

sealed interface V3InitializationCommandsAction {
    data object ViewAttached : V3InitializationCommandsAction
    data object ViewDetached : V3InitializationCommandsAction
    data class CommandClicked(val command: V3InitializationCommand) : V3InitializationCommandsAction
}
