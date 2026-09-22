package com.bailout.stickk.ubi4.versions.v3.presentation.main

import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceIdentity

data class V3MainUiState(
    val visibleDisplays: Set<Int> = emptySet(),
    // A refresh also reapplies current secret-access/IME rules when displays are unchanged.
    val navigationRevision: Long = 0,
    val batteryPercent: Int? = null,
    val deviceIdentity: V3DeviceIdentity? = null,
)

sealed interface V3MainAction {
    data object ViewCreated : V3MainAction
    data object ViewResumed : V3MainAction
    data class ViewStopped(
        val openingScanAfterDisconnect: Boolean,
        val deviceConnected: Boolean,
        val language: String,
    ) : V3MainAction
    data object NavigationRefreshRequested : V3MainAction
    data object ViewDestroyed : V3MainAction
    data object DeviceConnected : V3MainAction
    data object WidgetStorageInitializationRequested : V3MainAction
    data object SavedConnectionRestoreRequested : V3MainAction
    data object LastConnectionResetRequested : V3MainAction
    data class SerialNumberReceived(val serial: String) : V3MainAction
}
