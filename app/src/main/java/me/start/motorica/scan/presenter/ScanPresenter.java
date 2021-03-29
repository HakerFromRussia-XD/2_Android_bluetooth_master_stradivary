package me.start.motorica.scan.presenter;

import android.app.Activity;

public interface ScanPresenter {
    void scanItemClick(int position, String name);
    void pairedItemClick(int position);
    void leItemClick(int position);
    void itemClick(int position);
    void startScanning();
    void onStart(Activity activity);
    void onStop();
    void disconnect();
    void setOnPauseActivity(boolean onPauseActivity);
    int getOurGadgets();
    void setStartFlags (String deviceName);
}
