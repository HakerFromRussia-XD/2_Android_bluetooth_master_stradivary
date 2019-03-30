package me.aflak.libraries.ui.chat.interactor;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.BluetoothCallback;
import me.aflak.bluetooth.DeviceCallback;
import me.aflak.bluetooth.ParserCallback;


public class ChatInteractorImpl implements ChatInteractor {
    private Bluetooth bluetooth;

    public ChatInteractorImpl(Bluetooth bluetooth) {
        this.bluetooth = bluetooth;
    }

    @Override
    public boolean isBluetoothEnabled() {
        return bluetooth.isEnabled();
    }

    @Override
    public void enableBluetooth() {
        bluetooth.enable();
    }

    @Override
    public void connectToDevice(BluetoothDevice device, DeviceCallback callback) {
        bluetooth.setDeviceCallback(callback);
        bluetooth.connectToDevice(device);
    }

    @Override
    public void parsingExperimental(ParserCallback parser) {
        bluetooth.enableParsing(parser);
    }

    @Override
    public void sendMessageByte (byte[] message){
        bluetooth.send(message);
    }

    @Override
    public void sendMessagestr(String message) {
        bluetooth.sendstr(message); //Sending as "US-ASCII" by default
    }

    @Override
    public void onStart(BluetoothCallback bluetoothCallback, Activity activity) {
        bluetooth.onStart();
        bluetooth.setCallbackOnUI(activity);
        bluetooth.setBluetoothCallback(bluetoothCallback);
    }

    @Override
    public void onStop() {
        bluetooth.onStop();
    }
}
