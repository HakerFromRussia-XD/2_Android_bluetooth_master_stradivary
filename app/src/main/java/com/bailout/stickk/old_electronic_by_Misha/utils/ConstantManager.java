package com.bailout.stickk.old_electronic_by_Misha.utils;

public interface  ConstantManager {

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
    byte DISABLE_ANGLE_CONTROL_TYPE         =0x09;
    byte SPEED_INCREMENT_TYPE               =0x0A;
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
    boolean DISABLE_UPDATIONG_GRAPH         =false;//false - рабочее состояние

    ////////////////////////////////////////////////
/**                     delays                      **/
    ////////////////////////////////////////////////

    int GRAPH_UPDATE_DELAY  =50;
    int SKIP_GRAPH_СYCLE_FOR_SEND_UPDATE_REQUEST = 20;//максимальное количество пропускаемых циклов
    // обновления графика при отсутствии ответа от кисти 50 милисекундном периоде обновления и
    // четырёх циклах ожидания, следующий запрос обновления параметров будет отослан максимум
    // через 200 миллисекунд
}
