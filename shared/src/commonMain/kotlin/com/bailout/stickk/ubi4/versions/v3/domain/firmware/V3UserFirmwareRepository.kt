package com.bailout.stickk.ubi4.versions.v3.domain.firmware

import kotlinx.coroutines.flow.Flow

/** Progress of the existing user update session, independent of its Android dialog. */
data class V3UserFirmwareStatus(
    val phase: String = "idle",
    val boardNumber: Int = 0,
    val boardCount: Int = 0,
    val progress: Int = 0,
    val blocksInteraction: Boolean = false,
)

object V3UserFirmwarePolicy {
    fun isEnabled(isV3: Boolean, selectedRole: Int): Boolean = isV3 && selectedRole == 2

    fun isTransferActive(phase: String, blocksInteraction: Boolean): Boolean =
        blocksInteraction && phase !in listOf("offered", "complete")

    fun shouldResumeAfterUpdate(previousPhase: String, phase: String): Boolean =
        phase == "complete" && previousPhase != "complete"
}

interface V3UserFirmwareRepository {
    fun observe(): Flow<V3UserFirmwareStatus>
    fun refreshEnvironment()
    fun startUpdate()
    fun postponeUpdate()
    fun acknowledgeCompletion()
    fun close()
}
