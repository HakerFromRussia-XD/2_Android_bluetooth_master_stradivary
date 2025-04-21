package com.bailout.stickk.ubi4.ble;


import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static final HashMap<String, String> attributes = new HashMap();
    public static String MAIN_CHANNEL = "43680201-4d74-1001-726b-526f64696f6e";
    public static String A1_MAIN_CHANNEL = "0000fff4-0000-1000-8000-00805f9b34fb";

    // Sample Commands.
    public static Boolean SHOW_EVERYONE_RECEIVE_BYTE = false;
    public static String READ = "READ";
    public static String WRITE = "WRITE";
    public static String NOTIFY = "NOTIFY";

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
