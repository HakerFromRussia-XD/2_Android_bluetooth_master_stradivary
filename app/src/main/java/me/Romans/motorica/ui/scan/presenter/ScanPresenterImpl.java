package me.Romans.motorica.ui.scan.presenter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import java.util.List;

import me.Romans.bluetooth.DeviceCallback;
import me.Romans.motorica.ui.chat.view.ChartActivity;
import me.Romans.motorica.ui.scan.interactor.ScanInteractor;
import me.Romans.motorica.ui.scan.view.ScanView;
import me.Romans.bluetooth.BluetoothCallback;
import me.Romans.bluetooth.DiscoveryCallback;
import me.Romans.motorica.R;

/**
 * Created by Omar on 20/12/2017.
 */

public class ScanPresenterImpl implements ScanPresenter{
    private ScanView view;
    private ScanInteractor interactor;
    private boolean canceledDiscovery = false;
    private boolean firstStart = true;
    private int checkDevicePosition = 0;

    public ScanPresenterImpl(ScanView view, ScanInteractor interactor) {
        this.view = view;
        this.interactor = interactor;
    }

    @Override
    public void onStart(Activity activity) {
        interactor.onStart(bluetoothCallback, activity);
        if(interactor.isBluetoothEnabled()){
            BluetoothDevice device = interactor.getPairedDevice(checkDevicePosition);
            interactor.checkAvailableDevice(device, communicationCallback);
            if(firstStart){
                startScanning();
                view.showPairedList(interactor.getPairedDevices());
                firstStart = false;
            }
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
    }

    @Override
    public void scanItemClick(int position) {
        canceledDiscovery = true;
        interactor.stopScanning();
        interactor.pair(position);
        view.setScanStatus(R.string.bluetooth_pairing, true);
        view.showProgress(true);
    }

    @Override
    public void pairedItemClick(int position) {
        BluetoothDevice device = interactor.getPairedDevice(Integer.parseInt(interactor.getPairedDevices().get(position).split(":")[1])-1);
        ChartActivity chatActivity = new ChartActivity();
        chatActivity.GetPosition_My(device);
        view.navigateToChat("device", device);
        if( device.getName().split("-")[0].equals("MLT") ||
            device.getName().split("-")[0].equals("FNG") ||
            device.getName().split("-")[0].equals("FNS") ||
            device.getName().split(" ")[0].equals("MLT") ||
            device.getName().split(" ")[0].equals("FNG") ||
            device.getName().split(" ")[0].equals("FNS")) {
            ChartActivity.monograbVersion = false; //false - многосхват
//            chatActivity.flagUseHDLCProcol = true; //удалить это вот для отладки надо, чтобы не переименовывать комп
        }
        if( device.getName().split("-")[0].equals("STR") ||
            device.getName().split("-")[0].equals("CBY") ||
            device.getName().split("-")[0].equals("HND") ||
            device.getName().split(" ")[0].equals("STR") ||
            device.getName().split(" ")[0].equals("CBY") ||
            device.getName().split(" ")[0].equals("HND")){
            ChartActivity.monograbVersion = true; //true - односхват
//            chatActivity.flagUseHDLCProcol = true;
        }
        if( device.getName().split("-")[0].equals("IND") ||
            device.getName().split("-")[0].equals(" IND") ||
            device.getName().split(" ")[0].equals("IND")){
            ChartActivity.monograbVersion = true; //true - односхват
            ChartActivity.flagUseHDLCProtocol = true; //true - при использовании протокола hdlc
        }
        if( device.getName().split("-")[0].equals("MLX") ||
            device.getName().split("-")[0].equals(" MLX") ||
            device.getName().split(" ")[0].equals("MLX") ||
            device.getName().split("-")[0].equals("FNX") ||
            device.getName().split("-")[0].equals(" FNX") ||
            device.getName().split(" ")[0].equals("FNX") ||
            device.getName().split(" ")[0].equals("MacBook")){
            ChartActivity.monograbVersion = false; //false - многосхват
            ChartActivity.flagUseHDLCProtocol = true; //true - при использовании протокола hdlc
        }
    }

    @Override
    public void disconnect(){
        interactor.disconnect();
    }


    private DeviceCallback communicationCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            System.out.println("ScanPresenter--------------> onDeviceCheckConnected" + device);
            interactor.disconnect();
            BluetoothDevice nextDevice = interactor.getPairedDevice(checkDevicePosition++);
            if (nextDevice == null) {
                checkDevicePosition = 0;
                nextDevice = interactor.getPairedDevice(checkDevicePosition);
            }
            interactor.checkAvailableDevice(nextDevice, communicationCallback);
        }

        @Override
        public void onDeviceDisconnected(final BluetoothDevice device, String message) {
            System.out.println("ScanPresenter--------------> onDeviceCheckDisconnected");
        }

        @Override
        public void onMessage(String message) {
        }

        @Override
        public void onError(String message) {
        }

        @Override
        public void onConnectError(final BluetoothDevice device, String message) {
            System.out.println("ScanPresenter--------------> ta samaya oshibka " + device);
            interactor.disconnect();
            BluetoothDevice nextDevice = interactor.getPairedDevice(checkDevicePosition++);
            if (nextDevice == null) {
                checkDevicePosition = 0;
                nextDevice = interactor.getPairedDevice(checkDevicePosition);
            }
            interactor.checkAvailableDevice(nextDevice, communicationCallback);
        }
    };

    private DiscoveryCallback discoveryCallback = new DiscoveryCallback() {
        @Override
        public void onDiscoveryStarted() {
            view.showToast("Discovery started");
        }

        @Override
        public void onDiscoveryFinished() {
            if(!canceledDiscovery){
                view.setScanStatus(R.string.bluetooth_scan_finished, false);
                view.showProgress(false);
                view.enableScanButton(true);
            }
        }

        @Override
        public void onDeviceFound(BluetoothDevice device) {
            view.addDeviceToScanList(device.getAddress()+" : "+device.getName());
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
}
