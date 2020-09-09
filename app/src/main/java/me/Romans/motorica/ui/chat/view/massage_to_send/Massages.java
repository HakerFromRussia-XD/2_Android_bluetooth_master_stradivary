package me.Romans.motorica.ui.chat.view.massage_to_send;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import me.Romans.motorica.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.utils.ConstantManager;

import static me.Romans.motorica.ui.chat.view.ChartActivity.GESTURE_SETTINGS;
import static me.Romans.motorica.ui.chat.view.ChartActivity.NUMBER_CELL;

public class Massages implements ChatPresenter{
    @Inject public ChatPresenter presenter;


    //////////////////////////////////////////////////////////////////////////////
    /**                      составление блютуз посылок                        **/
    //////////////////////////////////////////////////////////////////////////////
    public byte[] CompileMassageSettings(byte numberFinger, int intValueFingerAngle,
                                         int intValueFingerSpeed){
        byte[] TextByteTriggerSettings = new byte[8];
        TextByteTriggerSettings[0] = 0x03;
        TextByteTriggerSettings[1] = numberFinger;
        TextByteTriggerSettings[2] = ConstantManager.WRITE;
        TextByteTriggerSettings[3] = GESTURE_SETTINGS;
        TextByteTriggerSettings[4] = NUMBER_CELL;
        TextByteTriggerSettings[5] = (byte) intValueFingerSpeed;
        TextByteTriggerSettings[6] = (byte) intValueFingerAngle;
        TextByteTriggerSettings[7] = presenter.calculationCRC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMessageSettingsHDLC(byte numberFinger, int intValueFingerAngle,
                                              int intValueFingerSpeed){
        byte[] TextByteTriggerSettings = new byte[7];
        TextByteTriggerSettings[0] = numberFinger;
        TextByteTriggerSettings[1] = ConstantManager.WRITE;
        TextByteTriggerSettings[2] = GESTURE_SETTINGS;
        TextByteTriggerSettings[3] = NUMBER_CELL;
        TextByteTriggerSettings[4] = (byte) intValueFingerSpeed;
        TextByteTriggerSettings[5] = (byte) intValueFingerAngle;
        TextByteTriggerSettings[6] = presenter.calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }

    @Override
    public void onCreate(Intent intent) {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onStart(Activity activity) {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onHelloWorld(byte[] textbyte) {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void disable() {

    }

    @Override
    public void setOnPauseActivity(boolean onPauseActivity) {

    }

    @Override
    public byte calculationCRC(byte[] textByteTreegSettings) {
        return 0;
    }

    @Override
    public byte calculationCRC_HDLC(byte[] textByteTreegSettings) {
        return 0;
    }
}
