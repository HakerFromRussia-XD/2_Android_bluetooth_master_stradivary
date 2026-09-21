package com.bailout.stickk.ubi4.versions.v3.domain.games

data class V3Game(val title: String, val packageName: String, val launcherActivity: String, val versionCode: Long)

enum class V3GameAction { INSTALL, UPDATE, PLAY, UNAVAILABLE }

data class V3GameAvailability(
    val game: V3Game,
    val action: V3GameAction,
    val isInstalled: Boolean,
    val hasManifestUrl: Boolean,
)
