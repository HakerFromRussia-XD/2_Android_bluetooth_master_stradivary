package me.romans.motorica.old_electronic_by_Misha.data;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.romans.bluetooth.Bluetooth;

/**
 * Created by Omar on 20/12/2017.
 */

@Module
public class BluetoothModule {
    private Context context;

    public BluetoothModule(Context context) {
        this.context = context;
    }

    @Provides @Singleton
    public Context provideContext(){
        return context;
    }

    @Provides @Singleton
    public Bluetooth provideBluetooth(){
        return new Bluetooth(context);
    }
}
