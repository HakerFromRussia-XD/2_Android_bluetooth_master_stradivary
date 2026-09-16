package com.bailout.stickk.ubi4.versions.v3.domain.device

import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import kotlinx.coroutines.flow.Flow

/** Current connection context; shared device classification is a platform-independent value. */
data class V3DeviceSession(
    val profile: V3DeviceProfile,
    val address: String,
    val restoredFromSnapshot: Boolean,
)

interface V3DeviceSessionRepository {
    val updates: Flow<Unit>
    fun getSession(): V3DeviceSession
}
