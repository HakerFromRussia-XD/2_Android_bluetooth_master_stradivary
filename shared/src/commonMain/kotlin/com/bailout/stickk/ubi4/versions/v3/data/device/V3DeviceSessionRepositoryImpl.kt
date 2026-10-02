package com.bailout.stickk.ubi4.versions.v3.data.device

import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSession
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// Connection and screen lifecycle share one gate for all V3 device operations.
internal val deviceInteractionEnabledState = MutableStateFlow(false)

class V3DeviceSessionRepositoryImpl : V3DeviceSessionRepository {
    override val updates = UiState.updateFlow.map { Unit }
    override fun getSession() = V3DeviceSession(
        profile = if (UiState.isInterfaceV3Activated) UiState.activeV3DeviceProfile else V3DeviceProfile.NOT_V3,
        address = WidgetRepoProvider.mac(),
        restoredFromSnapshot = WidgetState.dbSnapshotAppliedWithCrc,
    )
}
