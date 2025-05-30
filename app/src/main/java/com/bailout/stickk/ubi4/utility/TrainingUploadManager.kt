package com.bailout.stickk.ubi4.utility

import android.util.Log

import android.content.Context
import android.widget.Toast
import com.bailout.stickk.ubi4.data.repository.Ubi4TrainingRepository
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.channels.BufferOverflow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TrainingUploadManager {

    /** Состояние процесса */
    enum class State { IDLE, RUNNING, EXPORTING, DONE, ERROR }

    /** публичные флоу для UI */
    val stateFlow = MutableStateFlow(State.IDLE)
    val progressFlow = MutableSharedFlow<Int>(
        replay = 1,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Возвращает и увеличивает номер чекпоинта в SharedPreferences */
    private fun getNextCheckpointNumber(context: Context): Int {
        val prefs = context.getSharedPreferences(PreferenceKeysUBI4.TRAINING_PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(PreferenceKeysUBI4.KEY_CHECKPOINT_NUMBER, 1)
        prefs.edit().putInt(PreferenceKeysUBI4.KEY_CHECKPOINT_NUMBER, current + 1).apply()
        return current
    }

    /** Запускает pipeline: upload SSE → download ZIP → распаковка и переименование */
    fun launch(
        context: Context,
        repo: Ubi4TrainingRepository,
        token: String,
        serial: String
    ) = CoroutineScope(Dispatchers.IO).launch {
        val dir = context.getExternalFilesDir(null) ?: error("External storage unavailable")
        Log.d("TrainingUploadManager", "Working directory: ${dir.absolutePath}")
        dir.listFiles()?.forEach { Log.d("TrainingUploadManager", "Found file in dir: ${it.name}") }

        // 1) Стартуем загрузку
        stateFlow.value = State.RUNNING

        // Формируем multipart
        val serialPart = MultipartBody.Part.createFormData("serial", serial)
        // данные: .emg8 и паспорт
        val dataFile = dir.listFiles()?.filter { it.name.endsWith(".emg8") }?.maxByOrNull { it.lastModified() }
            ?: error("log-file not found")
        Log.d("TrainingUploadManager", "Data file: ${dataFile.absolutePath}")
        val passportFile = File(dir, "${dataFile.name}.data_passport").takeIf { it.exists() }
            ?: error("passport not found")
        Log.d("TrainingUploadManager", "Passport file: ${passportFile.absolutePath}")

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

        try {
            // 2) Выполняем upload и читаем SSE
            val resp = repo.api.uploadTrainingData(token, serialPart, files)
            if (!resp.isSuccessful) throw IOException("Upload failed ${resp.code()}")

            val source = resp.body()?.source() ?: error("empty body")
            var lastCheckpoint: String? = null

            while (true) {
                val raw = try { source.readUtf8LineStrict() }
                catch (e: java.io.EOFException) { break }

                // интересует только строка, начинающаяся с "data:"
                if (!raw.startsWith("data:")) continue

                val payload = raw.removePrefix("data:").trim()
                if (payload.startsWith("{")) {
                    // JSON с полем message
                    lastCheckpoint = Json.parseToJsonElement(payload)
                        .jsonObject["message"]?.jsonPrimitive?.content
                    Log.d("TrainingUploadManager", "Extracted lastCheckpoint = $lastCheckpoint")
                } else {
                    // числовой прогресс
                    payload.toIntOrNull()?.let { progressFlow.tryEmit(it) }
                }
            }

            val checkpoint = lastCheckpoint ?: throw IOException("No checkpoint received")

            // 3) Переключаем UI на экспорт
            stateFlow.value = State.EXPORTING

            // 4) Скачиваем и распаковываем ZIP
            val (zipFile, unpacked) = repo.downloadAndUnpackCheckpoint(token, checkpoint, dir)
            Log.d("TrainingUploadManager", "Downloaded zip: ${zipFile.absolutePath}")
            Log.d("TrainingUploadManager", "Unpacked files: ${unpacked.joinToString { it.name }}")

            // Удаляем ZIP
            zipFile.delete()

            // Извлекаем .ckpt и .bin
            val ckptSrc = unpacked.first { it.extension == "ckpt" }
            val binSrc  = unpacked.first { it.extension == "bin" }

            // timestamp из epoch‑секунд в имени ckpt
            val epochStr = Regex("_(\\d{9,})$").find(ckptSrc.nameWithoutExtension)?.groupValues?.get(1)
            val formatter = SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault())
            val ts = epochStr
                ?.toLongOrNull()
                ?.let { secs -> formatter.format(Date(secs * 1000)) }
                ?: formatter.format(Date())
            val num = getNextCheckpointNumber(context)
            val destDir = dir

            // Переименовываем
            val ckptDst = File(destDir, "checkpoint_№${num}_$ts.ckpt")
            val binDst  = File(destDir, "params_$ts.bin")
            Log.d("TrainingUploadManager", "Renaming checkpoint: ${ckptSrc.absolutePath} -> ${ckptDst.absolutePath}")
            Log.d("TrainingUploadManager", "Renaming params: ${binSrc.absolutePath} -> ${binDst.absolutePath}")
            ckptSrc.copyTo(ckptDst, overwrite = true)
            binSrc.copyTo(binDst, overwrite = true)
            Log.d("TrainingUploadManager", "Checkpoint saved to: ${ckptDst.absolutePath}")
            Log.d("TrainingUploadManager", "Params saved to: ${binDst.absolutePath}")

            // Чистим временные файлы
            unpacked.forEach {
                Log.d("TrainingUploadManager", "Deleting temp file: ${it.absolutePath}")
                it.delete()
            }

            // 5) Успех
            stateFlow.value = State.DONE
            progressFlow.tryEmit(100)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Модель обучена 🎉", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Throwable) {
            Log.e("TrainingUploadManager", "Error during training upload pipeline", e)
            stateFlow.value = State.ERROR
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка обучения: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}