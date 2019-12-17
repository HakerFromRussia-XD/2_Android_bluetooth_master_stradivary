package me.Romans.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.invoke.ConstantCallSite;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class Bluetooth {
    private static final int REQUEST_ENABLE_BT = 1111;
    public TextView CH1;

    private Activity activity;
    private Context context;
    private UUID uuid;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private BluetoothDevice device, devicePair;
    private BufferedReader input;
    private InputStreamReader in;
    private OutputStream out;

    private DeviceCallback deviceCallback;
    private ParserCallback parserCallback;
    private DiscoveryCallback discoveryCallback;
    private BluetoothCallback bluetoothCallback;
    private boolean connected;
    private boolean logic_disconnect;
    private boolean DEBUG = false;

    private boolean runOnUi;


    public Bluetooth(Context context){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> Bluetooth");}
        initialize(context, UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
    }

    public Bluetooth(Context context, UUID uuid){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> Bluetooth2");}
        initialize(context, uuid);
    }

    private void initialize(Context context, UUID uuid){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> initialize");}
        this.context = context;
        this.uuid = uuid;
        this.deviceCallback = null;
        this.discoveryCallback = null;
        this.bluetoothCallback = null;
        this.connected = false;
        this.runOnUi = false;
    }

    public void onStart(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> onStart connected:" + connected);}
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if(bluetoothManager!=null) {
                bluetoothAdapter = bluetoothManager.getAdapter();
            }
        }
        else{
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        context.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public void onStop(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> onStop connected:" + connected);}
        context.unregisterReceiver(bluetoothReceiver);
    }

    public void showEnableDialog(Activity activity){
        if(bluetoothAdapter!=null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public void enable(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> enable connected:" + connected);}
        if(bluetoothAdapter!=null) {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        }
    }

    public void disable(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> disable connected:" + connected);}
        if(bluetoothAdapter!=null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
            }
        }
    }

    public BluetoothSocket getSocket(){
        return socket;
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public boolean isEnabled(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> isEnabled connected:" + connected);}
        if(bluetoothAdapter!=null) {
            return bluetoothAdapter.isEnabled();
        }
        return false;
    }

    public void setCallbackOnUI(Activity activity){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> setCallbackOnUI connected:" + connected);}
        this.activity = activity;
        this.runOnUi = true;
    }

    public void onActivityResult(int requestCode, final int resultCode){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> onActivityResult connected:" + connected);}
        if(bluetoothCallback!=null){
            if(requestCode==REQUEST_ENABLE_BT){
                ThreadHelper.run(runOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        if(resultCode==Activity.RESULT_CANCELED){
                            bluetoothCallback.onUserDeniedActivation();
                        }
                    }
                });
            }
        }
    }

    public void connectToAddress(String address, boolean insecureConnection) {
        if (DEBUG) {System.out.println("BLUETOOTH--------------> connectToAddress connected:" + connected);}
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        connectToDevice(device, insecureConnection);
    }

    public void connectToAddress(String address) {
        connectToAddress(address, false);
    }

    public void connectToName(String name, boolean insecureConnection) {
        if (DEBUG) {System.out.println("BLUETOOTH--------------> connectToName:" + name + "connected:" + connected);}
        for (BluetoothDevice blueDevice : bluetoothAdapter.getBondedDevices()) {
            if (blueDevice.getName().equals(name)) {
                connectToDevice(blueDevice, insecureConnection);
                return;
            }
        }
    }

    public void connectToName(String name) {
        connectToName(name, false);
    }

    public void connectToDevice(BluetoothDevice device, boolean insecureConnection){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> connectToDevice:" + device + "connected:" + connected);}
        new ConnectThread(device, insecureConnection).start();
    }

    public void connectToDevice(BluetoothDevice device){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> connectToDevice2:" + device + "connected:" + connected);}
        connectToDevice(device, false);
    }

    public void disconnect() {
        if (DEBUG) {System.out.println("BLUETOOTH--------------> disconnect connected:" + connected);}
        try {
            socket.close();
            if (DEBUG) {System.out.println("BLUETOOTH--------------> disconnect socket.close eeeee!!! connected:" + connected);}
        } catch (final IOException e) {
            if(deviceCallback !=null) {
                ThreadHelper.run(runOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        if (DEBUG) {System.out.println("BLUETOOTH--------------> disconnect ERROR deviceCallback = null connected:" + connected);}
                        deviceCallback.onError(e.getMessage());
                        try {
                            Thread.sleep(3000);
                        }catch (Exception e){}
                    }
                });
            }
        }
    }

    public boolean isConnected(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> isConnected connected:" + connected);}
        return connected;
    }

    public void send(byte[] msg, String charset){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> send" + msg[0] + "connected:" + connected);}
        try {
            if(!TextUtils.isEmpty(charset)) {
//                out.write(msg.getByte(charset));//Eg: "US-ASCII" as default
//                out.write(msg.getByte());//Sending as UTF-8
            }else {
                out.write(msg);//Eg: "US-ASCII" as default
            }
        } catch (final IOException e) {
            connected=false;
            if(deviceCallback !=null){
                ThreadHelper.run(runOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        if (DEBUG) {System.out.println("BLUETOOTH--------------> send ERROR deviceCallback = null connected:" + connected);}
                        deviceCallback.onDeviceDisconnected(device, e.getMessage());
                    }
                });
            }
        }
    }

    public void sendstr(String msgstr, String charset){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> sendstr connected:" + connected);}
        try {
            if(!TextUtils.isEmpty(charset)) {
                out.write(msgstr.getBytes(charset));//Eg: "US-ASCII" as default
//                out.write(msg.getByte());//Sending as UTF-8
            }else {
                out.write(msgstr.getBytes());//Sending as UTF-8
            }
        } catch (final IOException e) {
            connected=false;
            if(deviceCallback !=null){
                ThreadHelper.run(runOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        deviceCallback.onDeviceDisconnected(device, e.getMessage());
                    }
                });
            }
        }
    }


    public void send(byte[] msg){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> send2 connected:" + connected);}
        send(msg, null);
    }

    public void sendstr(String msgstr){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> sendstr connected:" + connected);}
        sendstr(msgstr, "US-ASCII");
    }

    public List<BluetoothDevice> getPairedDevices(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> getPairedDevices connected:" + connected);}
        return new ArrayList<>(bluetoothAdapter.getBondedDevices());
    }

    public void startScanning(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> startScanning connected:" + connected);}
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        context.registerReceiver(scanReceiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    public void stopScanning(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> stopScanning connected:" + connected);}
        context.unregisterReceiver(scanReceiver);
        bluetoothAdapter.cancelDiscovery();
    }

    public void pair(BluetoothDevice device){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> pair connected:" + connected);}
        context.registerReceiver(pairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        devicePair=device;
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (final Exception e) {
            if(discoveryCallback!=null) {
                ThreadHelper.run(runOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        if (DEBUG) {System.out.println("BLUETOOTH--------------> pair ERROR discoveryCallback = null connected:" + connected);}
                        discoveryCallback.onError(e.getMessage());
                    }
                });
            }
        }
    }

    public void unpair(BluetoothDevice device) {
        if (DEBUG) {System.out.println("BLUETOOTH--------------> unpair connected:" + connected);}
        context.registerReceiver(pairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        devicePair=device;
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (final Exception e) {
            if(discoveryCallback!=null) {
                ThreadHelper.run(runOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        discoveryCallback.onError(e.getMessage());
                    }
                });
            }
        }
    }

    private class ReceiveThread extends Thread implements Runnable{
        private int test = 2928;
        private int msg = 65;
        private int summator = 0;     //для проверки суммы первых символов МТ и последнего $
        private int msgLenght = 0;    //для свапа младших и старших байт длинны данных
        private int msgRegister = 0;  //для свапа младших и старших байт номера регистра
        private int msgChannel = 0;   //для номера канала
        private int msgLevelCH = 1250;
        private int msgCurrent = 0;
        private int msgLevelCH1 = 0;
        private int msgLevelCH2 = 0;
        private int msgBatteryTension = 0;
        private byte msgIndicationState = 0;
        private byte msgBlockIndication = 0;
        private byte msgRoughnessOfSensors = 0;
        private int lowByte = 0;     //для записи младшего байта при перемене младших и старших байт
        private int i=1;
//        private boolean firstRead = true;
        private boolean errorReception = false;        //true-ошибка на принимающей стороне false-ошибок нет
        private boolean request = true;                //true-ответ false-запрос
        public boolean no_error = true;                //true-нет ошибок false-есть ошибки
        private boolean msgCorrectAcceptance = true;   //true-безошиобочная CRC false-шибочная CRC
        private int branchOfParsing = 1;               //1-певый набор команд, 2-общая полыка с данными, 3-состояние параметров руки на момент включения
        private StringBuffer msgstr = new StringBuffer();
        private byte[] txtbyteout = {0x01, 0x02} ;
        private volatile boolean mFinish = false;

//        public void finish()
//        {
//            System.out.println("BLUETOOTH--------------> ReceiveThread завершение потока connected:" + connected);
//            mFinish = true;
//        }

        public void run(){
            if (DEBUG) {System.out.println("BLUETOOTH--------------> ReceiveThread connected:" + connected);}
            System.out.println("BLUETOOTH--------------> firstRead: " + bluetoothCallback.getFirstRead());
            try {
                while((msg = input.read()) != -1) //((System.in).read(msg)) //((System.in).read(msg))   //((input.read())) != -1
                {
                    if(!mFinish) {
                        if((i == 1)||(i == 2)||((i == (8+msgLenght)))){ //адекватный приём!!!!
                            summator += msg;
                            if (summator == 197){
                                msgCorrectAcceptance = true;
                                if (DEBUG) {System.out.println("<-- Принята посылка :)");}
                            } else {
                                if(((i == (8+msgLenght))&&(msg != 36))||((i == 1)&&(msg != 77))){
                                    if (((i == 9) && (msg != 35)) || ((i == 1) && (msg != 36))) {
                                        resetAllVariables();
                                        msgCorrectAcceptance = false;
                                    }
                                }
                            }
                        }

                        if ((i == 1) && (msg == 77)){ //выбор ветки для парсинга и отправки
                            branchOfParsing = BluetoothConstantManager.OLD_NOT_USE_PROTOCOL;
                        } else {
                            if ((i == 1) && (msg == 36)){
                                branchOfParsing = BluetoothConstantManager.OLD_PROTOCOL; //парсит постоянно прилетающие данные
                                //за первый заход устанавливает начальные параметры,
                                //а за последующие выдаёт данные на графики, свичи и сикбары
                            } else {
                                if (parserCallback.getFlagUseHDLCProcol()){
                                    branchOfParsing = BluetoothConstantManager.HDLC_PROTOCOL;
                                    //парсит hdlc посылки
                                } else {
                                    if (i == 1){
                                        branchOfParsing = BluetoothConstantManager.RESET_ALL_VARIABLE;
                                    }
                                }
                            }
                        }
                        if (branchOfParsing == BluetoothConstantManager.RESET_ALL_VARIABLE){
                            resetAllVariables();
                        }

                        if (branchOfParsing == BluetoothConstantManager.OLD_NOT_USE_PROTOCOL){
                            if(i == 3){
                                lowByte = msg;
                            }
                            if(i == 4){
                                msgLenght = (msg << 8) + lowByte; //msgLenght содержит количество байт данных в посылке
                                if (DEBUG) {System.out.println("<-- длина строки:"+msgLenght);}
                            }
                            if(i == 5){
                                if(msg == 1){
                                    request = true;
                                } else {
                                    request = false;
                                }
                            }
                            if(i == 6){
                                lowByte = msg;
                            }
                            if(i == 7){
                                msgRegister = (msg << 8) + lowByte;  //msgRegister содержит номер регистра
                                if (DEBUG) {System.out.println("<-- номер регистра:"+msgRegister);}
                            }
                            if(i == 8){
                                lowByte = msg;
                            }
                            if(i == 9){
                                msgChannel = (msg << 8) + lowByte;
                                if (DEBUG) {System.out.println("<-- номер канала:"+msgChannel);}
                            }
                            if (msgChannel == 65535){
                                errorReception = true;
                                if (DEBUG) {System.out.println("<-- детект ошибки: " + errorReception);}
                            } else {
                                errorReception = false;
                            }
                            switch (msgChannel){
                                case 1:
                                    if(i == 10){
                                        lowByte = msg;
                                    }
                                    if(i == 11){
                                        msgLevelCH = (msg << 8) + lowByte; //msgLevelCH уровень канала 1
                                        if (DEBUG) {System.out.println("<-- уровень CH1:"+msgLevelCH);}
                                    }
                                    break;
                                case 2:
                                    if(i == 10){
                                        lowByte = msg;
                                    }
                                    if(i == 11){
                                        msgLevelCH = (msg << 8) + lowByte; //msgLevelCH уровень канала 2
                                        if (DEBUG) {System.out.println("<-- уровень CH2:"+msgLevelCH);}
                                    }
                                    break;
                                default:
                                    break;
                            }
                            if(no_error) {
                                i++;
                                msgstr.append((char)msg);
                            }
                            if(i >= (msgLenght+9)){
                                resetAllVariables();
                            }
                            if(((deviceCallback != null) && (msg == 36))){
                                final String msgCopy = String.valueOf(msgstr);
                                final Integer msgLenghtf = msgLenght;
                                final Boolean requestf = request;
                                final Integer msgRegtsterf = msgRegister;
                                final Integer msgChannelf = msgChannel;
                                final Integer msgLevelCHf = msgLevelCH;
                                final Boolean msgCorrectAcceptancef = msgCorrectAcceptance;
                                final Boolean errorReceptionf = errorReception;
                                ThreadHelper.run(runOnUi, activity, new Runnable() {
                                    @Override
                                    public void run() {
                                        if(no_error && msgCorrectAcceptance) {
                                            parserCallback.givsLenhgt(msgLenghtf);
                                            parserCallback.givsChannel(msgChannelf);
                                            parserCallback.givsRequest(requestf);
                                            parserCallback.givsRegister(msgRegtsterf);
                                            parserCallback.givsLevelCH(msgLevelCHf, msgChannelf);
                                            parserCallback.givsErrorReception(errorReceptionf);
                                            deviceCallback.onMessage(msgCopy);
                                            if (DEBUG) {System.out.println("<-- сделал цикл по ветке 1:"+ msgCopy);}
                                        }
                                        parserCallback.givsCorrectAcceptance(msgCorrectAcceptancef);
                                        resetAllVariables();
                                    }
                                });
                            }
                        }

                        if (bluetoothCallback.getFirstRead()) {
                            if (branchOfParsing == BluetoothConstantManager.OLD_PROTOCOL){
                                if(i == 2){
                                    lowByte = msg;
                                }
                                if(i == 3){
                                    msgCurrent = (lowByte << 8) + msg; //msgLenght содержит количество байт данных в посылке
                                }
                                if(i == 4){
                                    lowByte = msg;
                                }
                                if(i == 5){
                                    msgLevelCH1 = (lowByte << 8) + msg;  //msgLevelCH уровень канала 1
                                    if (DEBUG) {System.out.println("<-- уровень порога CH1:" + msgLevelCH1 + " i = " + i + " no_error=" + no_error);}
                                }
                                if(i == 6){
                                    lowByte = msg;
                                }
                                if(i == 7){
                                    msgLevelCH2 = (lowByte << 8) + msg; //msgLevelCH уровень канала 2
                                    if (DEBUG) {System.out.println("<-- уровень порога CH2:" + msgLevelCH2 + " i = " + i + " no_error=" + no_error);}
                                }
                                if(i == 8){
                                    msgIndicationState = (byte) msg;
                                }
                                if(i == 9){
                                    msgBlockIndication = (byte) msg;
                                }
                                if(i == 10){
                                    msgRoughnessOfSensors = (byte) msg;
                                }
                                if(no_error) {
                                    i++;
                                }
                                if(msg == 35){
                                    resetAllVariables();
                                }
                                if(((deviceCallback != null) && (msg == 35))){
                                    final String msgCopy = String.valueOf(msgstr);
                                    final Integer msgCurrentf = msgCurrent;
                                    final Integer msgLevelTrigCH1f = msgLevelCH1;
                                    final Integer msgLevelTrigCH2f = msgLevelCH2;
                                    final Byte msgIndicationInvertModef = msgIndicationState;
                                    final Byte msgBlockIndicationf = msgBlockIndication;
                                    final Byte msgRoughnessOfSensorsf = msgRoughnessOfSensors;
                                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                                        @Override
                                        public void run() {
                                            if(no_error && msgCorrectAcceptance) {
                                                parserCallback.givsStartParameters(msgCurrentf, msgLevelTrigCH1f, msgLevelTrigCH2f, msgIndicationInvertModef, msgBlockIndicationf, msgRoughnessOfSensorsf);
                                                parserCallback.setStartParametersInChartActivity();
                                                deviceCallback.onMessage(msgCopy);
                                                if (DEBUG) {System.out.println("<-- сделал цикл по ветке 2 первая итерация:"+ msgCopy +" no_error="+no_error);}
                                            }
                                            resetAllVariables();
                                        }
                                    });
                                }
                            }
                        } else {
                            if (branchOfParsing == BluetoothConstantManager.OLD_PROTOCOL){
                                if(i == 2){
                                    lowByte = msg;
                                }
                                if(i == 3){
                                    msgCurrent = (lowByte << 8) + msg; //msgLenght содержит количество байт данных в посылке
                                }
                                if(i == 4){
                                    lowByte = msg;
                                }
                                if(i == 5){
                                    msgLevelCH1 = (lowByte << 8) + msg;  //msgLevelCH уровень канала 1
                                    if (DEBUG) {System.out.println("<-- уровень CH1:" + msgLevelCH1 + " i = " + i + " no_error=" + no_error);}
                                }
                                if(i == 6){
                                    lowByte = msg;
                                }
                                if(i == 7){
                                    msgLevelCH2 = (lowByte << 8) + msg; //msgLevelCH уровень канала 2
                                    if (DEBUG) {System.out.println("<-- уровень CH2:" + msgLevelCH2 + " i = " + i + " no_error=" + no_error);}
                                }
                                if(i == 8){
                                    msgIndicationState = (byte) msg;
                                }
                                if(i == 9){
                                    lowByte = msg;
                                }
                                if(i == 10){
                                    msgBatteryTension = (lowByte << 8) + msg; //msgBatteryTension показания батарейки
                                }
                                if(no_error) {
                                    i++;
                                }
                                if(msg == 35){
                                    resetAllVariables();
                                }
                                if(((deviceCallback != null) && (msg == 35))){
                                    final String msgCopy = String.valueOf(msgstr);
                                    final Integer msgCurrentf = msgCurrent;
                                    final Integer msgLevelCH1f = msgLevelCH1;
                                    final Integer msgLevelCH2f = msgLevelCH2;
                                    final Byte msgIndicationStatef = msgIndicationState;
                                    final Integer msgBatteryTensionf = msgBatteryTension;
                                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                                        @Override
                                        public void run() {
                                            if(no_error && msgCorrectAcceptance) {
//                                                System.out.println("BLUETOOTH--------------> receive");
                                                parserCallback.givsGeneralParcel(msgCurrentf, msgLevelCH1f, msgLevelCH2f, msgIndicationStatef, msgBatteryTensionf);
                                                deviceCallback.onMessage(msgCopy);
                                                if (DEBUG) {System.out.println("<-- сделал цикл по ветке 2:"+ msgCopy +" no_error="+no_error);}
                                            }
                                            resetAllVariables();
                                        }
                                    });
                                }
                            }
                        }

                        if (branchOfParsing == BluetoothConstantManager.HDLC_PROTOCOL){
                            System.out.println("BLUETOOTH--------------> HDLC uses " + parserCallback.getFlagUseHDLCProcol());
                        }
                    } else return; //завершение потока
                }
            } catch (final IOException e) {
                connected=false;
                if(deviceCallback != null){
                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            if (DEBUG) {System.out.println("BLUETOOTH--------------> ReceiveThread ERROR deviceCallback = null connected:" + connected);}
                            deviceCallback.onDeviceDisconnected(device, e.getMessage());
                        }
                    });
                }
            }
        }

        public void resetAllVariables() {
            msgstr.setLength(0);
            no_error = true;
            msgLenght = 0;
            msgRegister = 0;
            summator = 0;
            branchOfParsing = 0;
            msgCorrectAcceptance = true;
            i=1;
        }
    }

    private class ConnectThread extends Thread {
        ConnectThread(BluetoothDevice device, boolean insecureConnection) {
            if (DEBUG) {System.out.println("BLUETOOTH--------------> ConnectThread connected:" + connected);}
            Bluetooth.this.device=device;
            try {
                if(insecureConnection){
                    Bluetooth.this.socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                }
                else{
                    Bluetooth.this.socket = device.createRfcommSocketToServiceRecord(uuid);
                }
            } catch (IOException e) {
                if(deviceCallback !=null){
                    if (DEBUG) {System.out.println("BLUETOOTH--------------> ConnectThread ERROR deviceCallback = null connected:" + connected);}
                    deviceCallback.onError(e.getMessage());
                }
            }
        }


        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
                out = socket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ISO-8859-1"));
                in = new InputStreamReader(socket.getInputStream());
                connected=true;

                new ReceiveThread().start();
//                receiveThread.finish();

                if(deviceCallback !=null) {
                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            deviceCallback.onDeviceConnected(device);
                        }
                    });
                }
            } catch (final IOException e) {
                if(deviceCallback !=null) {
                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            if (DEBUG) {System.out.println("BLUETOOTH--------------> ConnectThread ERROR deviceCallback null in progress connected:" + connected);}
                            deviceCallback.onConnectError(device, e.getMessage());
                        }
                    });
                }

                try {
                    socket.close();
                        if (DEBUG) {System.out.println("BLUETOOTH--------------> ConnectThread socket.close connected:" + connected);}
                } catch (final IOException closeException) {
                    if (deviceCallback != null) {
                        ThreadHelper.run(runOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                if (DEBUG) {System.out.println("BLUETOOTH--------------> ConnectThread ERROR deviceCallback null in progress 2 connected:" + connected);}
                                deviceCallback.onError(closeException.getMessage());//Could not connect. New attempt in 3sec...
                            }
                        });
                    }
                }
            }
        }
    }

    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) {System.out.println("BLUETOOTH--------------> BroadcastReceiver scanReceiver connected:" + connected);}
            String action = intent.getAction();
            if(action!=null) {
                switch (action) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        if (state == BluetoothAdapter.STATE_OFF) {
                            if (discoveryCallback != null) {
                                ThreadHelper.run(runOnUi, activity, new Runnable() {
                                    @Override
                                    public void run() {
                                        discoveryCallback.onError("Bluetooth turned off");
                                    }
                                });
                            }
                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        if (discoveryCallback != null){
                            ThreadHelper.run(runOnUi, activity, new Runnable() {
                                @Override
                                public void run() {
                                    discoveryCallback.onDiscoveryStarted();
                                }
                            });
                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        context.unregisterReceiver(scanReceiver);
                        if (discoveryCallback != null){
                            ThreadHelper.run(runOnUi, activity, new Runnable() {
                                @Override
                                public void run() {
                                    discoveryCallback.onDiscoveryFinished();
                                }
                            });
                        }
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (discoveryCallback != null){
                            ThreadHelper.run(runOnUi, activity, new Runnable() {
                                @Override
                                public void run() {
                                    discoveryCallback.onDeviceFound(device);
                                }
                            });
                        }
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver pairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DEBUG) {
                System.out.println("BLUETOOTH--------------> BroadcastReceiverBLE pairReceiver connected:" + connected);
                Log.d("BLE", "PAIR RECEIVER");
            }

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState	= intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    context.unregisterReceiver(pairReceiver);
                    if(discoveryCallback!=null){
                        ThreadHelper.run(runOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                discoveryCallback.onDevicePaired(devicePair);
                            }
                        });
                    }
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    context.unregisterReceiver(pairReceiver);
                    if(discoveryCallback!=null){
                        ThreadHelper.run(runOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                discoveryCallback.onDeviceUnpaired(devicePair);
                            }
                        });
                    }
                }
            }
        }
    };

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) {System.out.println("BLUETOOTH--------------> BroadcastReceiver bluetoothReceiver connected:" + connected);}
            final String action = intent.getAction();
            if (action!=null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if(bluetoothCallback!=null) {
                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            switch (state) {
                                case BluetoothAdapter.STATE_OFF:
                                    bluetoothCallback.onBluetoothOff();
                                    break;
                                case BluetoothAdapter.STATE_TURNING_OFF:
                                    bluetoothCallback.onBluetoothTurningOff();
                                    break;
                                case BluetoothAdapter.STATE_ON:
                                    bluetoothCallback.onBluetoothOn();
                                    break;
                                case BluetoothAdapter.STATE_TURNING_ON:
                                    bluetoothCallback.onBluetoothTurningOn();
                                    break;
                            }
                        }
                    });
                }
            }
        }
    };

    public void setDeviceCallback(DeviceCallback deviceCallback) {
        if (DEBUG) {System.out.println("BLUETOOTH--------------> setDeviceCallback connected:" + connected);}
        this.deviceCallback = deviceCallback;
    }

    public void setDeviceCallback2(DeviceCallback deviceCallback) {
        if (DEBUG) {if (DEBUG) {System.out.println("BLUETOOTH--------------> setDeviceCallback connected:" + connected);}}
        this.deviceCallback = deviceCallback;
    }

    public void enableParsing(ParserCallback parserCallback) {
        if (DEBUG) {System.out.println("BLUETOOTH--------------> enableParsing connected:" + connected);}
        this.parserCallback = parserCallback;
    }

    public void removeCommunicationCallback(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> removeCommunicationCallback connected:" + connected);}
        this.deviceCallback = null;
    }

    public void setDiscoveryCallback(DiscoveryCallback discoveryCallback){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> setDiscoveryCallback connected:" + connected);}
        this.discoveryCallback = discoveryCallback;
    }

    public void removeDiscoveryCallback(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> removeDiscoveryCallback connected:" + connected);}
        this.discoveryCallback = null;
    }

    public void setBluetoothCallback(BluetoothCallback bluetoothCallback){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> BroadcastReceiverBLE connected:" + connected);}
        this.bluetoothCallback = bluetoothCallback;
    }

    public void removeBluetoothCallback(){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> BroadcastReceiverBLE connected:" + connected);}
        this.bluetoothCallback = null;
    }
}