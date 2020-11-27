package me.romans.motorica.scan.view;

import android.bluetooth.BluetoothDevice;

import java.util.List;

import me.romans.motorica.scan.data.ScanItem;

public interface ScanView{
    void showPairedList(List<String> items);
    void addDeviceToScanList(String item, BluetoothDevice device);
    void clearScanList();
    void clearPairedList();
    void setScanStatus(String status, boolean enabled);
    void setScanStatus(int resId, boolean enabled);
    void showProgress(boolean enabled);
    void enableScanButton(boolean enabled);
    void showToast(String message);
    void navigateToChat(String extraName, BluetoothDevice extraDevice);
    void setNewStageCellScanList (int numberCell, int setImage, String setText);
    List<ScanItem> getMyScanList ();
    void loadData();
    void buildScanListView();
    boolean isFirstStart();
}
