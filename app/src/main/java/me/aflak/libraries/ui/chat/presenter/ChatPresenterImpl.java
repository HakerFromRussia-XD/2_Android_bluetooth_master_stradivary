package me.aflak.libraries.ui.chat.presenter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;

import me.aflak.bluetooth.BluetoothCallback;
import me.aflak.bluetooth.DeviceCallback;
import me.aflak.bluetooth.ParserCallback;
import me.aflak.libraries.R;
import me.aflak.libraries.ui.chat.interactor.ChatInteractor;
import me.aflak.libraries.ui.chat.view.ChatView;

public class ChatPresenterImpl implements ChatPresenter {
    private ChatView view;
    private ChatInteractor interactor;
    private BluetoothDevice device;
    private byte aByte[] = {0x4D, 0x54, 0x01, 0x00, 0x00, 0x03, 0x00, 0x01, 0x24} ;
    private byte txtbyteout1[] = {0x4D, 0x54, 0x07, 0x00, 0x01, 0x02, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24}; //компановка для отправки порогов сигналов 0x77 заменяемые данные всего 15 байт
    private byte txtbyteout2[] = {0x4D, 0x54, 0x01, 0x00, 0x00, 0x03, 0x00, 0x77, 0x24};                                     //компановка для запроса сигналов на датчиках 0x77 заменяемые данные всего 9 байт
    private byte txtbyteout3[] = {0x4D, 0x54, 0x06, 0x00, 0x01, 0x04, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24}; //компановка для настройки схватов сигналов 0x77 заменяемые данные всего 15 байт
    private byte txtbyteout4[] = {0x4D, 0x54, 0x0F, 0x00, 0x01, 0x05, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24};//компановка для настройки переходов между двумя схватами 0x77 заменяемые данные всего 23 байт
    private byte txtbyteout5[] = {0x4D, 0x54, 0x05, 0x00, 0x01, 0x04, 0x00, 0x77, 0x77, 0x77, 0x77, 0x77, 0x24};             //компановка для установки скорости и угла определённого движка 0x77 заменяемые данные всего 13 байт
    private byte txtbyteout6[] = {0x4D, 0x54, 0x01, 0x00, 0x01, 0x06, 0x00, 0x77, 0x24};                                     //компановка для запроса сигналов на датчиках 0x77 заменяемые данные всего 9 байт
    public ChatPresenterImpl(ChatView view, ChatInteractor interactor) {
        this.view = view;
        this.interactor = interactor;
    }

    @Override
    public void onCreate(Intent intent) {
        if (intent.getExtras() != null) {
            device = intent.getExtras().getParcelable("device");
            System.out.println("ВАЖНО!!!!!!!!!!!! ДЕВАЙС:" + device);
            view.enableHWButton(false);
        } else {
            System.out.println("ПИЗДА!!!!!!!!!!!! ПОСЫЛКИ НЕТ!");
        }
    }




        @Override
    public void onHelloWorld(byte[] txtbyte) {
//        for (int i = 0; i < txtbyte.length; i++) //aByte.length
//        {
//            txtbyte[i] = (byte) (txtbyte[i]-((byte) 0x30));
//            if (txtbyte[i] > 0x0F){
//                txtbyte[i] = (byte) (txtbyte[i] - 0x07);
//                if(txtbyte[i] > 0x0F){
//                    txtbyte[i] = (byte) (txtbyte[i] - 0x20);
//                }
//            }
//            if(i%2==0){
//                txtbyte[i] = (byte) (txtbyte[i] << 4);
//            } else {
//                txtbyte[i-1] = (byte) (txtbyte[i-1]+txtbyte[i]);
//            }
//        }
//        System.arraycopy(txtbyte, 0, txtbyteout, 0, 2);
//        for (int i=0; i < txtbyte.length; i++){
//            txtbyteout.add(new byte[(txtbyte[i])]);
//        }
//        for (int i = 1; i < txtbyte.length-1; i+=2) //aByte.length
//        {
//            txtbyteout.remove(i);
//        }
//        for (int i = 0; i < 1; i++) //aByte.length
//        {
//            System.arraycopy(txtbyte, i*2, txtbyteout, i, 1);
//        }
        switch (txtbyte[0]) {
            case 1: //компановка посылки записи порогов на любой канал
                System.out.println("номер канала получателя:" + txtbyte[1]);
                for (int i = 1; i < txtbyte.length; i++)
                {
                    txtbyteout1[i + 6] = txtbyte[i];
                }
                interactor.sendMessageByte(txtbyteout1);
                break;
            case 2:
                System.out.println("--> тип компановки:" + txtbyte[0]);
                System.out.println("--> номер канала получателя:" + txtbyte[1]);
                txtbyteout2[7] = txtbyte[1];
                interactor.sendMessageByte(txtbyteout2);
                break;
            case 3:
                System.out.println("--> тип компановки:" + txtbyte[0]);
                System.out.println("--> номер пальца:" + txtbyte[1]);
                System.out.println("--> значение скорости:" + txtbyte[5]);
                System.out.println("--> значение угла:" + txtbyte[6]);
                System.out.println("--> значение CRC:" + txtbyte[7]);
                for (int i = 1; i < txtbyte.length; i++)
                {
                    txtbyteout3[i + 6] = txtbyte[i];
                }
                interactor.sendMessageByte(txtbyteout3);
                break;
            case 4:
                System.out.println("--> тип компановки:" + txtbyte[0]);
                for (int i = 1; i < txtbyte.length; i++)
                {
                    txtbyteout4[i + 6] = txtbyte[i];
                }
                interactor.sendMessageByte(txtbyteout4);
                break;
            case 5:
                System.out.println("--> тип компановки:" + txtbyte[0]);
                for (int i = 1; i < txtbyte.length; i++)
                {
                    txtbyteout5[i + 6] = txtbyte[i];
                }
                interactor.sendMessageByte(txtbyteout5);
                break;
            case 6:
                System.out.println("--> тип компановки:" + txtbyte[0]);
                System.out.println("--> номер жеста:" + txtbyte[1]);
                for (int i = 1; i < txtbyte.length; i++)
                {
                    txtbyteout6[i + 6] = txtbyte[i];
                }
                interactor.sendMessageByte(txtbyteout6);
                break;
            default:
                System.out.println("--> тип компановки:" + txtbyte[0]);
                System.out.println("--> номер канала получателя:" + txtbyte[1]);
                view.appendMessage("--> отправка на канал" + txtbyte[1]);
                for (int i = 1; i < txtbyte.length; i++) //aByte.length
                {
                    System.out.println("--> КОМПАНОВКА ПОСЫЛКИ:" + txtbyte[i]);
                }
                interactor.sendMessageByte(txtbyte);
                break;
        }
        System.out.println("--> первый байт:" + txtbyteout1[0]);
    }

//    @Override
//    public void sendData(byte[] data) {
//
//    }
//
//    @Override
//    public void sendData() {
//        interactor.sendMessagestr(String.valueOf(integer));
//        System.out.println("принятая длинна:");
//    }


    public ParserCallback parserCallback = new ParserCallback(){

        //public TextView CH1;

        @Override
        public Integer givsLenhgt(int lenght) {
            Integer integer =  new Integer(lenght);
            System.out.println("принятая длинна:"+integer);
            return integer;
        }


        @Override
        public void givsRequest(Boolean request) {
            Boolean bolean = new Boolean(request);
            System.out.println("приём:"+bolean);
        }

        @Override
        public void givsChannel(int channel) {
            Integer numberChannel = new Integer(channel);
            System.out.println("номер канала:"+numberChannel);
        }

        @Override
        public void givsLevelCH(int levelCH, int channel) {
            Integer lelvel = new Integer(levelCH);
            Integer numberChannel = new Integer(channel);
            view.setValueCH(levelCH, numberChannel);
            System.out.println("принятый уровень CH:"+lelvel);
        }

        @Override
        public void givsRegister(Integer register) {
            Integer registr = new Integer(register);
            System.out.println("принятая значение регистра:"+registr);
        }

        @Override
        public void givsCorrectAcceptance(Boolean correct_acceptence) {
            Boolean boleann = new Boolean(correct_acceptence);
            System.out.println("проверка CRC:"+boleann);
        }
    };

    private DeviceCallback communicationCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            view.setStatus(R.string.bluetooth_connected);
            view.enableHWButton(true);
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, String message) {
            System.out.println("ChatPresenter--------------> onDeviceDisconnected");
            view.setStatus(R.string.bluetooth_connecting);
            view.enableHWButton(false);
//            interactor.connectToDevice(device, communicationCallback);
            interactor.parsingExperimental(parserCallback);
        }

        @Override
        public void onMessage(String message) {
            String string = new String(message);
            view.appendMessage("<-- " + string);
        }

        @Override
        public void onError(String message) {
            view.setStatus(message);
        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            System.out.println("ChatPresenter--------------> onConnectError");
            view.setStatus(R.string.bluetooth_connect_in_3sec);
            view.showToast("New attempt in 3 sec...");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    interactor.connectToDevice(device, communicationCallback);
                    interactor.parsingExperimental(parserCallback);
                }
            }, 3000);
        }
    };

    @Override
    public void onStart(Activity activity) {
        System.out.println("ChatPresenter--------------> onStart");
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
        interactor.onStop();
    }

    @Override
    public void setDeviceCallback2(Activity activity){
        interactor.onStart (bluetoothCallback, activity);
        if (interactor.isBluetoothEnabled()) {
            interactor.connectToDevice2(device, communicationCallback);
        } else {
            interactor.enableBluetooth();
        }
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
            System.out.println("ChatPresenter--------------> onBluetoothOn");
            interactor.connectToDevice(device, communicationCallback);
            interactor.parsingExperimental(parserCallback);
            view.setStatus(R.string.bluetooth_connecting);
            view.enableHWButton(false);
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
    };
}
