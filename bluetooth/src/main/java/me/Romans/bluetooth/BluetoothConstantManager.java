package me.Romans.bluetooth;

public interface BluetoothConstantManager {

    Integer RESET_ALL_VARIABLE      =0;
    Integer OLD_NOT_USE_PROTOCOL    =1;
    Integer OLD_PROTOCOL            =2;
    Integer HDLC_PROTOCOL           =3;

    int ENDPOINT_POSITION			=  0;
    int HDLC_ADDRESS_REG            =  6;
    int MOTOR_REVERS                = 30;
    int MIO1_TRIG_HDLC				= 38;
    int MIO2_TRIG_HDLC				= 39;
    int CURR_LIMIT_HDLC				= 40;
    int CURR_MAIN_DATA_HDLC		    = 41; //MIO1(2B), MIO2(2B), Current(2B)
    int TRIG_MODE_HDLC				= 42;
    int HAND_ON_OFF_REG				= 43;
    int HAND_MOTIONS_CNT_HDLC       = 44;
    int ADC_BUFF_CHOISES_HDLC       = 45;
}
