package me.aflak.bluetooth;

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

    private boolean runOnUi;

    public Bluetooth(Context context){
        System.out.println("BLUETOOTH--------------> Bluetooth");
        initialize(context, UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
    }

    public Bluetooth(Context context, UUID uuid){
        System.out.println("BLUETOOTH--------------> Bluetooth2");
        initialize(context, uuid);
    }

    private void initialize(Context context, UUID uuid){
        System.out.println("BLUETOOTH--------------> initialize");
        this.context = context;
        this.uuid = uuid;
        this.deviceCallback = null;
        this.discoveryCallback = null;
        this.bluetoothCallback = null;
        this.connected = false;
        this.runOnUi = false;
    }

    public void onStart(){
        System.out.println("BLUETOOTH--------------> onStart connected:" + connected);
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
        System.out.println("BLUETOOTH--------------> onStop connected:" + connected);
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
        System.out.println("BLUETOOTH--------------> enable connected:" + connected);
        if(bluetoothAdapter!=null) {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        }
    }

    public void disable(){
        System.out.println("BLUETOOTH--------------> disable connected:" + connected);
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
        System.out.println("BLUETOOTH--------------> isEnabled connected:" + connected);
        if(bluetoothAdapter!=null) {
            return bluetoothAdapter.isEnabled();
        }
        return false;
    }

    public void setCallbackOnUI(Activity activity){
        System.out.println("BLUETOOTH--------------> setCallbackOnUI connected:" + connected);
        this.activity = activity;
        this.runOnUi = true;
    }

    public void onActivityResult(int requestCode, final int resultCode){
        System.out.println("BLUETOOTH--------------> onActivityResult connected:" + connected);
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
        System.out.println("BLUETOOTH--------------> connectToAddress connected:" + connected);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        connectToDevice(device, insecureConnection);
    }

    public void connectToAddress(String address) {
        connectToAddress(address, false);
    }

    public void connectToName(String name, boolean insecureConnection) {
        System.out.println("BLUETOOTH--------------> connectToName:" + name + "connected:" + connected);
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
        System.out.println("BLUETOOTH--------------> connectToDevice:" + device + "connected:" + connected);
        new ConnectThread(device, insecureConnection).start();
    }

    public void connectToDevice(BluetoothDevice device){
        System.out.println("BLUETOOTH--------------> connectToDevice2:" + device + "connected:" + connected);
        connectToDevice(device, false);
    }

    public void disconnect() {
        System.out.println("BLUETOOTH--------------> disconnect connected:" + connected);
        try {
            socket.close();
            System.out.println("BLUETOOTH--------------> disconnect socket.close eeeee!!! connected:" + connected);
        } catch (final IOException e) {
            if(deviceCallback !=null) {
                ThreadHelper.run(runOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("BLUETOOTH--------------> disconnect ERROR deviceCallback = null connected:" + connected);
                        deviceCallback.onError(e.getMessage());
//                        try {
//                            Thread.sleep(3000);
//                        }catch (Exception e){}
                    }
                });
            }
        }
    }

    public boolean isConnected(){
        System.out.println("BLUETOOTH--------------> isConnected connected:" + connected);
        return connected;
    }

    public void send(byte[] msg, String charset){
        System.out.println("BLUETOOTH--------------> send" + msg[0] + "connected:" + connected);
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
                        System.out.println("BLUETOOTH--------------> send ERROR deviceCallback = null connected:" + connected);
                        deviceCallback.onDeviceDisconnected(device, e.getMessage());
                    }
                });
            }
        }
    }

    public void sendstr(String msgstr, String charset){
        System.out.println("BLUETOOTH--------------> sendstr connected:" + connected);
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
        System.out.println("BLUETOOTH--------------> send2 connected:" + connected);
        send(msg, null);
    }

    public void sendstr(String msgstr){
        System.out.println("BLUETOOTH--------------> sendstr connected:" + connected);
        sendstr(msgstr, "US-ASCII");
    }

    public List<BluetoothDevice> getPairedDevices(){
        System.out.println("BLUETOOTH--------------> getPairedDevices connected:" + connected);
        return new ArrayList<>(bluetoothAdapter.getBondedDevices());
    }

    public void startScanning(){
        System.out.println("BLUETOOTH--------------> startScanning connected:" + connected);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        context.registerReceiver(scanReceiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    public void stopScanning(){
        System.out.println("BLUETOOTH--------------> stopScanning connected:" + connected);
        context.unregisterReceiver(scanReceiver);
        bluetoothAdapter.cancelDiscovery();
    }

    public void pair(BluetoothDevice device){
        System.out.println("BLUETOOTH--------------> pair connected:" + connected);
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
                        System.out.println("BLUETOOTH--------------> pair ERROR discoveryCallback = null connected:" + connected);
                        discoveryCallback.onError(e.getMessage());
                    }
                });
            }
        }
    }

    public void unpair(BluetoothDevice device) {
        System.out.println("BLUETOOTH--------------> unpair connected:" + connected);
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
        private int lowByte = 0;     //для записи младшего байта при перемене младших и старших байт
        private int i=1;
        private boolean errorReception = false;        //true-ошибка на принимающей стороне false-ошибок нет
        private boolean request = true;                //true-ответ false-запрос
        public boolean no_error = true;                //true-нет ошибок false-есть ошибки
        private boolean msgCorrectAcceptance = true;   //true-безошиобочная CRC false-шибочная CRC
        private StringBuffer msgstr = new StringBuffer();
        private byte[] txtbyteout = {0x01, 0x02} ;
        private volatile boolean mFinish = false;

        public void finish()		//Инициирует завершение потока
        {
            System.out.println("BLUETOOTH--------------> ReceiveThread завершение потока connected:" + connected);
            mFinish = true;
        }

        public void run(){
            System.out.println("BLUETOOTH--------------> ReceiveThread connected:" + connected);
            try {
                while((msg = input.read()) != -1) //((System.in).read(msg)) //((System.in).read(msg))   //((input.read())) != -1
                {
                    if(!mFinish) {
                        if ((i == 1)||(i == 9)){
                            summator += msg;
                            if (summator == 71){
                                msgCorrectAcceptance = true;
                                System.out.println("<-- Принята посылка :)");
                            } else {
                                if (((i == (3 + msgLenght)) && (msg != 35)) || ((i == 1) && (msg != 36))) {
                                    System.out.println("<-- Пришла лажа :(");
                                    System.out.println("<-- summator:" + summator);
                                    no_error = false;
                                    msgstr.setLength(0);
                                    msgLenght = 0;
                                    msgRegister = 0;
                                    summator = 0;
                                    i = 1;
                                    msgCorrectAcceptance = false;
                                }
                            }
                        }
                        if(i == 2){
                            lowByte = msg;
                        }
                        if(i == 3){
                            msgLenght = (msg << 8) + lowByte; //msgLenght содержит количество байт данных в посылке
                            System.out.println("<-- длина строки:"+msgLenght);
                        }
                        if(i == 4){
                            lowByte = msg;
                        }
                        if(i == 5){
                            msgChannel = (msg << 8) + lowByte;  //msgRegister содержит номер регистра
                            System.out.println("<-- уровень CH2:"+msgChannel);
                        }
                        if(i == 6){
                            lowByte = msg;
                        }
                        if(i == 7){
                            msgLevelCH = (msg << 8) + lowByte; //msgLevelCH уровень канала 1
                            System.out.println("<-- уровень CH1:"+msgLevelCH);
                        }
                        if(no_error) {
                            i++;
                            msgstr.append((char)msg);
                        }
                        if(i > 10){
                            //System.out.println("------> i=" +i+" msgLenght="+msgLenght);
                            msgstr.setLength(0);
                            no_error = true;
                            msgLenght = 0;
                            msgRegister = 0;
                            summator = 0;
                            i=1;
                        }
                        if(((deviceCallback != null) && (msg == 35))||(!no_error)){
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
                                        System.out.println("<-- сделал цикл:"+ msgCopy);
                                    }
                                    parserCallback.givsCorrectAcceptance(msgCorrectAcceptancef);
                                    msgstr.setLength(0);
                                    no_error = true;
                                    msgLenght = 0;
                                    msgRegister = 0;
                                    summator = 0;
                                    i=1;
                                }
                            });
                        }
                    } else return; //завершение потока
                }

            } catch (final IOException e) {
                connected=false;
                if(deviceCallback != null){
                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("BLUETOOTH--------------> ReceiveThread ERROR deviceCallback = null connected:" + connected);
                            deviceCallback.onDeviceDisconnected(device, e.getMessage());
                        }
                    });
                }
            }
        }
    }

    private class ConnectThread extends Thread {
        ConnectThread(BluetoothDevice device, boolean insecureConnection) {
            System.out.println("BLUETOOTH--------------> ConnectThread connected:" + connected);
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
                    System.out.println("BLUETOOTH--------------> ConnectThread ERROR deviceCallback = null connected:" + connected);
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
                            System.out.println("BLUETOOTH--------------> ConnectThread ERROR deviceCallback null in progress connected:" + connected);
                            deviceCallback.onConnectError(device, e.getMessage());
                        }
                    });
                }

                try {
                    socket.close();
                    System.out.println("BLUETOOTH--------------> ConnectThread socket.close connected:" + connected);
                } catch (final IOException closeException) {
                    if (deviceCallback != null) {
                        ThreadHelper.run(runOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("BLUETOOTH--------------> ConnectThread ERROR deviceCallback null in progress 2 connected:" + connected);
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
            System.out.println("BLUETOOTH--------------> BroadcastReceiver scanReceiver connected:" + connected);
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
            System.out.println("BLUETOOTH--------------> BroadcastReceiverBLE pairReceiver connected:" + connected);
            Log.d("BLE", "PAIR RECEIVER");

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
            System.out.println("BLUETOOTH--------------> BroadcastReceiver bluetoothReceiver connected:" + connected);
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
        System.out.println("BLUETOOTH--------------> setDeviceCallback connected:" + connected);
        this.deviceCallback = deviceCallback;
    }

    public void setDeviceCallback2(DeviceCallback deviceCallback) {
        System.out.println("BLUETOOTH--------------> setDeviceCallback connected:" + connected);
        this.deviceCallback = deviceCallback;
    }

    public void enableParsing(ParserCallback parserCallback) {
        System.out.println("BLUETOOTH--------------> enableParsing connected:" + connected);
        this.parserCallback = parserCallback;
    }

    public void removeCommunicationCallback(){
        System.out.println("BLUETOOTH--------------> removeCommunicationCallback connected:" + connected);
        this.deviceCallback = null;
    }

    public void setDiscoveryCallback(DiscoveryCallback discoveryCallback){
        System.out.println("BLUETOOTH--------------> setDiscoveryCallback connected:" + connected);
        this.discoveryCallback = discoveryCallback;
    }

    public void removeDiscoveryCallback(){
        System.out.println("BLUETOOTH--------------> removeDiscoveryCallback connected:" + connected);
        this.discoveryCallback = null;
    }

    public void setBluetoothCallback(BluetoothCallback bluetoothCallback){
        System.out.println("BLUETOOTH--------------> BroadcastReceiverBLE connected:" + connected);
        this.bluetoothCallback = bluetoothCallback;
    }

    public void removeBluetoothCallback(){
        System.out.println("BLUETOOTH--------------> BroadcastReceiverBLE connected:" + connected);
        this.bluetoothCallback = null;
    }
}