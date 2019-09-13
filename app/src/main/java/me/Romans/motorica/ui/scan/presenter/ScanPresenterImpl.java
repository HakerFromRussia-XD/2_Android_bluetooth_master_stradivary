package me.Romans.motorica.ui.scan.presenter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import me.Romans.motorica.ui.scan.interactor.ScanInteractor;
import me.Romans.motorica.ui.scan.view.ScanView;
import me.Romans.bluetooth.BluetoothCallback;
import me.Romans.bluetooth.DiscoveryCallback;
import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.view.ChatActivity;

/**
 * Created by Omar on 20/12/2017.
 */

public class ScanPresenterImpl implements ScanPresenter{
    private ScanView view;
    private ScanInteractor interactor;
    private boolean canceledDiscovery = false;
    private boolean firstStart = true;

    public ScanPresenterImpl(ScanView view, ScanInteractor interactor) {
        this.view = view;
        this.interactor = interactor;
    }

    @Override
    public void onStart(Activity activity) {
        interactor.onStart(bluetoothCallback, activity);
        if(interactor.isBluetoothEnabled() && firstStart){
            startScanning();
            view.showPairedList(interactor.getPairedDevices());
            firstStart = false;
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
    public void startScanning() {
        view.clearScanList();
        view.showProgress(true);
        view.enableScanButton(false);
        view.setScanStatus(R.string.bluetooth_scanning);
        interactor.scanDevices(discoveryCallback);
        canceledDiscovery = false;
    }

    @Override
    public void scanItemClick(int position) {
        canceledDiscovery = true;
        interactor.stopScanning();
        interactor.pair(position);
        view.setScanStatus(R.string.bluetooth_pairing);
        view.showProgress(true);
    }

    @Override
    public void pairedItemClick(int position) {
        BluetoothDevice device = interactor.getPairedDevice(Integer.parseInt(interactor.getPairedDevices().get(position).split(" ")[0])-1);
        view.navigateToChat("device", device);

        if( device.getName().split("-")[0].equals("MLT") ||
            device.getName().split("-")[0].equals("FNG") ||
            device.getName().split("-")[0].equals("FNS") ||
            device.getName().split(" ")[0].equals("MLT") ||
            device.getName().split(" ")[0].equals("FNG") ||
            device.getName().split(" ")[0].equals("FNS")) {
            view.showToast("многосхватная версия");
        }
        if( device.getName().split("-")[0].equals("STR") ||
            device.getName().split("-")[0].equals("CBY") ||
            device.getName().split("-")[0].equals("HND") ||
            device.getName().split(" ")[0].equals("STR") ||
            device.getName().split(" ")[0].equals("CBY") ||
            device.getName().split(" ")[0].equals("HND") ||
            device.getName().equals("ASUS")){
            view.showToast("односхватная версия");
        }
    }

    private DiscoveryCallback discoveryCallback = new DiscoveryCallback() {
        @Override
        public void onDiscoveryStarted() {
            view.showToast("Discovery started");
        }

        @Override
        public void onDiscoveryFinished() {
            if(!canceledDiscovery){
                view.setScanStatus(R.string.bluetooth_scan_finished);
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
            view.setScanStatus(message);
        }
    };

    private BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override
        public void onBluetoothTurningOn() {
            view.setScanStatus(R.string.bluetooth_turning_on);
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
    };
}
