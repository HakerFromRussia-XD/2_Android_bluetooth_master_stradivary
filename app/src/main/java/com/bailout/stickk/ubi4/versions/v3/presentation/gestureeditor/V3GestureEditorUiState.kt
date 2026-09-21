package com.bailout.stickk.ubi4.versions.v3.presentation.gestureeditor

import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.V3GestureSettings

data class V3GestureEditorUiState(
    val name: String = "",
    val nameInput: String = "",
    val isEditingName: Boolean = false,
    val loadedSettings: V3GestureSettings? = null,
    val pendingSettings: List<V3GestureSettingsUpdate> = emptyList(),
    val initialOpenRequested: Boolean = false,
)

data class V3GestureSettingsUpdate(val id: Long, val settings: V3GestureSettings?)

sealed interface V3GestureEditorAction {
    data object ViewCreated : V3GestureEditorAction
    data class ViewStarted(val gestureId: Int) : V3GestureEditorAction
    data object ViewStopped : V3GestureEditorAction
    data object RendererReady : V3GestureEditorAction
    data class SettingsApplied(val updateId: Long) : V3GestureEditorAction
    data object InitialOpenHandled : V3GestureEditorAction
    data object ViewDestroyed : V3GestureEditorAction
    data object EditNameClicked : V3GestureEditorAction
    data class NameChanged(val text: String) : V3GestureEditorAction
    data object SaveNameRequested : V3GestureEditorAction
}
