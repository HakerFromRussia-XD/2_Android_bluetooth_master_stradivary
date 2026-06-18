package com.bailout.stickk.ubi4.game

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bailout.stickk.R
import com.bailout.stickk.intro.SplashScreen
import com.bailout.stickk.ubi4.ble.BluetoothLeService
import com.bailout.stickk.ubi4.data.state.GameControlSignal
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.motorica.gamecontrol.GameControlSnapshot
import com.motorica.gamecontrol.IGameControlCallback
import com.motorica.gamecontrol.IGameControlService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GameControlBridgeService : Service() {
    private val callbacks = RemoteCallbackList<IGameControlCallback>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val snapshotLock = Any()
    private var wakeLock: PowerManager.WakeLock? = null
    private var latestSnapshot = GameControlSnapshot()
    private var sequence = 0L
    private var lastPublishElapsedMs = 0L
    private var lastBleIntentLogElapsedMs = 0L
    private var systemReceiverRegistered = false

    private val binder = object : IGameControlService.Stub() {
        override fun getLatestSnapshot(): GameControlSnapshot {
            val snapshot = synchronized(snapshotLock) {
                this@GameControlBridgeService.latestSnapshot
            }
            Log.d(TAG, "${LOG_PREFIX}binder getLatestSnapshot uid=${Binder.getCallingUid()} seq=${snapshot.seq} open=${snapshot.openLevel} close=${snapshot.closeLevel} connected=${snapshot.connected}")
            return snapshot
        }

        override fun registerCallback(callback: IGameControlCallback?) {
            if (callback == null) return
            val registered = callbacks.register(callback)
            val snapshot = synchronized(snapshotLock) {
                this@GameControlBridgeService.latestSnapshot
            }
            Log.d(TAG, "${LOG_PREFIX}binder registerCallback uid=${Binder.getCallingUid()} registered=$registered seq=${snapshot.seq} open=${snapshot.openLevel} close=${snapshot.closeLevel} connected=${snapshot.connected}")
            try {
                callback.onGameControlSnapshot(snapshot)
            } catch (_: RemoteException) {
                Log.w(TAG, "${LOG_PREFIX}binder registerCallback initial callback failed")
            }
        }

        override fun unregisterCallback(callback: IGameControlCallback?) {
            if (callback != null) {
                callbacks.unregister(callback)
                Log.d(TAG, "${LOG_PREFIX}binder unregisterCallback uid=${Binder.getCallingUid()}")
            }
        }
    }

    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    val current = synchronized(snapshotLock) {
                        latestSnapshot
                    }
                    publishSnapshot(current.openLevel, current.closeLevel, connected = true, force = true, source = "gatt_connected")
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    WidgetState.gameControlSignalFlow.value = GameControlSignal(connected = false)
                    publishSnapshot(openLevel = 0, closeLevel = 0, connected = false, force = true, source = "gatt_disconnected")
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    publishSensorStream(intent)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        seedLatestSnapshot()
        startForeground(NOTIFICATION_ID, createNotification())
        acquireWakeLock()
        registerBleReceivers()
        serviceScope.launch {
            WidgetState.gameControlSignalFlow.collect { signal ->
                publishSnapshot(
                    openLevel = signal.openLevel,
                    closeLevel = signal.closeLevel,
                    connected = signal.connected,
                    force = !signal.connected,
                    source = "state_flow"
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        unregisterBleReceivers()
        synchronized(snapshotLock) {
            callbacks.kill()
        }
        serviceScope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    private fun seedLatestSnapshot() {
        val signal = WidgetState.gameControlSignalFlow.value
        latestSnapshot = GameControlSnapshot(
            VERSION,
            ++sequence,
            SystemClock.elapsedRealtime(),
            signal.openLevel.coerceIn(0, 255),
            signal.closeLevel.coerceIn(0, 255),
            signal.connected
        )
    }

    private fun publishSensorStream(intent: Intent) {
        val data = intent.getByteArrayExtra(BluetoothLeService.SENSORS_STREAM_V3)
            ?: return
        if (data.size < 2) return
        val openLevel = data[0].toInt() and 0xFF
        val closeLevel = data[1].toInt() and 0xFF
        val now = SystemClock.elapsedRealtime()
        if (now - lastBleIntentLogElapsedMs >= BLE_INTENT_LOG_INTERVAL_MS) {
            lastBleIntentLogElapsedMs = now
            Log.d(TAG, "${LOG_PREFIX}ble intent open=$openLevel close=$closeLevel size=${data.size}")
        }
        WidgetState.gameControlSignalFlow.value = GameControlSignal(
            openLevel = openLevel,
            closeLevel = closeLevel,
            connected = true
        )
        publishSnapshot(openLevel, closeLevel, connected = true, force = false, source = "ble_intent")
    }

    private fun publishSnapshot(openLevel: Int, closeLevel: Int, connected: Boolean, force: Boolean, source: String) {
        synchronized(snapshotLock) {
            val now = SystemClock.elapsedRealtime()
            if (!force && now - lastPublishElapsedMs < MIN_PUBLISH_INTERVAL_MS) return
            lastPublishElapsedMs = now

            val snapshot = GameControlSnapshot(
                VERSION,
                ++sequence,
                now,
                openLevel.coerceIn(0, 255),
                closeLevel.coerceIn(0, 255),
                connected
            )
            latestSnapshot = snapshot
            if (!connected || sequence % 30L == 0L) {
                Log.d(TAG, "${LOG_PREFIX}publish source=$source seq=$sequence open=${snapshot.openLevel} close=${snapshot.closeLevel} connected=$connected")
            }

            val count = callbacks.beginBroadcast()
            try {
                if (!connected || sequence % 30L == 0L) {
                    Log.d(TAG, "${LOG_PREFIX}binder callbacks count=$count seq=${snapshot.seq}")
                }
                for (i in 0 until count) {
                    try {
                        callbacks.getBroadcastItem(i).onGameControlSnapshot(snapshot)
                    } catch (_: RemoteException) {
                        Log.w(TAG, "${LOG_PREFIX}binder callback failed index=$i seq=${snapshot.seq}")
                    }
                }
            } finally {
                callbacks.finishBroadcast()
            }
        }
    }

    private fun createNotification(): android.app.Notification {
        ensureNotificationChannel()
        val intent = Intent(this, SplashScreen::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Motorica Start")
            .setContentText("Game control is active")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Motorica game control",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MotoricaStart:GameControlBridge"
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun registerBleReceivers() {
        val filter = IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(bleReceiver, filter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(bleReceiver, filter)
        }
        systemReceiverRegistered = true
    }

    private fun unregisterBleReceivers() {
        runCatching { LocalBroadcastManager.getInstance(this).unregisterReceiver(bleReceiver) }
        if (systemReceiverRegistered) {
            runCatching { unregisterReceiver(bleReceiver) }
            systemReceiverRegistered = false
        }
    }

    companion object {
        const val ACTION_BIND = "com.motorica.gamecontrol.BIND"
        const val PERMISSION = "com.motorica.gamecontrol.permission.CONTROL_GAME"
        private const val TAG = "MotoricaGameBridge"
        private const val LOG_PREFIX = "[BLE stk-game debug] "
        private const val VERSION = 1
        private const val CHANNEL_ID = "motorica_game_control"
        private const val NOTIFICATION_ID = 14001
        private const val MIN_PUBLISH_INTERVAL_MS = 33L
        private const val BLE_INTENT_LOG_INTERVAL_MS = 1000L
        private const val WAKE_LOCK_TIMEOUT_MS = 6L * 60L * 60L * 1000L

        fun start(context: Context) {
            val intent = Intent(context, GameControlBridgeService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
