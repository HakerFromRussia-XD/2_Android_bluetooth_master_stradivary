//package com.bailout.stickk.ubi4.ble
//
//import android.content.Context
//import android.content.Intent
//import android.content.ServiceConnection
//import android.content.SharedPreferences
//import android.os.Handler
//import android.os.Looper
//import android.view.View
//import android.widget.ExpandableListView
//import android.widget.SimpleExpandableListAdapter
//import com.bailout.stickk.scan.view.ScanActivity
//import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
//import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
//
//class BleConnector(
//    private val settings: SharedPreferences,
//    private val scanActivityClass: Class<*> = ScanActivity::class.java // по умолчанию ScanActivity
//) {
//    var bluetoothLeService: BluetoothLeService? = null
//    var isServiceBound: Boolean = false
//    var mConnected: Boolean = false
//    var endFlag: Boolean = true
//    var percentSynchronize: Int = 0
//
//    // UI-ссылки – их MainActivity задаст после создания экземпляра
//    lateinit var connectView: View
//    lateinit var disconnectView: View
//    lateinit var gattServicesList: ExpandableListView
//
//    // ServiceConnection задаётся в MainActivity и передаётся сюда
//    lateinit var serviceConnection: ServiceConnection
//
//    // Флаг отключения – если true, отключение было намеренным
//    var isDisconnected: Boolean = false
//
//    /**
//     * Привязывает BLE-сервис к активности.
//     */
//    fun bindBleService() {
//        val intent = Intent(main, BluetoothLeService::class.java)
//        isServiceBound = main.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
//    }
//
//    /**
//     * Сбрасывает сохранённый MAC-адрес последнего подключения.
//     */
//    private fun resetLastMAC() {
//        settings.edit().putString(PreferenceKeysUBI4.LAST_CONNECTION_MAC_UBI4, "null").apply()
//    }
//
//    /**
//     * Открывает активность сканирования и завершает текущую.
//     */
//    fun openScanActivity() {
//        System.err.println("BleConnector: openScanActivity()")
//        resetLastMAC()
//        val intent = Intent(main, scanActivityClass)
//        main.startActivity(intent)
//        main.finish()
//    }
//
//    /**
//     * Инициирует отключение:
//     * – Устанавливает флаг отключения.
//     * – Вызывает disconnect() у BLE-сервиса (не вызывайте close() здесь!).
//     * – Обновляет UI.
//     * – Не отвязывает сервис и не вызывает переход – это делает BroadcastReceiver при получении STATE_DISCONNECTED.
//     * – В качестве fallback через 2000 мс, если событие не придёт, производится переход.
//     */
//    fun disconnect() {
//        System.err.println("BleConnector: disconnect()")
//        isDisconnected = true
//        bluetoothLeService?.let { service ->
//            println("--> BleConnector: инициируем disconnect()")
//            service.disconnect()
//            // Не вызывайте service.close() здесь – это сделает BluetoothGattCallback через Handler
//            // Не отвязывайте сервис сразу – дождитесь события STATE_DISCONNECTED
//            gattServicesList.setAdapter(null as SimpleExpandableListAdapter?)
//        }
//        mConnected = false
//        endFlag = true
//        main.runOnUiThread {
//            main.mConnectView.visibility = View.GONE
//            main.mDisconnectView.visibility = View.VISIBLE
//        }
//        main.invalidateOptionsMenu()
//        percentSynchronize = 0
//        // Fallback: если событие STATE_DISCONNECTED не придёт, через 2000 мс принудительно переходим:
//        Handler(Looper.getMainLooper()).postDelayed({
//            if (isDisconnected) {
//                System.err.println("BleConnector: fallback – принудительный переход к ScanActivity")
//                openScanActivity()
//            }
//        }, 2000)
//    }
//}