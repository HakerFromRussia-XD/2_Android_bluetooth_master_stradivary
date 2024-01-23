package com.bailout.stickk.old_electronic_by_Misha.ui.chat.interactor;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import com.bailout.bluetooth.BluetoothCallback;
import com.bailout.bluetooth.DeviceCallback;
import com.bailout.bluetooth.ParserCallback;


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
