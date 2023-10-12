package com.bailout.stick.scan.data;

import javax.inject.Singleton;

import dagger.Component;
import com.bailout.stick.old_electronic_by_Misha.data.BluetoothModule;
import com.bailout.stick.scan.view.ScanActivity;

@Singleton
@Component(modules = {BluetoothModule.class, ScanModule.class})
public interface ScanComponent {
    void inject(ScanActivity scanActivity);
}
