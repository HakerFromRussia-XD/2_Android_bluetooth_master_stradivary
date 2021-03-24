package me.romans.motorica.new_electronic_by_Rodeon.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import java.util.ArrayList;

public interface ConstantManager {

    boolean SHOW_EVERYONE_RECEIVE_BYTE = false;
    String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    //TODO тут настраивается имена модулей, которые будут работаь с навеесным блютуз модулем(BT05 поумолчанию)
    String EXTRAS_DEVICE_TYPE = "FEST-A";
    String EXTRAS_DEVICE_TYPE_2 = "BT05";
    String EXTRAS_DEVICE_TYPE_3 = "BLE_tesst_service—•—";

    ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    ////////////////////////////////////////////////
/**                     delays                      **/
    ////////////////////////////////////////////////
    int GRAPH_UPDATE_DELAY  = 50;
}
