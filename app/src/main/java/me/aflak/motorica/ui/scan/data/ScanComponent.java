package me.aflak.motorica.ui.scan.data;

import javax.inject.Singleton;

import dagger.Component;
import me.aflak.motorica.data.BluetoothModule;
import me.aflak.motorica.ui.scan.view.ScanActivity;

/**
 * Created by Omar on 20/12/2017.
 */

@Singleton
@Component(modules = {BluetoothModule.class, ScanModule.class})
public interface ScanComponent {
    void inject(ScanActivity scanActivity);
}
