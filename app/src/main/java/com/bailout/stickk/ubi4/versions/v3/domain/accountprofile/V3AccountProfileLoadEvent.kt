package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

/** One session per view. Overlapping refreshes retain the old shared token/client/retry state. */
class V3AccountProfileLoadSession(val context: V3AccountProfileContext) {
    internal var token = ""
    internal var clientId = 0
    internal var attemptedRequest = 1
}

sealed interface V3AccountProfileLoadEvent {
    data object Authorized : V3AccountProfileLoadEvent
    data class ProfileLoaded(val profile: V3AccountProfile) : V3AccountProfileLoadEvent
    data object RefreshFinished : V3AccountProfileLoadEvent
    data object ProfileUnavailable : V3AccountProfileLoadEvent
    data class ServerError(val message: String) : V3AccountProfileLoadEvent
    data object NoUserData : V3AccountProfileLoadEvent
}
