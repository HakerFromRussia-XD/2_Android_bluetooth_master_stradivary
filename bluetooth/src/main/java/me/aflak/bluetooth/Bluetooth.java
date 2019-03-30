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

    private boolean runOnUi;

    public Bluetooth(Context context){
        initialize(context, UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
    }

    public Bluetooth(Context context, UUID uuid){
        initialize(context, uuid);
    }

    private void initialize(Context context, UUID uuid){
        this.context = context;
        this.uuid = uuid;
        this.deviceCallback = null;
        this.discoveryCallback = null;
        this.bluetoothCallback = null;
        this.connected = false;
        this.runOnUi = false;
    }

    public void onStart(){
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
        if(bluetoothAdapter!=null) {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        }
    }

    public void disable(){
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
        if(bluetoothAdapter!=null) {
            return bluetoothAdapter.isEnabled();
        }
        return false;
    }

    public void setCallbackOnUI(Activity activity){
        this.activity = activity;
        this.runOnUi = true;
    }

    public void onActivityResult(int requestCode, final int resultCode){
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
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        connectToDevice(device, insecureConnection);
    }
    
    public void connectToAddress(String address) {
        connectToAddress(address, false);
    }
    
    public void connectToName(String name, boolean insecureConnection) {
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
        new ConnectThread(device, insecureConnection).start();
    }
    
    public void connectToDevice(BluetoothDevice device){
        connectToDevice(device, false);
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (final IOException e) {
            if(deviceCallback !=null) {
                ThreadHelper.run(runOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        deviceCallback.onError(e.getMessage());
                    }
                });
            }
        }
    }

    public boolean isConnected(){
        return connected;
    }

    public void send(byte[] msg, String charset){
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
                        deviceCallback.onDeviceDisconnected(device, e.getMessage());
                    }
                });
            }
        }
    }

    public void sendstr(String msgstr, String charset){
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
        send(msg, null);
    }

    public void sendstr(String msgstr){
        sendstr(msgstr, "US-ASCII");
    }

    public List<BluetoothDevice> getPairedDevices(){
        return new ArrayList<>(bluetoothAdapter.getBondedDevices());
    }

    public void startScanning(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        context.registerReceiver(scanReceiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    public void stopScanning(){
        context.unregisterReceiver(scanReceiver);
        bluetoothAdapter.cancelDiscovery();
    }

    public void pair(BluetoothDevice device){
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
                        discoveryCallback.onError(e.getMessage());
                    }
                });
            }
        }
    }

    public void unpair(BluetoothDevice device) {
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
        private int msgRegtster = 0;  //для свапа младших и старших байт номера регистра
        private int msgChannel = 0;   //для номера канала
        private int msgLevelCH = 1250;
        private int lowByte = 0;     //для записи младшего байта при перемене младших и старших байт
        private int i=1;
        private boolean request = true;                //true-ответ false-запрос
        public boolean no_error = true;                //true-нет ошибок false-есть ошибки
        private boolean msgCorrectAcceptance = true;   //true-безошиобочная CRC false-шибочная CRC
        private StringBuffer msgstr = new StringBuffer();
        private byte[] txtbyteout = {0x01, 0x02} ;
        public void run(){
            try {
                while((msg = input.read()) != -1) //((System.in).read(msg)) //((System.in).read(msg))   //((input.read())) != -1
                {
                    if((i == 1)||(i == 2)||((i == (8+msgLenght)))){
                        summator += msg;
                        if (summator == 197){
                            msgCorrectAcceptance = true;
                            System.out.println("<-- Принята посылка :)");
                        } else {
                            if(((i == (8+msgLenght))&&(msg != 36))||((i == 1)&&(msg != 77))||((i == 2)&&(msg != 84))){
                                System.out.println("<-- Пришла лажа :(");
                                System.out.println("<-- summator:"+summator);
                                no_error = false;
                                msgstr.setLength(0);
                                msgLenght = 0;
                                msgRegtster = 0;
                                summator = 0;
                                i = 1;
                                msgCorrectAcceptance = false;
                            }
                        }
                    }

                    if(i == 3){
                        lowByte = msg;
                    }
                    if(i == 4){
                        msgLenght = (msg << 8) + lowByte; //msgLenght содержит количество байт данных в посылке
                        System.out.println("<-- длина строки:"+msgLenght);
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
                        msgRegtster = (msg << 8) + lowByte;  //msgRegtster содержит номер регистра
                        System.out.println("<-- номер регистра:"+msgRegtster);
                    }
                    if((i >= 8)&&(i <=(msgLenght+7))){
                        System.out.println("<-- считывание данных:" + msg);
                        if(msg == 36){
                            System.out.println("<-- Пришла лажа :((");
                            no_error = false;
                        } else {
                            if(i == 8){
                                lowByte = msg;
                            }
                            if(i == 9){
                                msgChannel = (msg << 8) + lowByte;
                                System.out.println("<-- номер канала:"+msgChannel);
                            }
                            switch (msgChannel){
                                case 1:
                                    if(i == 10){
                                        lowByte = msg;
                                    }
                                    if(i == 11){
                                        msgLevelCH = (msg << 8) + lowByte; //msgLevelCH уровень канала 1
                                        System.out.println("<-- уровень CH1:"+msgLevelCH);
                                    }
                                    break;
                                case 2:
                                    if(i == 10){
                                        lowByte = msg;
                                    }
                                    if(i == 11){
                                        msgLevelCH = (msg << 8) + lowByte; //msgLevelCH уровень канала 1
                                        System.out.println("<-- уровень CH1:"+msgLevelCH);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    if(no_error) {
                        i++;
                        msgstr.append((char)msg);
                    }
                    if(i == (9+msgLenght)) {
                        //System.out.println("lenght:"+msgstr.length()+" ОБНУЛЕНИЕ i="+i);
                    }
                    if(i > (msgLenght+9)){
                        //System.out.println("------> i=" +i+" msgLenght="+msgLenght);
                        msgstr.setLength(0);
                        no_error = true;
                        msgLenght = 0;
                        msgRegtster = 0;
                        summator = 0;
                        i=1;
                    }
                    if(((deviceCallback != null) && (msg == 36))||(!no_error)){
                        final String msgCopy = String.valueOf(msgstr);
                        final Integer msgLenghtf = msgLenght;
                        final Boolean requestf = request;
                        final Integer msgRegtsterf = msgRegtster;
                        final Integer msgChannelf = msgChannel;
                        final Integer msgLevelCHf = msgLevelCH;
                        final Boolean msgCorrectAcceptancef = msgCorrectAcceptance;
                        ThreadHelper.run(runOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                if(no_error && msgCorrectAcceptance) {
                                    parserCallback.givsLenhgt(msgLenghtf);
                                    parserCallback.givsChannel(msgChannelf);
                                    parserCallback.givsRequest(requestf);
                                    parserCallback.givsRegister(msgRegtsterf);
                                    parserCallback.givsLevelCH(msgLevelCHf, msgChannelf);
                                    deviceCallback.onMessage(msgCopy);
                                    System.out.println("<-- сделал цикл:"+ msgCopy);
                                }
                                parserCallback.givsCorrectAcceptance(msgCorrectAcceptancef);
                                msgstr.setLength(0);
                                no_error = true;
                                msgLenght = 0;
                                msgRegtster = 0;
                                summator = 0;
                                i=1;
                            }
                        });
                    }
                }

            } catch (final IOException e) {
                connected=false;
                if(deviceCallback != null){
                    ThreadHelper.run(runOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            deviceCallback.onDeviceDisconnected(device, e.getMessage());
                        }
                    });
                }
            }
        }
    }

    private class ConnectThread extends Thread {    
        ConnectThread(BluetoothDevice device, boolean insecureConnection) {
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
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
                out = socket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "ISO-8859-1"));
                in = new InputStreamReader(socket.getInputStream());
                connected=true;

                new ReceiveThread().start();

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
                            deviceCallback.onConnectError(device, e.getMessage());
                        }
                    });
                }

                try {
                    socket.close();
                } catch (final IOException closeException) {
                    if (deviceCallback != null) {
                        ThreadHelper.run(runOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                deviceCallback.onError(closeException.getMessage());
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
        this.deviceCallback = deviceCallback;
    }

    public void enableParsing(ParserCallback parserCallback) {
        this.parserCallback = parserCallback;
    }

    public void removeCommunicationCallback(){
        this.deviceCallback = null;
    }

    public void setDiscoveryCallback(DiscoveryCallback discoveryCallback){
        this.discoveryCallback = discoveryCallback;
    }

    public void removeDiscoveryCallback(){
        this.discoveryCallback = null;
    }

    public void setBluetoothCallback(BluetoothCallback bluetoothCallback){
        this.bluetoothCallback = bluetoothCallback;
    }

    public void removeBluetoothCallback(){
        this.bluetoothCallback = null;
    }
}