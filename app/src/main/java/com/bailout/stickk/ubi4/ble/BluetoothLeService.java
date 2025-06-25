/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bailout.stickk.ubi4.ble;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys;
import com.bailout.stickk.ubi4.data.state.BLEState;
import com.bailout.stickk.ubi4.utility.EncodeByteToHex;

import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes;
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import timber.log.Timber;

@SuppressLint("MissingPermission")
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_STATE = "com.example.bluetooth.le.ACTION_STATE";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String CHARACTERISTIC_UUID = "com.example.bluetooth.le.CHARACTERISTIC_UUID";
    public final static String MIO_DATA = "com.example.bluetooth.le.MIO_DATA";
    public final static String FESTO_A_DATA = "com.example.bluetooth.le.FESTO_A_DATA";
    public final static String OPEN_MOTOR_DATA = "com.example.bluetooth.le.OPEN_MOTOR_DATA";
    public final static String CLOSE_MOTOR_DATA = "com.example.bluetooth.le.CLOSE_MOTOR_DATA";
    public final static String SHUTDOWN_CURRENT_HDLE = "com.example.bluetooth.le.SHUTDOWN_CURRENT_HDLE";
    public final static String SENSORS_DATA_THREAD_FLAG = "com.example.bluetooth.le.SENSORS_DATA_THREAD_FLAG";

    public final static String MIO_DATA_NEW = "com.example.bluetooth.le.MIO_DATA_NEW";
    public final static String SENS_VERSION_NEW_DATA = "com.example.bluetooth.le.SENS_VERSION_NEW_DATA";
    public final static String OPEN_THRESHOLD_NEW_DATA = "com.example.bluetooth.le.OPEN_THRESHOLD_NEW_DATA";
    public final static String CLOSE_THRESHOLD_NEW_DATA = "com.example.bluetooth.le.CLOSE_THRESHOLD_NEW_DATA";
    public final static String SENS_OPTIONS_NEW_DATA = "com.example.bluetooth.le.SENS_OPTIONS_NEW_DATA";
    public final static String SET_GESTURE_NEW_DATA = "com.example.bluetooth.le.SET_GESTURE_NEW_DATA";
    public final static String SET_REVERSE_NEW_DATA = "com.example.bluetooth.le.SET_REVERSE_NEW_DATA";
    public final static String ADD_GESTURE_NEW_DATA = "com.example.bluetooth.le.ADD_GESTURE_NEW_DATA";
    public final static String CALIBRATION_NEW_DATA = "com.example.bluetooth.le.CALIBRATION_NEW_DATA";
    public final static String SET_ONE_CHANNEL_NEW_DATA = "com.example.bluetooth.le.SET_ONE_CHANNEL_NEW_DATA";
    public final static String STATUS_CALIBRATION_NEW_DATA = "com.example.bluetooth.le.STATUS_CALIBRATION_NEW_DATA";
    public final static String CHANGE_GESTURE_NEW_DATA = "com.example.bluetooth.le.CHANGE_GESTURE_NEW_DATA";
    public final static String SERIAL_NUMBER_NEW_DATA = "com.example.bluetooth.le.SERIAL_NUMBER_NEW_DATA";
    public final static String SHUTDOWN_CURRENT_NEW_DATA = "com.example.bluetooth.le.SHUTDOWN_CURRENT_NEW_DATA";
    public final static String ROTATION_GESTURE_NEW_VM_DATA = "com.example.bluetooth.le.ROTATION_GESTURE_NEW_VM_DATA";
    public final static String DRIVER_VERSION_NEW_DATA = "com.example.bluetooth.le.DRIVER_VERSION_NEW_DATA";

    //ubi4
    //TODO ЗАЧЕМ??!? public final static String MAIN_CHANNEL = "com.example.bluetooth.le.MAIN_CHANNEL";
    public final static String MAIN_CHANNEL = "com.example.bluetooth.le.MAIN_CHANNEL";
    public final static String CONFIRMATION_SEND = "com.example.bluetooth.le.CONFIRMATION_SEND";
    ///////////////////////////// самая быстрая передача данных
    private final Handler handler = new Handler(Looper.getMainLooper());
    public ReceiverCallback receiverCallback;

    public void sendDataToReceiver(String state) {
        handler.post(() -> {
            if (receiverCallback != null) {
                receiverCallback.onDataReceived(state);
            }
        });
    }

    public void setReceiverCallback(ReceiverCallback callback) {
        this.receiverCallback = callback;
    }

    public interface ReceiverCallback {
        void onDataReceived(String state);
    }

    ///////////////////////////////
    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic, final String state) {
        final Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);

        final byte[] data = characteristic.getValue();

        if (data != null && data.length > 0) {
            if (String.valueOf(characteristic.getUuid()).equals(com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL)){
                if (state.equals(SampleGattAttributes.WRITE)) { intent.putExtra(CONFIRMATION_SEND,"");
                   // Log.d("TestSendByteArray","BleCommand was send");
                }
                if (state.equals(SampleGattAttributes.NOTIFY)) { intent.putExtra(MAIN_CHANNEL, data); }
            }
            if (state.equals(SampleGattAttributes.WRITE)) { intent.putExtra(CHARACTERISTIC_UUID, String.valueOf(characteristic.getUuid())); }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.MIO_MEASUREMENT)){
                intent.putExtra(MIO_DATA, data);
                intent.putExtra(SENSORS_DATA_THREAD_FLAG, false);
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.FESTO_A_CHARACTERISTIC)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(FESTO_A_DATA, data); intent.putExtra(ACTION_STATE, SampleGattAttributes.READ);}
                if (state.equals(SampleGattAttributes.WRITE)) { intent.putExtra(FESTO_A_DATA, data); intent.putExtra(ACTION_STATE, SampleGattAttributes.WRITE);}
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.OPEN_MOTOR_HDLE)){
                intent.putExtra(OPEN_MOTOR_DATA, data);
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.CLOSE_MOTOR_HDLE)){
                intent.putExtra(CLOSE_MOTOR_DATA, data);
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.MIO_MEASUREMENT_NEW)) {
                intent.putExtra(MIO_DATA_NEW, data);
                intent.putExtra(SENSORS_DATA_THREAD_FLAG, false);
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SENS_VERSION_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SENS_VERSION_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.OPEN_THRESHOLD_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(OPEN_THRESHOLD_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.CLOSE_THRESHOLD_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(CLOSE_THRESHOLD_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SENS_OPTIONS_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SENS_OPTIONS_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SET_GESTURE_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SET_GESTURE_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SET_REVERSE_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SET_REVERSE_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SET_ONE_CHANNEL_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SET_ONE_CHANNEL_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.STATUS_CALIBRATION_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(STATUS_CALIBRATION_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.ADD_GESTURE_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(ADD_GESTURE_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.CALIBRATION_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(CALIBRATION_NEW_DATA, data); intent.putExtra(ACTION_STATE, SampleGattAttributes.READ);}
                if (state.equals(SampleGattAttributes.WRITE)){ intent.putExtra(CALIBRATION_NEW_DATA, data); intent.putExtra(ACTION_STATE, SampleGattAttributes.WRITE);}
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SERIAL_NUMBER_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SERIAL_NUMBER_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SHUTDOWN_CURRENT_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SHUTDOWN_CURRENT_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.ROTATION_GESTURE_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(ROTATION_GESTURE_NEW_VM_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.DRIVER_VERSION_NEW)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(DRIVER_VERSION_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.DRIVER_VERSION_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(DRIVER_VERSION_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.MIO_MEASUREMENT_NEW_VM)) {
//                System.err.println("MIO_DATA_NEW from service data=" + data[0]);
                intent.putExtra(MIO_DATA_NEW, data);
                intent.putExtra(SENSORS_DATA_THREAD_FLAG, false);
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SENS_VERSION_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SENS_VERSION_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.OPEN_THRESHOLD_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(OPEN_THRESHOLD_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.CLOSE_THRESHOLD_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(CLOSE_THRESHOLD_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SENS_OPTIONS_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SENS_OPTIONS_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SET_GESTURE_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SET_GESTURE_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SET_REVERSE_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SET_REVERSE_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SET_ONE_CHANNEL_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SET_ONE_CHANNEL_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.STATUS_CALIBRATION_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(STATUS_CALIBRATION_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.CHANGE_GESTURE_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(CHANGE_GESTURE_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.ADD_GESTURE_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(ADD_GESTURE_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.CALIBRATION_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(CALIBRATION_NEW_DATA, data); intent.putExtra(ACTION_STATE, SampleGattAttributes.READ);}
                if (state.equals(SampleGattAttributes.WRITE)){ intent.putExtra(CALIBRATION_NEW_DATA, data); intent.putExtra(ACTION_STATE, SampleGattAttributes.WRITE);}
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SERIAL_NUMBER_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SERIAL_NUMBER_NEW_DATA, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SHUTDOWN_CURRENT_NEW_VM)) {
                if (state.equals(SampleGattAttributes.READ)) { intent.putExtra(SHUTDOWN_CURRENT_NEW_DATA, data); }
            }

            //TEST
            if (String.valueOf(characteristic.getUuid()).equals(SampleGattAttributes.SHUTDOWN_CURRENT_HDLE)){
                intent.putExtra(SHUTDOWN_CURRENT_HDLE, data);
            }
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
        else {
            sendBroadcast(intent);
        }

    }



    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                Log.d("BLE_DEBUG11", "onConnectionStateChange: STATE_CONNECTED, status: " + status);
//                BLEState.INSTANCE.publishConnecting();
//                broadcastUpdate(ACTION_GATT_CONNECTED);
//                requestMTU();
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                Log.d("BLE_DEBUG11", "onConnectionStateChange: STATE_DISCONNECTED, status: " + status);
//                BLEState.INSTANCE.publishDisconnect();
//                broadcastUpdate(ACTION_GATT_DISCONNECTED);
//                // Вызываем close() с небольшой задержкой, чтобы убедиться, что разрыв завершён
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                    close();
//                }, 500);
//            }
//        }



        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d("onConnectionStateChangeTAG", "onConnectionStateChange: status=" + status + ", newState=" + newState);
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                Log.d("BLE_DEBUG11", "onConnectionStateChange: STATE_CONNECTED, status: " + status);
                broadcastUpdate(intentAction);
                requestMTU();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.d("BLE_DEBUG11", "onConnectionStateChange: STATE_DISCONNECTED, status: " + status);
                broadcastUpdate(intentAction);
                if (receiverCallback != null) {
                    receiverCallback.onDataReceived(SampleGattAttributes.NOTIFY);
                }
            }
        }


        private void requestMTU() {
            int mtu = 256; // Maximum allowed 517 - 3 bytes do BLE  //256 + 3

            mBluetoothGatt.requestMtu(mtu);

//            System.err.println("BLE debug -> mtu=$mtu");
        }


        @Override
        public void onMtuChanged(BluetoothGatt gatt,int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d("TestSendByteArray", "status ="+status + "MTU: "+mtu);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.err.println("BLE debug onMtuChanged GATT_SUCCESS");
                mBluetoothGatt.discoverServices();
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("onConnectionStateChangeTAG", "onServicesDiscovered: status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BLEState.INSTANCE.publishReady();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    System.err.println("установили доп параметры соединения");
                    mBluetoothGatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_OPTION_NO_PREFERRED);
                }
            } else {
                BLEState.INSTANCE.publishError();
                Timber.tag(TAG).w("onServicesDiscovered received: %s", status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(characteristic, SampleGattAttributes.READ);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d("TestSendByteArray","status =" +status + " " + EncodeByteToHex.Companion.bytesToHexString(characteristic.getValue()));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendDataToReceiver(SampleGattAttributes.WRITE);
//                Log.d("TestSendByteArray","запись удалась!!");
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                System.err.println("запись не удалась");
//                Log.d("TestSendByteArray","запись не удалась");

            }
        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] d   = characteristic.getValue();
            String hex = EncodeByteToHex.bytesToHexString(d);
            Log.d("BLE_RAW", "UUID=" + characteristic.getUuid() + "  len=" + d.length + "  " + hex);
            Log.d("onCharacteristicChanged", "Характеристика изменилась: " +
                    characteristic.getUuid().toString() + ", данные: " +
                    EncodeByteToHex.Companion.bytesToHexString(characteristic.getValue()));
            broadcastUpdate(characteristic, SampleGattAttributes.NOTIFY);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
        else {
            sendBroadcast(intent);
        }

    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Timber.e("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Timber.e("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Timber.tag(TAG).w("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Timber.d("Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Timber.tag(TAG).w("Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Timber.d("Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.tag(TAG).w("BluetoothAdapter not initialized");
            return;
        }
//        refreshGattCache();
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.d("BLE_DEBUG11", "close(): вызов close() на mBluetoothGatt");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    public boolean refreshGattCache() {
        try {
            if (mBluetoothGatt == null) return false;
            Method refreshMethod = mBluetoothGatt.getClass().getMethod("refresh");
            if (refreshMethod != null) {
                boolean success = (boolean) refreshMethod.invoke(mBluetoothGatt);
                Log.d("BLE_DEBUG", "refresh() returned " + success);
                return success;
            }
        } catch (Exception e) {
            Log.e("BLE_DEBUG", "Could not invoke refresh method", e);
        }
        return false;
    }


    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.tag(TAG).w("BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Request a write {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic a write.
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.tag(TAG).w("BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.tag(TAG).w("BluetoothAdapter not initialized");
            return;
        }
        Log.d("BLEController",
                "Устанавливаю уведомления для " + characteristic.getUuid() + ", enabled: " + enabled);
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
