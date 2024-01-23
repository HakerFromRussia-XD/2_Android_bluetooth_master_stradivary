package com.bailout.stickk.old_electronic_by_Misha.ui.chat.presenter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;

import com.bailout.bluetooth.BluetoothCallback;
import com.bailout.bluetooth.DeviceCallback;
import com.bailout.bluetooth.ParserCallback;
import com.bailout.stickk.R;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.interactor.NemoStandInteractor;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.view.NemoStandView;

public class NemoStandPresenterImpl implements NemoStandPresenter {
    private final NemoStandView view;
    private final NemoStandInteractor interactor;
    private BluetoothDevice device;
    private int attemptConnect = 0;
    private final boolean DEBUG = true;
    private final byte[] aByte = {0x4D, 0x54, 0x01, 0x00, 0x00, 0x03, 0x00, 0x01, 0x24};
    private final byte[] txtbyteout1 = {0x4D, 0x54, 0x07, 0x00, 0x01, 0x02, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24}; //компановка для отправки порогов сигналов 0x77 заменяемые данные всего 15 байт
    private final byte[] txtbyteout2 = {0x4D, 0x54, 0x01, 0x00, 0x00, 0x03, 0x00, 0x77, 0x24};                                     //компановка для запроса сигналов на датчиках 0x77 заменяемые данные всего 9 байт
    private final byte[] txtbyteout3 = {0x4D, 0x54, 0x07, 0x00, 0x01, 0x04, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24}; //компановка для настройки схватов сигналов 0x77 заменяемые данные всего 15 байт
    private final byte[] txtbyteout4 = {0x4D, 0x54, 0x0E, 0x00, 0x01, 0x05, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24};//компановка для настройки переходов между двумя схватами 0x77 заменяемые данные всего 23 байт
    private final byte[] txtbyteout5 = {0x4D, 0x54, 0x05, 0x00, 0x01, 0x04, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24};             //компановка для установки(применения/движения) скорости и угла определённого движка 0x77 заменяемые данные всего 13 байт
    private final byte[] txtbyteout6 = {0x4D, 0x54, 0x01, 0x00, 0x01, 0x06, 0x00, 0x77, 0x24};                                     //компановка для установки жеста 0x77 заменяемые данные всего 9 байт
    private final byte[] txtbyteout7 = {0x4D, 0x54, 0x01, 0x00, 0x01, 0x07, 0x00, 0x77, 0x24};                                     //компановка для установки схвата 0x77 заменяемые данные всего 9 байт
    private final byte[] txtbyteout8 = {0x4D, 0x54, 0x01, 0x00, 0x01, 0x08, 0x00, 0x77, 0x24};                                     //компановка для установки режима срабатывания 0x77 заменяемые данные всего 9 байт
    private final byte[] txtbyteout9 = {0x4D, 0x54, 0x01, 0x00, 0x01, 0x09, 0x00, 0x77, 0x24};                                     //компановка для иммитации срабатывания 0x77 заменяемые данные всего 9 байт
    private final byte[] txtbyteout10 ={0x4D, 0x54, 0x08, 0x00, 0x01, 0x0A, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24};//компановка для настройки бесконечного движения 0x77 заменяемые данные всего 16 байт
    private final byte[] txtbyteout11 ={0x4D, 0x54, 0x03, 0x00, 0x01, 0x0B, 0x00, 0x77, 0x77, 0x77, 0x24};                         //компановка для настройки тока останова и влючения инвертированного управления 0x77 заменяемые данные всего 11 байт
    private final byte[] txtbyteout12 ={0x4D, 0x54, 0x01, 0x00, 0x01, 0x0C, 0x00, 0x77, 0x24};                                     //компановка для включения/отключения(0х01/0х00) режима непрерывной отсылки параметров с руки всего 9 байт
    private final byte[] txtbyteout13 ={0x4D, 0x54, 0x01, 0x00, 0x01, 0x0D, 0x00, 0x77, 0x24};                                     //компановка для начального запроса параметров
    private final byte[] txtbyteout14 ={0x4D, 0x54, 0x01, 0x00, 0x01, 0x0E, 0x00, 0x77, 0x24};                                     //компановка для включения/отключения(0х01/0х00) блокировки
    private final byte[] txtbyteout15 ={0x4D, 0x54, 0x01, 0x00, 0x01, 0x0F, 0x00, 0x77, 0x77, 0x24};                               //компановка для задачи номера открытого и закрытого схватов 0x77 заменяемые данные всего 10 байт
    private final byte[] txtbyteout16 ={0x4D, 0x54, 0x01, 0x00, 0x01, 0x10, 0x00, 0x77, 0x24};                                     //компановка для установки грубости датчиков 0x77 заменяемые данные всего 9 байт
    private boolean onPauseActivity = false;

    public NemoStandPresenterImpl(NemoStandView view, NemoStandInteractor interactor) {
        this.view = view;
        this.interactor = interactor;
    }

    @Override
    public void onCreate(Intent intent) {
        if (intent.getExtras() != null) {
            device = intent.getExtras().getParcelable("device");
            if (DEBUG) {System.out.println("ВАЖНО!!!!!!!!!!!! ДЕВАЙС:   " + device);}
            view.enableInterface(false);
        } else {
            if (DEBUG) {System.out.println("ПИЗДА!!!!!!!!!!!! ПОСЫЛКИ НЕТ!");}
        }
    }

        @Override
    public void onHelloWorld(byte[] txtbyte) {
        interactor.sendMessageByte(txtbyte);
        System.err.println("txtbyte size: "+txtbyte.length);
        for (int i = 0; i < txtbyte.length; i++) {
            System.err.println("txtbyte["+i+"]: "+txtbyte[i]);
        }
    }

    public ParserCallback parserCallback = new ParserCallback(){

        @Override
        public Integer givesLenhgt(int lenght) {
            //            System.out.println("принятая длинна:" + integer);
            return lenght;
        }


        @Override
        public void givesRequest(Boolean request) {
            //            System.out.println("приём:" + bolean);
        }

        @Override
        public void givesChannel(int channel) {
//            Integer numberChannel = new Integer(channel);
//            System.out.println("номер канала:" + numberChannel);
        }

        @Override
        public void givesLevelCH(int levelCH, int channel) {
//            Integer lelvel = new Integer(levelCH);
//            Integer numberChannel = new Integer(channel);
//            view.setValueCH(levelCH, numberChannel);
//            if (DEBUG) {System.out.println("принятый уровень CH:" + lelvel);}
        }


        @Override
        public void givesGeneralParcel(int current, int levelCH1, int levelCH2, byte indicationState, int batteryTension) {
//            Integer receiveСurrent = new Integer(current);
//            Integer receiveLevelCH1 = new Integer(levelCH1);
//            Integer receiveLevelCH2 = new Integer(levelCH2);
//            Byte receiveIndicationState = new Byte(indicationState);
//            Integer receiveBatteryTension = new Integer(batteryTension);
//            view.setGeneralValue(receiveСurrent, receiveLevelCH1, receiveLevelCH2, receiveIndicationState, receiveBatteryTension);
        }

        @Override
        public void givesStartParameters(int current, int levelTrigCH1, int levelTrigCH2, byte indicationInvertMode, byte blockIndication, byte roughnessOfSensors) {
//            Integer receiveСurrent = new Integer(current);
//            Integer receiveLevelTrigCH1 = new Integer(levelTrigCH1);
//            Integer receiveLevelTrigCH2 = new Integer(levelTrigCH2);
//            Byte receiveIndicationInvertMode = new Byte(indicationInvertMode);
//            Byte receiveBlockIndication = new Byte(blockIndication);
//            Byte receiveRoughnessOfSensors = new Byte(roughnessOfSensors);
//            view.setStartParameters(receiveСurrent, receiveLevelTrigCH1, receiveLevelTrigCH2, receiveIndicationInvertMode,  receiveBlockIndication, receiveRoughnessOfSensors);
        }

        @Override
        public void givesStartParametersTrigCH1(int levelTrigCH1) {
//            Integer receiveLevelTrigCH1 = Integer.valueOf(levelTrigCH1);
//            view.setStartParametersTrigCH1 (receiveLevelTrigCH1);
        }

        @Override
        public void givesStartParametersTrigCH2(int levelTrigCH2) {
//            Integer receiveLevelTrigCH2 = Integer.valueOf(levelTrigCH2);
//            view.setStartParametersTrigCH2 (receiveLevelTrigCH2);
        }

        @Override
        public void givesStartParametersCurrent(int current) {
//            Integer receiveСurrent = current;
//            view.setStartParametersCurrent(receiveСurrent);
        }

        @Override
        public void givesStartParametersBlock(byte blockIndication) {
//            Byte receiveBlockIndication = new Byte(blockIndication);
//            view.setStartParametersBlock (receiveBlockIndication);
        }

        @Override
        public void givesStartParametersRoughness(byte roughnessOfSensors) {
//            Byte receiveRoughnessOfSensors = new Byte(roughnessOfSensors);
//            view.setStartParametersRoughness (receiveRoughnessOfSensors);
        }

        @Override
        public void givesStartParametersBattery(int batteryTension) {
//            Integer receiveBatteryTension = new Integer(batteryTension);
//            view.setStartParametersBattery (receiveBatteryTension);
        }


        @Override
        public void givesRegister(Integer register) {
//            Integer registr = new Integer(register);
//            System.out.println("принятая значение регистра:"+registr);
        }

        @Override
        public void givesCorrectAcceptance(Boolean correct_acceptence) {
//            Boolean boleann = new Boolean(correct_acceptence);
//            System.out.println("проверка CRC:" + boleann);
        }

        @Override
        public void givesErrorReception(Boolean givesErrorReception) {
            view.setErrorReception(givesErrorReception);
        }

        @Override
        public void setStartParametersInNemoStandActivity() {
            view.setStartParametersInNemoStandActivity();
        }

        @Override
        public boolean getFlagUseHDLCProtocol() {
            return false;
        }

        @Override
        public boolean getFlagReceptionExpectation() {
            return false;
        }

        @Override
        public void setFlagReceptionExpectation(Boolean flagReceptionExpectation) {
            view.setFlagReceptionExpectation(flagReceptionExpectation);
        }
    };

    private final DeviceCallback communicationCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            view.setStatus(R.string.bluetooth_connected);
            view.enableInterface(true);
            attemptConnect = 0;
        }

        @Override
        public void onDeviceDisconnected(final BluetoothDevice device, String message) {
            view.setStatus(R.string.bluetooth_connecting);
            view.enableInterface(false);
            if(!onPauseActivity){
                interactor.connectToDevice(device, communicationCallback);
            }
        }

        @Override
        public void onMessage(String message) {
        }

        @Override
        public void onError(String message) {
        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            view.setStatus(R.string.bluetooth_connect_in_3sec);
            if (DEBUG) {System.out.println("Подключение №" + attemptConnect);}
            attemptConnect += 1;
            if(attemptConnect < 5001) {
                new Handler().postDelayed(() -> {
                    if(!onPauseActivity){
                        interactor.connectToDevice(device, communicationCallback);
                    }
                }, 10);
            } else{
                attemptConnect = 0;
                view.showToastWithoutConnection();
            }
        }
    };

    @Override
    public void onStart(Activity activity) {
        interactor.onStart(bluetoothCallback, activity);
        if(interactor.isBluetoothEnabled()){
            interactor.connectToDevice(device, communicationCallback);
            interactor.parsingExperimental(parserCallback);
            view.setStatus(R.string.bluetooth_connecting);
        }
        else{
            interactor.enableBluetooth();
        }
    }

    @Override
    public void onStop() {
        System.out.println("NemoStandPresenter--------------> onResume"); interactor.onStop();
    }

    @Override
    public void onResume() { System.out.println("NemoStandPresenter--------------> onResume");}

    @Override
    public void onPause() {
        this.disconnect();
        System.out.println("NemoStandPresenter--------------> onPause");
    }

    @Override
    public void disconnect(){
        interactor.disconnect();
    }

    @Override
    public void disable(){
        interactor.disable();
    }

    private final BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override
        public void onBluetoothTurningOn() {

        }

        @Override
        public void onBluetoothOn() {
            if (DEBUG) {System.out.println("NemoStandPresenter--------------> onBluetoothOn");}
            interactor.connectToDevice(device, communicationCallback);
            interactor.parsingExperimental(parserCallback);
            view.setStatus(R.string.bluetooth_connecting);
            view.enableInterface(false);
        }

        @Override
        public void onBluetoothTurningOff() {

        }

        @Override
        public void onBluetoothOff() {
        }

        @Override
        public void onUserDeniedActivation() {

        }

        @Override
        public boolean getFirstRead() {

            return false;//view.getFirstRead();
        }
    };

    public void setOnPauseActivity(boolean onPauseActivity) {
        this.onPauseActivity = onPauseActivity;
    }

    public byte calculationCRC(byte[] bytes) {
        byte CRC = 0x00;
        for (int i = 1; i < bytes.length-1; i++){
            CRC += bytes[i];
            CRC = (byte) (CRC << 1);
        }
        return CRC;
    }

    public byte calculationCRC_HDLC (byte[] bytes) {
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
