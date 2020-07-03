package me.Romans.motorica.ui.scan.view;

import android.bluetooth.BluetoothDevice;

import java.util.List;

/**
 * Created by Omar on 20/12/2017.
 */

public interface ScanView{
    void showPairedList(List<String> items);
    void addDeviceToScanList(String item);
    void clearScanList();
    void clearPairedList();
    void setScanStatus(String status, boolean enabled);
    void setScanStatus(int resId, boolean enabled);
    void showProgress(boolean enabled);
    void enableScanButton(boolean enabled);
    void showToast(String message);
    void navigateToChat(String extraName, BluetoothDevice extraDevice);
}
