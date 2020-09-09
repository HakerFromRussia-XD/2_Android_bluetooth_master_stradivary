package me.Romans.motorica.ui.chat.view.massage_to_send;


import android.app.Activity;
import android.content.Intent;

import me.Romans.motorica.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.utils.ConstantManager;

import static me.Romans.motorica.ui.chat.view.ChartActivity.GESTURE_SETTINGS;
import static me.Romans.motorica.ui.chat.view.ChartActivity.NUMBER_CELL;

public class Massages implements ChatPresenter {
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
        TextByteTriggerSettings[7] = calculationCRC(TextByteTriggerSettings);
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
        TextByteTriggerSettings[6] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageSettingsDubbingHDLC(byte numberFinger, int intValueFingerAngle,
                                                     int intValueFingerSpeed){
        byte[] TextByteTriggerSettings = new byte[7];
        if(NUMBER_CELL == 0){
            TextByteTriggerSettings[0] = numberFinger;
            TextByteTriggerSettings[1] = ConstantManager.WRITE;
            TextByteTriggerSettings[2] = GESTURE_SETTINGS;
            TextByteTriggerSettings[3] = 0x06;
            TextByteTriggerSettings[4] = (byte) intValueFingerSpeed;
            TextByteTriggerSettings[5] = (byte) intValueFingerAngle;
            TextByteTriggerSettings[6] = calculationCRC_HDLC(TextByteTriggerSettings);
        } else {
            if(NUMBER_CELL == 2){
                TextByteTriggerSettings[0] = numberFinger;
                TextByteTriggerSettings[1] = ConstantManager.WRITE;
                TextByteTriggerSettings[2] = GESTURE_SETTINGS;
                TextByteTriggerSettings[3] = 0x07;
                TextByteTriggerSettings[4] = (byte) intValueFingerSpeed;
                TextByteTriggerSettings[5] = (byte) intValueFingerAngle;
                TextByteTriggerSettings[6] = calculationCRC_HDLC(TextByteTriggerSettings);
            } else {
                if(NUMBER_CELL == 4){
                    TextByteTriggerSettings[0] = numberFinger;
                    TextByteTriggerSettings[1] = ConstantManager.WRITE;
                    TextByteTriggerSettings[2] = GESTURE_SETTINGS;
                    TextByteTriggerSettings[3] = 0x08;
                    TextByteTriggerSettings[4] = (byte) intValueFingerSpeed;
                    TextByteTriggerSettings[5] = (byte) intValueFingerAngle;
                    TextByteTriggerSettings[6] = calculationCRC_HDLC(TextByteTriggerSettings);
                }
            }
        }

        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageSettingsCalibrationHDLC(byte type, byte numberChannel,
                                                        byte rec, int inf, byte bSwitch){
        byte[] TextByteTriggerSettings4 = new byte[7];
        switch (rec){
            case ConstantManager.READ:
                switch (type){
                    case ConstantManager.OPEN_ANGEL_CALIB_TYPE:
                    case ConstantManager.CLOSE_ANGEL_CALIB_TYPE:
                    case ConstantManager.WIDE_ANGEL_CALIB_TYPE:
                    case ConstantManager.U_CALIB_TYPE:
                        byte[] TextByteTriggerSettings = new byte[4];
                        TextByteTriggerSettings[0] = numberChannel;
                        TextByteTriggerSettings[1] = rec;
                        TextByteTriggerSettings[2] = type;
                        TextByteTriggerSettings[3] = calculationCRC_HDLC(TextByteTriggerSettings);
                        return TextByteTriggerSettings;
                    case ConstantManager.TEMP_CALIB_TYPE:
                    case ConstantManager.CURRENTS_CALIB_TYPE:
                        byte[] TextByteTriggerSettings2 = new byte[5];
                        TextByteTriggerSettings2[0] = numberChannel;
                        TextByteTriggerSettings2[1] = rec;
                        TextByteTriggerSettings2[2] = type;
                        TextByteTriggerSettings2[3] = (byte) inf;
                        TextByteTriggerSettings2[4] = calculationCRC_HDLC(TextByteTriggerSettings2);
                        return TextByteTriggerSettings2;
                }
            case ConstantManager.WRITE:
                switch (type){
                    case ConstantManager.OPEN_STOP_CLOSE_CALIB_TYPE:
                    case ConstantManager.SPEED_CALIB_TYPE:
                    case ConstantManager.ANGLE_CALIB_TYPE:
                    case ConstantManager.ETE_CALIBRATION_CALIB_TYPE:
                    case ConstantManager.EEPROM_SAVE_CALIB_TYPE:
                    case ConstantManager.ANGLE_FIX_CALIB_TYPE:
                    case ConstantManager.SET_ADDR_CALIB_TYPE:
                    case ConstantManager.TEMP_CALIB_TYPE:
                    case ConstantManager.CURRENT_TIMEOUT_CALIB_TYPE:
                    case ConstantManager.OPEN_ANGEL_CALIB_TYPE:
                    case ConstantManager.CLOSE_ANGEL_CALIB_TYPE:
                    case ConstantManager.MAGNET_INVERT_CALIB_TYPE:
                    case ConstantManager.REVERS_MOTOR_CALIB_TYPE:
                    case ConstantManager.ZERO_CROSSING_CALIB_TYPE:
                        byte[] TextByteTriggerSettings = new byte[5];
                        TextByteTriggerSettings[0] = numberChannel;
                        TextByteTriggerSettings[1] = rec;
                        TextByteTriggerSettings[2] = type;
                        TextByteTriggerSettings[3] = (byte) inf;
                        TextByteTriggerSettings[4] = calculationCRC_HDLC(TextByteTriggerSettings);
                        return TextByteTriggerSettings;
                    case ConstantManager.WIDE_ANGEL_CALIB_TYPE:
                        byte[] TextByteTriggerSettings2 = new byte[6];
                        TextByteTriggerSettings2[0] = numberChannel;
                        TextByteTriggerSettings2[1] = rec;
                        TextByteTriggerSettings2[2] = type;
                        TextByteTriggerSettings2[3] = (byte) (inf >> 8);
                        TextByteTriggerSettings2[4] = (byte) inf;
                        TextByteTriggerSettings2[5] = calculationCRC_HDLC(TextByteTriggerSettings2);
                        return TextByteTriggerSettings2;
                    case ConstantManager.CURRENT_CONTROL_CALIB_TYPE:
                        byte[] TextByteTriggerSettings3 = new byte[7];
                        TextByteTriggerSettings3[0] = numberChannel;
                        TextByteTriggerSettings3[1] = rec;
                        TextByteTriggerSettings3[2] = type;
                        TextByteTriggerSettings3[3] = bSwitch;
                        TextByteTriggerSettings3[4] = (byte) (inf >> 8);
                        TextByteTriggerSettings3[5] = (byte)  inf;
                        TextByteTriggerSettings3[6] = calculationCRC_HDLC(TextByteTriggerSettings3);
                        return TextByteTriggerSettings3;
                }
            default:
                return TextByteTriggerSettings4;
        }
    }
    @Override
    public void onCreate(Intent intent) {

    }

    @Override
    public void onHelloWorld(byte[] textbyte) {

    }

    @Override
    public void onStart(Activity activity) {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

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
    public byte calculationCRC(byte[] bytes) {
        byte CRC = 0x00;
        for (int i = 1; i < bytes.length-1; i++){
            CRC += bytes[i];
            CRC = (byte) (CRC << 1);
        }
        return CRC;
    }

    @Override
    public byte calculationCRC_HDLC(byte[] bytes) {
        byte CRC = (byte) 0xFF;
        boolean b = false;
        for (int i = 0; i < bytes.length-1; i++){
            CRC ^= bytes[i];
            for (int j = 0; j < 8; j++)
            {
                b = ((CRC & 0x80) >> 7) != 0;
                CRC = (byte) (b  ? (CRC << 1) ^ 0x31 : CRC << 1);
            }
        }
        return CRC;
    }
}
