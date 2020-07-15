package me.Romans.motorica.ui.chat.interactor;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import me.Romans.bluetooth.BluetoothCallback;
import me.Romans.bluetooth.DeviceCallback;
import me.Romans.bluetooth.ParserCallback;


public interface ChatInteractor {
    boolean isBluetoothEnabled();
    void enableBluetooth();
    void connectToDevice(BluetoothDevice device, DeviceCallback callback);
    void parsingExperimental(ParserCallback parser);
    void sendMessageByte (byte[] message);
    void setIterator (int i);
    void sendMessagestr(String message);
    void onStart(BluetoothCallback bluetoothCallback, Activity activity);
    void onStop();
    void disconnect();
    void disable();
}
