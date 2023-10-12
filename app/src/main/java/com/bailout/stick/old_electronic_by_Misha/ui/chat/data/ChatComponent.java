package com.bailout.stick.old_electronic_by_Misha.ui.chat.data;

import javax.inject.Singleton;

import dagger.Component;
import com.bailout.stick.old_electronic_by_Misha.data.BluetoothModule;
import com.bailout.stick.old_electronic_by_Misha.ui.chat.view.ChartActivity;
import com.bailout.stick.old_electronic_by_Misha.ui.chat.view.gesture_settings.FragmentGestureSettings;
import com.bailout.stick.old_electronic_by_Misha.ui.chat.view.gesture_settings.FragmentGestureSettings2;
import com.bailout.stick.old_electronic_by_Misha.ui.chat.view.gesture_settings.FragmentGestureSettings3;
import com.bailout.stick.old_electronic_by_Misha.ui.chat.view.gripper_settings.FragmentGripperSettings;
import com.bailout.stick.old_electronic_by_Misha.ui.chat.view.Massages;
import com.bailout.stick.old_electronic_by_Misha.ui.chat.view.service_settings.FragmentServiceSettings;
import com.bailout.stick.old_electronic_by_Misha.ui.chat.view.service_settings.FragmentServiceSettingsMono;

@Singleton
@Component(modules = {BluetoothModule.class, ChatModule.class})
public interface ChatComponent {
    void inject(ChartActivity chatActivity);
    void inject(FragmentGripperSettings fragmentGripperSettings);
    void inject(FragmentGestureSettings2 fragmentGestureSettings2);
    void inject(FragmentGestureSettings3 fragmentGestureSettings3);
    void inject(FragmentGestureSettings fragmentGestureSettings);
    void inject(FragmentServiceSettings fragmentServiceSettings);
    void inject(FragmentServiceSettingsMono fragmentServiceSettingsMono);
    void inject(Massages massages);
}
