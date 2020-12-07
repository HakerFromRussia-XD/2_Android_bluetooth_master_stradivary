package me.romans.motorica.scan.data;

import javax.inject.Singleton;

import dagger.Component;
import me.romans.motorica.old_electronic_by_Misha.data.BluetoothModule;
import me.romans.motorica.scan.view.ScanActivity;

/**
 * Created by Omar on 20/12/2017.
 */

@Singleton
@Component(modules = {BluetoothModule.class, ScanModule.class})
public interface ScanComponent {
    void inject(ScanActivity scanActivity);
}
