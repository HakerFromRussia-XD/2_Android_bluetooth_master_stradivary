package me.start.motorica.scan.interactor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

import me.start.bluetooth.Bluetooth;
import me.start.bluetooth.BluetoothCallback;
import me.start.bluetooth.DeviceCallback;
import me.start.bluetooth.DiscoveryCallback;
import me.start.motorica.scan.data.ScanItem;


public class ScanInteractorImpl implements ScanInteractor {
    private final Bluetooth bluetooth;
    private DiscoveryCallback presenterDiscoveryCallback;
    private List<BluetoothDevice> discoveredDevices;
    private int ourGadgets = 0;

    public ScanInteractorImpl(Bluetooth bluetooth) {
        this.bluetooth = bluetooth;
        this.bluetooth.setDiscoveryCallback(new DiscoveryCallback() {
            @Override
            public void onDiscoveryStarted() {
                presenterDiscoveryCallback.onDiscoveryStarted();
            }

            @Override
            public void onDiscoveryFinished() {
                presenterDiscoveryCallback.onDiscoveryFinished();
            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {
                presenterDiscoveryCallback.onDeviceFound(device);
                discoveredDevices.add(device);
            }

            @Override
            public void onDevicePaired(BluetoothDevice device) {
                presenterDiscoveryCallback.onDevicePaired(device);
            }

            @Override
            public void onDeviceUnpaired(BluetoothDevice device) {
                presenterDiscoveryCallback.onDeviceUnpaired(device);
            }

            @Override
            public void onError(String message) {
                presenterDiscoveryCallback.onError(message);
            }
        });
    }

    @Override
    public void onStart(BluetoothCallback bluetoothCallback, Activity activity) {
        this.bluetooth.onStart();
        this.bluetooth.setCallbackOnUI(activity);
        this.bluetooth.setBluetoothCallback(bluetoothCallback);
    }

    @Override
    public void onStop() {
        this.bluetooth.onStop();
    }

    @Override
    public void checkAvailableDevice(BluetoothDevice device, DeviceCallback callback) {
        bluetooth.setDeviceCallback(callback);
        bluetooth.checkConnectToDevice(device);
    }

    @Override
    public boolean isBluetoothEnabled() {
        return bluetooth.isEnabled();
    }

    @Override
    public void enableBluetooth() {
        bluetooth.enable();
    }

    @SuppressLint("MissingPermission")
    @Override
    public List<String> getPairedDevices(boolean filteringOursDevices) {
        List<String> items = new ArrayList<>();
        int position = 0;
        ourGadgets = 0;
        for(BluetoothDevice device : bluetooth.getPairedDevices()){
            if(
                    device.getName().contains("MLT") ||
                    device.getName().contains("FNG") ||
                    device.getName().contains("FNS") ||
                    device.getName().contains("MLX") ||
                    device.getName().contains("FNX") ||
                    device.getName().contains("STR") ||
                    device.getName().contains("CBY") ||
                    device.getName().contains("IND") ||
                    device.getName().contains("HND") ||
                    device.getName().contains("NEMO") ||
                    device.getName().contains("STAND") ||
                    device.getName().contains("FEST") ||
                    !filteringOursDevices
            ){
//                System.err.println("ScanInteractorImpl--------------> "+device+" "+device.getName()+":"+(position+1));
                items.add(device.getName()+":"+(position+1));
                ourGadgets++;
            } else {
//                System.err.println("ScanInteractorImpl--------------> "+device+" "+device.getName()+":"+(position+1));
                items.add(".");
            }
            position++;
        }
        for (int i = 0; i < items.size(); i++) {
            if(items.get(i).equals(".") || items.get(i).equals(".\n") || items.get(i).equals(".\r") || items.get(i).equals(".\n\r") || items.get(i).equals(".\r\n")){
//                System.err.println("ScanInteractorImpl--------------> remove: position="+(i+1));
                items.remove(i);
                i--;
            }
        }
        return items;
    }

    @SuppressLint("MissingPermission")
    @Override
    public ArrayList<ScanItem> getPairedDevicesItem(boolean filteringOursDevices) {
        ArrayList<ScanItem> items = new ArrayList<>();
        int position = 0;
        ourGadgets = 0;
        for(BluetoothDevice device : bluetooth.getPairedDevices()){
            if(
                    device.getName().contains("MLT") ||
                    device.getName().contains("FNG") ||
                    device.getName().contains("FNS") ||
                    device.getName().contains("MLX") ||
                    device.getName().contains("FNX") ||
                    device.getName().contains("STR") ||
                    device.getName().contains("CBY") ||
                    device.getName().contains("IND") ||
                    device.getName().contains("HND") ||
                    device.getName().contains("NEMO") ||
                    device.getName().contains("STAND") ||
                    device.getName().contains("FEST") ||
                    !filteringOursDevices
            ){
//                System.err.println("ScanInteractorImpl--------------> "+device+" "+device.getName()+":"+(position+1));
                items.add(position,
                        new ScanItem(
                                device.getName(),
                                device.getAddress(),
                                (position+1),
                    true));
                ourGadgets++;
            } else {
//                System.err.println("ScanInteractorImpl--------------> "+device+" "+device.getName()+":"+(position+1));
                items.add(position,
                        new ScanItem(
                                ".",
                                ".",
                                (position+1),
                                true));
            }
            position++;
        }
        for (int i = 0; i < items.size(); i++) {
            if(items.get(i).getTitle().equals(".") || items.get(i).getTitle().equals(".\n") || items.get(i).getTitle().equals(".\r") || items.get(i).getTitle().equals(".\n\r") || items.get(i).equals(".\r\n")){
//                System.err.println("ScanInteractorImpl--------------> remove: position="+items.get(i).getTitle()+" "+(i+1));
                items.remove(i);
                i--;
            }
        }
        return items;
    }

    @Override
    public void pair(int position) {
        bluetooth.pair(discoveredDevices.get(position));
    }

    @Override
    public void scanDevices(DiscoveryCallback callback) {
        presenterDiscoveryCallback = callback;
        discoveredDevices = new ArrayList<>();
        bluetooth.startScanning();
    }

    @Override
    public void stopScanning() {
        bluetooth.stopScanning();
    }

    @Override
    public BluetoothDevice getPairedDevice(int position) {
        if(position<bluetooth.getPairedDevices().size()){
            return bluetooth.getPairedDevices().get(position);
        }
        return null;
    }

    @Override
    public void disconnect(){
        bluetooth.disconnect();
    }

    public int getOurGadgets() {
        return ourGadgets;
    }
}
