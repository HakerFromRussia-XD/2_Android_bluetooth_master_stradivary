package com.bailout.stickk.ubi4.ui.fragments.account.games

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bailout.stickk.BuildConfig
import java.io.File

object GamePackageInstaller {
    const val ACTION_INSTALL_STATUS =
        "com.bailout.stickk.ubi4.game.ACTION_INSTALL_STATUS"
    const val EXTRA_STATUS = "status"
    const val EXTRA_MESSAGE = "message"

    fun install(context: Context, apk: File) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(BuildConfig.MOTORICA_STK_PACKAGE)
        }
        val sessionId = packageInstaller.createSession(params)
        var session: PackageInstaller.Session? = null
        try {
            session = packageInstaller.openSession(sessionId)
            apk.inputStream().use { input ->
                session.openWrite(apk.name, 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            val callbackIntent = Intent(context, GameInstallStatusReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                callbackIntent,
                flags
            )
            session.commit(pendingIntent.intentSender)
        } catch (error: Throwable) {
            runCatching { packageInstaller.abandonSession(sessionId) }
            throw error
        } finally {
            session?.close()
        }
    }
}

class GameInstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirmIntent != null) {
                    context.startActivity(confirmIntent)
                }
            }
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                LocalBroadcastManager.getInstance(context).sendBroadcast(
                    Intent(GamePackageInstaller.ACTION_INSTALL_STATUS)
                        .putExtra(GamePackageInstaller.EXTRA_STATUS, status)
                        .putExtra(GamePackageInstaller.EXTRA_MESSAGE, message)
                )
                if (status != PackageInstaller.STATUS_SUCCESS && message != null) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
