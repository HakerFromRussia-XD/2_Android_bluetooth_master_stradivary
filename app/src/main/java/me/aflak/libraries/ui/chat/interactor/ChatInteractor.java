package me.aflak.libraries.ui.chat.interactor;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import me.aflak.bluetooth.BluetoothCallback;
import me.aflak.bluetooth.DeviceCallback;
import me.aflak.bluetooth.ParserCallback;


public interface ChatInteractor {
    boolean isBluetoothEnabled();
    void enableBluetooth();
    void connectToDevice(BluetoothDevice device, DeviceCallback callback);//добавить сюда свою сигнатуру матода
    void parsingExperimental(ParserCallback parser);
    void sendMessageByte (byte[] message);
    void sendMessagestr(String message);
    void onStart(BluetoothCallback bluetoothCallback, Activity activity);
    void onStop();
}
