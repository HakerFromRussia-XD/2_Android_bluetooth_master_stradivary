package me.start.motorica.scan.presenter;

import android.app.Activity;

import java.util.ArrayList;

import me.start.motorica.scan.data.ScanItem;

public interface ScanPresenter {
    void scanItemClick(int position, String name);
    void leItemClick(int position);
    void pairedItemClick(int position);
    void startScanning();
    void onStart(Activity activity);
    void onStop();
    void disconnect();
    void setOnPauseActivity(boolean onPauseActivity);
    int getOurGadgets();
    void setStartFlags (String deviceName);
    ArrayList<ScanItem> getPairedList ();
}
