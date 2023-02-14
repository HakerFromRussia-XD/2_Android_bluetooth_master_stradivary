package me.start.motorica.scan.presenter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

import me.start.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity;
import me.start.motorica.scan.data.ScanItem;
import me.start.motorica.scan.interactor.ScanInteractor;
import me.start.motorica.scan.view.ScanView;
import me.start.bluetooth.BluetoothCallback;
import me.start.bluetooth.DiscoveryCallback;

@SuppressLint("MissingPermission")
public class ScanPresenterImpl implements ScanPresenter{
    private static final String TAG = "ScanPresenterImpl";
    private final ScanView view;
    private final ScanInteractor interactor;
    private Thread pauseStartThread;
    private Thread setStatusThread;
    private boolean canceledDiscovery = false;
    private boolean firstStart = true;
    private int checkDevicePosition;
    private boolean onPauseActivity = false;
    public int scanListPosition = 0;
    private boolean flagDiscovering = true;
    private boolean flagPairNewDevice = false;
    private final int position = 0;
    private int positionPairDevice = 0;
    private List<String> pairedDeviceNames;
    private ArrayList<ScanItem> pairedDevices;
    private List<String> TEST_scanDeviceNames;
    private boolean firstScanClick = true;
    private final boolean flagCheckingNow = false;

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
//            pairedDeviceNames = interactor.getPairedDevices(view.getFilteringOursDevices());
            pairedDevices = interactor.getPairedDevicesItem(view.getFilteringOursDevices());
            view.showPairedList(pairedDevices);
//            view.getMyScanList();
        }
        else{
            interactor.enableBluetooth();
        }
    }

    public ArrayList<ScanItem> getPairedList () {
        return interactor.getPairedDevicesItem(view.getFilteringOursDevices());
    }

    @Override
    public void onStop() {
        interactor.onStop();
        firstStart = true;
    }

    @Override
    public void startScanning() {
//        view.clearScanList();
        interactor.scanDevices(discoveryCallback);
        canceledDiscovery = false;
        scanListPosition = 0;
    }

    @Override
    public void scanItemClick(int position, String name) {
        System.err.println("TEST scanItemClick position: "+position);
        if(firstScanClick){//исключение многоразавого нажатия на отсканированные давайсы
            if(flagDiscovering){//для отсечения ошибки клика по сканлисту после завершения сканирования
                canceledDiscovery = true;
                flagPairNewDevice = true;
                interactor.stopScanning();
                positionPairDevice = position;
                interactor.pair(position);
            } else {
                canceledDiscovery = true;
                flagPairNewDevice = true;
                positionPairDevice = position;// функция interactor.pair(position); вызавается после завершения чека доступности текущего утройства
                if (!flagCheckingNow) { interactor.pair(position); }//или сразу, если все спаренные устройства уже проверенн
            }
            ChartActivity chatActivity = new ChartActivity();
            chatActivity.getName(name);
            firstScanClick = false;
        }
    }

    private void afterScanItemClick(int position){ interactor.pair(position); }

    @Override
    public void leItemClick(int position) {
        final BluetoothDevice device = view.getLeDevices().get(position);
        if (device == null) return;
        view.navigateToLEChart("device", device);
    }

    @Override
    public void pairedItemClick(int position) {
        if(pairedDeviceNames == null) {pairedDeviceNames = interactor.getPairedDevices(view.getFilteringOursDevices());}

        BluetoothDevice device = interactor.getPairedDevice(Integer.parseInt(pairedDeviceNames.get(position).split(":")[1])-1);
        ChartActivity chatActivity = new ChartActivity();
        chatActivity.getNameFromDevice(device);
        view.navigateToChart("device", device);
    }

    @Override
    public void disconnect(){
        interactor.disconnect();
    }


    private final DiscoveryCallback discoveryCallback = new DiscoveryCallback() {
        @Override
        public void onDiscoveryStarted() {
            flagDiscovering = true;//мой флаг, для отсечения ошибки клика по сканлисту после завершения сканирования
//            view.showToast("Discovery started");
        }

        @Override
        public void onDiscoveryFinished() {
            if(!canceledDiscovery){
//                view.showProgress(false);
//                view.enableScanButton(true);
                flagDiscovering = false;//мой флаг, для отсечения ошибки клика по сканлисту после завершения сканирования
            }
        }

        @Override
        public void onDeviceFound(BluetoothDevice device) {}

        @Override
        public void onDevicePaired(BluetoothDevice device) {
            view.navigateToChart("device", device);
        }

        @Override
        public void onDeviceUnpaired(BluetoothDevice device) { }

        @Override
        public void onError(String message) {
            view.setScanStatus(message, true);
        }
    };

    private final BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override
        public void onBluetoothTurningOn() {}

        @Override
        public void onBluetoothOn() { startScanning(); }

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
        public boolean getFirstRead() { return false; }
    };

    public void setOnPauseActivity(boolean onPauseActivity) {
        this.onPauseActivity = onPauseActivity;
    }

    @Override
    public int getOurGadgets() { return interactor.getOurGadgets(); }

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
        return deviceName.contains("MLT") ||
                deviceName.contains("FNG") ||
                deviceName.contains("FNS") ||
                deviceName.contains("MLX") ||
                deviceName.contains("FNX") ||
                deviceName.contains("STR") ||
                deviceName.contains("CBY") ||
                deviceName.contains("IND") ||
                deviceName.contains("HND") ||
                deviceName.contains("NEMO") ||
                deviceName.contains("STAND") ||
                deviceName.contains("FEST");
    }
}
