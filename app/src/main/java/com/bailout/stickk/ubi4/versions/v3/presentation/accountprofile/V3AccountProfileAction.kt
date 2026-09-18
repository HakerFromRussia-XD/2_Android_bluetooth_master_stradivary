package com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile

sealed interface V3AccountProfileAction {
    data class ViewAttached(val loadInBackground: Boolean, val hasCachedBoards: Boolean) : V3AccountProfileAction
    data object ViewDetached : V3AccountProfileAction
    data object LoadRequested : V3AccountProfileAction
    data object HeaderRefreshRequested : V3AccountProfileAction
    data class HeaderRendered(val revision: Long) : V3AccountProfileAction
    data class MessageShown(val id: Long) : V3AccountProfileAction
}
