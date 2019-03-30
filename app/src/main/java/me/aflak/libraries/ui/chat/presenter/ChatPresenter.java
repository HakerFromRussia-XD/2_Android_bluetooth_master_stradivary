package me.aflak.libraries.ui.chat.presenter;

import android.app.Activity;
import android.content.Intent;


public interface ChatPresenter {
    void onCreate(Intent intent);
    void onHelloWorld(byte[] textbyte);
//    void onHelloWorld2(byte[] data);
    void onStart(Activity activity);
    void onStop();
}
