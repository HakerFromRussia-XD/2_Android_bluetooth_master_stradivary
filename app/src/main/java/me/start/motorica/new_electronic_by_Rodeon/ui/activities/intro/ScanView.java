package me.start.motorica.new_electronic_by_Rodeon.ui.activities.intro;

import android.bluetooth.BluetoothDevice;

public interface ScanView {
    void addDeviceToScanList(String item, BluetoothDevice device);
    void clearScanList();
}
