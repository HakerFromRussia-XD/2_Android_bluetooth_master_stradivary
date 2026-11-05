package com.bailout.stickk.ubi4.versions.v3.presentation.games

sealed interface V3GamesAction {
    data object ViewAttached : V3GamesAction
    data object ViewResumed : V3GamesAction
    data object ViewDetached : V3GamesAction
    data class PrimaryClicked(val packageName: String) : V3GamesAction
    data class DeleteClicked(val packageName: String) : V3GamesAction
    data class EffectHandled(val id: Long) : V3GamesAction
    data class PlatformActionFailed(val message: V3GamesMessage) : V3GamesAction
}
