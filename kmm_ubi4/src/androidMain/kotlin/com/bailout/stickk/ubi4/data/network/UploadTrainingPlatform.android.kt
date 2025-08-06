package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.data.network.BaseUrlUtilsUBI4.PASSPORT_BASE
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

actual suspend fun uploadTrainingDataSsePlatform(
    token: String,
    serial: String,
    pairs: List<Pair<SharedFile, SharedFile>>,
    onProgress: (Int) -> Unit
): String {
    val client = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val multipart = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
        addFormDataPart("serial", serial)
        pairs.flatMap { listOf(it.first, it.second) }
            .forEach { file ->
                val bytes = file.readBytes()
                val body = bytes.toRequestBody("application/octet-stream".toMediaType())
                addFormDataPart("files", file.name, body)
            }
    }.build()

    val request = Request.Builder()
        .url("${PASSPORT_BASE}passport_data/")
        .header("Authorization", token)
        .header("Accept", "text/event-stream")
        .post(multipart)
        .build()

    client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) throw IOException("Upload failed ${resp.code}")
        val body = resp.body ?: throw IOException("Empty body")
        return body.source().collectSseCheckpoint(onProgress)
    }
}

/* ---------- внутренние хелперы SSE ---------- */

private val progressRe = Regex("""^(\d{1,3})%?$""")

private data class ParseResult(val progress: Int?, val checkpoint: String?)

private fun parseEventBlock(eventLines: List<String>): ParseResult {
    var eventType: String? = null
    val dataParts = mutableListOf<String>()

    for (l in eventLines) {
        when {
            l.startsWith("event:") -> eventType = l.removePrefix("event:").trim()
            l.startsWith("data:") -> dataParts += l.removePrefix("data:").trim()
        }
    }

    val data = dataParts.joinToString("\n")
    var progress: Int? = null
    var checkpoint: String? = null

    if (progressRe.matches(data)) {
        progress = data.removeSuffix("%").toIntOrNull()?.coerceIn(0, 100)
    }

    if (eventType == "complete" || (data.startsWith("{") && "\"message\"" in data)) {
        try {
            val jsonObj = Json.parseToJsonElement(data).jsonObject
            jsonObj["progress"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.let {
                progress = it.coerceIn(0, 100)
            }
            val msg = jsonObj["message"]?.jsonPrimitive?.contentOrNull
                ?: jsonObj["checkpoint"]?.jsonPrimitive?.contentOrNull
            if (!msg.isNullOrBlank()) checkpoint = msg
        } catch (_: Exception) {
        }
    }

    return ParseResult(progress, checkpoint)
}

private suspend fun BufferedSource.collectSseCheckpoint(onProgress: (Int) -> Unit): String {
    val eventLines = mutableListOf<String>()
    var lastProgress = -1
    var checkpoint: String? = null

    while (!exhausted()) {
        coroutineContext.ensureActive() // проверка отмены

        val line = try {
            readUtf8LineStrict()
        } catch (_: Exception) {
            break
        }

        if (line.isBlank()) {
            val (progress, cp) = parseEventBlock(eventLines)
            if (progress != null && progress != lastProgress) {
                lastProgress = progress
                onProgress(progress)
            }
            if (cp != null) {
                checkpoint = cp
                break
            }
            eventLines.clear()
        } else {
            eventLines += line
        }
    }

    if (checkpoint == null && eventLines.isNotEmpty()) {
        val (_, cp) = parseEventBlock(eventLines)
        if (cp != null) checkpoint = cp
    }

    return checkpoint ?: error("Не удалось получить checkpoint ")
}