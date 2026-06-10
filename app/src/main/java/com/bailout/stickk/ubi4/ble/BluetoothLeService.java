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
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bailout.stickk.ubi4.blelog.BleLogStore;
import com.bailout.stickk.ubi4.data.state.BLEState;
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4;
import com.bailout.stickk.ubi4.utility.EncodeByteToHex;

import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

import static com.bailout.stickk.ubi4.utility.logging.PlatformLog_androidKt.platformLog;

@SuppressLint("MissingPermission")
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private static final int DESIRED_MTU = 247;
    private static final long RSSI_POLL_INTERVAL_MS = 25_000L;
    private static final double CONNECTION_INTERVAL_UNIT_MS = 1.25d;
    private static final int SUPERVISION_TIMEOUT_UNIT_MS = 10;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int connectionUpdateEventCounter = 0;
    private final Object notificationsLock = new Object();
    private final Set<UUID> enabledNotificationUuids = new HashSet<>();
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);

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
    // ответ от Ромы: чтоб прокинуть данные механизмом интента в сервис
    public final static String MAIN_CHANNEL = "com.example.bluetooth.le.MAIN_CHANNEL";
    public final static String SENSORS_STREAM_V3 = "com.example.bluetooth.le.SENSORS_STREAM_V3";
    public final static String MAIN_CHANNEL_V3_SERIALPORTCHAR = "com.example.bluetooth.le.MAIN_CHANNEL_V3_SERIALPORTCHAR";
    public final static String CONFIRMATION_SEND = "com.example.bluetooth.le.CONFIRMATION_SEND";
    public final static String ACTION_NOTIFICATION_SUBSCRIBED = "com.example.bluetooth.le.ACTION_NOTIFICATION_SUBSCRIBED";
    public final static String EXTRA_NOTIFICATION_UUID = "com.example.bluetooth.le.EXTRA_NOTIFICATION_UUID";
    public final static String EXTRA_NOTIFICATION_ENABLED = "com.example.bluetooth.le.EXTRA_NOTIFICATION_ENABLED";
    public final static String EXTRA_GATT_STATUS = "com.example.bluetooth.le.EXTRA_GATT_STATUS";
    ///////////////////////////// самая быстрая передача данных
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Handler connectionHandler = new Handler(Looper.getMainLooper());
    private final Runnable rssiPollRunnable = new Runnable() {
        @Override
        public void run() {
            final BluetoothGatt gatt = mBluetoothGatt;
            if (gatt == null) {
                return;
            }
            final boolean requested = gatt.readRemoteRssi();
            platformLog(TAG, "readRemoteRssi requested: " + requested);
            connectionHandler.postDelayed(this, RSSI_POLL_INTERVAL_MS);
        }
    };
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

    private void startLinkMaintenance() {
        connectionHandler.removeCallbacks(rssiPollRunnable);
        connectionHandler.postDelayed(rssiPollRunnable, RSSI_POLL_INTERVAL_MS);
    }

    private void stopLinkMaintenance() {
        connectionHandler.removeCallbacks(rssiPollRunnable);
    }

    private void requestStablePhy(BluetoothGatt gatt) {
        if (gatt == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        try {
            // 1M обычно стабильнее на «капризных» BLE-стэках и при слабом RSSI, чем принудительный 2M.
            gatt.setPreferredPhy(
                    BluetoothDevice.PHY_LE_1M_MASK,
                    BluetoothDevice.PHY_LE_1M_MASK,
                    BluetoothDevice.PHY_OPTION_NO_PREFERRED
            );
        } catch (Exception e) {
            Log.w(TAG, "setPreferredPhy failed: " + e.getMessage());
        }
    }

    private void discoverServicesSafely(BluetoothGatt gatt, String reason) {
        if (gatt == null) {
            return;
        }
        final boolean started = gatt.discoverServices();
        platformLog(TAG, "discoverServices(" + reason + "): " + started);
    }

    private void requestMtuOrDiscoverServices(BluetoothGatt gatt) {
        if (gatt == null) {
            return;
        }
        final boolean mtuRequested = gatt.requestMtu(DESIRED_MTU);
        platformLog(TAG, "requestMtu(" + DESIRED_MTU + "): " + mtuRequested);
        if (!mtuRequested) {
            discoverServicesSafely(gatt, "mtu request rejected");
        }
    }

    private void closeGattQuietly(BluetoothGatt gatt) {
        if (gatt == null) {
            return;
        }
        try {
            gatt.close();
        } catch (Exception e) {
            Log.w(TAG, "BluetoothGatt.close() failed: " + e.getMessage());
        }
    }

    private void trackNotificationState(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (characteristic == null) {
            return;
        }
        synchronized (notificationsLock) {
            if (enabled) {
                enabledNotificationUuids.add(characteristic.getUuid());
            } else {
                enabledNotificationUuids.remove(characteristic.getUuid());
            }
        }
    }

    private void clearTrackedNotifications() {
        synchronized (notificationsLock) {
            enabledNotificationUuids.clear();
        }
    }

    private List<UUID> snapshotTrackedNotifications() {
        synchronized (notificationsLock) {
            return new ArrayList<>(enabledNotificationUuids);
        }
    }

    private BluetoothGattCharacteristic findCharacteristicByUuid(BluetoothGatt gatt, UUID uuid) {
        if (gatt == null || uuid == null) {
            return null;
        }
        final List<BluetoothGattService> services = gatt.getServices();
        if (services == null) {
            return null;
        }
        for (BluetoothGattService service : services) {
            final BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
            if (characteristic != null) {
                return characteristic;
            }
        }
        return null;
    }

    private void unsubscribeFromTrackedNotifications(BluetoothGatt gatt) {
        if (gatt == null) {
            clearTrackedNotifications();
            return;
        }
        final List<UUID> trackedUuids = snapshotTrackedNotifications();
        if (trackedUuids.isEmpty()) {
            return;
        }
        for (UUID uuid : trackedUuids) {
            final BluetoothGattCharacteristic characteristic = findCharacteristicByUuid(gatt, uuid);
            if (characteristic == null) {
                Log.w(TAG, "Characteristic not found while disabling notify: " + uuid);
                synchronized (notificationsLock) {
                    enabledNotificationUuids.remove(uuid);
                }
                continue;
            }
            try {
                gatt.setCharacteristicNotification(characteristic, false);
            } catch (Exception e) {
                Log.w(TAG, "setCharacteristicNotification(false) failed for " + uuid + ": " + e.getMessage());
            }

            final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
            if (descriptor == null) {
                Log.w(TAG, "Descriptor CCCD not found while disabling notify for " + uuid);
                synchronized (notificationsLock) {
                    enabledNotificationUuids.remove(uuid);
                }
                continue;
            }

            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            try {
                final boolean writeStarted = gatt.writeDescriptor(descriptor);
                if (!writeStarted) {
                    Log.w(TAG, "CCCD disable write not started for " + uuid);
                }
            } catch (Exception e) {
                Log.w(TAG, "CCCD disable write failed for " + uuid + ": " + e.getMessage());
            }
            synchronized (notificationsLock) {
                enabledNotificationUuids.remove(uuid);
            }
        }
    }

    private String gattStatusToName(int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "GATT_SUCCESS";
            case 8:
                return "GATT_CONN_TIMEOUT";
            case 19:
                return "GATT_CONN_TERMINATE_PEER_USER";
            case 22:
                return "GATT_CONN_TERMINATE_LOCAL_HOST";
            case 34:
                return "GATT_CONN_LMP_TIMEOUT";
            case 62:
                return "GATT_CONN_FAIL_ESTABLISH";
            case 133:
                return "GATT_ERROR(133)";
            default:
                return "GATT_STATUS_" + status;
        }
    }

    ///////////////////////////////
    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic, final String state) {
        final Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);

        final byte[] data = characteristic.getValue();

        if (data != null && data.length > 0) {
            if (String.valueOf(characteristic.getUuid()).equals(com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC)){
                if (state.equals(SampleGattAttributes.NOTIFY)) { intent.putExtra(MAIN_CHANNEL, data); }
            }
            if (String.valueOf(characteristic.getUuid()).equals(com.bailout.stickk.ubi4.ble.SampleGattAttributes.SENSORS_STREAM_UUID)){
                if (state.equals(SampleGattAttributes.WRITE)) { intent.putExtra(CONFIRMATION_SEND,"");  }
                if (state.equals(SampleGattAttributes.NOTIFY)) { intent.putExtra(SENSORS_STREAM_V3, data);  }
            }
            if (String.valueOf(characteristic.getUuid()).equals(com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID)){
                if (state.equals(SampleGattAttributes.NOTIFY)) { intent.putExtra(MAIN_CHANNEL_V3_SERIALPORTCHAR, data); }
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
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            platformLog(
                    "onConnectionStateChangeTAG",
                    "onConnectionStateChange: status=" + status + " (" + gattStatusToName(status) + "), newState=" + newState
            );
            platformLog(
                    "STATE_CHANGE",
                    "status=" + status + " (" + gattStatusToName(status) + "), newState=" + newState
            );
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                platformLog("BLE_DEBUG11", "onConnectionStateChange: STATE_CONNECTED, status: " + status);
                connectionUpdateEventCounter = 0;
                platformLog(
                        "STATE_CONNECTED",
                        "appDoesNotCallRequestConnectionPriority=true, proceed=requestMtuOrDiscoverServices"
                );
                broadcastUpdate(ACTION_GATT_CONNECTED);
//                requestHighConnectionPriority(gatt);
                requestMtuOrDiscoverServices(gatt);
                return;
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                platformLog("BLE_DEBUG11", "onConnectionStateChange: STATE_DISCONNECTED, status: " + status);
                stopLinkMaintenance();
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                BLEState.INSTANCE.publishError();
                if (receiverCallback != null) {
                    receiverCallback.onDataReceived(SampleGattAttributes.NOTIFY);
                }
                if (gatt == mBluetoothGatt) {
                    closeGattQuietly(mBluetoothGatt);
                    mBluetoothGatt = null;
                } else {
                    closeGattQuietly(gatt);
                }
                clearTrackedNotifications();
                return;
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                stopLinkMaintenance();
                BLEState.INSTANCE.publishError();
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                if (gatt == mBluetoothGatt) {
                    closeGattQuietly(mBluetoothGatt);
                    mBluetoothGatt = null;
                } else {
                    closeGattQuietly(gatt);
                }
                clearTrackedNotifications();
            }
        }


        @Override
        public void onMtuChanged(BluetoothGatt gatt,int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            platformLog("TestSendByteArray", "status ="+status + "MTU: "+mtu);
            platformLog("MTU_CHANGED", "mtu=" + mtu + ", status=" + status + " (" + gattStatusToName(status) + ")");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.err.println("BLE debug onMtuChanged GATT_SUCCESS");
                discoverServicesSafely(gatt, "mtu changed");
            } else {
                discoverServicesSafely(gatt, "mtu change failed");
            }
        }

        // Hidden callback in framework BluetoothGattCallback.
        // If runtime forwards it to app layer, these are real negotiated BLE link params.
        @SuppressWarnings("unused")
        public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout, int status) {
            connectionUpdateEventCounter++;
            platformLog(
                    "ON_CONNECTION_UPDATED" + connectionUpdateEventCounter,
                    formatConnectionParams(interval, latency, timeout, status)
            );
        }

        private String formatConnectionParams(int intervalUnits, int latency, int timeoutUnits, int status) {
            final double intervalMs = intervalUnits * CONNECTION_INTERVAL_UNIT_MS;
            final int timeoutMs = timeoutUnits * SUPERVISION_TIMEOUT_UNIT_MS;
            return "interval=" + intervalMs + "ms(" + intervalUnits + "u), " +
                    "latency=" + latency + ", " +
                    "timeout=" + timeoutMs + "ms(" + timeoutUnits + "u), " +
                    "status=" + status + " (" + gattStatusToName(status) + ")";
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            platformLog("onConnectionStateChangeTAG", "onServicesDiscovered: status=" + status);
            platformLog("SERVICES_DISCOVERED", "status=" + status + " (" + gattStatusToName(status) + ")");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BLEState.INSTANCE.publishReady();
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                requestStablePhy(gatt);
                startLinkMaintenance();
            } else {
                BLEState.INSTANCE.publishError();
                Timber.tag(TAG).w("onServicesDiscovered received: %s", status);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                platformLog(TAG, "onReadRemoteRssi failed with status=" + status + " (" + gattStatusToName(status) + ")");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BleLogStore.logIncoming(characteristic.getValue());
                broadcastUpdate(characteristic, SampleGattAttributes.READ);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            platformLog("TestSendByteArray","status =" +status + " " + EncodeByteToHex.Companion.bytesToHexString(characteristic.getValue()));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendDataToReceiver(SampleGattAttributes.WRITE);
//                platformLog("TestSendByteArray","запись удалась!!");
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                System.err.println("запись не удалась");
//                platformLog("TestSendByteArray","запись не удалась");

            }
        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] d   = characteristic.getValue();
            if (!shouldHideFromBleLog(characteristic)) {
                BleLogStore.logIncoming(d);
            }
            String hex = String.valueOf(EncodeByteToHex.bytesToHexString(d));
            platformLog("BLE_RAW", "UUID=" + characteristic.getUuid() + "  len=" + d.length + "  " + hex);
            platformLog("onCharacteristicChanged", "Характеристика изменилась: " +
                    characteristic.getUuid().toString() + ", данные: " +
                    EncodeByteToHex.Companion.bytesToHexString(characteristic.getValue()));
            broadcastUpdate(characteristic, SampleGattAttributes.NOTIFY);
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            final BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            final boolean isCccdDescriptor = UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)
                    .equals(descriptor.getUuid());
            // На Android 14 и ниже descriptor.getValue() в callback часто не содержит записанное значение,
            // поэтому проверка только через Arrays.equals(...) даёт false даже при успешной подписке.
            final boolean hasEnableValue = java.util.Arrays.equals(
                    descriptor.getValue(),
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            );
            final boolean notificationEnabled = BluetoothGatt.GATT_SUCCESS == status
                    && isCccdDescriptor
                    && (hasEnableValue || (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0);

            final Intent intent = new Intent(ACTION_NOTIFICATION_SUBSCRIBED);
            intent.putExtra(EXTRA_NOTIFICATION_UUID, String.valueOf(characteristic.getUuid()));
            intent.putExtra(EXTRA_GATT_STATUS, status);
//            intent.putExtra(
//                    EXTRA_NOTIFICATION_ENABLED,
//                    BluetoothGatt.GATT_SUCCESS == status
//                            && java.util.Arrays.equals(
//                            descriptor.getValue(),
//                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                    )
//            );
            intent.putExtra(EXTRA_NOTIFICATION_ENABLED, notificationEnabled);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
            else {
                sendBroadcast(intent);
            }
        }
    };

    private boolean shouldHideFromBleLog(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) return false;
        String uuid = String.valueOf(characteristic.getUuid());
        boolean isGraphStream = uuid.equals(com.bailout.stickk.ubi4.ble.SampleGattAttributes.SENSORS_STREAM_UUID)
                || uuid.equals(SampleGattAttributes.MIO_MEASUREMENT)
                || uuid.equals(SampleGattAttributes.MIO_MEASUREMENT_NEW)
                || uuid.equals(SampleGattAttributes.MIO_MEASUREMENT_NEW_VM);
        if (!isGraphStream) return false;

        return getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(PreferenceKeysUbi4.BLE_LOG_HIDE_GRAPH_STREAM, true);
    }

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
        platformLog("CONNECT_CALL", "address=" + address + ", appDoesNotCallRequestConnectionPriority=true");

        if (mBluetoothGatt != null) {
            stopLinkMaintenance();
            closeGattQuietly(mBluetoothGatt);
            mBluetoothGatt = null;
            clearTrackedNotifications();
        }

        final BluetoothDevice device;
        try {
            device = mBluetoothAdapter.getRemoteDevice(address);
        } catch (IllegalArgumentException e) {
            Timber.tag(TAG).w("Invalid device address: %s", address);
            return false;
        }
        if (device == null) {
            Timber.tag(TAG).w("Device not found.  Unable to connect.");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }
        if (mBluetoothGatt == null) {
            Timber.tag(TAG).w("connectGatt returned null.");
            return false;
        }
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
        unsubscribeFromTrackedNotifications(mBluetoothGatt);
        stopLinkMaintenance();
//        refreshGattCache();
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            clearTrackedNotifications();
            return;
        }
        unsubscribeFromTrackedNotifications(mBluetoothGatt);
        stopLinkMaintenance();
        platformLog("BLE_DEBUG11", "close(): вызов close() на mBluetoothGatt");
        closeGattQuietly(mBluetoothGatt);
        mBluetoothGatt = null;
        clearTrackedNotifications();
    }


    public boolean refreshGattCache() {
        try {
            if (mBluetoothGatt == null) return false;
            Method refreshMethod = mBluetoothGatt.getClass().getMethod("refresh");
            if (refreshMethod != null) {
                boolean success = (boolean) refreshMethod.invoke(mBluetoothGatt);
                platformLog("BLE_DEBUG", "refresh() returned " + success);
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
        BleLogStore.logOutgoing(characteristic.getValue());
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
        if (characteristic == null) {
            Timber.tag(TAG).w("Characteristic is null");
            return;
        }
        platformLog("BLEController",
                "Устанавливаю уведомления для " + characteristic.getUuid() + ", enabled: " + enabled);
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        trackNotificationState(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                CLIENT_CHARACTERISTIC_CONFIG_UUID);
        if (descriptor == null) {
            Log.w("BLEController", "Descriptor CCCD not found for " + characteristic.getUuid());
            return;
        }
        descriptor.setValue(
                enabled
                        ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        );
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
        close();
        super.onDestroy();
    }
}
