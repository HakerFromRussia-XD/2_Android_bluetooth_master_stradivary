package com.bailout.stickk.scan.data;

import javax.inject.Singleton;

import dagger.Component;
import com.bailout.stickk.old_electronic_by_Misha.data.BluetoothModule;
import com.bailout.stickk.scan.view.ScanActivity;

@Singleton
@Component(modules = {BluetoothModule.class, ScanModule.class})
public interface ScanComponent {
    void inject(ScanActivity scanActivity);
}
