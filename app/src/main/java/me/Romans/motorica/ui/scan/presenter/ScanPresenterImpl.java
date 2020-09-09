package me.Romans.motorica.ui.scan.presenter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.List;

import me.Romans.bluetooth.DeviceCallback;
import me.Romans.motorica.ui.chat.view.ChartActivity;
import me.Romans.motorica.ui.scan.data.ScanItem;
import me.Romans.motorica.ui.scan.interactor.ScanInteractor;
import me.Romans.motorica.ui.scan.view.ScanView;
import me.Romans.bluetooth.BluetoothCallback;
import me.Romans.bluetooth.DiscoveryCallback;
import me.Romans.motorica.R;


/**
 * Created by Omar on 20/12/2017.
 */

public class ScanPresenterImpl implements ScanPresenter{
    private static final String TAG = "ScanPresenterImpl";
    private ScanView view;
    private ScanInteractor interactor;
    private Thread pauseStartThread;
    private Thread setStatusThread;
    private boolean canceledDiscovery = false;
    private boolean firstStart = true;
    private int checkDevicePosition;
    private boolean onPauseActivity = false;
    public int scanListPosition = 0;
    private boolean flagDiscovering = true;
    private boolean flagPairNewDevice = false;
    private int position = 0;
    private int positionPairDevice = 0;
    private List<String> pairedDeviceNames;
    private boolean firstScanClick = true;
    private boolean flagCheckingNow = false;

    public ScanPresenterImpl(ScanView view, ScanInteractor interactor) {
        this.view = view;
        this.interactor = interactor;
    }

    @Override
    public void onStart(Activity activity) {
        interactor.onStart(bluetoothCallback, activity);
        checkDevicePosition = 0;
        positionPairDevice = 0;
        if(interactor.isBluetoothEnabled()){
            startScanning();
            pairedDeviceNames = interactor.getPairedDevices();
            view.showPairedList(pairedDeviceNames);
            pauseCheckDevicesThread(5000);
        }
        else{
            interactor.enableBluetooth();
        }
    }

    @Override
    public void onStop() {
        interactor.onStop();
        view.clearPairedList();
        firstStart = true;
    }

    @Override
    public void startScanning() {
        view.clearScanList();
        view.showProgress(true);
        view.enableScanButton(false);
        view.setScanStatus(R.string.bluetooth_scanning, true);
        interactor.scanDevices(discoveryCallback);
        canceledDiscovery = false;
        scanListPosition = 0;
    }

    @Override
    public void scanItemClick(int position, String name) {
        if(firstScanClick){//исключение многоразавого нажатия на отсканированные давайсы
            if(flagDiscovering){//для отсечения ошибки клика по сканлисту после завершения сканирования
                canceledDiscovery = true;
                flagPairNewDevice = true;
                interactor.stopScanning();
                positionPairDevice = position;
                interactor.pair(position);
                view.setScanStatus(R.string.bluetooth_pairing, true);
                view.showProgress(true);
                view.enableScanButton(false);
            } else {
                canceledDiscovery = true;
                flagPairNewDevice = true;
                positionPairDevice = position;// функция interactor.pair(position); вызавается после завершения чека доступности текущего утройства
                if (!flagCheckingNow) {interactor.pair(position);}//или сразу, если все спаренные устройства уже проверенны
                view.setScanStatus(R.string.bluetooth_pairing, true);
                view.showProgress(true);
                view.enableScanButton(false);
            }
            ChartActivity chatActivity = new ChartActivity();
            chatActivity.getName(name);
            firstScanClick = false;
        }
    }

    private void afterScanItemClick(int position){
        interactor.pair(position);
    }

    @Override
    public void pairedItemClick(int position) {
        if(pairedDeviceNames == null) {pairedDeviceNames = interactor.getPairedDevices();}
        BluetoothDevice device = interactor.getPairedDevice(Integer.parseInt(pairedDeviceNames.get(position).split(":")[2])-1);
        ChartActivity chatActivity = new ChartActivity();
        chatActivity.getNameFromDevice(device);
        view.navigateToChat("device", device);
    }

    @Override
    public void itemClick(int position) {
        if (view.getMyScanList() != null){
            String typeDevice = view.getMyScanList().get(position).getTitle().split(":")[1];
            if(typeDevice.equals("p")){
                pairedItemClick(position);
            } else {
                if(typeDevice.equals("s")){
                    scanItemClick(Integer.parseInt(view.getMyScanList().get(position).getTitle().split(":")[2]), view.getMyScanList().get(position).getTitle().split(":")[0]);
                }
            }
        }
    }

    @Override
    public void disconnect(){
        interactor.disconnect();
    }


    private DeviceCallback communicationCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            flagCheckingNow = false;
            interactor.disconnect();
            if(!onPauseActivity){
                if(flagPairNewDevice){
                    afterScanItemClick(positionPairDevice);
                    flagPairNewDevice = false;
                } else {
                    checkDevices();
                }
            }
            String deviceName = device.getName();
            for(int i = 0; i < pairedDeviceNames.size(); i++) {
                if(deviceName.equals(pairedDeviceNames.get(i).split(":")[0])){
                    if(!onPauseActivity){view.setNewStageCellScanList(i,R.drawable.circle_16_green, pairedDeviceNames.get(i));}
                }
            }
        }

        @Override
        public void onDeviceDisconnected(final BluetoothDevice device, String message) {
            System.err.println("ScanPresenter--------------> onDeviceCheckDisconnected "+message+" "+device);
        }

        @Override
        public void onMessage(String message) {
            System.err.println("ScanPresenter--------------> onMessage "+message);
        }

        @Override
        public void onError(String message) {
            System.err.println("ScanPresenter--------------> onError "+message);
        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            flagCheckingNow = false;
            if(!onPauseActivity){
                if(flagPairNewDevice){
                    afterScanItemClick(positionPairDevice);
                    flagPairNewDevice = false;
                } else {
                    checkDevices();
                }
            }
            String deviceName = device.getName();
            for(int i = 0; i < pairedDeviceNames.size(); i++) {
                if(deviceName.equals(pairedDeviceNames.get(i).split(":")[0])){
                    if(!onPauseActivity){view.setNewStageCellScanList(i,R.drawable.circle_16_red, pairedDeviceNames.get(i));}
                }
            }
        }
    };

    private DiscoveryCallback discoveryCallback = new DiscoveryCallback() {
        @Override
        public void onDiscoveryStarted() {
            flagDiscovering = true;//мой флаг, для отсечения ошибки клика по сканлисту после завершения сканирования
//            view.showToast("Discovery started");
        }

        @Override
        public void onDiscoveryFinished() {
            if(!canceledDiscovery){
                view.setScanStatus(R.string.bluetooth_scan_finished, false);
                view.showProgress(false);
                view.enableScanButton(true);
                flagDiscovering = false;//мой флаг, для отсечения ошибки клика по сканлисту после завершения сканирования
            }
        }

        @Override
        public void onDeviceFound(BluetoothDevice device) {
            boolean check = checkOurName(device.getName()+":s:"+scanListPosition);

            if(check){//проверяет соответствие серийника найденного устройства соответствию нашим серийникам
                boolean equals = false;
                if(pairedDeviceNames == null) {pairedDeviceNames=interactor.getPairedDevices();}
                for(int i=0; i<pairedDeviceNames.size(); i++){//меняет флаг если наше устройство уже находится в списке спаренных
                    if(pairedDeviceNames.get(i).split(":")[0].equals(device.getName())){
                        equals = true;
                    }
                }
                if(equals){//это условие проверяет есть ли найденное устройство в списке спаренных
                    equals = false;
                } else {
                    if(device.getType() == 1){//тип один - компьютеры и телефоны, тип - 2 вякая хуйня и те профили в протезах, которые мы хотим отсечь
                        //TODO организавать проверку на совпадение имён в скан листе
                        List<ScanItem> scanItemList = view.getMyScanList();
                        boolean canAdd = true;
                        for(int i=0; i<scanItemList.size(); i++){//проверяет есть ли в списке отсканированных устройств вновь найденное
                            if(scanItemList.get(i).getTitle().split(":")[1].equals("s")){
                                if(scanItemList.get(i).getTitle().split(":")[0].equals(device.getName())){
                                    canAdd = false;
                                }
                            }
                        }
                        if(canAdd){
                            view.addDeviceToScanList(device.getName()+":s:"+scanListPosition, device);
                        } else {
                            canAdd = true;
                        }
                    }
                }
            }
            scanListPosition++;
        }

        @Override
        public void onDevicePaired(BluetoothDevice device) {
            view.navigateToChat("device", device);
        }

        @Override
        public void onDeviceUnpaired(BluetoothDevice device) {
        }

        @Override
        public void onError(String message) {
            view.setScanStatus(message, true);
        }
    };

    private BluetoothCallback bluetoothCallback = new BluetoothCallback() {

        @Override
        public void onBluetoothTurningOn() {
            view.setScanStatus(R.string.bluetooth_turning_on, true);
        }

        @Override
        public void onBluetoothOn() {
            startScanning();
            view.showPairedList(interactor.getPairedDevices());
        }

        @Override
        public void onBluetoothTurningOff() {
            interactor.stopScanning();
            view.showToast("You need to enable your bluetooth...");
        }

        @Override
        public void onBluetoothOff() {
        }

        @Override
        public void onUserDeniedActivation() {
        }



        @Override
        public boolean getFirstRead() {
            return false;
        }
    };

    public void setOnPauseActivity(boolean onPauseActivity) {
        this.onPauseActivity = onPauseActivity;
    }

    @Override
    public int getOurGadgets() {
        return interactor.getOurGadgets();
    }

    public void setStartFlags (String deviceName){
        if(     deviceName.split("-")[0].equals("MLT") ||
                deviceName.split("-")[0].equals("FNG") ||
                deviceName.split("-")[0].equals("FNS") ||
                deviceName.split(" ")[0].equals("MLT") ||
                deviceName.split(" ")[0].equals("FNG") ||
                deviceName.split(" ")[0].equals("FNS")) {
            ChartActivity.monograbVersion = false; //false - многосхват
//            chatActivity.flagUseHDLCProcol = true; //удалить это вот для отладки надо, чтобы не переименовывать комп
        }
        if(     deviceName.split("-")[0].equals("STR") ||
                deviceName.split("-")[0].equals("CBY") ||
                deviceName.split("-")[0].equals("HND") ||
                deviceName.split(" ")[0].equals("STR") ||
                deviceName.split(" ")[0].equals("CBY") ||
                deviceName.split(" ")[0].equals("HND")){
            ChartActivity.monograbVersion = true; //true - односхват
//            chatActivity.flagUseHDLCProcol = true;
        }
        if(     deviceName.split("-")[0].equals("IND") ||
                deviceName.split("-")[0].equals(" IND") ||
                deviceName.split(" ")[0].equals("IND")){
            ChartActivity.monograbVersion = true; //true - односхват
            ChartActivity.flagUseHDLCProtocol = true; //true - при использовании протокола hdlc
        }
        if(     deviceName.split("-")[0].equals("MLX") ||
                deviceName.split("-")[0].equals(" MLX")||
                deviceName.split(" ")[0].equals("MLX") ||
                deviceName.split("-")[0].equals("FNX") ||
                deviceName.split("-")[0].equals(" FNX")||
                deviceName.split(" ")[0].equals("FNX")
        ){//device.getName().split(" ")[0].equals("MacBook")
            ChartActivity.monograbVersion = false; //false - многосхват
            ChartActivity.flagUseHDLCProtocol = true; //true - при использовании протокола hdlc
        }
    }

    private boolean checkOurName (String deviceName){
        if(     deviceName.split("-")[0].equals("MLT") ||
                deviceName.split("-")[0].equals("FNG") ||
                deviceName.split("-")[0].equals("FNS") ||
                deviceName.split(" ")[0].equals("MLT") ||
                deviceName.split(" ")[0].equals("FNG") ||
                deviceName.split(" ")[0].equals("FNS") ||
                deviceName.split("-")[0].equals("STR") ||
                deviceName.split("-")[0].equals("CBY") ||
                deviceName.split("-")[0].equals("HND") ||
                deviceName.split(" ")[0].equals("STR") ||
                deviceName.split(" ")[0].equals("CBY") ||
                deviceName.split(" ")[0].equals("HND") ||
                deviceName.split("-")[0].equals("IND") ||
                deviceName.split("-")[0].equals(" IND")||
                deviceName.split(" ")[0].equals("IND") ||
                deviceName.split("-")[0].equals("MLX") ||
                deviceName.split("-")[0].equals(" MLX")||
                deviceName.split(" ")[0].equals("MLX") ||
                deviceName.split("-")[0].equals("FNX") ||
                deviceName.split("-")[0].equals(" FNX")||
                deviceName.split(" ")[0].equals("FNX")) {
            return true;
        } else {
            return false;
        }
    }

    private void checkDevices(){
        BluetoothDevice device = interactor.getPairedDevice(checkDevicePosition);
        if (device != null) {
            if(checkOurName(device.getName())){
                flagCheckingNow = true;
                interactor.checkAvailableDevice(device, communicationCallback);
                checkDevicePosition++;
            } else {
                checkDevicePosition++;
                checkDevices();
            }
        }
    }

    public void pauseCheckDevicesThread (final int pauseTime) {
        pauseStartThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(pauseTime);
                }catch (Exception ignored){}
                checkDevices();
            }
        });
        pauseStartThread.start();
    }
}
