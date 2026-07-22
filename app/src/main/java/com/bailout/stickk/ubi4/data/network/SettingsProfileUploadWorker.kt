package com.bailout.stickk.ubi4.data.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bailout.stickk.ubi4.bootstrap.SharedBootstrapper
import com.bailout.stickk.ubi4.data.local.db.AndroidCtx
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepositoryProvider
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlin.coroutines.cancellation.CancellationException

class SettingsProfileUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val serial = inputData.getString(KEY_SERIAL)?.trim().orEmpty()
        val lang = inputData.getString(KEY_LANG)?.takeUnless { it.isBlank() } ?: DEFAULT_LANG
        val reason = inputData.getString(KEY_REASON)?.takeUnless { it.isBlank() } ?: "unknown"

        if (serial.isBlank()) {
            platformLog(TAG, "skip: serial is blank reason=$reason")
            return Result.success()
        }

        ensureSharedStorageInitialized()
        platformLog(TAG, "start: reason=$reason serial=$serial lang=$lang attempt=$runAttemptCount")

        return runCatching {
            Ubi4SettingsProfileSender().sendProfile1SettingsForSerial(
                serial = serial,
                lang = lang
            )
        }.fold(
            onSuccess = { result ->
                platformLog(TAG, "payload=${result.settingsPayload}")
                platformLog(TAG, "success: reason=$reason deviceId=${result.deviceId} response=${result.serverResponse}")
                Result.success()
            },
            onFailure = { error ->
                if (error is CancellationException) {
                    platformLog(TAG, "cancelled: reason=$reason")
                    throw error
                }
                platformLog(TAG, "failed: reason=$reason error=${error.message ?: error::class.simpleName}")
                Result.failure()
            }
        )
    }

    private fun ensureSharedStorageInitialized() {
        AndroidCtx.appContext = applicationContext
        if (SettingsProfileRepositoryProvider.getOrNull() == null) {
            SharedBootstrapper.initialize()
        }
    }

    companion object {
        const val KEY_SERIAL = "serial"
        const val KEY_LANG = "lang"
        const val KEY_REASON = "reason"

        private const val TAG = "SettingsProfileUploadWorker"
        private const val DEFAULT_LANG = "en"
    }
}
