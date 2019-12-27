package me.Romans.motorica.utils;

public interface ConstantManager {

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

    byte ADDR_MIO1                  =(byte) 0xFA;
    byte ADDR_MIO2                  =(byte) 0xFA;
    byte ADDR_ENDPOINT_POSITION     =(byte) 0xFF;
    byte ADDR_CUR_LIMIT             =(byte) 0xFA;
    byte ADDR_BUFF_CHOISES          =(byte) 0xFA;
    byte ADDR_BLOCK                 =(byte) 0xFA;
    byte ADDR_MAIN_DATA             =(byte) 0xFA;
    byte ADDR_TRIG_MODE             =(byte) 0xFA;
    byte ADDR_BATTERY               =(byte) 0xFF;
    byte READ                       =0x01;
    byte WRITE                      =0x02;

    ////////////////////////////////////////////////
/**                     delays                      **/
    ////////////////////////////////////////////////

    int GRAPH_UPDATE_DELAY  =50;
    int SKIP_GRAPH_СYCLE_FOR_SEND_UPDATE_REQUEST = 10;//максимальное количество пропускаемых циклов
    // обновления графика при отсутствии ответа от кисти 50 милисекундном периоде обновления и
    // четырёх циклах ожидания, следующий запрос обновления параметров будет отослан максимум
    // через 200 миллисекунд
}
