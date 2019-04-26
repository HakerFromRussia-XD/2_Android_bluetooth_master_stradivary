package me.aflak.libraries.ui.chat.data;

import javax.inject.Singleton;

import dagger.Component;
import me.aflak.libraries.data.BluetoothModule;
import me.aflak.libraries.ui.chat.view.ChatActivity;
import me.aflak.libraries.ui.chat.view.Gesture_settings.Gesture_settings;
import me.aflak.libraries.ui.chat.view.Gesture_settings.Gesture_settings2;
import me.aflak.libraries.ui.chat.view.Gesture_settings.Gesture_settings3;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings2;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings3;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings4;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings5;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings6;
import me.aflak.libraries.ui.chat.view.InfinitySettings;

/**
 * Created by Omar on 20/12/2017.
 */
@Singleton
@Component(modules = {BluetoothModule.class, ChatModule.class})
public interface ChatComponent {
    void inject(ChatActivity chatActivity);
    void inject(GripperSettings gripperSettings);
    void inject(Gesture_settings gesture_settings);
    void inject(GripperSettings2 gripperSettings2);
    void inject(Gesture_settings2 gesture_settings2);
    void inject(GripperSettings3 gripperSettings3);
    void inject(GripperSettings4 gripperSettings4);
    void inject(Gesture_settings3 gesture_settings3);
    void inject(GripperSettings5 gripperSettings5);
    void inject(GripperSettings6 gripperSettings6);
    void inject(InfinitySettings infinitySettings);
}
