package me.start.motorica.scan.data;

import javax.inject.Singleton;

import dagger.Component;
import me.start.motorica.old_electronic_by_Misha.data.BluetoothModule;
import me.start.motorica.scan.view.ScanActivity;

@Singleton
@Component(modules = {BluetoothModule.class, ScanModule.class})
public interface ScanComponent {
    void inject(ScanActivity scanActivity);
}
