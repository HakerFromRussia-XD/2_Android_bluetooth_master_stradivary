package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.data.network.BaseUrlUtilsUBI4.PASSPORT_BASE
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

actual suspend fun uploadTrainingDataSsePlatform(
    token: String,
    serial: String,
    pairs: List<Pair<SharedFile, SharedFile>>,
    onProgress: (Int) -> Unit
): String {
    // Копируем конфигурацию из PlatformClientProvider: без таймаутов на чтение/запись
    val client = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS) // без таймаута на запись
        .readTimeout(0, TimeUnit.SECONDS)  // без таймаута на чтение (важно для SSE)
        .retryOnConnectionFailure(true)
        .build()

    val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
    multipartBuilder.addFormDataPart("serial", serial)
    pairs.forEach { (dataFile, passportFile) ->
        listOf(dataFile, passportFile).forEach { file ->
            val bytes = file.readBytes()
            val body = bytes.toRequestBody("application/octet-stream".toMediaType())
            multipartBuilder.addFormDataPart("files", file.name, body)
        }
    }

    val request = Request.Builder()
        .url("${PASSPORT_BASE}passport_data/")
        .header("Authorization", token)
        .header("Accept", "text/event-stream")
        .post(multipartBuilder.build())
        .build()

    client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) throw IOException("Upload failed ${resp.code}")
        val body = resp.body ?: throw IOException("Empty body")
        val source: BufferedSource = body.source()
        var checkpoint: String? = null
        val eventLines = mutableListOf<String>()
        val progressRe = Regex("^\\d+%?$")

        while (!source.exhausted()) {
            val line = try {
                source.readUtf8LineStrict()
            } catch (e: Exception) {
                // если чтение зависло или сокет закрылся — прерываем
                break
            }

            if (line.isBlank()) {
                var eventType: String? = null
                val dataParts = mutableListOf<String>()
                for (l in eventLines) {
                    when {
                        l.startsWith("event:") -> eventType = l.removePrefix("event:").trim()
                        l.startsWith("data:") -> dataParts += l.removePrefix("data:").trim()
                    }
                }
                val data = dataParts.joinToString("\n")

                if (progressRe.matches(data)) {
                    data.removeSuffix("%").toIntOrNull()?.let {
                        onProgress(it.coerceIn(0, 100))
                    }
                }

                if (eventType == "complete" || (data.startsWith("{") && "\"message\"" in data)) {
                    try {
                        val jsonObj = Json.parseToJsonElement(data).jsonObject
                        jsonObj["progress"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.let {
                            onProgress(it.coerceIn(0, 100))
                        }
                        val msg = jsonObj["message"]?.jsonPrimitive?.contentOrNull
                        if (!msg.isNullOrBlank()) {
                            checkpoint = msg
                            break
                        }
                    } catch (_: Exception) {}
                }

                eventLines.clear()
            } else {
                eventLines += line
            }
        }

        // Финальный flush, если чекпоинт ещё не получен
        if (checkpoint == null && eventLines.isNotEmpty()) {
            var eventType: String? = null
            val dataParts = mutableListOf<String>()
            for (l in eventLines) {
                when {
                    l.startsWith("event:") -> eventType = l.removePrefix("event:").trim()
                    l.startsWith("data:") -> dataParts += l.removePrefix("data:").trim()
                }
            }
            val data = dataParts.joinToString("\n")
            if (progressRe.matches(data)) {
                data.removeSuffix("%").toIntOrNull()?.let {
                    onProgress(it.coerceIn(0, 100))
                }
            }
            if (eventType == "complete" || (data.startsWith("{") && "\"message\"" in data)) {
                try {
                    val jsonObj = Json.parseToJsonElement(data).jsonObject
                    val msg = jsonObj["message"]?.jsonPrimitive?.contentOrNull
                    if (!msg.isNullOrBlank()) checkpoint = msg
                } catch (_: Exception) {}
            }
        }

        return checkpoint ?: error("Не удалось получить checkpoint из SSE")
    }
}