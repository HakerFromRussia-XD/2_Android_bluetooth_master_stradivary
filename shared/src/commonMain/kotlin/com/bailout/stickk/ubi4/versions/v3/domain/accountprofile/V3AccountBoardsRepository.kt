package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

import kotlinx.coroutines.flow.Flow

data class V3AccountBoard(
    val name: String?, val deviceCode: Int, val deviceAddress: Int,
    val version: String?, val isInBootloader: Boolean = false,
    val isUpdateAvailable: Boolean? = null, val canUpdate: Boolean = true,
)

/** Null boards retain the previous rows when the device snapshot is temporarily empty. */
data class V3AccountBoardsSnapshot(val boards: List<V3AccountBoard>?, val installedVersions: Map<Int, String>)

interface V3AccountBoardsRepository {
    fun getBoards(): List<V3AccountBoard>
    fun getCachedBoards(): List<V3AccountBoard>?
    fun cacheBoards(boards: List<V3AccountBoard>)
    val changes: Flow<Unit>
    val bootloaderChanges: Flow<Pair<Int, Boolean>>
}
