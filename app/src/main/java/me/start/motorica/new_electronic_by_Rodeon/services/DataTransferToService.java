package me.start.motorica.new_electronic_by_Rodeon.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class DataTransferToService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.err.println("DataTransferToService onBind()");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            int sensor_level_1 = intent.getIntExtra("sensor_level_1", 0);
            int sensor_level_2 = intent.getIntExtra("sensor_level_2", 0);
            int open_ch_num = intent.getIntExtra("open_ch_num", 0);
            int close_ch_num = intent.getIntExtra("close_ch_num", 0);

            System.err.println("DataTransferToService onStartCommand()  sensor_level_1=" + sensor_level_1 + "  sensor_level_2=" + sensor_level_2 + "  open_ch_num=" + open_ch_num + "  close_ch_num=" + close_ch_num);

            Intent in = new Intent("Sensor_levels");
            in.putExtra("sensor_level_1", sensor_level_1);
            in.putExtra("sensor_level_2", sensor_level_2);
            in.putExtra("open_ch_num", open_ch_num);
            in.putExtra("close_ch_num", close_ch_num);
            sendBroadcast(in);
        } else {
            System.err.println("DataTransferToService \"intent null\" onStartCommand"); //key_move
        }


        return super.onStartCommand(intent, flags, startId);
    }
}
