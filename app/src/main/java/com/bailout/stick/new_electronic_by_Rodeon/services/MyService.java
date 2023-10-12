package com.bailout.stick.new_electronic_by_Rodeon.services;

import static androidx.core.app.NotificationCompat.PRIORITY_HIGH;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.bailout.stick.R;

public class MyService extends Service {

    // Constants
    private static final int ID_SERVICE = 101;
    private CountDownTimer timerGraphEnteringData;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // do stuff like register for BroadcastReceiver, etc.
        Toast.makeText(getApplicationContext(), "MyService onCreate()", Toast.LENGTH_SHORT).show();
        startGraphEnteringDataTimer();
        // Create the Foreground Service
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        startForeground(ID_SERVICE, notification);
    }


    private void startGraphEnteringDataTimer() {
//        timerGraphEnteringData.cancel();
//        timerGraphEnteringData = object : CountDownTimer(5000000, 25) {
//            override fun onTick(millisUntilFinished: Long) {
//                if (plotData) {
//                    addEntry(10, 255)
//                    addEntry(10, 255)
//                    addEntry(10, 255)
//                    addEntry(115, 150)
//                    addEntry(115, 150)
//                    addEntry(115, 150)
//                    addEntry(10, 255)
//                    addEntry(10, 255)
//                    addEntry(10, 255)
//                    addEntry(115, 150)
//                    addEntry(115, 150)
//                    addEntry(115, 150)
//                    plotData = false
//                }
//                addEntry(main!!.getDataSens1(), main!!.getDataSens2())
//            }
//
//            override fun onFinish() {
//                startGraphEnteringDataTimer()
//            }
//        }.start()
        new CountDownTimer(34000, 5000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Toast.makeText(getApplicationContext(), "MyService CountDownTimer onTick()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                Toast.makeText(getApplicationContext(), "MyService CountDownTimer onFinish()", Toast.LENGTH_SHORT).show();
//                startGraphEnteringDataTimer();
            }
        }.start();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "my_service_channelid";
        String channelName = "My Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }
}
