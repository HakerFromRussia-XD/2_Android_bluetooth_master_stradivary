package com.bailout.stickk.new_electronic_by_Rodeon.ble;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static final HashMap<String, String> attributes = new HashMap<>();
    // Sample Characteristics.
    public static String MIO_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String SET_ADC_CURRENT_TRESHOLD_HDLE = "0000fe41-8e22-4541-9d4c-21edae82ed19";
    public static String SHUTDOWN_CURRENT_HDLE = "0000fe42-8e22-4541-9d4c-21edae82ed19";
    public static String START_UP_STEP_HDLE = "0000fe43-8e22-4541-9d4c-21edae82ed19";
    public static String START_UP_TIME_HDLE = "0000fe44-8e22-4541-9d4c-21edae82ed19";
    public static String DEAD_ZONE_HDLE = "0000fe45-8e22-4541-9d4c-21edae82ed19";
    public static String OPEN_THRESHOLD_HDLE = "0000fe46-8e22-4541-9d4c-21edae82ed19";
    public static String CLOSE_THRESHOLD_HDLE = "0000fe47-8e22-4541-9d4c-21edae82ed19";
    public static String OPEN_MOTOR_HDLE = "0000fe48-8e22-4541-9d4c-21edae82ed19";
    public static String CLOSE_MOTOR_HDLE = "0000fe49-8e22-4541-9d4c-21edae82ed19";
    public static String SENSITIVITY_HDLE = "0000fe4a-8e22-4541-9d4c-21edae82ed19";
    public static String SENS_OPTIONS = "0000fe4d-8e22-4541-9d4c-21edae82ed19";
    public static String ADD_GESTURE = "0000fe4e-8e22-4541-9d4c-21edae82ed19";
    public static String SET_GESTURE = "0000fe4f-8e22-4541-9d4c-21edae82ed19";
    public static String SET_REVERSE = "0000fe50-8e22-4541-9d4c-21edae82ed19";
    public static String RESET_TO_FACTORY_SETTINGS = "0000fe51-8e22-4541-9d4c-21edae82ed19";
    public static String SET_ONE_CHANNEL = "0000fe52-8e22-4541-9d4c-21edae82ed19";
    public static String SET_START_UPDATE = "0000fe53-8e22-4541-9d4c-21edae82ed19";
    public static String SET_SELECT_SCALE = "0000fe54-8e22-4541-9d4c-21edae82ed19";
    public static String AUTO_CALIBRATION = "0000fe55-8e22-4541-9d4c-21edae82ed19";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String FESTO_A_CHARACTERISTIC = "0000ffe1-0000-1000-8000-00805f9b34fb";

    //      характеристики переработанного стека
    public static String DRIVER_VERSION_NEW = "00002a26-0000-1000-8000-00805f9b34fb";
    public static String OPEN_THRESHOLD_NEW = "43686172-4d74-726b-0000-526f64696f6e"; //(без блокировки у Родиона)
    public static String CLOSE_THRESHOLD_NEW = "43686172-4d74-726b-0001-526f64696f6e";
    public static String OPEN_MOTOR_NEW = "43686172-4d74-726b-0002-526f64696f6e";
    public static String CLOSE_MOTOR_NEW = "43686172-4d74-726b-0003-526f64696f6e";
    public static String ADD_GESTURE_NEW = "43686172-4d74-726b-0004-526f64696f6e";
    public static String SET_GESTURE_NEW = "43686172-4d74-726b-0005-526f64696f6e"; // (с блокировкой у Родиона)
    public static String SET_REVERSE_NEW = "43686172-4d74-726b-0006-526f64696f6e";
    public static String SET_ONE_CHANNEL_NEW = "43686172-4d74-726b-0007-526f64696f6e";
    public static String CALIBRATION_NEW = "43686172-4d74-726b-0008-526f64696f6e";// Чтение 0 - не калиброван, 1 - идет калибровка, 2 - одного из моторов нет, 3 - одного из энкодеров нет , 4 - один из моторов прокручивается, 5 - перетянуты винты, 6 - откалиброван  Запись 0 начинает калибровку левой кисти, 1 правой
    public static String STATUS_CALIBRATION_NEW = "43686172-4d74-726b-0009-526f64696f6e";// 6 байт по состоянию на каждый палец ( 0 - не калиброван, 1 - идет калибровка, 2 - мотора нет, 3 - энкодера нет , 4 - мотор прокручивается, 5 - перетянут винт, 6 - откалиброван )
    public static String MOVE_ALL_FINGERS_NEW = "43686172-4d74-726b-000a-526f64696f6e";// 6 байт по положению на каждый палец
    public static String CHANGE_GESTURE_NEW = "43686172-4d74-726b-000b-526f64696f6e";// 13 байт по положению на каждый палец на каждое положение + 1 байт номера жеста
    public static String SHUTDOWN_CURRENT_NEW = "43686172-4d74-726b-000c-526f64696f6e";// 6 байт по отсечке на каждый палец
    public static String ROTATION_GESTURE_NEW = "43686172-4d74-726b-000d-526f64696f6e";// 4 байта, превый 0/1 - вкл/выкл; второй - тип 0 - Одиночное сжетие обеих мышц, 1 - дабл тап, 2 - нажатие в упоре; третий - продожительность пика, четвёртый - продолжительность паузы (единица 1/20 секунды (такт датчика))

    public static String RESET_TO_FACTORY_SETTINGS_NEW = "43686172-4d74-726b-0100-526f64696f6e";

    public static String SENS_OPTIONS_NEW = "43686172-4d74-726b-0200-526f64696f6e";//"43686172-4d74-726b-0002-526f64696f6e";
    public static String MIO_MEASUREMENT_NEW = "43686172-4d74-726b-0201-526f64696f6e";
    public static String SENS_VERSION_NEW = "43686172-4d74-726b-0202-526f64696f6e";
    public static String SENS_ENABLED_NEW = "43686172-4d74-726b-0203-526f64696f6e"; // 0-управление от датчиков отключео 1-управление от датчиков включено

    public static String SERIAL_NUMBER_NEW = "43686172-4d74-726b-0300-526f64696f6e"; // 16 байт инфа о номере для телеметрии\

    //    характеристики переработанного стека для Новой Ваниной и Мишиной версии
    public static String OPEN_THRESHOLD_NEW_VM = "43680000-4d74-1001-726b-526f64696f6e";// уровень порога дасчика открытия (без блокировки у Родиона)
    public static String CLOSE_THRESHOLD_NEW_VM = "43680001-4d74-1001-726b-526f64696f6e";// уровень порога дасчика закрытия (без блокировки у Родиона)
    public static String OPEN_MOTOR_NEW_VM = "43680002-4d74-1001-726b-526f64696f6e";// открытие кисти 1 - для движения, 0 - для остановки
    public static String CLOSE_MOTOR_NEW_VM = "43680003-4d74-1001-726b-526f64696f6e";// закрытие кисти 1 - для движения, 0 - для остановки
    public static String ADD_GESTURE_NEW_VM = "43680004-4d74-1001-726b-526f64696f6e";// 159 байт углы и стартовые задержки всех пальцев во всех 14-ти жестах
    public static String SET_GESTURE_NEW_VM = "43680005-4d74-1001-726b-526f64696f6e";// установка номера активного жеста (0-13) (с блокировкой у Родиона)
    public static String SET_REVERSE_NEW_VM = "43680006-4d74-1001-726b-526f64696f6e";// 1-й байт - 1 для свапа сенсоров, 2-й байт номер активного жеста, 3-й моды работы протеза: 0 - стандартный 1 - стенд 2 - демо, 4-й и 5-й - номер циклов повторения в режиме стенд х10
    public static String SET_ONE_CHANNEL_NEW_VM = "43680007-4d74-1001-726b-526f64696f6e";// установка одноканального режима управления (1 - активация 0 - деактивация)
    public static String CALIBRATION_NEW_VM = "43680008-4d74-1001-726b-526f64696f6e";// Чтение: 0 - не калиброван, 1 - идет калибровка, 2 - одного из моторов нет, 3 - одного из энкодеров нет , 4 - один из моторов прокручивается, 5 - перетянуты винты, 6 - откалиброван  Запись 0 начинает калибровку левой кисти, 1 правой
    public static String STATUS_CALIBRATION_NEW_VM = "43680009-4d74-1001-726b-526f64696f6e";// 6 байт по состоянию на каждый палец ( 0 - не калиброван, 1 - идет калибровка, 2 - мотора нет, 3 - энкодера нет , 4 - мотор прокручивается, 5 - перетянут винт, 6 - откалиброван )
    public static String MOVE_ALL_FINGERS_NEW_VM = "4368000a-4d74-1001-726b-526f64696f6e";// 6 байт по положению на каждый палец
    public static String CHANGE_GESTURE_NEW_VM = "4368000b-4d74-1001-726b-526f64696f6e";// 25 байт 1 байт номера жеста + угол на каждый палец в двух состояниях + время задержки на каждый палец в двух состояниях
    public static String SHUTDOWN_CURRENT_NEW_VM = "4368000c-4d74-1001-726b-526f64696f6e";// 6 байт по отсечке на каждый палец

    public static String RESET_TO_FACTORY_SETTINGS_NEW_VM = "43680100-4d74-1001-726b-526f64696f6e";// сброс к заводским настройкам (для активации отправляется 0х01)

    public static String SENS_OPTIONS_NEW_VM = "43680200-4d74-1001-726b-526f64696f6e";// инфа о настройке датчиков (1-й и 14-й байты в посылке - чувствительность, 27-й - режимы работы ЕМГ: 0х09 -нормальный 0х07 - чувствительный 0х0А - искуственный интеллек,  28-й - автокалибровка(для активации отправляется 0х01))
    public static String MIO_MEASUREMENT_NEW_VM = "43680201-4d74-1001-726b-526f64696f6e";// первые 2 байта сигналы датчиков, 3-й байт активный в данное время жест, 4-9 байты текущие углы энкодеров пальцев, 10-й байт код ошибки(для отладки, расшифровку кодов можно спросить у Лёши), 11-12 байты сокращённый UUID последней характеристики запрошенной с телефона (для системы подтверждения приёма)
    public static String SENS_VERSION_NEW_VM = "43680202-4d74-1001-726b-526f64696f6e";// версия платы сенсоров
    public static String DRIVER_VERSION_NEW_VM = "43680590-4d74-1001-726b-526f64696f6e";// версия драйвера(основной прошивки платы)
    public static String SENS_ENABLED_NEW_VM = "43680203-4d74-1001-726b-526f64696f6e";// 0-управление от датчиков отключео 1-управление от датчиков включено

    public static String SERIAL_NUMBER_NEW_VM = "43680300-4d74-1001-726b-526f64696f6e";// 16 байт инфа о номере для телеметрии\

    public static String ROTATION_GESTURE_NEW_VM = "43680400-4d74-1001-726b-526f64696f6e";// 8 байт, превый 0/1 - вкл/выкл; второй - всегда 0; третий - время для переключения жеста; четвёртый - всегда 0; пятый - 0/1 вкл/выкл блокировки датчиками; шестой - время блокировки датчиками; седьмой - стартовыйж ест группы ротации; восьмой - конечный жест группы ротации
    // второй - 0;
    // третий - продожительность пика для переключения жеста,
    // четвёртый - 0;
    // пятый - 0/1 вкл/выкл блокировки датчиками,
    // шестой - время удержания обоих датчиков для включения блокировки,
    // седьмой - стартовый жест в группе ротации,
    // восьмой - конечный жест в группе ротации)


    // Sample Commands.
    public static String READ = "READ";
    public static String WRITE = "WRITE";
    public static String WRITE_WR = "WRITE_WR";
    public static String NOTIFY = "NOTIFY";

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("00001810-0000-1000-8000-00805f9b34fb", "Что-то моё");
        attributes.put("0000fe40-cc7a-482a-984a-7f2ed5b3e58f", "Наша кастомная характеристика");
        // Sample Characteristics.
        attributes.put(MIO_MEASUREMENT, "MIO Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");

        attributes.put(SET_ADC_CURRENT_TRESHOLD_HDLE, "Set ADC Current Treshold Hdle");
        attributes.put(SHUTDOWN_CURRENT_HDLE, "Shutdown Current Hdle");
        attributes.put(START_UP_STEP_HDLE, "Start Up Step Hdle");
        attributes.put(START_UP_TIME_HDLE, "Start Up Time Hdle");
        attributes.put(DEAD_ZONE_HDLE, "Dead Zone Hdle");
        attributes.put(OPEN_THRESHOLD_HDLE, "Open Threshold Hdle");
        attributes.put(CLOSE_THRESHOLD_HDLE, "Close Threshold Hdle");
        attributes.put(OPEN_MOTOR_HDLE, "Motor Open Hdle");
        attributes.put(CLOSE_MOTOR_HDLE, "Motor Close Hdle");
        attributes.put(SENSITIVITY_HDLE, "Sensitivity Hdle");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
