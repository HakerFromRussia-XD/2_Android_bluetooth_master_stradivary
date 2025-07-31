//package com.bailout.stickk.ubi4.data.repository
//
//import android.util.Log
//import collectSseEvents
//import com.bailout.stickk.ubi4.data.network.BaseUrlUtilsUBI4
//import com.bailout.stickk.ubi4.data.network.NetworkResult
//import com.bailout.stickk.ubi4.data.network.PlatformClientProvider
//import com.bailout.stickk.ubi4.data.network.Ubi4RequestsApi
//import com.bailout.stickk.ubi4.models.network.SerialTokenRequest
//import com.bailout.stickk.ubi4.models.network.TakeDataRequest
//import io.ktor.client.request.accept
//import io.ktor.client.request.forms.MultiPartFormDataContent
//import io.ktor.client.request.forms.formData
//import io.ktor.client.request.header
//import io.ktor.client.request.post
//import io.ktor.client.request.setBody
//import io.ktor.client.request.url
//import io.ktor.client.statement.HttpResponse
//import io.ktor.client.statement.bodyAsChannel
//import io.ktor.client.statement.bodyAsText
//import io.ktor.http.HttpHeaders
//import io.ktor.utils.io.jvm.javaio.copyTo
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.jsonObject
//import kotlinx.serialization.json.jsonPrimitive
//import java.io.File
//import java.io.IOException
//import java.util.zip.ZipFile
//
//class Ubi4TrainingRepository(
//    private val api: Ubi4RequestsApi
//) {
//
//    /** 1) API Key + serial + password → JWT */
//    suspend fun fetchTokenBySerial(
//        apiKey: String,
//        serial: String,
//        password: String
//    ): String {
//        Log.d("Repo", "→ loginBySerial with serial='$serial'")
//        val req = SerialTokenRequest(serialNumber = serial, password = password)
//        return when (val result = api.loginBySerial(apiKey, req)) {
//            is NetworkResult.Success -> {
//                val body = result.value
//                val type = body.tokenType.replaceFirstChar { it.uppercase() }
//                "$type ${body.accessToken}"
//            }
//            is NetworkResult.Error -> {
//                throw IOException("Login failed ${result.code}: ${result.message}")
//            }
//        }
//    }
//
//    /** 2) token + serial → паспорт (YAML + filename) */
//    suspend fun fetchAndSavePassport(
//        token: String,
//        serial: String,
//        cacheDir: File
//    ): File {
//        return when (val result = api.getPassportData(token, serial)) {
//            is NetworkResult.Success -> {
//                val pr = result.value
//                File(cacheDir, pr.filename).apply {
//                    writeText(pr.content)
//                }
//            }
//            is NetworkResult.Error -> {
//                Log.e("Ubi4Repo", "Passport failed ${result.code}: ${result.message}")
//                throw IOException("Passport failed ${result.code}: ${result.message}")
//            }
//        }
//    }
//
////    /*** 3) serial + files → SSE-stream или JSON*/
//
//    suspend fun uploadTrainingData(
//        token: String,
//        serial: String,
//        pairs: List<Pair<File, File>>,
//        onProgress: (Int) -> Unit
//    ): String {
//        // строим multipart как раньше
//        val multipart = MultiPartFormDataContent(
//            formData {
//                append("serial", serial)
//                pairs.forEach { (dataFile, passportFile) ->
//                    listOf(dataFile, passportFile).forEach { file ->
//                        append(
//                            "files",
//                            file.readBytes(),
//                            io.ktor.http.Headers.build {
//                                append(HttpHeaders.ContentDisposition, """form-data; name="files"; filename="${file.name}"""")
//                                append(HttpHeaders.ContentType, io.ktor.http.ContentType.Application.OctetStream.toString())
//                            }
//                        )
//                    }
//                }
//            }
//        )
//
//        // отправляем через sseClient, чтобы не было вмешательств плагинов в поток
//        val resp: HttpResponse = PlatformClientProvider.sseClient.post {
//            url("${BaseUrlUtilsUBI4.PASSPORT_BASE}passport_data/")
//            header(HttpHeaders.Authorization, token)
//            accept(io.ktor.http.ContentType.Text.EventStream)
//            setBody(multipart)
//        }
//
//        // если сервер сразу вернул JSON
//        if ("application/json" in resp.headers[HttpHeaders.ContentType].orEmpty()) {
//            val raw = resp.bodyAsText()
//            return Json.parseToJsonElement(raw)
//                .jsonObject["message"]!!
//                .jsonPrimitive
//                .content
//        }
//
//        // иначе — SSE
//        val checkpoint = resp.collectSseEvents { prog ->
//            onProgress(prog.coerceIn(0, 100))
//        } ?: error("Не удалось получить checkpoint из SSE")
//
//        return checkpoint
//    }
////
//
//    /**
//     * 4) token + checkpoint-name → ZIP → распаковка
//     */
//    suspend fun downloadAndUnpackCheckpoint(
//        token: String,
//        checkpoint: String,
//        outputDir: File
//    ): Pair<File, List<File>> {
//        val resp = api.downloadArchive(token, TakeDataRequest(listOf(checkpoint)))
//        if (resp.status.value !in 200..299) {
//            throw IOException("Download failed ${resp.status.value}")
//        }
//
//        // сохраняем ZIP
//        val zipFile = File(outputDir, "$checkpoint.zip").apply {
//            resp.bodyAsChannel().copyTo(outputStream())
//        }
//
//        // распаковываем
//        val unpacked = mutableListOf<File>()
//        ZipFile(zipFile).use { zip ->
//            zip.entries().asSequence().forEach { entry ->
//                val outFile = File(outputDir, entry.name)
//                zip.getInputStream(entry).use { inp ->
//                    outFile.outputStream().use { out -> inp.copyTo(out) }
//                }
//                unpacked += outFile
//            }
//        }
//        return zipFile to unpacked
//    }
//}
//
