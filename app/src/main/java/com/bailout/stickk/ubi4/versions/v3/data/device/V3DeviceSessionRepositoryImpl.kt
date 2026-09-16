package com.bailout.stickk.ubi4.versions.v3.data.device

import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSession
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3DeviceSessionRepository
import kotlinx.coroutines.flow.map

class V3DeviceSessionRepositoryImpl : V3DeviceSessionRepository {
    override val updates = UiState.updateFlow.map { Unit }
    override fun getSession() = V3DeviceSession(
        profile = if (UiState.isInterfaceV3Activated) UiState.activeV3DeviceProfile else V3DeviceProfile.NOT_V3,
        address = WidgetRepoProvider.mac(),
        restoredFromSnapshot = WidgetState.dbSnapshotAppliedWithCrc,
    )
}
