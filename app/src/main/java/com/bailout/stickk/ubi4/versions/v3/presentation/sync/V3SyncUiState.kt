package com.bailout.stickk.ubi4.versions.v3.presentation.sync

data class V3SyncUiState(
    val isShowing: Boolean = false,
    val isProgressVisible: Boolean = false,
    val isIndeterminate: Boolean = false,
    val progress: Int = 0,
    // null: synchronization has not requested a change to the surrounding panels.
    val chromeVisible: Boolean? = null,
)

enum class V3SyncAction { ViewCreated, StartupShown, ViewAttached, ViewDetached }
