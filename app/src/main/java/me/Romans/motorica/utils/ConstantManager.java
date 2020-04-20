package me.Romans.motorica.utils;

public interface  ConstantManager {

    ////////////////////////////////////////////////
/**                      3D                         **/
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

    ////////////////////////////////////////////////
/**                    transfer                     **/
    ////////////////////////////////////////////////

    byte ADDR_BRODCAST              =(byte) 0xFF;
    byte ADDR_MIO1                  =(byte) 0xFA;
    byte ADDR_MIO2                  =(byte) 0xFA;
    byte ADDR_ENDPOINT_POSITION     =(byte) 0xFF;
    byte ADDR_CUR_LIMIT             =(byte) 0xFA;
    byte ADDR_BUFF_CHOISES          =(byte) 0xFA;
    byte ADDR_BLOCK                 =(byte) 0xFA;
    byte ADDR_MAIN_DATA             =(byte) 0xFA;
    byte ADDR_TRIG_MODE             =(byte) 0xFA;
    byte ADDR_BATTERY               =(byte) 0xFA;
    byte ADDR_SOURCE_ADC            =(byte) 0xFA;
    byte READ                               =0x01;
    byte WRITE                              =0x02;
    byte SPEED_CALIB_TYPE                   =0x02;
    byte ANGLE_CALIB_TYPE                   =0x01;
    byte OPEN_STOP_CLOSE_CALIB_TYPE         =0x00;
    byte SET_ADDR_CALIB_TYPE                =0x06;
    byte TEMP_CALIB_TYPE                    =0x07;
    byte CURRENT_CONTROL_CALIB_TYPE         =0x05;
    byte CURRENT_TIMEOUT_CALIB_TYPE         =0x1C;
    byte CURRENTS_CALIB_TYPE                =0x04;
    byte ETE_CALIBRATION_CALIB_TYPE         =0x03;
    byte EEPROM_SAVE_CALIB_TYPE             =0x08;
    byte ANGLE_FIX_CALIB_TYPE               =0x22;
    byte OPEN_ANGEL_CALIB_TYPE              =0x17;
    byte CLOSE_ANGEL_CALIB_TYPE             =0x18;
    byte WIDE_ANGEL_CALIB_TYPE              =0X1B;
    byte MAGNET_INVERT_CALIB_TYPE           =0x1D;
    byte REVERS_MOTOR_CALIB_TYPE            =0x1E;
    byte ZERO_CROSSING_CALIB_TYPE           =0x20;
    byte E_CALIB_TYPE                       =0x08;
    byte U_CALIB_TYPE                       =0x19;
    byte O_S_C_CALIB_TYPE                   =0x00;
    byte ADDR_CALIB_TYPE                    =0x00;
    boolean DISABLE_UPDATIONG_GRAPH         =true;//false - рабочее состояние

    ////////////////////////////////////////////////
/**                     delays                      **/
    ////////////////////////////////////////////////

    int GRAPH_UPDATE_DELAY  =50;
    int SKIP_GRAPH_СYCLE_FOR_SEND_UPDATE_REQUEST = 20;//максимальное количество пропускаемых циклов
    // обновления графика при отсутствии ответа от кисти 50 милисекундном периоде обновления и
    // четырёх циклах ожидания, следующий запрос обновления параметров будет отослан максимум
    // через 200 миллисекунд
}
