package com.bailout.stickk.ubi4.data.network

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.utility.logging.platformLog
import java.util.concurrent.TimeUnit

object SettingsProfileUploadWorkScheduler {
    private const val TAG = "SettingsProfileUploadWork"
    private const val DISCONNECT_WORK_NAME = "settings_profile_upload_on_disconnect"
    private const val APP_CLOSE_WORK_NAME = "settings_profile_upload_on_app_close"
    private const val DEFAULT_DISCONNECT_DELAY_SECONDS = 10L
    private const val DEFAULT_APP_CLOSE_DELAY_SECONDS = 3L
    private const val DEFAULT_LANG = "en"

    @Volatile
    private var uploadEnqueuedForCurrentConnection = false

    fun onConnected(context: Context) {
        uploadEnqueuedForCurrentConnection = false
        cancelDisconnectUpload(context)
        platformLog(TAG, "connected: reset upload guard")
    }

    fun enqueueDisconnectUpload(
        context: Context,
        reason: String,
        lang: String = DEFAULT_LANG,
        delaySeconds: Long = DEFAULT_DISCONNECT_DELAY_SECONDS
    ) {
        enqueue(
            context = context,
            uniqueWorkName = DISCONNECT_WORK_NAME,
            reason = reason,
            lang = lang,
            delaySeconds = delaySeconds.coerceAtLeast(0L),
            policy = ExistingWorkPolicy.KEEP
        )
    }

    fun enqueueAppCloseUpload(
        context: Context,
        lang: String = DEFAULT_LANG
    ) {
        enqueue(
            context = context,
            uniqueWorkName = APP_CLOSE_WORK_NAME,
            reason = "app_close",
            lang = lang,
            delaySeconds = DEFAULT_APP_CLOSE_DELAY_SECONDS,
            policy = ExistingWorkPolicy.REPLACE
        )
    }

    fun cancelAppCloseUpload(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(APP_CLOSE_WORK_NAME)
        uploadEnqueuedForCurrentConnection = false
        platformLog(TAG, "cancel app close upload")
    }

    fun cancelDisconnectUpload(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(DISCONNECT_WORK_NAME)
        platformLog(TAG, "cancel disconnect upload")
    }

    private fun enqueue(
        context: Context,
        uniqueWorkName: String,
        reason: String,
        lang: String,
        delaySeconds: Long,
        policy: ExistingWorkPolicy
    ) {
        val serial = SettingsProfileManager.serial().trim()
        if (serial.isBlank()) {
            platformLog(TAG, "skip enqueue: serial is blank reason=$reason")
            return
        }
        if (uploadEnqueuedForCurrentConnection) {
            platformLog(TAG, "skip enqueue: upload already enqueued reason=$reason serial=$serial")
            return
        }
        uploadEnqueuedForCurrentConnection = true

        val request = OneTimeWorkRequestBuilder<SettingsProfileUploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    SettingsProfileUploadWorker.KEY_SERIAL to serial,
                    SettingsProfileUploadWorker.KEY_LANG to lang.ifBlank { DEFAULT_LANG },
                    SettingsProfileUploadWorker.KEY_REASON to reason
                )
            )
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(uniqueWorkName, policy, request)

        platformLog(
            TAG,
            "enqueue: reason=$reason serial=$serial delay_seconds=$delaySeconds work=$uniqueWorkName"
        )
    }
}
