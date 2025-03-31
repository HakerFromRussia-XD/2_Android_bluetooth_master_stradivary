package com.bailout.stickk.ubi4.ble

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.View
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import com.bailout.stickk.scan.view.ScanActivity
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main

class DisconnectHelper(
    private var bluetoothLeService: BluetoothLeService?,
    private val activity: Activity,
    private val mConnectView: View,
    private val mDisconnectView: View,
    private val mGattServicesList: ExpandableListView
) {
    // Флаги состояния
    var isConnected: Boolean = true
    var isDisconnected: Boolean = false
    var endFlag: Boolean = false
    var percentSynchronize: Int = 0

    // Флаг «привязан ли сервис»
    private var isServiceBound: Boolean = false

    /**
     * ServiceConnection, которым владеет DisconnectHelper.
     * MainActivity будет вызывать bindService(..., serviceConnection, ...).
     */


    /**
     * Метод для разрыва соединения и отвязки сервиса.
     */

}