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

    suspend fun fetchAndSavePassport(
        token: String,
        serial: String,
        cacheDir: File
    ): File {
        val resp = api.getPassportData(auth = token, serial = serial)
        if (!resp.isSuccessful) throw IOException("Passport failed ${resp.code()}")
        val pr = resp.body()!!
        val out = File(cacheDir, pr.filename)
        out.writeText(pr.content)
        return out
    }


    suspend fun uploadTrainingData(
        token: String,
        serial: String,
        dataFile: File,
        passportFile: File,
        onProgress: (String) -> Unit
    ): String {

        // multipart: plain поле serial + два binary-файла
        val serialPart = MultipartBody.Part.createFormData("serial", serial)
        val files = listOf(
            MultipartBody.Part.createFormData(
                "files", dataFile.name,
                dataFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            ),
            MultipartBody.Part.createFormData(
                "files", passportFile.name,
                passportFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
        )

        // POST /train/upload
        val resp = api.uploadTrainingData(
            auth = token,
            serial = serialPart,
            files = files
        )
        if (!resp.isSuccessful) throw IOException("Upload failed ${resp.code()}")

        // SSE-стрим: в progress-строках обычное число, а checkpoint приходит в JSON-сообщении
        var lastCheckpoint: String? = null
        resp.body()?.source()?.use { src ->
            while (!src.exhausted()) {
                val line = src.readUtf8Line() ?: break     // "data: 42" или "data: {"message":"checkpoint_X"}"
                onProgress(line)

                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.startsWith("{") && payload.contains("message")) {
                    lastCheckpoint = Json.parseToJsonElement(payload)
                        .jsonObject["message"]!!.jsonPrimitive.content
                }
            }
        }

        return lastCheckpoint ?: throw IOException("No checkpoint name received from SSE")
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