package com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile

import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileHeader
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountBoard
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareLocalFile

data class V3AccountProfileUiState(
    val header: V3AccountProfileHeader? = null,
    val headerRevision: Long = 0,
    val hasCachedProfile: Boolean = false,
    val isContentVisible: Boolean = false,
    val isTokenLoaded: Boolean = false,
    val areBoardServiceActionsVisible: Boolean = false,
    val boards: List<V3AccountBoard> = emptyList(),
    val boardSubmissions: List<V3AccountBoardsSubmission> = emptyList(),
    val isBoardRenderingReady: Boolean = false,
    val refreshCompletionId: Long = 0,
    val messages: List<V3AccountProfileMessage> = emptyList(),
    val firmwareCatalogSize: Int? = null,
    val firmwareCatalogRevision: Long = 0,
    val firmwareCatalogError: Exception? = null,
    val firmwareUpdates: Map<Int, Boolean> = emptyMap(),
    val firmwareDownloadRequestId: Long? = null,
    val firmwareMessages: List<V3ServiceFirmwareMessage> = emptyList(),
)

data class V3AccountBoardsSubmission(val id: Long, val boards: List<V3AccountBoard>, val submitImmediately: Boolean)

/** A null serverMessage selects the existing localized "no user data" long Toast. */
data class V3AccountProfileMessage(val id: Long, val serverMessage: String?)

/** Queued messages preserve the starting toast even when downloading finishes immediately. */
sealed interface V3ServiceFirmwareMessage {
    val id: Long
    data class CatalogUnavailable(override val id: Long) : V3ServiceFirmwareMessage
    data class NotFound(override val id: Long) : V3ServiceFirmwareMessage
    data class DownloadStarted(override val id: Long, val requestId: Long) : V3ServiceFirmwareMessage
    data class FilesDownloaded(
        override val id: Long, val requestId: Long, val files: List<V3ServiceFirmwareLocalFile>,
    ) : V3ServiceFirmwareMessage
    data class DownloadFailed(override val id: Long, val requestId: Long, val error: Exception) : V3ServiceFirmwareMessage
}
