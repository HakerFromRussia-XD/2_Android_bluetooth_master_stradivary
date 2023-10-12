package com.bailout.stick.old_electronic_by_Misha;

import com.bailout.stick.old_electronic_by_Misha.data.BluetoothModule;
import androidx.multidex.MultiDexApplication;

import com.yandex.metrica.YandexMetrica;
import com.yandex.metrica.YandexMetricaConfig;

public class MyApp extends MultiDexApplication {
    private static MyApp app;
    private BluetoothModule bluetoothModule;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        bluetoothModule = new BluetoothModule(this);

        //App Metrica
        // Creating an extended library configuration.
        YandexMetricaConfig config = YandexMetricaConfig.newConfigBuilder("4140aa12-7386-4c82-8b5c-ca6ac12f6a85").build();
        // Initializing the AppMetrica SDK.
        YandexMetrica.activate(getApplicationContext(), config);
        // Automatic tracking of user activity.
        YandexMetrica.enableActivityAutoTracking(this);
    }

    public static MyApp app() {
        return app;
    }

    public BluetoothModule bluetoothModule() {
        return bluetoothModule;
    }
}
