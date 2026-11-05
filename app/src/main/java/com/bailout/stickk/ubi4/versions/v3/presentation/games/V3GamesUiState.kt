package com.bailout.stickk.ubi4.versions.v3.presentation.games

import com.bailout.stickk.ubi4.versions.v3.domain.games.V3Game
import com.bailout.stickk.ubi4.versions.v3.domain.games.V3GameAvailability

data class V3GamesUiState(
    val cards: List<V3GameCardState> = emptyList(),
    val effects: List<V3GamesEffect> = emptyList(),
)

data class V3GameCardState(val availability: V3GameAvailability, val isActionEnabled: Boolean = true)
data class V3GamesEffect(val id: Long, val command: V3GamesCommand)

sealed interface V3GamesCommand {
    data class LaunchGame(val game: V3Game) : V3GamesCommand
    data class OpenStore(val packageName: String) : V3GamesCommand
    data class UninstallGame(val packageName: String) : V3GamesCommand
    data class ShowMessage(val message: V3GamesMessage, val detail: String? = null) : V3GamesCommand
}

enum class V3GamesMessage {
    MANIFEST_URL_MISSING, MANIFEST_LOAD_FAILED, NOT_PUBLISHED, STORE_CHECK_FAILED,
    STORE_OPEN_FAILED, LAUNCH_FAILED, UNINSTALL_FAILED,
}
