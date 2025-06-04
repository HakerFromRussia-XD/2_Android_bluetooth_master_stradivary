package com.bailout.stickk.ubi4.utility

import android.content.Context
import android.widget.Toast
import com.bailout.stickk.ubi4.data.repository.Ubi4TrainingRepository
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.IOException
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipFile

object TrainingUploadManager {

    /* ------------------- публичные флоу ------------------- */

    enum class State { IDLE, RUNNING, EXPORTING, DONE, ERROR }
    val stateFlow    = MutableStateFlow(State.IDLE)
    val progressFlow = MutableSharedFlow<Int>(
        replay = 1, extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /* ------------------- API ------------------- */

    /**
     * Отправляет одну или много пар ( .emg8  +  .emg8.data_passport ).
     * Если `pairs` не передан – берётся **последний** .emg8 в директории.
     */
    fun launch(
        ctx   : Context,
        repo  : Ubi4TrainingRepository,
        token : String,
        serial: String,
        pairs : List<Pair<File, File>>? = null
    ): Job = CoroutineScope(Dispatchers.IO).launch {
        val dir = ctx.getExternalFilesDir(null) ?: error("No external storage")

        // --------- 0. Составляем пары файлов ---------
        val batch: List<Pair<File, File>> = pairs ?: run {
            val data = dir.listFiles()?.filter { it.extension == "emg8" }
                ?.maxByOrNull { it.lastModified() }
                ?: error(".emg8 not found")
            val pass = File(dir, "${data.name}.data_passport")
                .takeIf(File::exists) ?: error("passport not found")
            listOf(data to pass)
        }

        try {
            // --------- 1. upload + SSE ---------
            stateFlow.value = State.RUNNING
            val ckpt = repo.uploadBatch(token, serial, batch) { s ->
                s.removePrefix("data:").trim().toIntOrNull()
                    ?.let { progressFlow.tryEmit(it.coerceIn(0, 100)) }
            }

            // --------- 2. Скачиваем и распаковываем ---------
            stateFlow.value = State.EXPORTING
            val (zip, files) = repo.downloadAndUnpack(token, ckpt, dir)

            // --------- 3. Переименовываем ckpt / bin ---------
            val ckptSrc = files.first { it.extension == "ckpt" }
            val binSrc  = files.first { it.extension == "bin" }
            val ts      = ckptSrc.extractTimestamp()
            val num     = ctx.nextCheckpointNumber()

            ckptSrc.copyTo(File(dir, "checkpoint_№${num}_${ts}.ckpt"), overwrite = true)
            binSrc .copyTo(File(dir, "params_$ts.bin"),           overwrite = true)
            zip.delete(); files.forEach(File::delete)

            // --------- 4. Успех ---------
            stateFlow.value = State.DONE
            progressFlow.tryEmit(100)
            withContext(Dispatchers.Main) {
                Toast.makeText(ctx, "Модель обучена 🎉", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Throwable) {
            stateFlow.value = State.ERROR
            withContext(Dispatchers.Main) {
                Toast.makeText(ctx, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /* ------------------- private-helpers ------------------- */

    private inline fun Context.nextCheckpointNumber(): Int =
        getSharedPreferences(PreferenceKeysUBI4.TRAINING_PREFS, Context.MODE_PRIVATE).run {
            val cur = getInt(PreferenceKeysUBI4.KEY_CHECKPOINT_NUMBER, 1)
            edit().putInt(PreferenceKeysUBI4.KEY_CHECKPOINT_NUMBER, cur + 1).apply(); cur
        }

    private fun File.extractTimestamp(): String =
        Regex("_(\\d{9,})$").find(nameWithoutExtension)
            ?.groupValues?.get(1)?.toLongOrNull()
            ?.let { Date(it * 1_000) }
            ?.let { SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault()).format(it) }
            ?: SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
}

/* ========================================================================== */
/* =====  расширения к  Ubi4TrainingRepository  (короткие, в том же файле)  ==*/
/* ========================================================================== */

private suspend fun Ubi4TrainingRepository.uploadBatch(
    token : String,
    serial: String,
    pairs : List<Pair<File, File>>,
    onSse : (String) -> Unit
): String {
    /** формируем multipart: serial + 2N файлов */
    fun File.part() = MultipartBody.Part.createFormData(
        "files", name, asRequestBody("application/octet-stream".toMediaTypeOrNull())
    )

    val serialPart = MultipartBody.Part.createFormData("serial", serial)
    val fileParts  = pairs.flatMap { (emg, pass) -> listOf(emg.part(), pass.part()) }

    val resp = api.uploadTrainingData(
        auth   = token,
        serial = serialPart,
        files  = fileParts
    )
    if (!resp.isSuccessful) throw IOException("Upload failed ${resp.code()}")

    var ckpt: String? = null
    resp.body()?.source()?.use { src ->
        while (!src.exhausted()) {
            val ln = src.readUtf8Line() ?: break
            onSse(ln)
            if (ln.startsWith("data:") && ln.contains("message"))
                ckpt = kotlinx.serialization.json.Json.parseToJsonElement(ln.removePrefix("data:").trim())
                    .jsonObject["message"]!!.jsonPrimitive.content
        }
    }
    return ckpt ?: error("checkpoint not found in SSE")
}

    private suspend fun Ubi4TrainingRepository.downloadAndUnpack(
        token: String,
        ckpt : String,
        outDir: File
    ): Pair<File, List<File>> {
        val zip = File(outDir, "$ckpt.zip")
        val body = api.downloadArchive(
            auth = token,
            request = com.bailout.stickk.ubi4.data.network.model.TakeDataRequest(listOf(ckpt))
        ).body() ?: error("empty response")

        body.byteStream().use { input -> zip.outputStream().use { input.copyTo(it) } }

        val unpacked = mutableListOf<File>()
        ZipFile(zip).use { z ->
            z.entries().asSequence().forEach { e ->
                val out = File(outDir, e.name)
                z.getInputStream(e).use { it.copyTo(out.outputStream()) }
                unpacked += out
            }
        }
        return zip to unpacked
}