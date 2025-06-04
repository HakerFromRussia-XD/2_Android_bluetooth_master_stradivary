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
        pairs: List<Pair<File, File>>,          // ← список «данные + паспорт»
        onProgress: (String) -> Unit
    ): String {

        val serialPart = MultipartBody.Part.createFormData("serial", serial)

        // ➋  превращаем все пары в один общий список `files`
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

        val resp = api.uploadTrainingData(
            auth   = token,
            serial = serialPart,
            files  = fileParts
        )
        if (!resp.isSuccessful) throw IOException("Upload failed ${resp.code()}")

        var lastCheckpoint: String? = null
        resp.body()?.source()?.use { src ->
            while (!src.exhausted()) {
                val line = src.readUtf8Line() ?: break
                onProgress(line)                               // «data: NN»

                if (line.startsWith("data:") &&
                    line.contains("checkpoint")
                ) {
                    lastCheckpoint = Json.parseToJsonElement(
                        line.removePrefix("data:")
                    ).jsonObject["message"]!!.jsonPrimitive.content
                }
            }
        }
        return lastCheckpoint ?: error("No checkpoint name in SSE")
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