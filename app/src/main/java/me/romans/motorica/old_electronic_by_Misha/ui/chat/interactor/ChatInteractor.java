package me.romans.motorica.old_electronic_by_Misha.ui.chat.interactor;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import me.romans.bluetooth.BluetoothCallback;
import me.romans.bluetooth.DeviceCallback;
import me.romans.bluetooth.ParserCallback;


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
