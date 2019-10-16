package me.Romans.motorica.ui.chat.data;

import javax.inject.Singleton;

import dagger.Component;
import me.Romans.motorica.data.BluetoothModule;
import me.Romans.motorica.ui.chat.view.ChatActivity;
import me.Romans.motorica.ui.chat.view.Gesture_settings.FragmentGestureSettings;
import me.Romans.motorica.ui.chat.view.Gesture_settings.FragmentGestureSettings2;
import me.Romans.motorica.ui.chat.view.Gesture_settings.FragmentGestureSettings3;
import me.Romans.motorica.ui.chat.view.Gripper_settings.FragmentGripperSettings;
import me.Romans.motorica.ui.chat.view.InfinitySettings;
import me.Romans.motorica.ui.chat.view.Service_settings.FragmentServiceSettings;
import me.Romans.motorica.ui.chat.view.Service_settings.FragmentServiceSettingsMono;
import me.Romans.motorica.ui.chat.view.experimental.DualChart;

/**
 * Created by Omar on 20/12/2017.
 */
@Singleton
@Component(modules = {BluetoothModule.class, ChatModule.class})
public interface ChatComponent {
    void inject(ChatActivity chatActivity);
    void inject(InfinitySettings infinitySettings);
    void inject(DualChart dualChart);
    void inject(FragmentGripperSettings fragmentGripperSettings);
    void inject(FragmentGestureSettings2 fragmentGestureSettings2);
    void inject(FragmentGestureSettings3 fragmentGestureSettings3);
    void inject(FragmentGestureSettings fragmentGestureSettings);
    void inject(FragmentServiceSettings fragmentServiceSettings);
    void inject(FragmentServiceSettingsMono fragmentServiceSettingsMono);
}
