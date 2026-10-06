package com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile

sealed interface V3AccountProfileAction {
    data class ViewAttached(val loadInBackground: Boolean) : V3AccountProfileAction
    data object ViewDetached : V3AccountProfileAction
    data object LoadRequested : V3AccountProfileAction
    data object HeaderRefreshRequested : V3AccountProfileAction
    data class HeaderRendered(val revision: Long) : V3AccountProfileAction
    data class MessageShown(val id: Long) : V3AccountProfileAction
    data object FirmwareCatalogRefreshRequested : V3AccountProfileAction
    data object CachedBoardsApplied : V3AccountProfileAction
    data object BoardRenderingReady : V3AccountProfileAction
    data object BoardUpdatesStarted : V3AccountProfileAction
    data object BoardUpdatesStopped : V3AccountProfileAction
    data class BoardSubmissionRendered(val id: Long) : V3AccountProfileAction
    data class FirmwareFilesRequested(val deviceAddress: Int, val requestId: Long) : V3AccountProfileAction
    data class FirmwareMessageShown(val id: Long) : V3AccountProfileAction
}
