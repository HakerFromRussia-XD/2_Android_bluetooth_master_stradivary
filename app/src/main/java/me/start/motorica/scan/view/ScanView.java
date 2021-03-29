package me.start.motorica.scan.view;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

import me.start.motorica.scan.data.ScanItem;

public interface ScanView{
    void showPairedList(List<String> items);
    void addDeviceToScanList(String item, BluetoothDevice device);
    void addLEDeviceToScanList(String item, BluetoothDevice device);
    void clearScanList();
    void clearPairedList();
    void setScanStatus(String status, boolean enabled);
    void setScanStatus(int resId, boolean enabled);
    void showProgress(boolean enabled);
    void enableScanButton(boolean enabled);
    void showToast(String message);
    void navigateToChart(String extraName, BluetoothDevice extraDevice);
    void navigateToLEChart(String extraName, BluetoothDevice extraDevice);
    void setNewStageCellScanList (int numberCell, int setImage, String setText);
    List<ScanItem> getMyScanList ();
    void loadData();
    void buildScanListView();
    boolean isFirstStart();
    ArrayList<BluetoothDevice>  getLeDevices();
}
