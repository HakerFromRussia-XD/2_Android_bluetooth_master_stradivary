package me.start.motorica.new_electronic_by_Rodeon.services.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import me.start.motorica.new_electronic_by_Rodeon.services.MyService;
import timber.log.Timber;

public class BootCompletedReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        Toast toast = Toast.makeText(context.getApplicationContext(),
                "BootCompletedReceiver onReceive()" + intent.getAction(), Toast.LENGTH_LONG);
        toast.show();
//        context.startService(new Intent(context, MyService.class));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, MyService.class));
        } else {
            context.startService(new Intent(context, MyService.class));
        }
    }
}