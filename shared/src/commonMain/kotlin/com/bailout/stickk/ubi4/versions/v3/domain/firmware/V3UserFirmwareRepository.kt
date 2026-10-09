package com.bailout.stickk.ubi4.versions.v3.domain.firmware

import kotlinx.coroutines.flow.Flow

/** Progress of the existing user update session, independent of its Android dialog. */
data class V3UserFirmwareStatus(
    val phase: String = "idle",
    val boardNumber: Int = 0,
    val boardCount: Int = 0,
    val progress: Int = 0,
    val blocksInteraction: Boolean = false,
    val detail: String = "",
)

/** Frozen update queue and progress; its persisted representation belongs to data. */
data class V3UserFirmwareSession<Target>(
    val deviceId: String,
    val targets: List<Target>,
    val completed: Set<Int> = emptySet(),
    val attempted: Set<Int> = emptySet(),
    val formatVersion: Int = 1,
)

interface V3UserFirmwareTarget<Version> {
    val address: Int
    val version: Version
}

interface V3UserFirmwareBoard<Version> {
    val address: Int
    val version: Version?
    val isMain: Boolean
}

object V3UserFirmwarePolicy {
    fun isEnabled(isV3: Boolean, selectedRole: Int): Boolean = isV3 && selectedRole == 2

    fun blocksInteraction(phase: String): Boolean =
        phase in setOf("offered", "preparing", "updating", "verifying", "complete")

    fun isTransferActive(phase: String, blocksInteraction: Boolean): Boolean =
        blocksInteraction && phase !in listOf("offered", "complete")

    fun shouldResumeAfterUpdate(previousPhase: String, phase: String): Boolean =
        phase == "complete" && previousPhase != "complete"

    fun canResumeJournal(formatVersion: Int): Boolean = formatVersion >= 2

    fun <Version : Comparable<Version>> canStartFirstAttempt(
        firstAttempt: Boolean, installedVersion: Version?, targetVersion: Version,
    ): Boolean = firstAttempt && installedVersion != null && installedVersion < targetVersion

    /** Keep target objects intact; archive paths and serialization belong to the caller. */
    fun <Target, Version : Comparable<Version>> queue(
        installedVersions: Map<Int, Version?>,
        targets: List<Target>,
        targetAddress: (Target) -> Int,
        targetVersion: (Target) -> Version,
    ): List<Target> = targets.filter { target ->
        val address = targetAddress(target)
        if (address !in installedVersions) return@filter false
        val installed = requireNotNull(installedVersions[address]) { "Unknown firmware version at $address" }
        installed < targetVersion(target)
    }.sortedBy { if (targetAddress(it) == 0) 1 else 0 }

    fun <Version> completed(isMain: Boolean, installedVersion: Version?, targetVersion: Version): Boolean =
        isMain && installedVersion == targetVersion

    fun <Version> retry(isMain: Boolean, installedVersion: Version?, targetVersion: Version): Boolean =
        !isMain && installedVersion != null && installedVersion != targetVersion
}

interface V3UserFirmwareActionsRepository {
    fun startUpdate()
    fun postponeUpdate()
    fun acknowledgeCompletion()
}

interface V3UserFirmwareRepository : V3UserFirmwareActionsRepository {
    fun observe(): Flow<V3UserFirmwareStatus>
    fun refreshEnvironment()
    fun close()
}
