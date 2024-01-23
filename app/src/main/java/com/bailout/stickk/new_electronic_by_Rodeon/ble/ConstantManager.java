package com.bailout.stickk.new_electronic_by_Rodeon.ble;

public interface ConstantManager {

    boolean SHOW_EVERYONE_RECEIVE_BYTE = false;
    String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    String DEVICE_NAME = "";
    String EXTRAS_DEVICE_TYPE_FEST_A = "FEST-A";
    String EXTRAS_DEVICE_TYPE_BT05 = "BT05";
    String EXTRAS_DEVICE_TYPE_MY_IPHONE = "BLE_test_services—•";
    String DEVICE_TYPE_FEST_H = "FEST-H";
    String DEVICE_TYPE_FEST_X = "FEST-X";
    String DEVICE_TYPE_FEST_TEST = "TEST";


    int REQUEST_ENABLE_BT = 1;

    byte[] READ_REGISTER = {0x00}; // просто заглушка для того чтобы функция отправки команды при чтении смотрелась красиво

    ////////////////////////////////////////////////
/* *                      3D                         * */
    ////////////////////////////////////////////////
    String MODEDEL_0="STR2/STR2_big_finger_part18.obj";
    String MODEDEL_1="STR2/STR2_big_finger_part19.obj";
    String MODEDEL_2="STR2/STR2_big_finger_part1.obj";
    String MODEDEL_3="STR2/STR2_part3.obj";
    String MODEDEL_4="STR2/STR2_part9.obj";
    String MODEDEL_5="STR2/STR2_part13.obj";
    String MODEDEL_6="STR2/STR2_part14.obj";
    String MODEDEL_7="STR2/STR2_ukazatelnii_part15.obj";
    String MODEDEL_8="STR2/STR2_ukazatelnii_part4.obj";
    String MODEDEL_9="STR2/STR2_ukazatelnii_part17.obj";
    String MODEDEL_10="STR2/STR2_srednii_part8.obj";
    String MODEDEL_11="STR2/STR2_srednii_part6.obj";
    String MODEDEL_12="STR2/STR2_srednii_part16.obj";
    String MODEDEL_13="STR2/STR2_bezimiannii_part10.obj";
    String MODEDEL_14="STR2/STR2_bezimiannii_part7.obj";
    String MODEDEL_15="STR2/STR2_bezimiannii_part11.obj";
    String MODEDEL_16="STR2/STR2_mizinec_part12.obj";
    String MODEDEL_17="STR2/STR2_mizinec_part5.obj";
    String MODEDEL_18="STR2/STR2_mizinec_part2.obj";

    //  new model
    String MODEDEL_0_NEW="STR2_NEW/STR2_big_finger_part18_new.obj";
    String MODEDEL_1_NEW="STR2_NEW/STR2_big_finger_part19_new.obj";
    String MODEDEL_2_NEW="STR2_NEW/STR2_big_finger_part1_new.obj";
    String MODEDEL_3_NEW="STR2_NEW/STR2_big_finger_part3_new.obj";
    String MODEDEL_4_NEW="STR2_NEW/STR2_part9_new.obj";
    String MODEDEL_5_NEW="STR2_NEW/STR2_part13_new.obj";
    String MODEDEL_6_NEW="STR2_NEW/STR2_part14_new.obj";
    String MODEDEL_7_NEW="STR2_NEW/STR2_ukazatelnii_part15_new.obj";
    String MODEDEL_8_NEW="STR2_NEW/STR2_ukazatelnii_part4_new.obj";
    String MODEDEL_9_NEW="STR2_NEW/STR2_ukazatelnii_part17_new.obj";
    String MODEDEL_10_NEW="STR2_NEW/STR2_srednii_part8_new.obj";
    String MODEDEL_11_NEW="STR2_NEW/STR2_srednii_part6_new.obj";
    String MODEDEL_12_NEW="STR2_NEW/STR2_srednii_part16_new.obj";
    String MODEDEL_13_NEW="STR2_NEW/STR2_bezimiannii_part10_new.obj";
    String MODEDEL_14_NEW="STR2_NEW/STR2_bezimiannii_part7_new.obj";
    String MODEDEL_15_NEW="STR2_NEW/STR2_bezimiannii_part11_new.obj";
    String MODEDEL_16_NEW="STR2_NEW/STR2_mizinec_part12_new.obj";
    String MODEDEL_17_NEW="STR2_NEW/STR2_mizinec_part2_new.obj";
    String MODEDEL_18_NEW="STR2_NEW/STR2_mizinec_part5_new.obj";
    Integer MAX_NUMBER_DETAILS = 19;

    ////////////////////////////////////////////////
/* *                     delays                      * */
    ////////////////////////////////////////////////
    int GRAPH_UPDATE_DELAY  = 25;
    int NOTIFY_UPDATE_DELAY  = 250;
    int RECONNECT_BLE_PERIOD = 1000;
    int COUNT_ATTEMPTS_TO_UPDATE = 50;
}
