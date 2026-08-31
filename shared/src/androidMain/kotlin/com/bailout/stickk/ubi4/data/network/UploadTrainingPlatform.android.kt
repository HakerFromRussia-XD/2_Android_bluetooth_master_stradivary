package com.bailout.stickk.ubi4.data.network

import android.util.Log
import android.widget.Toast
import com.bailout.stickk.ubi4.AndroidContextProvider
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.local.BoardInfoStruct
import com.bailout.stickk.ubi4.data.local.FirmwareInfoStruct
import com.bailout.stickk.ubi4.data.local.MLModelSettings
import com.bailout.stickk.ubi4.data.network.BaseUrlUtilsUBI4.PASSPORT_BASE
import com.bailout.stickk.ubi4.data.state.BoardInfoState
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.MLModelSettingsState
import com.bailout.stickk.ubi4.models.network.ModelVersions
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble.BleEnvironment
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.GlobalParameters.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.utility.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.withContext
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
import kotlin.time.Duration.Companion.seconds

fun getModelVersionsFileName(): String {
    return "model_versions.json"
}

@OptIn(FlowPreview::class, ExperimentalStdlibApi::class)
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

    val omgModuleAddress = baseSubDevicesInfoStructSet
        .firstOrNull { it.deviceCode == PreferenceKeysUbi4.DeviceCode.OMG_MODULE.id }
        ?.deviceAddress
        ?: 10

    // запрос информации об оптике
    var opticsBoardHardwareInfo = BoardInfoStruct()
    try {
        BleEnvironment.getBleCommandExecutor().bleCommandWithQueue(
            BLECommands.requestOpticsBoardSettings(omgModuleAddress),
            MAIN_CHANNEL_CHARACTERISTIC,
            WRITE
        ) {}

        opticsBoardHardwareInfo = BoardInfoState.boardInfoFlow.replayCache
            .lastOrNull()
            ?: BoardInfoState.boardInfoFlow
                .timeout(5.seconds)
                .first()
    }
    catch (e: Exception) {
        val errorText = "Optics data not received: ${e.message}"
        Log.e("modelVersions", errorText)
        withContext(Dispatchers.Main) {
            showToast(errorText)
        }
    }

    // запрос параметров оптики
    var firmwareInfo = FirmwareInfoStruct()
    try {
        BleEnvironment.getBleCommandExecutor().bleCommandWithQueue(
            BLECommands.requestProductFWInfoType(omgModuleAddress),
            MAIN_CHANNEL_CHARACTERISTIC,
            WRITE
        ) {}

        firmwareInfo = FirmwareInfoState.firmwareInfoFlow.replayCache
            .lastOrNull { it.deviceAddress == omgModuleAddress }
            ?: FirmwareInfoState.firmwareInfoFlow
                .filter { it.deviceAddress == omgModuleAddress }
                .timeout(5.seconds)
                .first()
    }
    catch (e: Exception) {
        val errorText = "Optics parameters data not received: ${e.message}"
        Log.e("modelVersions", errorText)
        withContext(Dispatchers.Main) {
            showToast(errorText)
        }
    }

    // запрос ml параметров
    var mlModelSettings = MLModelSettings()
    try {
        BleEnvironment.getBleCommandExecutor().bleCommandWithQueue(
            BLECommands.requestMLModelSettings(omgModuleAddress),
            MAIN_CHANNEL_CHARACTERISTIC,
            WRITE
        ) {}

        mlModelSettings = MLModelSettingsState.mlModelSettingsFlow.replayCache.lastOrNull()
            ?: MLModelSettingsState.mlModelSettingsFlow
                .timeout(5.seconds)
                .first()
    }
    catch (e: Exception) {
        val errorText = "ML parameters data not received: ${e.message}"
        Log.e("modelVersions", errorText)
        withContext(Dispatchers.Main) {
            showToast(errorText)
        }
    }

    // Версия приложения
    var appVersion = ""
    try {
        appVersion = AndroidContextProvider.context.packageManager
            .getPackageInfo(AndroidContextProvider.context.packageName, 0)
            .versionName
            .toString()
    }
    catch (e: Exception) {
        val errorText = "App version not received: ${e.message}"
        Log.e("modelVersions", errorText)
        withContext(Dispatchers.Main) {
            showToast(errorText)
        }
    }

    val multipart = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
        val modelVersions = ModelVersions(
            boardName = opticsBoardHardwareInfo.boardName,
            boardCode = 4,  // opticsBoardHardwareInfo.boardCode,
            boardHardwareVersion = opticsBoardHardwareInfo.boardVersionString,
            boardSoftwareVersion = firmwareInfo.fwVersion,
            modelCode = mlModelSettings.modelCode,
            modelVersion = "${mlModelSettings.majorModelVersion}.${mlModelSettings.minorModelVersion}.${mlModelSettings.quickfixModelVersion}"
        )
        if (modelVersions.boardSoftwareVersion == "0.0.0")
            modelVersions.boardSoftwareVersion = "0.1.5"
        Log.i("modelVersions", modelVersions.toString())

        val json = Json { ignoreUnknownKeys = true }
        val jsonString = json.encodeToString(ModelVersions.serializer(), modelVersions)
        val jsonBody = jsonString.toRequestBody("application/json".toMediaType())
        addFormDataPart("files", getModelVersionsFileName(), jsonBody)

        try {
            val externalDir = AndroidContextProvider.context.getExternalFilesDir(null)
            if (externalDir != null) {
                val jsonFile = java.io.File(externalDir, getModelVersionsFileName())
                jsonFile.writeText(jsonString)
                Log.i("modelVersions", "JSON saved to: ${jsonFile.absolutePath}")
            }
            else {
                Log.w("modelVersions", "External files directory is null")
            }
        }
        catch (e: Exception) {
            Log.e("modelVersions", "Failed to save JSON file: ${e.message}", e)
        }

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
        Log.d("UploadTraining", "Response code: ${resp.code}")
        Log.d("UploadTraining", "Response headers: ${resp.headers}")
        Log.e("UploadTraining", "Response message: ${resp.message}")
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