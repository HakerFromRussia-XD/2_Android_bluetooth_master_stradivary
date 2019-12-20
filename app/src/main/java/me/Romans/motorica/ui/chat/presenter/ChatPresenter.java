package me.Romans.motorica.ui.chat.presenter;

import android.app.Activity;
import android.content.Intent;


public interface ChatPresenter {
    void onCreate(Intent intent);
    void onHelloWorld(byte[] textbyte);
//    void onHelloWorld2(byte[] data);
    void onStart(Activity activity);
    void onStop();
    void onResume();
    void onPause();
    void setDeviceCallback2(Activity activity);
    void disconnect();
    void disable();
    byte calculationCRC(byte[] textByteTreegSettings);
    byte calculationCRC_HDLC(byte[] textByteTreegSettings);
}
