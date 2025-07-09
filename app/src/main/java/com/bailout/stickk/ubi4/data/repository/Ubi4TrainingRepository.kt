package com.bailout.stickk.ubi4.data.repository

import android.util.Log
import com.bailout.stickk.ubi4.data.network.ApiInterfaceUBI4
import com.bailout.stickk.ubi4.data.network.model.SerialTokenRequest
import com.bailout.stickk.ubi4.data.network.model.TakeDataRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

class Ubi4TrainingRepository(
    val api: ApiInterfaceUBI4
) {

    suspend fun fetchTokenBySerial(
        apiKey: String,
        serial: String,
        password: String
    ): String {
        Log.d("Repo", "→ loginBySerial with serial='$serial'")
        val req = SerialTokenRequest(serialNumber = serial, password = password)
        val resp = api.loginBySerial(apiKey, req)
        if (!resp.isSuccessful) throw IOException("Login failed ${resp.code()}")
        val body = resp.body()!!
        val capitalizedType = body.tokenType.replaceFirstChar { it.uppercase() }  // "Bearer"
        return "$capitalizedType ${body.accessToken}"
    }

//    suspend fun fetchAndSavePassport(
//        token: String,
//        serial: String,
//        cacheDir: File
//    ): File {
//        val resp = api.getPassportData(auth = token, serial = serial)
//        if (!resp.isSuccessful) throw IOException("Passport failed ${resp.code()}")
//        val pr = resp.body()!!
//        val out = File(cacheDir, pr.filename)
//        out.writeText(pr.content)
//        return out
//    }
    suspend fun fetchAndSavePassport(
        token: String,
        serial: String,
        cacheDir: File
    ): File {
        val resp = api.getPassportData(auth = token, serial = serial)
        if (!resp.isSuccessful) {
            val err = resp.errorBody()?.string().orEmpty()
            Log.e("Ubi4Repo", "Passport failed ${resp.code()}: $err")
            throw IOException("Passport failed ${resp.code()}: $err")
        }
        val pr = resp.body()!!
        val out = File(cacheDir, pr.filename)
        out.writeText(pr.content)
        return out
    }

    suspend fun uploadTrainingData(
        token: String,
        serial: String,
        pairs: List<Pair<File, File>>,
        onProgress: (String) -> Unit  // сюда будем отдавать только проценты при новом обучении
    ): String {
        // 1. Собираем MultipartBody.Part
        val serialPart = MultipartBody.Part.createFormData("serial", serial)
        val fileParts = pairs.flatMap { (data, passport) ->
            listOf(
                MultipartBody.Part.createFormData(
                    "files", data.name,
                    data.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                ),
                MultipartBody.Part.createFormData(
                    "files", passport.name,
                    passport.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
            )
        }

        // 2. Делаем запрос
        val resp = api.uploadTrainingData(auth = token, serial = serialPart, files = fileParts)
        if (!resp.isSuccessful) throw IOException("Upload failed ${resp.code()}")

        // 3. Смотрим заголовок Content-Type
        val contentType = resp.headers()["Content-Type"] ?: ""
        Log.d("Ubi4Repo", "uploadTrainingData: Content-Type = $contentType")

        // 4. Если сервер возвращает JSON (duplicate), сразу разбираем тело как строку
        if (contentType.contains("application/json")) {
            val raw = resp.body()?.string()
                ?: throw IOException("Empty JSON response")
            val json = Json.parseToJsonElement(raw).jsonObject
            val checkpoint = json["message"]!!.jsonPrimitive.content
            Log.d("Ubi4Repo", "→ JSON-ответ, checkpoint = $checkpoint")
            return checkpoint
        }

        // 5. Иначе — считаем, что это SSE (новое обучение). Читаем поток построчно:
        var lastCheckpoint: String? = null
        resp.body()?.source()?.use { src ->
            while (!src.exhausted()) {
                val line = src.readUtf8Line() ?: break
                // line выглядит как "data: 37" или "data: {\"message\":\"checkpoint-...\"}"
                val payload = line.removePrefix("data:").trim()

                // 5.1. Если payload — число (процент) → передаём в onProgress
                if (payload.matches(Regex("\\d+"))) {
                    onProgress(payload) // UI покажет проценты, например "37"
                    continue
                }

                // 5.2. Если payload содержит JSON с "message" → парсим checkpoint и выходим
                if (payload.startsWith("{") && payload.contains("\"message\"")) {
                    val parsed = Json.parseToJsonElement(payload)
                        .jsonObject["message"]!!
                        .jsonPrimitive
                        .content
                    lastCheckpoint = parsed
                    Log.d("Ubi4Repo", "→ SSE вернул checkpoint = $parsed")
                    break
                }
                // прочие случаи (например, пустые строки) игнорируем
            }
        }

        return lastCheckpoint
            ?: error("Не удалось получить checkpoint из SSE")
    }

    suspend fun downloadAndUnpackCheckpoint(
        token: String,
        checkpoint: String,
        outputDir: File
    ): Pair<File, List<File>> {
        val resp = api.downloadArchive(
            auth = token,
            request = TakeDataRequest(listOf(checkpoint))
        )
        if (!resp.isSuccessful) throw IOException("Download failed ${resp.code()}")

        // сохраняем zip
        val zipFile = File(outputDir, "$checkpoint.zip")
        resp.body()!!.byteStream().use { input ->
            zipFile.outputStream().use { output -> input.copyTo(output) }
        }
        // распаковываем
        val unpacked = mutableListOf<File>()
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val outFile = File(outputDir, entry.name)
                zip.getInputStream(entry).use { inp ->
                    outFile.outputStream().use { out -> inp.copyTo(out) }
                }
                unpacked += outFile
            }
        }
        return zipFile to unpacked
    }
}