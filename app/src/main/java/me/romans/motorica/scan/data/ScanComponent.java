package me.romans.motorica.scan.data;

import javax.inject.Singleton;

import dagger.Component;
import me.romans.motorica.old_electronic_by_Misha.data.BluetoothModule;
import me.romans.motorica.scan.view.ScanActivity;
import me.romans.motorica.scan.view.ScanActivity2;

@Singleton
@Component(modules = {BluetoothModule.class, ScanModule.class})
public interface ScanComponent {
    void inject(ScanActivity scanActivity);
    void inject(ScanActivity2 scanActivity2);
}
