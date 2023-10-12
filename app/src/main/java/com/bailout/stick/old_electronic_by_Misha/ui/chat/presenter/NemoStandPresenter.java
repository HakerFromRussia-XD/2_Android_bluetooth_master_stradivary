package com.bailout.stick.old_electronic_by_Misha.ui.chat.presenter;

import android.app.Activity;
import android.content.Intent;


public interface NemoStandPresenter {
    void onCreate(Intent intent);
    void onHelloWorld(byte[] textbyte);
    void onStart(Activity activity);
    void onStop();
    void onResume();
    void onPause();
    void disconnect();
    void disable();
    void setOnPauseActivity(boolean onPauseActivity);
    byte calculationCRC(byte[] textByteTreegSettings);
    byte calculationCRC_HDLC(byte[] textByteTreegSettings);
}
