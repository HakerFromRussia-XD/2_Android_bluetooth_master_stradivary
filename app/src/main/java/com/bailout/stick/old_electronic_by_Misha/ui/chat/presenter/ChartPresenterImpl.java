package com.bailout.stick.old_electronic_by_Misha.ui.chat.presenter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;

import com.bailout.stick.old_electronic_by_Misha.ui.chat.interactor.ChatInteractor;
import com.bailout.bluetooth.BluetoothCallback;
import com.bailout.bluetooth.DeviceCallback;
import com.bailout.bluetooth.ParserCallback;
import com.bailout.stick.R;
import com.bailout.stick.old_electronic_by_Misha.ui.chat.view.ChartView;

import static com.bailout.stick.old_electronic_by_Misha.ui.chat.view.ChartActivity.flagReceptionExpectation;
import static com.bailout.stick.old_electronic_by_Misha.ui.chat.view.ChartActivity.flagUseHDLCProtocol;

public class ChartPresenterImpl implements ChatPresenter {
    private ChartView view;
    private ChatInteractor interactor;
    private BluetoothDevice device;
    private int attemptConect = 0;
    private boolean DEBUG = true;
    private byte aByte[] = {0x4D, 0x54, 0x01, 0x00, 0x00, 0x03, 0x00, 0x01, 0x24};
    private byte txtbyteout1[] = {0x4D, 0x54, 0x07, 0x00, 0x01, 0x02, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24}; //компановка для отправки порогов сигналов 0x77 заменяемые данные всего 15 байт
    private byte txtbyteout2[] = {0x4D, 0x54, 0x01, 0x00, 0x00, 0x03, 0x00, 0x77, 0x24};                                     //компановка для запроса сигналов на датчиках 0x77 заменяемые данные всего 9 байт
    private byte txtbyteout3[] = {0x4D, 0x54, 0x07, 0x00, 0x01, 0x04, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24}; //компановка для настройки схватов сигналов 0x77 заменяемые данные всего 15 байт
    private byte txtbyteout4[] = {0x4D, 0x54, 0x0E, 0x00, 0x01, 0x05, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24};//компановка для настройки переходов между двумя схватами 0x77 заменяемые данные всего 23 байт
    private byte txtbyteout5[] = {0x4D, 0x54, 0x05, 0x00, 0x01, 0x04, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24};             //компановка для установки(применения/движения) скорости и угла определённого движка 0x77 заменяемые данные всего 13 байт
    private byte txtbyteout6[] = {0x4D, 0x54, 0x01, 0x00, 0x01, 0x06, 0x00, 0x77, 0x24};                                     //компановка для установки жеста 0x77 заменяемые данные всего 9 байт
    private byte txtbyteout7[] = {0x4D, 0x54, 0x01, 0x00, 0x01, 0x07, 0x00, 0x77, 0x24};                                     //компановка для установки схвата 0x77 заменяемые данные всего 9 байт
    private byte txtbyteout8[] = {0x4D, 0x54, 0x01, 0x00, 0x01, 0x08, 0x00, 0x77, 0x24};                                     //компановка для установки режима срабатывания 0x77 заменяемые данные всего 9 байт
    private byte txtbyteout9[] = {0x4D, 0x54, 0x01, 0x00, 0x01, 0x09, 0x00, 0x77, 0x24};                                     //компановка для иммитации срабатывания 0x77 заменяемые данные всего 9 байт
    private byte txtbyteout10[] ={0x4D, 0x54, 0x08, 0x00, 0x01, 0x0A, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24};//компановка для настройки бесконечного движения 0x77 заменяемые данные всего 16 байт
    private byte txtbyteout11[] ={0x4D, 0x54, 0x03, 0x00, 0x01, 0x0B, 0x00, 0x77, 0x77, 0x77, 0x24};                         //компановка для настройки тока останова и влючения инвертированного управления 0x77 заменяемые данные всего 11 байт
    private byte txtbyteout12[] ={0x4D, 0x54, 0x01, 0x00, 0x01, 0x0C, 0x00, 0x77, 0x24};                                     //компановка для включения/отключения(0х01/0х00) режима непрерывной отсылки параметров с руки всего 9 байт
    private byte txtbyteout13[] ={0x4D, 0x54, 0x01, 0x00, 0x01, 0x0D, 0x00, 0x77, 0x24};                                     //компановка для начального запроса параметров
    private byte txtbyteout14[] ={0x4D, 0x54, 0x01, 0x00, 0x01, 0x0E, 0x00, 0x77, 0x24};                                     //компановка для включения/отключения(0х01/0х00) блокировки
    private byte txtbyteout15[] ={0x4D, 0x54, 0x01, 0x00, 0x01, 0x0F, 0x00, 0x77, 0x77, 0x24};                               //компановка для задачи номера открытого и закрытого схватов 0x77 заменяемые данные всего 10 байт
    private byte txtbyteout16[] ={0x4D, 0x54, 0x01, 0x00, 0x01, 0x10, 0x00, 0x77, 0x24};                                     //компановка для установки грубости датчиков 0x77 заменяемые данные всего 9 байт
    private boolean onPauseActivity = false;

    public ChartPresenterImpl(ChartView view, ChatInteractor interactor) {
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
//        System.out.println("ChatPresenter--------------> HDLC uses " + parserCallback.getFlagUseHDLCProcol());
        if(parserCallback.getFlagUseHDLCProtocol()){
            interactor.setIterator(1);
            interactor.sendMessageByte(txtbyte);
        } else {
            switch (txtbyte[0]) {
                case 1: //компановка посылки записи порогов на любой канал
                    if (DEBUG) {System.out.println("номер канала получателя:" + txtbyte[1]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout1[i + 6] = txtbyte[i];
                    }
                    interactor.sendMessageByte(txtbyteout1);
                    break;
                case 2:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    if (DEBUG) {System.out.println("--> номер канала получателя:" + txtbyte[1]);}
                    txtbyteout2[7] = txtbyte[1];
                    interactor.sendMessageByte(txtbyteout2);
                    break;
                case 3:
                    if (DEBUG) {
                        System.out.println("--> тип компановки:" + txtbyte[0]);
                        System.out.println("--> номер пальца:" + txtbyte[1]);
                        System.out.println("--> значение скорости:" + txtbyte[5]);
                        System.out.println("--> значение угла:" + txtbyte[6]);
                        System.out.println("--> значение CRC:" + txtbyte[7]);
                    }
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout3[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout3.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout3[i]);
                    }
                    interactor.sendMessageByte(txtbyteout3);
                    break;
                case 4:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout4[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout4.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout4[i]);
                    }
                    interactor.sendMessageByte(txtbyteout4);
                    break;
                case 5:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout5[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout5.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout5[i]);
                    }
                    interactor.sendMessageByte(txtbyteout5);
                    break;
                case 6:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    if (DEBUG) {System.out.println("--> номер жеста:" + txtbyte[1]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout6[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout6.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout6[i]);
                    }
                    interactor.sendMessageByte(txtbyteout6);
                    break;
                case 7:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    if (DEBUG) {System.out.println("--> номер схвата:" + txtbyte[1]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout7[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout7.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout7[i]);
                    }
                    interactor.sendMessageByte(txtbyteout7);
                    break;
                case 8:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    if (DEBUG) {System.out.println("--> номер мода:" + txtbyte[1]);}
                    if(txtbyte[1] == 1) {view.showToast("классический режим"); }
                    if(txtbyte[1] == 2) {view.showToast("триггерный режим 1"); }
                    if(txtbyte[1] == 3) {view.showToast("триггерный режим 2"); }
                    if(txtbyte[1] == 4) {view.showToast("удерживающий режим 1"); }
                    if(txtbyte[1] == 5) {view.showToast("удерживающий режим 2"); }
                    if(txtbyte[1] == 6) {view.showToast("инвертированный удерживающий режим 1"); }
                    if(txtbyte[1] == 7) {view.showToast("инвертированный удерживающий режим 2"); }
                    if(txtbyte[1] == 8) {view.showToast("инвертированный классический режим"); }
                    if(txtbyte[1] == 9) {view.showToast("одноканальный режим датчик 1"); }
                    if(txtbyte[1] == 10) {view.showToast("одноканальный режим датчик 2"); }
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout8[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout8.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout8[i]);
                    }
                    interactor.sendMessageByte(txtbyteout8);
                    break;
                case 9:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    if (DEBUG) {System.out.println("--> срабатывание датчика:" + txtbyte[1]+"  lenght:"+txtbyte.length);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout9[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout9.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout9[i]);
                    }
                    interactor.sendMessageByte(txtbyteout9);
                    break;
                case 10:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout10[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout10.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout10[i]);
                    }
                    interactor.sendMessageByte(txtbyteout10);
                    break;
                case 11:
                    if (DEBUG) { System.out.println("--> тип компановки:" + txtbyte[0]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout11[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout11.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout11[i]);
                    }
                    interactor.sendMessageByte(txtbyteout11);
                    break;
                case 12:
                    if (DEBUG) { System.out.println("--> тип компановки:" + txtbyte[0]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout12[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout12.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout12[i]);
                    }
                    interactor.sendMessageByte(txtbyteout12);
                    break;
                case 13:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout13[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout13.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout13[i]);
                    }
                    interactor.sendMessageByte(txtbyteout13);
                    break;
                case 14:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout14[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout14.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout14[i]);
                    }
                    interactor.sendMessageByte(txtbyteout14);
                    break;
                case 15:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
                        txtbyteout15[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout15.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout15[i]);
                    }
                    interactor.sendMessageByte(txtbyteout15);
                    break;
                case 16:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]+" lenght:"+txtbyte.length);}
                    for (int i = 1; i < txtbyte.length; i++)
                    {
//                        txtbyteout16[i + 6] = 0;
                        txtbyteout16[i + 6] = txtbyte[i];
                    }
                    for (int i = 0; i < txtbyteout16.length; i++)
                    {
//                    System.out.println("<-- посылка:" + txtbyteout16[i]);
                    }
                    interactor.sendMessageByte(txtbyteout16);
                    break;
                default:
                    if (DEBUG) {System.out.println("--> тип компановки:" + txtbyte[0]);}
                    if (DEBUG) {System.out.println("--> номер канала получателя:" + txtbyte[1]);}
                    for (int i = 1; i < txtbyte.length; i++) //aByte.length
                    {
                        System.out.println("--> КОМПАНОВКА ПОСЫЛКИ:" + txtbyte[i]);

                    }
                    interactor.sendMessageByte(txtbyte);
                    break;
            }
        }

    }

    public ParserCallback parserCallback = new ParserCallback(){

        @Override
        public Integer givesLenhgt(int lenght) {
            Integer integer =  new Integer(lenght);
//            System.out.println("принятая длинна:" + integer);
            return integer;
        }


        @Override
        public void givesRequest(Boolean request) {
            Boolean bolean = new Boolean(request);
//            System.out.println("приём:" + bolean);
        }

        @Override
        public void givesChannel(int channel) {
            Integer numberChannel = new Integer(channel);
//            System.out.println("номер канала:" + numberChannel);
        }

        @Override
        public void givesLevelCH(int levelCH, int channel) {
            Integer lelvel = new Integer(levelCH);
            Integer numberChannel = new Integer(channel);
            view.setValueCH(levelCH, numberChannel);
            if (DEBUG) {System.out.println("принятый уровень CH:" + lelvel);}
        }


        @Override
        public void givesGeneralParcel(int current, int levelCH1, int levelCH2, byte indicationState, int batteryTension) {
            Integer receiveСurrent = new Integer(current);
            Integer receiveLevelCH1 = new Integer(levelCH1);
            Integer receiveLevelCH2 = new Integer(levelCH2);
            Byte receiveIndicationState = new Byte(indicationState);
            Integer receiveBatteryTension = new Integer(batteryTension);
            view.setGeneralValue(receiveСurrent, receiveLevelCH1, receiveLevelCH2, receiveIndicationState, receiveBatteryTension);
        }

        @Override
        public void givesStartParameters(int current, int levelTrigCH1, int levelTrigCH2, byte indicationInvertMode, byte blockIndication, byte roughnessOfSensors) {
            Integer receiveСurrent = new Integer(current);
            Integer receiveLevelTrigCH1 = new Integer(levelTrigCH1);
            Integer receiveLevelTrigCH2 = new Integer(levelTrigCH2);
            Byte receiveIndicationInvertMode = new Byte(indicationInvertMode);
            Byte receiveBlockIndication = new Byte(blockIndication);
            Byte receiveRoughnessOfSensors = new Byte(roughnessOfSensors);
            view.setStartParameters(receiveСurrent, receiveLevelTrigCH1, receiveLevelTrigCH2, receiveIndicationInvertMode,  receiveBlockIndication, receiveRoughnessOfSensors);
        }

        @Override
        public void givesStartParametersTrigCH1(int levelTrigCH1) {
            Integer receiveLevelTrigCH1 = new Integer(levelTrigCH1);
            view.setStartParametersTrigCH1 (receiveLevelTrigCH1);
        }

        @Override
        public void givesStartParametersTrigCH2(int levelTrigCH2) {
            Integer receiveLevelTrigCH2 = new Integer(levelTrigCH2);
            view.setStartParametersTrigCH2 (receiveLevelTrigCH2);
        }

        @Override
        public void givesStartParametersCurrent(int current) {
            Integer receiveСurrent = new Integer(current);
            view.setStartParametersCurrent(receiveСurrent);
        }

        @Override
        public void givesStartParametersBlock(byte blockIndication) {
            Byte receiveBlockIndication = new Byte(blockIndication);
            view.setStartParametersBlock (receiveBlockIndication);
        }

        @Override
        public void givesStartParametersRoughness(byte roughnessOfSensors) {
            Byte receiveRoughnessOfSensors = new Byte(roughnessOfSensors);
            view.setStartParametersRoughness (receiveRoughnessOfSensors);
        }

        @Override
        public void givesStartParametersBattery(int batteryTension) {
//            Integer receiveBatteryTension = new Integer(batteryTension);
//            view.setStartParametersBattery (receiveBatteryTension);
        }


        @Override
        public void givesRegister(Integer register) {
            Integer registr = new Integer(register);
//            System.out.println("принятая значение регистра:"+registr);
        }

        @Override
        public void givesCorrectAcceptance(Boolean correct_acceptence) {
            Boolean bolean = new Boolean(correct_acceptence);
//            System.out.println("проверка CRC:" + bolean);
        }

        @Override
        public void givesErrorReception(Boolean givesErrorReception) {
            Boolean bolean = new Boolean(givesErrorReception);
            view.setErrorReception(bolean);
        }

        @Override
        public void setStartParametersInNemoStandActivity() {
            view.setStartParametersInChartActivity();
        }

        @Override
        public boolean getFlagUseHDLCProtocol() {
            return flagUseHDLCProtocol;
        }

        @Override
        public boolean getFlagReceptionExpectation() {
            return flagReceptionExpectation;
        }

        @Override
        public void setFlagReceptionExpectation(Boolean flagReceptionExpectation) {
            view.setFlagReceptionExpectation(flagReceptionExpectation);
        }
    };

    private DeviceCallback communicationCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            if (DEBUG) {System.out.println("ChatPresenter--------------> onDeviceConnected");}
            view.setStatus(R.string.bluetooth_connected);
            view.enableInterface(true);
            attemptConect = 0;
        }

        @Override
        public void onDeviceDisconnected(final BluetoothDevice device, String message) {
            if (DEBUG) {System.out.println("ChatPresenter--------------> onDeviceDisconnected");}
            view.setStatus(R.string.bluetooth_connecting);
            view.enableInterface(false);
            if(!onPauseActivity){
                System.out.println("ChatPresenter--------------> pause call "+ false);
                interactor.connectToDevice(device, communicationCallback);
            }
        }

        @Override
        public void onMessage(String message) {
            String string = message;
        }

        @Override
        public void onError(String message) {
        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            if (DEBUG) {System.out.println("ChatPresenter--------------> onConnectError");}
            view.setStatus(R.string.bluetooth_connect_in_3sec);
            if (DEBUG) {System.out.println("Подключение №" + attemptConect);}
            attemptConect += 1;
            if(attemptConect < 5001) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!onPauseActivity){
                            interactor.connectToDevice(device, communicationCallback);
                        }
                    }
                }, 10);
            } else{
                attemptConect = 0;
                view.showToastWithoutConnection();
            }
        }
    };

    @Override
    public void onStart(Activity activity) {
        if (DEBUG) {System.out.println("ChatPresenter--------------> onStart");}
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
        System.out.println("ChatPresenter--------------> onResume"); interactor.onStop();
    }

    @Override
    public void onResume() { System.out.println("ChatPresenter--------------> onResume");}

    @Override
    public void onPause() {
        this.disconnect();
        System.out.println("ChatPresenter--------------> onPause");
    }

    @Override
    public void disconnect(){
        interactor.disconnect();
    }

    @Override
    public void disable(){
        interactor.disable();
    }

    private BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override
        public void onBluetoothTurningOn() {

        }

        @Override
        public void onBluetoothOn() {
            if (DEBUG) {System.out.println("ChatPresenter--------------> onBluetoothOn");}
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
            return view.getFirstRead();
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
