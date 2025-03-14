package com.bailout.stickk.old_electronic_by_Misha.ui.chat.data;

import javax.inject.Singleton;

import dagger.Component;
import com.bailout.stickk.old_electronic_by_Misha.data.BluetoothModule;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.view.NemoStandActivity;

@Singleton
@Component(modules = {BluetoothModule.class, NemoStandModule.class})
public interface NemoStandComponent {
    void inject(NemoStandActivity nemoStandActivity);
//    void inject(Massages massages);
}
