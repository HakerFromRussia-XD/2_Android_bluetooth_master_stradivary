package com.bailout.stickk.ubi4.data.network

// commonMain
import com.bailout.stickk.ubi4.models.network.SerialTokenRequest
import com.bailout.stickk.ubi4.models.network.TakeDataRequest
import com.bailout.stickk.ubi4.utility.logging.platformLog
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.errors.IOException

class Ubi4TrainingRepository(
    private val api: Ubi4RequestsApi
) {
    /** 1) API Key + serial + password → JWT */
    suspend fun fetchTokenBySerial(
        apiKey: String,
        serial: String,
        password: String
    ): String {
        platformLog("Repo", "→ loginBySerial with serial='$serial'")
        val req = SerialTokenRequest(serialNumber = serial, password = password)
        return when (val result = api.loginBySerial(apiKey, req)) {
            is NetworkResult.Success -> {
                val body = result.value
                val type = body.tokenType.replaceFirstChar { it.uppercase() }
                "$type ${body.accessToken}"
            }
            is NetworkResult.Error -> {
                throw IOException("Login failed ${result.code}: ${result.message}")
            }
        }
    }

    /** 2) token + serial → паспорт (YAML + filename) */
    suspend fun fetchAndSavePassport(
        token: String,
        serial: String,
        cacheDir: SharedFile
    ): SharedFile {
        return when (val result = api.getPassportData(token, serial)) {
            is NetworkResult.Success -> {
                val pr = result.value
                val out = cacheDir.child(pr.filename)
                out.writeText(pr.content)
                out
            }
            is NetworkResult.Error -> {
                platformLog("Ubi4Repo", "Passport failed ${result.code}: ${result.message}")
                throw IOException("Passport failed ${result.code}: ${result.message}")
            }
        }
    }


    suspend fun uploadTrainingData(
        token: String,
        serial: String,
        pairs: List<Pair<SharedFile, SharedFile>>,
        onProgress: (Int) -> Unit
    ): String {
        // делегируем платформенному пути (на Android — OkHttp реализация)
        return uploadTrainingDataSsePlatform(token, serial, pairs) { progress ->
            onProgress(progress)
        }
    }
    /**
     * 4) token + checkpoint-name → ZIP → распаковка
     */
    suspend fun downloadAndUnpackCheckpoint(
        token: String,
        checkpoint: String,
        outputDir: SharedFile
    ): Pair<SharedFile, List<SharedFile>> {
        val resp = api.downloadArchive(token, TakeDataRequest(listOf(checkpoint)))
        if (resp.status.value !in 200..299) {
            throw IOException("Download failed ${resp.status.value}")
        }

        // сохраняем ZIP
        val zipFile = outputDir.child("$checkpoint.zip")
        writeResponseBodyToSharedFile(resp, zipFile)

        // распаковываем (expect/actual)
        val (_, unpacked) = unzipArchive(zipFile, outputDir)
        return zipFile to unpacked
    }
}

// вспомогательная функция: записать тело в файл
suspend fun writeResponseBodyToSharedFile(resp: HttpResponse, dest: SharedFile) {
    val channel = resp.bodyAsChannel()
    when (dest) {
        // реализация делегируется в actual для платформы через expect/actual расширение
        else -> {
            // будет использовано actual-реализация writeFromChannel
            dest.writeFromChannel(channel)
        }
    }
}


