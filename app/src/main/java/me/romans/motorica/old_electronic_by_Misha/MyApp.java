package me.romans.motorica.old_electronic_by_Misha;

import android.app.Application;

import me.romans.motorica.old_electronic_by_Misha.data.BluetoothModule;

public class MyApp extends Application {
    private static MyApp app;
    private BluetoothModule bluetoothModule;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        bluetoothModule = new BluetoothModule(this);

    }

    public static MyApp app() {
        return app;
    }

    public BluetoothModule bluetoothModule() {
        return bluetoothModule;
    }
}
