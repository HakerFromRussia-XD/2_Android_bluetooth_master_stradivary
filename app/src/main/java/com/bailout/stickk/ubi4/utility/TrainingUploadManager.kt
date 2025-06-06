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

    /** Все возможные состояния операции */
    enum class State { IDLE, RUNNING, EXPORTING, DONE, ERROR }

    /** Флоу для состояния и прогресса (0–100%) */
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
        // 1. Берём папку externalFilesDir
        val dir = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("External storage unavailable")

        // 2. Формируем List<Pair<emg8, emg8.data_passport>> только из выбранных файлов
        val pairs: List<Pair<File, File>> = selectedEmg8Files.mapNotNull { emg ->
            val passport = File(dir, "${emg.name}.data_passport")
            if (passport.exists()) {
                emg to passport
            } else {
                Log.w("TrainingUploadManager", "Не найден passport для ${emg.name}, пропускаем")
                null
            }
        }

        // Если получилось пусто — нечего отправлять
        if (pairs.isEmpty()) {
            Log.e("TrainingUploadManager", "Из выбранных .emg8 ни одна пара (emg8 + passport) не найдена")
            // Возвращаем сразу отменённый Job
            return Job().apply { cancel() }
        }

        // 3. Запускаем корутину для upload → download → распаковка
        return CoroutineScope(Dispatchers.IO).launch {
            try {
                // ─── 3.1. Переключаем состояние в RUNNING ───
                stateFlow.value = State.RUNNING

                // ─── 3.2. Вызываем uploadTrainingData, передавая сразу все пары ───
                //      repo.uploadTrainingData сам отправит multipart со всеми парами
                val checkpoint: String = repo.uploadTrainingData(
                    token  = token,
                    serial = serial,
                    pairs  = pairs
                ) { rawSseLine ->
                    // SSE-строки вида "data: 37" или JSON "data: {\"message\":\"ckpt_123\"}"
                    rawSseLine
                        .removePrefix("data:")
                        .trim()
                        .toIntOrNull()
                        ?.let { progressFlow.tryEmit(it.coerceIn(0, 100)) }
                }

                Log.d("TrainingUploadManager", "Получили checkpoint = $checkpoint")

                // ─── 3.3. Меняем состояние на EXPORTING ───
                stateFlow.value = State.EXPORTING

                // ─── 3.4. Скачиваем архив и распаковываем:
                //      ВАЖНО: этот метод **не меняем**, он совпадает с вашим:
                //      suspend fun downloadAndUnpackCheckpoint(token, checkpoint, outputDir): Pair<File, List<File>>
                val (zipFile, unpackedFiles) = repo.downloadAndUnpackCheckpoint(
                    token      = token,
                    checkpoint = checkpoint,
                    outputDir  = dir
                )

                // ─── 3.5. Находим среди unpackedFiles .ckpt и .bin ───
                val ckptSrc: File = unpackedFiles.first { it.extension == "ckpt" }
                val binSrc : File = unpackedFiles.first { it.extension == "bin"  }

                // ─── 3.6. Формируем timestamp (если в имени .ckpt есть “_<число секунд>”, парсим,
                // otherwise — fallback на текущую дату) ───
                val ts: String = run {
                    val fallback = SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault())
                        .format(Date())
                    Regex("_(\\d{9,})\$").find(ckptSrc.nameWithoutExtension)
                        ?.groupValues?.get(1)
                        ?.toLongOrNull()
                        ?.let { secs ->
                            SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault())
                                .format(Date(secs * 1000))
                        } ?: fallback
                }

                // ─── 3.7. Берём следующий номер чекпоинта из SharedPreferences ───
                val nextNum = getNextCheckpointNumber(context)

                // ─── 3.8. Копируем оригинальные файлы в новые имена:
                //      checkpoint_№<nextNum>_<ts>.ckpt
                //      params_<ts>.bin
                val ckptDst = File(dir, "checkpoint_№${nextNum}_$ts.ckpt")
                val binDst  = File(dir, "params_$ts.bin")
                ckptSrc.copyTo(ckptDst, overwrite = true)
                binSrc.copyTo(binDst, overwrite = true)

                // ─── 3.9. Удаляем временный .zip и все распакованные файлы ───
                zipFile.delete()
                unpackedFiles.forEach { it.delete() }

                // ─── 3.10. Всё успешно ───
                stateFlow.value = State.DONE
                progressFlow.tryEmit(100)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Модель обучена 🎉 (checkpoint=$checkpoint)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            catch (e: Throwable) {
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

    /**
     * Берёт из SharedPreferences текущее значение KEY_CHECKPOINT_NUMBER (по имени TRAINING_PREFS),
     * возвращает это число и тут же увеличивает его на +1.
     */
    private fun getNextCheckpointNumber(ctx: Context): Int =
        ctx.getSharedPreferences(PreferenceKeysUBI4.TRAINING_PREFS, Context.MODE_PRIVATE).run {
            val cur = getInt(PreferenceKeysUBI4.KEY_CHECKPOINT_NUMBER, 1)
            edit().putInt(PreferenceKeysUBI4.KEY_CHECKPOINT_NUMBER, cur + 1).apply()
            cur
        }
}