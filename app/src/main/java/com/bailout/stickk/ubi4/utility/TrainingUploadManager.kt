package com.bailout.stickk.ubi4.utility

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.bailout.stickk.ubi4.data.network.SharedFile
import com.bailout.stickk.ubi4.data.network.sharedFile
import com.bailout.stickk.ubi4.data.network.Ubi4TrainingRepository
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

    enum class State { IDLE, RUNNING, EXPORTING, DONE, ERROR }

    val stateFlow = MutableStateFlow(State.IDLE)
    val progressFlow = MutableSharedFlow<Int>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun launch(
        context: Context,
        repo: Ubi4TrainingRepository,
        token: String,
        serial: String,
        selectedEmg8Files: List<File>
    ): Job {
        val dir = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("External storage unavailable")
        val dirShared = sharedFile(dir.absolutePath)

        // Собираем пары SharedFile (emg8 + passport)
        val pairsShared: List<Pair<SharedFile, SharedFile>> = selectedEmg8Files.mapNotNull { emg ->
            val passportFile = File(dir, "${emg.name}.data_passport")
            if (!passportFile.exists()) {
                Log.w("TrainingUploadManager", "Не найден passport для ${emg.name}, пропускаем")
                null
            } else {
                sharedFile(emg.absolutePath) to sharedFile(passportFile.absolutePath)
            }
        }

        if (pairsShared.isEmpty()) {
            Log.e("TrainingUploadManager", "Из выбранных .emg8 ни одна пара (emg8 + passport) не найдена")
            return Job().apply { cancel() }
        }

        return CoroutineScope(Dispatchers.IO).launch {
            try {
                stateFlow.value = State.RUNNING

                val checkpoint: String = repo.uploadTrainingData(
                    token = token,
                    serial = serial,
                    pairs = pairsShared
                ) { progressInt ->
                    Log.d("SSE", "callback progress: $progressInt")
                    progressFlow.tryEmit(progressInt.coerceIn(0, 100))
                }

                Log.d("TrainingUploadManager", "Получили checkpoint = $checkpoint")
                stateFlow.value = State.EXPORTING

                val (zipShared, unpackedShared) = repo.downloadAndUnpackCheckpoint(
                    token = token,
                    checkpoint = checkpoint,
                    outputDir = dirShared
                )

                // Ищем нужные файлы
                val ckptShared: SharedFile = unpackedShared.first { it.name.endsWith(".ckpt") }
                val binShared: SharedFile  = unpackedShared.first { it.name.endsWith(".bin") }

                val ckptSrc = ckptShared.toFile()
                val binSrc = binShared.toFile()

                val ts: String = run {
                    val fallback = SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault())
                        .format(Date())
                    Regex("_(\\d{9,})$").find(ckptSrc.nameWithoutExtension)
                        ?.groupValues?.get(1)
                        ?.toLongOrNull()
                        ?.let { secs ->
                            SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault())
                                .format(Date(secs * 1000))
                        } ?: fallback
                }

                val nextNum = getNextCheckpointNumber(context)
                val ckptDst = File(dir, "checkpoint_№${nextNum}_$ts.ckpt")
                val binDst = File(dir, "params_$ts.bin")

                ckptSrc.copyTo(ckptDst, overwrite = true)
                binSrc.copyTo(binDst, overwrite = true)

                // Очистка
                zipShared.toFile().delete()
                unpackedShared.forEach { it.toFile().delete() }

                stateFlow.value = State.DONE
                progressFlow.tryEmit(100)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Модель обучена (checkpoint=$checkpoint)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Throwable) {
                Log.e("TrainingUploadManager", "pipeline error", e)
                stateFlow.value = State.ERROR
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Ошибка обучения: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun getNextCheckpointNumber(ctx: Context): Int =
        ctx.getSharedPreferences(PreferenceKeysUBI4.TRAINING_PREFS, Context.MODE_PRIVATE).run {
            val cur = getInt(PreferenceKeysUBI4.KEY_CHECKPOINT_NUMBER, 1)
            edit().putInt(PreferenceKeysUBI4.KEY_CHECKPOINT_NUMBER, cur + 1).apply()
            cur
        }
}