package com.bailout.stickk.scan.interactor;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

import com.bailout.bluetooth.BluetoothCallback;
import com.bailout.bluetooth.DeviceCallback;
import com.bailout.bluetooth.DiscoveryCallback;
import com.bailout.stickk.scan.data.ScanItem;

public interface ScanInteractor {
    List<String> getPairedDevices(boolean filteringOursDevices);
    ArrayList<ScanItem> getPairedDevicesItem(boolean filteringOursDevices);
    BluetoothDevice getPairedDevice(int position);
    void scanDevices(DiscoveryCallback callback);
    boolean isBluetoothEnabled();
    void enableBluetooth();
    void stopScanning();
    void pair(int position);
    void onStart(BluetoothCallback bluetoothCallback, Activity activity);
    void onStop();
    void disconnect();
    int getOurGadgets();
}
