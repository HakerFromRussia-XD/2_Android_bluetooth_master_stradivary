package com.bailout.stickk.ubi4.versions.v3.presentation.prosthesisinformation

sealed interface V3ProsthesisInformationAction {
    data object ViewAttached : V3ProsthesisInformationAction
    data object ViewDetached : V3ProsthesisInformationAction
}
