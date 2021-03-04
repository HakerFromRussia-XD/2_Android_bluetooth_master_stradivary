package me.romans.motorica.new_electronic_by_Rodeon.ble;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;

public interface ConstantManager {

    boolean SHOW_EVERYONE_RECEIVE_BYTE = false;
    String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    //TODO тут настраивается имя модуля, для подключения нового протокола
    String EXTRAS_DEVICE_TYPE = "BT05";
    ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    ////////////////////////////////////////////////
/**                     delays                      **/
    ////////////////////////////////////////////////
    int GRAPH_UPDATE_DELAY  = 50;
}
