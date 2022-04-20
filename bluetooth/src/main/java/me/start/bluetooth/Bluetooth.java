package me.start.bluetooth;

import android.annotation.SuppressLint;
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
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@SuppressWarnings({"CommentedOutCode", "StatementWithEmptyBody", "JavaReflectionMemberAccess", "ConstantConditions"})
@SuppressLint("MissingPermission")
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
    private final boolean DEBUG = false;
    public Thread dumpingIVariableThread;
    public boolean dumpingIVariableThreadFlag = false;
    private volatile int i = 1;

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
        if(Build.VERSION_CODES.JELLY_BEAN_MR2 <= Build.VERSION.SDK_INT){
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
                ThreadHelper.run(runOnUi, activity, () -> {
                    if(resultCode==Activity.RESULT_CANCELED){
                        bluetoothCallback.onUserDeniedActivation();
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

    public void checkConnectToDevice(BluetoothDevice device, boolean insecureConnection){
        new CheckConnectThread(device, insecureConnection).start();
    }

    public void checkConnectToDevice(BluetoothDevice device){
        checkConnectToDevice(device, false);
    }

    public void disconnect() {
        if (DEBUG) {System.out.println("BLUETOOTH--------------> disconnect connected:" + connected);}
        try {
            socket.close();
            if (DEBUG) {System.out.println("BLUETOOTH--------------> disconnect socket.close eeeee!!! connected:" + connected);}
        } catch (final IOException e) {
            if(deviceCallback !=null) {
                ThreadHelper.run(runOnUi, activity, () -> {
                    if (DEBUG) {System.out.println("BLUETOOTH--------------> disconnect ERROR deviceCallback = null connected:" + connected);}
                    deviceCallback.onError(e.getMessage());
                    try {
                        Thread.sleep(1000);
                    }catch (Exception ignored){}
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
                ThreadHelper.run(runOnUi, activity, () -> {
                    if (DEBUG) {System.out.println("BLUETOOTH--------------> send ERROR deviceCallback = null connected:" + connected);}
                    System.out.println("BLUETOOTH--------------> disconnectit 1 connected:" + connected);
                    deviceCallback.onDeviceDisconnected(device, e.getMessage());
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
                ThreadHelper.run(runOnUi, activity, () -> {
                    System.out.println("BLUETOOTH--------------> disconnectit 2 connected:" + connected);
                    deviceCallback.onDeviceDisconnected(device, e.getMessage());
                });
            }
        }
    }


    public void send(byte[] msg){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> send2 connected:" + connected);}
        send(msg, null);
    }

    public void sendstr(String msgStr){
        if (DEBUG) {System.out.println("BLUETOOTH--------------> sendstr connected:" + connected);}
        sendstr(msgStr, "US-ASCII");
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
                ThreadHelper.run(runOnUi, activity, () -> {
                    if (DEBUG) {System.out.println("BLUETOOTH--------------> pair ERROR discoveryCallback = null connected:" + connected);}
                    discoveryCallback.onError(e.getMessage());
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
                ThreadHelper.run(runOnUi, activity, () -> discoveryCallback.onError(e.getMessage()));
            }
        }
    }

    @SuppressWarnings({"FieldCanBeLocal", "ConstantConditions"})
    private class ReceiveThread extends Thread implements Runnable{
        private final int test = 2928;
        private int msg = 65;
        private int summator = 0;     //для проверки суммы первых символов МТ и последнего $
        private int msgLenght = 0;    //для свапа младших и старших байт длинны данных
        private int msgRegister = 0;  //для свапа младших и старших байт номера регистра
        private int msgChannel = 0;   //для номера канала
        private int msgLevelCH = 1250;
        private int msgCRC = 12345;
        private int msgCurrent = 0;
        private int msgLevelCH1 = 0;
        private int msgLevelCH2 = 0;
        private int msgBatteryTension = 0;
        private byte msgIndicationState = 0;
        private byte msgBlockIndication = 0;
        private byte msgRoughnessOfSensors = 0;
        private int lowByte = 0;                //для записи младшего байта при перемене младших и старших байт
        private int addressHDLCMassage = 0;     //для сохранения адреса с которого пришла HDLC посылка
        private final int directionHDLCMassage = 0;   //для сохранения направления с которого пришла HDLC посылка
        private int typeHDLCMassage = 0;        //для сохранения типа принятой посылки HDLC
        private boolean request = true;                //true-ответ false-запрос
        public boolean no_error = true;                //true-нет ошибок false-есть ошибки
        private boolean msgCorrectAcceptance = true;   //true-безошиобочная CRC false-шибочная CRC
        private int branchOfParsing = 1;               //1-певый набор команд, 2-общая полыка с данными, 3-состояние параметров руки на момент включения
        private final StringBuffer msgStr = new StringBuffer();
        private final byte[] txtByteOut = {0x01, 0x02} ;
        private final byte[] byteMassCRC = new byte[10];

        public void run(){
            if (DEBUG) {System.out.println("BLUETOOTH--------------> ReceiveThread connected:" + connected);}
            boolean flagUseHDLCProtocol = parserCallback.getFlagUseHDLCProtocol();
            try {
                while((msg = input.read()) != -1) //((System.in).read(msg)) //((System.in).read(msg))   //((input.read())) != -1
                {
                    if(BluetoothConstantManager.SHOW_ALL_BT_MASSAGE)System.err.println("msg=" + msg + "   i= "+i );
                    boolean finish = false;
                    if(!finish) {
                        if (!flagUseHDLCProtocol){
                            if((i == 1)||(i == 2)||((i == (8+msgLenght)))){ //адекватный приём!!!!
                                summator += msg;
                                if (summator == 197){
                                    msgCorrectAcceptance = true;
                                    if (DEBUG) {System.out.println("<-- Принята посылка :)");}
                                } else {
                                    if(((i == (8+msgLenght))&&(msg != 36))||((i == 1)&&(msg != 77))){
                                        if (((i == 9) && (msg != 35)) || ((i == 1) && (msg != 36))) {
//                                            System.out.println("BLUETOOTH--------------> НАЧАЛО -> resetAllVariables");
                                            resetAllVariables();
                                            msgCorrectAcceptance = false;
                                        }
                                    }
                                }
                            }
                        }

                        if (flagUseHDLCProtocol){ //выбор ветки для парсинга и отправки
                            branchOfParsing = BluetoothConstantManager.HDLC_PROTOCOL;//парсит hdlc посылки
                        } else {
                            if ((i == 1) && (msg == 77)){
                                branchOfParsing = BluetoothConstantManager.OLD_NOT_USE_PROTOCOL;
                            } else {
                                if ((i == 1) && (msg == 36)){
                                    branchOfParsing = BluetoothConstantManager.OLD_PROTOCOL; //парсит постоянно прилетающие данные
                                    //за первый заход устанавливает начальные параметры,
                                    //а за последующие выдаёт данные на графики, свичи и сикбары
                                } else {
                                    if (i == 1){
                                        branchOfParsing = BluetoothConstantManager.RESET_ALL_VARIABLE;
                                    }
                                }
                            }
                        }

                        if (branchOfParsing == BluetoothConstantManager.RESET_ALL_VARIABLE){
//                            System.out.println("BLUETOOTH--------------> branchOfParsing -> RESET_ALL_VARIABLE -> resetAllVariables");
                            resetAllVariables();
                        }

                        if (branchOfParsing == BluetoothConstantManager.OLD_NOT_USE_PROTOCOL){
                            if(i == 3){
                                lowByte = msg;
                            }
                            if(i == 4){
                                msgLenght = (msg << 8) + lowByte;
                                if (DEBUG) {System.out.println("<-- длина строки:"+msgLenght);}
                            }
                            if(i == 5){
                                request = msg == 1;
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
                            //        private boolean firstRead = true;
                            //true-ошибка на принимающей стороне false-ошибок нет
                            boolean errorReception = msgChannel == 65535;
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
                                msgStr.append((char)msg);
                            }
                            if(i >= (msgLenght+9)){
                                resetAllVariables();
                            }
                            if(((deviceCallback != null) && (msg == 36))){
                                final String msgCopy = String.valueOf(msgStr);
                                final int msgLenghtf = msgLenght;
                                final Boolean requestf = request;
                                final Integer msgRegtsterf = msgRegister;
                                final int msgChannelf = msgChannel;
                                final int msgLevelCHf = msgLevelCH;
                                final Boolean msgCorrectAcceptancef = msgCorrectAcceptance;
                                final Boolean errorReceptionf = errorReception;
                                ThreadHelper.run(runOnUi, activity, () -> {
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
                                });
                            }
                        }

                        if (bluetoothCallback.getFirstRead()) {
                            if (branchOfParsing == BluetoothConstantManager.OLD_PROTOCOL){
                                if(i == 2){
                                    lowByte = msg;
                                }
                                if(i == 3){
                                    msgCurrent = (lowByte << 8) + msg;
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
                                    final String msgCopy = String.valueOf(msgStr);
                                    final int msgCurrentf = msgCurrent;
                                    final int msgLevelTrigCH1f = msgLevelCH1;
                                    final int msgLevelTrigCH2f = msgLevelCH2;
                                    final byte msgIndicationInvertModef = msgIndicationState;
                                    final byte msgBlockIndicationf = msgBlockIndication;
                                    final byte msgRoughnessOfSensorsf = msgRoughnessOfSensors;
                                    ThreadHelper.run(runOnUi, activity, () -> {
                                        if(no_error && msgCorrectAcceptance) {
                                            parserCallback.givsStartParameters(msgCurrentf, msgLevelTrigCH1f, msgLevelTrigCH2f, msgIndicationInvertModef, msgBlockIndicationf, msgRoughnessOfSensorsf);
                                            parserCallback.setStartParametersInChartActivity();
                                            deviceCallback.onMessage(msgCopy);
                                            if (DEBUG) {System.out.println("<-- сделал цикл по ветке 2 первая итерация:"+ msgCopy +" no_error="+no_error);}
                                        }
                                        resetAllVariables();
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
                                    final String msgCopy = String.valueOf(msgStr);
                                    final int msgCurrentf = msgCurrent;
                                    final int msgLevelCH1f = msgLevelCH1;
                                    final int msgLevelCH2f = msgLevelCH2;
                                    final byte msgIndicationStatef = msgIndicationState;
                                    final int msgBatteryTensionf = msgBatteryTension;
                                    ThreadHelper.run(runOnUi, activity, () -> {
                                        if(no_error && msgCorrectAcceptance) {
//                                                System.out.println("BLUETOOTH--------------> receive");
                                            parserCallback.givsGeneralParcel(msgCurrentf, msgLevelCH1f, msgLevelCH2f, msgIndicationStatef, msgBatteryTensionf);
                                            deviceCallback.onMessage(msgCopy);
                                            if (DEBUG) {System.out.println("<-- сделал цикл по ветке 2:"+ msgCopy +" no_error="+no_error);}
                                        }
                                        resetAllVariables();
                                    });
                                }
                            }
                        }

                        if (branchOfParsing == BluetoothConstantManager.HDLC_PROTOCOL){
//                            System.out.println("BLUETOOTH--------------> msg= " + (byte) msg+"  i= "+i);
                            if(i == 1){
                                addressHDLCMassage = (byte) msg;
//                                System.out.println("BLUETOOTH--------------> i= "+i+"  msg= "+msg);
//                                parserCallback.setFlagReceptionExpectation(false);
//                                startDumpingIVariableThread();
                            }
                            if(i == 2){
//                                System.out.println("BLUETOOTH--------------> i= "+i+"  msgRegister= "+msg);
                                msgRegister = (byte) msg;
                            }
                            if(i == 3){
//                                System.out.println("BLUETOOTH--------------> i= "+i+"  typeHDLCMassage= "+msg);
                                typeHDLCMassage = (byte) msg;
                            }
                            if(i == 4){ }
                            if(i == 5){ addressHDLCMassage = (byte) msg; byteMassCRC[0] = (byte) msg; }
                            switch (msgRegister){
                                case BluetoothConstantManager.WRITE:
                                    switch (typeHDLCMassage){
                                        case BluetoothConstantManager.MOVE_HDLC:
//                                            System.out.println("BLUETOOTH--------------> WRITE -> MOVE_HDLC");
                                            i++;
                                            if(i == 10){
//                                                System.out.println("BLUETOOTH--------------> WRITE -> MOVE_HDLC -> resetAllVariables");
                                                resetAllVariables();
                                            }
                                            break;
//                                                System.out.println("BLUETOOTH--------------> WRITE -> HDLC_39 -> resetAllVariables");
                                        case BluetoothConstantManager.HDLC_39:
//                                            System.out.println("BLUETOOTH--------------> WRITE -> HDLC_39");
                                        default:
//                                            System.out.println("BLUETOOTH--------------> WRITE -> DEFAULT");
                                            i++;
                                            if(i == 6){
//                                                System.out.println("BLUETOOTH--------------> WRITE -> resetAllVariables");
                                                resetAllVariables();
                                            }
                                            break;

                                    }
                                    break;
                                case BluetoothConstantManager.READ:
                                    switch (typeHDLCMassage){
                                        case BluetoothConstantManager.ENDPOINT_POSITION:
//                                            System.out.println("BLUETOOTH--------------> READ -> ENDPOINT_POSITION");
                                            i++;
                                            if(i == 6){
//                                                System.out.println("BLUETOOTH--------------> READ -> ENDPOINT_POSITION -> resetAllVariables");
                                                resetAllVariables();
                                            }
                                            break;
                                        case BluetoothConstantManager.MIO1_TRIG_HDLC:
//                                            System.out.println("BLUETOOTH--------------> READ -> MIO1_TRIG_HDLC");
                                            if(addressHDLCMassage == (byte) 0xFA){
                                                if(i == 6){ lowByte = (byte) msg; byteMassCRC[1] = (byte) msg; }
                                                if(i == 7){ msgLevelCH1 = (lowByte << 8) + msg;  byteMassCRC[2] = (byte) msg; }
                                                if(i == 8){
                                                    msgCRC = (byte) msg;
//                                                    System.out.println("BLUETOOTH--------------> принятая CRC= "+msgCRC);
//                                                    System.out.println("BLUETOOTH--------------> посчитанная CRC= "+calculationCRC(byteMassCRC, 3));
                                                }//обработчик CRC
                                            }
                                            i++;
                                            if(((deviceCallback != null) && (msgCRC == calculationCRC(byteMassCRC,3)))){
                                                final int msgLevelCH1f = msgLevelCH1;
                                                ThreadHelper.run(runOnUi, activity, () -> {
                                                    parserCallback.givsStartParametersTrigCH1(msgLevelCH1f);
                                                    parserCallback.setStartParametersInChartActivity();
//                                                            System.out.println("BLUETOOTH--------------> READ TRIG1 START PARAMETER ");
                                                    resetAllVariables();
                                                });
                                            }
                                            break;
                                        case BluetoothConstantManager.MIO2_TRIG_HDLC:
//                                            System.out.println("BLUETOOTH--------------> READ -> MIO2_TRIG_HDLC");
                                            if(addressHDLCMassage == (byte) 0xFA){
                                                if(i == 6){ lowByte = (byte) msg; byteMassCRC[1] = (byte) msg; }
                                                if(i == 7){ msgLevelCH2 = (lowByte << 8) + msg;  byteMassCRC[2] = (byte) msg; }
                                                if(i == 8){
                                                    msgCRC = (byte) msg;
//                                                    System.out.println("BLUETOOTH--------------> принятая CRC= "+msgCRC);
//                                                    System.out.println("BLUETOOTH--------------> посчитанная CRC= "+calculationCRC(byteMassCRC, 3));
                                                }//обработчик CRC
                                            }
                                            i++;
                                            if(((deviceCallback != null) && (msgCRC == calculationCRC(byteMassCRC,3)))){

                                                final int msgLevelCH2f = msgLevelCH2;
                                                ThreadHelper.run(runOnUi, activity, () -> {
                                                    parserCallback.givsStartParametersTrigCH2(msgLevelCH2f);
                                                    parserCallback.setStartParametersInChartActivity();
//                                                            System.out.println("BLUETOOTH--------------> READ TRIG2 START PARAMETER ");
                                                    resetAllVariables();
                                                });
                                            }
                                            break;
                                        case BluetoothConstantManager.CURR_LIMIT_HDLC:
//                                            System.out.println("BLUETOOTH--------------> READ -> CURR_LIMIT_HDLC");
                                            if(addressHDLCMassage == (byte) 0xFA){
                                                if(i == 6){ lowByte = (byte) msg; byteMassCRC[1] = (byte) msg; }
                                                if(i == 7){ msgCurrent = (lowByte << 8) + msg;  byteMassCRC[2] = (byte) msg; }
                                                if(i == 8){
                                                    msgCRC = (byte) msg;
//                                                    System.out.println("BLUETOOTH--------------> принятая CRC= "+msgCRC);
//                                                    System.out.println("BLUETOOTH--------------> посчитанная CRC= "+calculationCRC(byteMassCRC, 3));
                                                }//обработчик CRC
                                            }
                                            i++;
                                            if(((deviceCallback != null) && (msgCRC == calculationCRC(byteMassCRC,3)))) {

                                                final int msgCurrentf = msgCurrent;
                                                ThreadHelper.run(runOnUi, activity, () -> {
                                                    parserCallback.givsStartParametersCurrrent(msgCurrentf);
                                                    parserCallback.setStartParametersInChartActivity();
//                                                            System.out.println("BLUETOOTH--------------> READ CURRENT START PARAMETER ");
                                                    resetAllVariables();
                                                });
                                            }
                                            break;
                                        case BluetoothConstantManager.CURR_BAT_HDLC:
//                                            System.out.println("BLUETOOTH--------------> READ -> CURR_BAT_HDLC");
                                            if(i == 6){ msgBatteryTension = (byte) msg; byteMassCRC[1] = (byte) msg;}
                                            if(i == 7){
                                                msgCRC = (byte) msg;
//                                                    System.out.println("BLUETOOTH--------------> принятая CRC= "+msgCRC);
//                                                    System.out.println("BLUETOOTH--------------> посчитанная CRC= "+calculationCRC(byteMassCRC, 2));
                                            }//обработчик CRC
                                            i++;
                                            if(((deviceCallback != null) && (msgCRC == calculationCRC(byteMassCRC,2)))) {

                                                final int msgBatteryTensionf = msgBatteryTension;
                                                ThreadHelper.run(runOnUi, activity, () -> {
                                                    parserCallback.givsStartParametersBattery(msgBatteryTensionf);
//                                                            System.out.println("BLUETOOTH--------------> READ BATTERY START PARAMETER ");
                                                    resetAllVariables();
                                                });
                                            }
                                            break;
                                        case BluetoothConstantManager.BLOCK_PERMISSION_HDLC:
//                                            System.out.println("BLUETOOTH--------------> READ -> BLOCK_PERMISSION_HDLC");
                                            if(addressHDLCMassage == (byte) 0xFA){
                                                if(i == 6){ msgBlockIndication = (byte) msg; byteMassCRC[1] = (byte) msg;}
                                                if(i == 7){
                                                    msgCRC = (byte) msg;
//                                                    System.out.println("BLUETOOTH--------------> принятая CRC= "+msgCRC);
//                                                    System.out.println("BLUETOOTH--------------> посчитанная CRC= "+calculationCRC(byteMassCRC, 2));
                                                }//обработчик CRC
                                            }
                                            i++;
                                            if(((deviceCallback != null) && (msgCRC == calculationCRC(byteMassCRC,2)))) {

                                                final byte msgBlockIndicationf = msgBlockIndication;
                                                ThreadHelper.run(runOnUi, activity, () -> {
                                                    parserCallback.givsStartParametersBlock(msgBlockIndicationf);
                                                    parserCallback.setStartParametersInChartActivity();
//                                                            System.out.println("BLUETOOTH--------------> READ BLOCK START PARAMETER ");
                                                    resetAllVariables();
                                                });
                                            }
                                            break;
                                        case BluetoothConstantManager.ADC_BUFF_CHOISES_HDLC:
//                                            System.out.println("BLUETOOTH--------------> READ -> ADC_BUFF_CHOISES_HDLC");
                                            if(addressHDLCMassage == (byte) 0xFA){
                                                if(i == 6){ msgRoughnessOfSensors = (byte) msg;}
                                                if(i == 7){}//обработчик CRC
                                            }
                                            i++;
                                            if(((deviceCallback != null) && (i == 8))) {

                                                final byte msgRoughnessOfSensorsf = msgRoughnessOfSensors;
                                                ThreadHelper.run(runOnUi, activity, () -> {
                                                    parserCallback.givsStartParametersRoughness (msgRoughnessOfSensorsf);
                                                    parserCallback.setStartParametersInChartActivity();
//                                                            System.out.println("BLUETOOTH--------------> READ ROUGHNESS START PARAMETER ");
                                                    resetAllVariables();
                                                });
                                            }
                                            break;
                                        case BluetoothConstantManager.CURR_MAIN_DATA_HDLC:
//                                            System.out.println("BLUETOOTH--------------> READ -> CURR_MAIN_DATA_HDLC");
                                            if(addressHDLCMassage == (byte) 0xFA){
                                                if(i == 6){ lowByte = msg; byteMassCRC[1] = (byte) msg; }
                                                if(i == 7){ msgLevelCH1 = (lowByte << 8) + msg;  byteMassCRC[2] = (byte) msg; }
                                                if(i == 8){ lowByte = msg; byteMassCRC[3] = (byte) msg; }
                                                if(i == 9){ msgLevelCH2 = (lowByte << 8) + msg; byteMassCRC[4] = (byte) msg;}
//                                            if(i == 10){ lowByte = msg; }
//                                            if(i == 11){ msgCurrent = (lowByte << 8) + msg; }
                                                if(i == 10){
                                                    msgCRC = (byte) msg;
//                                                    System.out.println("BLUETOOTH--------------> принятая CRC= "+msgCRC);
//                                                    System.out.println("BLUETOOTH--------------> посчитанная CRC= "+calculationCRC(byteMassCRC, 5));
                                                }
                                            }
                                            i++;
                                            if(((deviceCallback != null) && (msgCRC == calculationCRC(byteMassCRC,5)))){
                                                final String msgCopy = String.valueOf(msgStr);
                                                final int msgCurrentf = msgCurrent;
                                                final int msgLevelCH1f = msgLevelCH1;
                                                final int msgLevelCH2f = msgLevelCH2;
                                                final byte msgIndicationStatef = msgIndicationState;
                                                final int msgBatteryTensionf = msgBatteryTension;
                                                ThreadHelper.run(runOnUi, activity, () -> {
//                                                System.out.println("BLUETOOTH--------------> receive");
                                                    parserCallback.givsGeneralParcel(msgCurrentf, msgLevelCH1f, msgLevelCH2f, msgIndicationStatef, msgBatteryTensionf);
                                                    deviceCallback.onMessage(msgCopy);
                                                    if (DEBUG) {System.out.println("<-- сделал цикл по ветке 2:"+ msgCopy +" no_error="+no_error);}
//                                                            System.out.println("BLUETOOTH--------------> READ -> CURR_MAIN_DATA_HDLC -> resetAllVariables");
                                                    resetAllVariables();
                                                });
                                            }
                                            break;
                                    }
                                    break;
                                default:
//                                    System.out.println("BLUETOOTH--------------> DEFAULT");
                                    i++;
                                    break;
                            }
                        }
                    } else return; //завершение потока
                }
            } catch (final IOException e) {
                connected=false;
                if(deviceCallback != null){
                    ThreadHelper.run(runOnUi, activity, () -> {
                        if (DEBUG) {System.out.println("BLUETOOTH--------------> ReceiveThread ERROR deviceCallback = null connected:" + connected);}
                        System.out.println("BLUETOOTH--------------> disconnectit 3 connected:" + connected);
                        deviceCallback.onDeviceDisconnected(device, e.getMessage());
                    });
                }
            }
        }

        public void resetAllVariables() {
            msgStr.setLength(0);
            no_error =true;
            summator = 0;
            msgLenght = 0;
            msgRegister = 0;
            branchOfParsing = 0;
            msgCorrectAcceptance = true;
            dumpingIVariableThreadFlag = false;
            parserCallback.setFlagReceptionExpectation(false);
            addressHDLCMassage = 0;
            typeHDLCMassage = 0;
            msgRegister = 0;
            msgCRC=12345;
            i = 1 ;
//            System.out.println("BLUETOOTH--------------> RESET ALL VARIABLES");
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private class CheckReceiveThread extends Thread implements Runnable {
        private final boolean mFinish = false;
        private int msg = 65;

        public void run() {
            try {
                while ((msg = input.read()) != -1)
                {
                    if (mFinish) {deviceCallback.onDeviceDisconnected(device, "CheckReceiveThread->return");return;}
                }
            } catch (final IOException e) {
                connected = false;
                if (deviceCallback != null) {
                    ThreadHelper.run(runOnUi, activity, () -> {
                        System.out.println("BLUETOOTH--------------> disconnectit 5 connected:" + connected);
                        deviceCallback.onDeviceDisconnected(device, e.getMessage());
                    });
                }
            }
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
            System.out.println("BLUETOOTH--------------> ConnectThread " + device);
            try {
                socket.connect();
                out = socket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
                in = new InputStreamReader(socket.getInputStream());
                connected=true;

                new ReceiveThread().start();
//                receiveThread.finish();

                if(deviceCallback !=null) {
                    ThreadHelper.run(runOnUi, activity, () -> deviceCallback.onDeviceConnected(device));
                }
            } catch (final IOException e) {
                if(deviceCallback !=null) {
                    ThreadHelper.run(runOnUi, activity, () -> {
                        if (DEBUG) {System.out.println("BLUETOOTH--------------> ConnectThread ERROR deviceCallback null in progress connected:" + connected);}
                        deviceCallback.onConnectError(device, e.getMessage());
                    });
                }

                try {
                    socket.close();
                    if (DEBUG) {System.out.println("BLUETOOTH--------------> ConnectThread socket.close connected:" + connected);}
                } catch (final IOException closeException) {
                    if (deviceCallback != null) {
                        ThreadHelper.run(runOnUi, activity, () -> {
                            if (DEBUG) {System.out.println("BLUETOOTH--------------> ConnectThread ERROR deviceCallback null in progress 2 connected:" + connected);}
                            deviceCallback.onError(closeException.getMessage());//Could not connect. New attempt in 3sec...
                        });
                    }
                }
            }
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private class CheckConnectThread extends Thread {
        private final boolean mFinish = false;

        CheckConnectThread(BluetoothDevice device, boolean insecureConnection) {
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
                    deviceCallback.onError(e.getMessage());
                }
            }
        }


        public void run() {
            final BluetoothDevice myDevice = device;

            bluetoothAdapter.cancelDiscovery();
            System.out.println("BLUETOOTH--------------> ConnectCheckThread");
            try {
                socket.connect();
                out = socket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
                in = new InputStreamReader(socket.getInputStream());
                connected=true;
//
                new CheckReceiveThread().start();
                if (mFinish) {
                    socket.close();
                    return;
                }
                if(deviceCallback !=null) {
                    ThreadHelper.run(runOnUi, activity, () -> deviceCallback.onDeviceConnected(myDevice));
                }
            } catch (final IOException e) {
                if(deviceCallback !=null) {
                    ThreadHelper.run(runOnUi, activity, () -> deviceCallback.onConnectError(myDevice, e.getMessage()));
                }

                try {
                    socket.close();
                } catch (final IOException closeException) {
                    if (deviceCallback != null) {
                        ThreadHelper.run(runOnUi, activity, () -> deviceCallback.onError(closeException.getMessage()));
                    }
                }
            }
        }
    }


    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
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
                                ThreadHelper.run(runOnUi, activity, () -> discoveryCallback.onError("Bluetooth turned off"));
                            }
                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        if (discoveryCallback != null){
                            ThreadHelper.run(runOnUi, activity, () -> discoveryCallback.onDiscoveryStarted());
                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        context.unregisterReceiver(scanReceiver);
                        if (discoveryCallback != null){
                            ThreadHelper.run(runOnUi, activity, () -> discoveryCallback.onDiscoveryFinished());
                        }
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (discoveryCallback != null){
                            ThreadHelper.run(runOnUi, activity, () -> discoveryCallback.onDeviceFound(device));
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
                        ThreadHelper.run(runOnUi, activity, () -> discoveryCallback.onDevicePaired(devicePair));
                    }
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    context.unregisterReceiver(pairReceiver);
                    if(discoveryCallback!=null){
                        ThreadHelper.run(runOnUi, activity, () -> discoveryCallback.onDeviceUnpaired(devicePair));
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
                    ThreadHelper.run(runOnUi, activity, () -> {
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

    public void startDumpingIVariableThread () {
        dumpingIVariableThread = new Thread(() -> {
            System.out.println("BLUETOOTH--------------> старт потока обнуления i");
            dumpingIVariableThreadFlag = true;
            try {
                Thread.sleep(BluetoothConstantManager.TIME_DAMPING_HDLC_MS);
            }catch (Exception ignored){}
            while (dumpingIVariableThreadFlag){
                System.out.println("BLUETOOTH--------------> обнуления i выполнилось");
                i =1;
                dumpingIVariableThreadFlag = false;
            }
        });
        dumpingIVariableThread.start();
    }

    public byte calculationCRC(byte[] bytes, int length) {
        byte CRC = (byte) 0xff;
        boolean b;
        for (int i = 0; i < length; i++){
            CRC ^= bytes[i];
            for (int j = 0; j < 8; j++)
            {
                b = ((CRC & 0x80) >> 7) != 0;
                CRC = (byte) (b  ? (CRC << 1) ^ 0x31 : CRC << 1);
            }
        }
        return CRC;
    }

    public void setIterator (int i){
        this.i = i;
    }
}