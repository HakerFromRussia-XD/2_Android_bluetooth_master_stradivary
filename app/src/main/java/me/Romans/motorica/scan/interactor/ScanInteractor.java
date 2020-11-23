package me.Romans.motorica.scan.interactor;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import java.util.List;

import me.Romans.bluetooth.BluetoothCallback;
import me.Romans.bluetooth.DeviceCallback;
import me.Romans.bluetooth.DiscoveryCallback;

public interface ScanInteractor {
    List<String> getPairedDevices();
    BluetoothDevice getPairedDevice(int position);
    void scanDevices(DiscoveryCallback callback);
    boolean isBluetoothEnabled();
    void enableBluetooth();
    void stopScanning();
    void pair(int position);
    void onStart(BluetoothCallback bluetoothCallback, Activity activity);
    void onStop();
    void checkAvailableDevice(BluetoothDevice device, DeviceCallback callback);
    void disconnect();
    int getOurGadgets();
}
