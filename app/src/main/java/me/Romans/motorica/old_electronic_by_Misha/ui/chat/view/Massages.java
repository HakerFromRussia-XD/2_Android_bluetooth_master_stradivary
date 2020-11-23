package me.Romans.motorica.old_electronic_by_Misha.ui.chat.view;


import android.app.Activity;
import android.content.Intent;

import me.Romans.bluetooth.BluetoothConstantManager;
import me.Romans.motorica.old_electronic_by_Misha.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.old_electronic_by_Misha.utils.ConstantManager;

import static me.Romans.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity.GESTURE_SETTINGS;
import static me.Romans.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity.NUMBER_CELL;

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
                    case ConstantManager.SPEED_INCREMENT_TYPE:
                    case ConstantManager.DISABLE_ANGLE_CONTROL_TYPE:
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
    public byte[] CompileMassageControlComplexGesture(int GESTURE_NUMBER){
        byte[] TextByteTriggerSettings = new byte[2];
        TextByteTriggerSettings[0] = 0x06;
        TextByteTriggerSettings[1] = (byte) GESTURE_NUMBER;
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageControl(byte numberFinger){
        byte[] TextByteTriggerSettings = new byte[6];
        TextByteTriggerSettings[0] = 0x05;
        TextByteTriggerSettings[1] = numberFinger;
        TextByteTriggerSettings[2] = 0x02;
        TextByteTriggerSettings[3] = 0x14;
        TextByteTriggerSettings[4] = NUMBER_CELL;
        TextByteTriggerSettings[5] = calculationCRC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageControlHDLC(byte numberFinger){
        byte[] TextByteTriggerSettings = new byte[5];
        TextByteTriggerSettings[0] = numberFinger;
        TextByteTriggerSettings[1] = 0x02;
        TextByteTriggerSettings[2] = 0x14;
        TextByteTriggerSettings[3] = NUMBER_CELL;
        TextByteTriggerSettings[4] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageTriggerMod(int Trigger_id){
        byte[] TextByteTriggerSettings = new byte[2];
        TextByteTriggerSettings[0] = 0x08;
        TextByteTriggerSettings[1] = (byte) Trigger_id;
        System.out.println("Trigger mod:" + Trigger_id);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageTriggerModHDLC(int Trigger_id){
        byte[] TextByteTriggerSettings = new byte[5];
        TextByteTriggerSettings[0] = ConstantManager.ADDR_TRIG_MODE;
        TextByteTriggerSettings[1] = ConstantManager.WRITE;
        TextByteTriggerSettings[2] = BluetoothConstantManager.TRIG_MODE_HDLC;
        TextByteTriggerSettings[3] = (byte) Trigger_id;
        TextByteTriggerSettings[4] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageMainDataHDLC(){
        byte[] TextByteTriggerSettings = new byte[4];
        TextByteTriggerSettings[0] = ConstantManager.ADDR_MAIN_DATA;
        TextByteTriggerSettings[1] = ConstantManager.READ;
        TextByteTriggerSettings[2] = BluetoothConstantManager.CURR_MAIN_DATA_HDLC;
        TextByteTriggerSettings[3] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageSensorActivate(int numberSensor){
        byte[] TextByteTriggerSettings = new byte[4];
        TextByteTriggerSettings[0] = 0x09;
        TextByteTriggerSettings[1] = (byte) numberSensor;
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageSensorActivateHDLC(){
        byte[] TextByteTriggerSettings = new byte[5];
        TextByteTriggerSettings[0] = ConstantManager.ADDR_ENDPOINT_POSITION;
        TextByteTriggerSettings[1] = ConstantManager.WRITE;
        TextByteTriggerSettings[2] = BluetoothConstantManager.ENDPOINT_POSITION;
        TextByteTriggerSettings[3] = BluetoothConstantManager.NOP;
        TextByteTriggerSettings[4] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageSensorActivate2HDLC(byte cell){
        byte[] TextByteTriggerSettings = new byte[5];
        TextByteTriggerSettings[0] = ConstantManager.ADDR_BRODCAST;
        TextByteTriggerSettings[1] = ConstantManager.WRITE;
        TextByteTriggerSettings[2] = BluetoothConstantManager.MOVE_HDLC;
        TextByteTriggerSettings[3] = cell;
        TextByteTriggerSettings[4] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageSettingsNotUseInternalADCHDLC(byte notUse){
        byte[] TextByteTriggerSettings = new byte[5];
        TextByteTriggerSettings[0] = ConstantManager.ADDR_SOURCE_ADC;
        TextByteTriggerSettings[1] = ConstantManager.WRITE;
        TextByteTriggerSettings[2] = BluetoothConstantManager.ADC_SOURCE_HDLC;
        TextByteTriggerSettings[3] = notUse;
        TextByteTriggerSettings[4] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageCurrentSettingsAndInvert(int Current, byte Invert) {
        byte[] TextByteTriggerSettings = new byte[4];
        TextByteTriggerSettings[0] = 0x0B;
        TextByteTriggerSettings[1] = (byte) Current;
        TextByteTriggerSettings[2] = (byte) (Current >> 8);
        TextByteTriggerSettings[3] = Invert;
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageCurrentSettingsAndInvertHDLC(int Current) {
        byte[] TextByteTriggerSettings = new byte[6];
        TextByteTriggerSettings[0] = ConstantManager.ADDR_CUR_LIMIT;
        TextByteTriggerSettings[1] = ConstantManager.WRITE;
        TextByteTriggerSettings[2] = BluetoothConstantManager.CURR_LIMIT_HDLC;
        TextByteTriggerSettings[3] = (byte) Current;
        TextByteTriggerSettings[4] = (byte) (Current >> 8);
        TextByteTriggerSettings[5] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMessageSetGeneralParcel (byte turningOn){
        byte[] TextByteTriggerSettings = new byte[2];
        TextByteTriggerSettings[0] = 0x0C;
        TextByteTriggerSettings[1] = turningOn;
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageReadStartParameters() {
        byte[] TextByteTriggerSettings = new byte[2];
        TextByteTriggerSettings[0] = 0x0D;
        TextByteTriggerSettings[1] = 0x00;
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageBlockMode(byte onBlockMode) {
        byte[] TextByteTriggerSettings = new byte[2];
        TextByteTriggerSettings[0] = 0x0E;
        TextByteTriggerSettings[1] = onBlockMode; // 0x01 on     0x00 off
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageIlluminationMode(boolean onIlluminationMode) {
        byte[] TextByteSetIlluminationMode;
        if (onIlluminationMode) {
            TextByteSetIlluminationMode = new byte[] {(byte) 0xF9, 0x02, 0x24, 0x00, 0x08,
                    (byte) 0xFF, 0x00, 0x00, (byte) 0xFF, 0x00,
                    0x00, (byte) 0xFF, 0x00, 0x00, (byte) 0xFF,
                    0x00, 0x00, (byte) 0xFF, 0x00, 0x00,
                    (byte) 0xFF, 0x00, 0x00, (byte) 0xFF, 0x00,
                    0x00, (byte) 0xFF, 0x00, 0x00, 0x7B};
        }else {
            TextByteSetIlluminationMode = new byte[] {(byte) 0xF9, 0x02, 0x24, 0x00, 0x08,
                    0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, (byte) 0x9C};
        }
        return TextByteSetIlluminationMode;
    }
    public byte[] CompileMassageBlockModeHDLC(byte onBlockMode) {
        byte[] TextByteTriggerSettings = new byte[5];
        TextByteTriggerSettings[0] = ConstantManager.ADDR_BLOCK;
        TextByteTriggerSettings[1] = ConstantManager.WRITE;
        TextByteTriggerSettings[2] = BluetoothConstantManager.BLOCK_ON_OFF_HDLC;
        TextByteTriggerSettings[3] = onBlockMode; // 0x01 on     0x00 off
        TextByteTriggerSettings[4] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageSwitchGesture(byte openGesture, byte closeGesture) {
        byte[] TextByteTriggerSettings = new byte[3];
        TextByteTriggerSettings[0] = 0x0F;
        TextByteTriggerSettings[1] = openGesture;
        TextByteTriggerSettings[2] = closeGesture;
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageSwitchGestureHDLC(byte numGesture) {
        byte[] TextByteTriggerSettings = new byte[5];
        TextByteTriggerSettings[0] = (byte) 0xFA;
        TextByteTriggerSettings[1] = (byte) 0x02;
        TextByteTriggerSettings[2] = (byte) 0x34;
        TextByteTriggerSettings[3] = numGesture;
        TextByteTriggerSettings[4] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageRoughness(byte roughness) {
        byte[] TextByteTriggerSettings = new byte[5];
        TextByteTriggerSettings[0] = 0x10;
        TextByteTriggerSettings[1] = roughness; // 0x01 on     0x00 off
        return TextByteTriggerSettings;
    }
    public byte[] CompileMassageRoughnessHDLC(byte roughness) {
        byte[] TextByteTriggerSettings = new byte[5];
        TextByteTriggerSettings[0] = ConstantManager.ADDR_BUFF_CHOISES;
        TextByteTriggerSettings[1] = ConstantManager.WRITE;
        TextByteTriggerSettings[2] = BluetoothConstantManager.ADC_BUFF_CHOISES_HDLC;
        TextByteTriggerSettings[3] = roughness;
        TextByteTriggerSettings[4] = calculationCRC_HDLC(TextByteTriggerSettings);
        return TextByteTriggerSettings;
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
