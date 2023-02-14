package me.start.motorica.scan.view;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

import me.start.motorica.scan.data.ScanItem;

public interface ScanView{
    void showPairedList(ArrayList<ScanItem> items);
    void addDeviceToScanList(String item, String address, BluetoothDevice device);
    void addLEDeviceToLeDevicesList(BluetoothDevice device, int rssi);
    void clearScanList();
    void setScanStatus(String status, boolean enabled);
    void setScanStatus(int resId, boolean enabled);
    void showProgress(boolean enabled);
    void enableScanButton(boolean enabled);
    void showToast(String message);
    void navigateToChart(String extraName, BluetoothDevice extraDevice);
    void navigateToLEChart(String extraName, BluetoothDevice extraDevice);
    boolean getFilteringOursDevices ();
    boolean isFirstStart();
    ArrayList<BluetoothDevice>  getLeDevices();
}
