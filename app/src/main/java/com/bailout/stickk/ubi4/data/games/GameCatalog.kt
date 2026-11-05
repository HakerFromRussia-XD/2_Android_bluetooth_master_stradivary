package com.bailout.stickk.ubi4.data.games

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
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
    val versionCode: Long,
    val usesMotoricaControl: Boolean? = null,
)

internal object GameCatalog {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseGames(body: String, expectedStkPackageName: String): List<RemoteGame> {
        val entries = json.parseToJsonElement(body).jsonObject["games"]?.jsonArray
            ?: error("Games are missing from games catalog")
        val games = entries.mapNotNull { entry ->
            val game = entry.jsonObject
            val android = game["android"]?.takeUnless { it is JsonNull }?.jsonObject
                ?: game.takeIf { it["packageName"] != null }
                ?: return@mapNotNull null // The catalog can also contain iOS-only games.
            val id = game.requiredString("id")
            val parsed = RemoteGame(
                id = id,
                title = game["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                packageName = android.requiredString("packageName"),
                launcherActivity = android.requiredString("launcherActivity"),
                versionName = android["versionName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                versionCode = android["versionCode"]?.jsonPrimitive?.long
                    ?: error("Game $id versionCode is missing"),
                usesMotoricaControl = android["usesMotoricaControl"]?.jsonPrimitive?.booleanOrNull,
            )
            require(parsed.packageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))) {
                "Invalid game package name"
            }
            if (id == "stk") require(parsed.packageName == expectedStkPackageName) { "Unexpected STK package name" }
            parsed
        }
        require(games.map { it.packageName }.distinct().size == games.size) { "Duplicate game package name" }
        return games
    }

    private fun kotlinx.serialization.json.JsonObject.requiredString(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: error("Game $key is missing")
}
