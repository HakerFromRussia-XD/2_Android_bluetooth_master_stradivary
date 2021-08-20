package me.start.motorica.old_electronic_by_Misha.ui.chat.interactor;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import me.start.bluetooth.Bluetooth;
import me.start.bluetooth.BluetoothCallback;
import me.start.bluetooth.DeviceCallback;
import me.start.bluetooth.ParserCallback;


public class ChartInteractorImpl implements ChatInteractor {
    private final Bluetooth bluetooth;

    public ChartInteractorImpl(Bluetooth bluetooth) {
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

    @Override
    public void disconnect(){
        bluetooth.disconnect();
    }

    @Override
    public void disable(){
        bluetooth.disable();
    }

    public void setIterator(int i){
        bluetooth.setIterator(i);
    }
}
