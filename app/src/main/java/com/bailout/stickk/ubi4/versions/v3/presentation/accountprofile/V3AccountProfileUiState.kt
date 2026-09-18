package com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile

import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.V3AccountProfileHeader

data class V3AccountProfileUiState(
    val header: V3AccountProfileHeader? = null,
    val headerRevision: Long = 0,
    val hasCachedProfile: Boolean = false,
    val isContentVisible: Boolean = false,
    val isTokenLoaded: Boolean = false,
    val refreshCompletionId: Long = 0,
    val messages: List<V3AccountProfileMessage> = emptyList(),
)

/** A null serverMessage selects the existing localized "no user data" long Toast. */
data class V3AccountProfileMessage(val id: Long, val serverMessage: String?)
