package com.bailout.stickk.ubi4.ui.fragments.account.games

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

internal data class RemoteGame(
    val id: String,
    val title: String,
    val packageName: String,
    val launcherActivity: String,
    val versionName: String,
    val versionCode: Long
) {
    companion object {
        const val STK_ID = "stk"

        fun localFallback(packageName: String): RemoteGame =
            RemoteGame(
                id = STK_ID,
                title = "",
                packageName = packageName,
                launcherActivity = "$packageName.SuperTuxKartActivity",
                versionName = "",
                versionCode = 0L
            )
    }
}

internal enum class GameAction {
    INSTALL,
    UPDATE,
    PLAY,
    UNAVAILABLE
}

internal object GameCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseStk(body: String, expectedPackageName: String): RemoteGame {
        val root = json.parseToJsonElement(body).jsonObject
        val game = root["games"]
            ?.jsonArray
            ?.firstOrNull { entry ->
                entry.jsonObject["id"]?.jsonPrimitive?.contentOrNull == RemoteGame.STK_ID
            }
            ?.jsonObject
            ?: error("STK is missing from games catalog")
        val android = game["android"]?.jsonObject ?: game
        val parsed = RemoteGame(
            id = RemoteGame.STK_ID,
            title = game["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            packageName = android.requiredString("packageName"),
            launcherActivity = android.requiredString("launcherActivity"),
            versionName = android["versionName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            versionCode = android["versionCode"]?.jsonPrimitive?.long
                ?: error("STK versionCode is missing")
        )
        require(parsed.packageName == expectedPackageName) {
            "Unexpected STK package name"
        }
        return parsed
    }

    fun action(remoteGame: RemoteGame?, installedVersionCode: Long?): GameAction =
        when {
            installedVersionCode != null &&
                (remoteGame == null || installedVersionCode >= remoteGame.versionCode) ->
                GameAction.PLAY
            remoteGame == null -> GameAction.UNAVAILABLE
            installedVersionCode == null -> GameAction.INSTALL
            else -> GameAction.UPDATE
        }

    private fun kotlinx.serialization.json.JsonObject.requiredString(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: error("STK $key is missing")
}
