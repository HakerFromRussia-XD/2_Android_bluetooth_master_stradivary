package com.bailout.stickk.ubi4.ui.main

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ble.BluetoothLeService

class ControllerBleStatusConnection(
    private val context: Context,
    private val indicator: ImageView
): DefaultLifecycleObserver {


    private var isRegistered = false

    @Volatile
    var isReconnecting = false
        set(value) {
            field = value
            applyStateColor()
        }

    private enum class UiState { Connected, DisconnectedOrReconnecting }
    private var currentState: UiState = UiState.DisconnectedOrReconnecting

    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    currentState = UiState.DisconnectedOrReconnecting
                    applyStateColor()
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    isReconnecting = false
                    currentState = UiState.Connected
                    applyStateColor()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    isReconnecting = true
                    currentState = UiState.DisconnectedOrReconnecting
                    applyStateColor()
                }


            }

        }

    }
    private fun applyStateColor() {
        when {
            isReconnecting || currentState == UiState.DisconnectedOrReconnecting ->
                setIndicator(R.drawable.circle_16_red)
            currentState == UiState.Connected ->
                setIndicator(R.drawable.circle_16_green)
        }
    }

    private fun setIndicator(@DrawableRes resId: Int) {
        indicator.setImageDrawable(ContextCompat.getDrawable(context, resId))
    }

    override fun onStart(owner: LifecycleOwner) {
        register()
        currentState = UiState.DisconnectedOrReconnecting
        applyStateColor()
    }

    override fun onStop(owner: LifecycleOwner) {
        unregister()
    }


    private fun register() {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            LocalBroadcastManager.getInstance(context).registerReceiver(bleReceiver, filter)
        } else {
            context.registerReceiver(bleReceiver, filter)
        }
        isRegistered = true
    }

    private fun unregister() {
        if (!isRegistered) return
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(bleReceiver)
            } else {
                context.unregisterReceiver(bleReceiver)
            }
        } catch (_: IllegalArgumentException) { }
        isRegistered = false
    }


    object UiBridges {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        var bleStatusController: ControllerBleStatusConnection? = null
    }


}