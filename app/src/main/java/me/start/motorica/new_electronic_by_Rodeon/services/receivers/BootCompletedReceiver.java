package me.start.motorica.new_electronic_by_Rodeon.services.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import me.start.motorica.new_electronic_by_Rodeon.services.MyService;
import timber.log.Timber;

public class BootCompletedReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        Toast toast = Toast.makeText(context.getApplicationContext(),
                "BootCompletedReceiver onReceive()" + intent.getAction(), Toast.LENGTH_LONG);
        toast.show();
        Timber.d("service--> BootCompletedReceiver onReceive()%s", intent.getAction());
        context.startService(new Intent(context, MyService.class));
    }
}