package me.Romans.motorica.scan.data;

import javax.inject.Singleton;

import dagger.Component;
import me.Romans.motorica.old_electronic_by_Misha.data.BluetoothModule;
import me.Romans.motorica.scan.view.ScanActivity;

/**
 * Created by Omar on 20/12/2017.
 */

@Singleton
@Component(modules = {BluetoothModule.class, ScanModule.class})
public interface ScanComponent {
    void inject(ScanActivity scanActivity);
}
