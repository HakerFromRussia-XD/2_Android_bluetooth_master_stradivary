package com.bailout.stickk.ubi4.utility

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.bailout.stickk.ubi4.data.repository.Ubi4TrainingRepository
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TrainingUploadManager {

    /** Текущее состояние пайплайна. */
    enum class State { IDLE, RUNNING, EXPORTING, DONE, ERROR }

    /** UI-флоу: состояние и проценты. */
    val stateFlow = MutableStateFlow(State.IDLE)
    val progressFlow = MutableSharedFlow<Int>(
        replay = 1,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /* ---------- публичный API ---------- */

    fun launch(
        context: Context,
        repo: Ubi4TrainingRepository,
        token: String,
        serial: String
    ): Job = CoroutineScope(Dispatchers.IO).launch {
        val dir = context.getExternalFilesDir(null) ?: error("External storage unavailable")
        Log.d("TrainingUploadManager", "Working dir = ${dir.absolutePath}")

        try {
            /*--------- 0. Подготовка файлов ---------*/
            val dataFile = dir.listFiles()
                ?.filter { it.name.endsWith(".emg8") }
                ?.maxByOrNull { it.lastModified() }
                ?: error(".emg8 file not found")

            val passportFile = File(dir, "${dataFile.name}.data_passport")
                .takeIf { it.exists() }
                ?: error("passport not found")

            /*--------- 1. Загрузка + SSE-прогресс ---------*/
            stateFlow.value = State.RUNNING

            val checkpoint = repo.uploadTrainingData(
                token, serial, dataFile, passportFile
            ) { raw ->
                // "data: 37"  – парсим в int
                raw.removePrefix("data:").trim().toIntOrNull()
                    ?.let { pct -> progressFlow.tryEmit(pct) }
            }
            Log.d("TrainingUploadManager", "checkpoint = $checkpoint")

            /*--------- 2. Скачиваем checkpoint.zip ---------*/
            stateFlow.value = State.EXPORTING
            val (zipFile, unpacked) = repo.downloadAndUnpackCheckpoint(token, checkpoint, dir)

            /*--------- 3. Извлекаем нужные файлы ---------*/
            val ckptSrc = unpacked.first { it.extension == "ckpt" }
            val binSrc  = unpacked.first { it.extension == "bin" }

            val ts = SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault())
                .format(Date())                    // fallback
                .let { fallback ->
                    Regex("_(\\d{9,})$").find(ckptSrc.nameWithoutExtension)
                        ?.groupValues?.get(1)
                        ?.toLongOrNull()
                        ?.let { secs -> SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault()).format(Date(secs * 1000)) }
                        ?: fallback
                }
            val nextNum = getNextCheckpointNumber(context)

            val ckptDst = File(dir, "checkpoint_№${nextNum}_$ts.ckpt")
            val binDst  = File(dir, "params_$ts.bin")
            ckptSrc.copyTo(ckptDst, overwrite = true)
            binSrc.copyTo(binDst,  overwrite = true)
            zipFile.delete()
            unpacked.forEach(File::delete)

            /*--------- 4. Успех ---------*/
            stateFlow.value = State.DONE
            progressFlow.tryEmit(100)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Модель обучена 🎉", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Throwable) {
            Log.e("TrainingUploadManager", "pipeline error", e)
            stateFlow.value = State.ERROR
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка обучения: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /* ---------- internal ---------- */

    private fun getNextCheckpointNumber(ctx: Context): Int =
        ctx.getSharedPreferences(PreferenceKeysUBI4.TRAINING_PREFS, Context.MODE_PRIVATE).run {
            val cur = getInt(PreferenceKeysUBI4.KEY_CHECKPOINT_NUMBER, 1)
            edit().putInt(PreferenceKeysUBI4.KEY_CHECKPOINT_NUMBER, cur + 1).apply()
            cur
        }
}