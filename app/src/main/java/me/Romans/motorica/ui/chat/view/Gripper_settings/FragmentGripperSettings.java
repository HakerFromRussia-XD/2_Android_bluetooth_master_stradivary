package me.Romans.motorica.ui.chat.view.Gripper_settings;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.Romans.motorica.MyApp;
import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.data.ChatModule;
import me.Romans.motorica.ui.chat.data.DaggerChatComponent;
import me.Romans.motorica.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.ui.chat.view.ChatActivity;
import me.Romans.motorica.ui.chat.view.ChatView;

public class FragmentGripperSettings extends Fragment implements ChatView {
    @BindView(R.id.gesture_use) Button gesture_use;
    private int indicatorTypeMessage = 0x04;
    private int GESTURE_NUMBER = 0x0000;
    private int GripperNumberStart1 = 0xA000;
    private int mySensorEvent1 = 0xB000;
    private int GripperNumberEnd1 = 0xC001;
    private int GripperNumberStart2 = 0xA001;
    private int mySensorEvent2 = 0xB001;
    private int GripperNumberEnd2 = 0xC000;
    private byte[] TextByteTreegSettings = new byte[15];
    private byte[] TextByteTreegControl = new byte[2];

    public View view;
    private ChatActivity chatActivity;
    private GripperSettingsGLSurfaceView glSurfaceView;
    private GripperSettingsRenderer renderer;

    @Inject ChatPresenter presenter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_gripper_settings, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(FragmentGripperSettings.this))
                .build().inject(FragmentGripperSettings.this);
        ButterKnife.bind(this, view);
        if (getActivity() != null) {chatActivity = (ChatActivity) getActivity();}

        glSurfaceView = view.findViewById(R.id.gl_surface_view);

        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) chatActivity.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            glSurfaceView.setEGLContextClientVersion(2);

            final DisplayMetrics displayMetrics = new DisplayMetrics();
            chatActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            // Set the renderer to our demo renderer, defined below.
            renderer = new GripperSettingsRenderer(getActivity(), glSurfaceView);
            glSurfaceView.setRenderer(renderer, displayMetrics.density);
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
        }

        for (int i = 0; i<chatActivity.MAX_NUMBER_DETAILS; i++) {
            chatActivity.indexCount = chatActivity.verticesArrey[i].length;
            System.err.println("LessonEightActivity--------> количество элементов в массиве №" + (i + 1) + " " + chatActivity.indexCount);
        }

        gesture_use.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                   chatActivity.CompileMassegeSwitchGesture((byte) 0x02, (byte) 0x03);
                   chatActivity.TranslateMassegeSwitchGesture();
                   chatActivity.fragmentManager.beginTransaction()
                           .remove(chatActivity.fragmentGripperSettings)
                           .commit();
                   chatActivity.firstTapRcyclerView = true;
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.onPause();
    }

    @Override
    public void setStatus(String status) { }
    @Override
    public void setStatus(int resId) { }
    @Override
    public void setValueCH(int levelCH, int numberChannel) { }
    @Override
    public void setErrorReception(boolean incomeErrorReception) { }
    @Override
    public void appendMessage(String message) { }
    @Override
    public void enableHWButton(boolean enabled) { }
    @Override
    public void showToast(String message) { }
    @Override
    public void onGestureClick(int position) { }
    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) { }
    @Override
    public void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication) { }
}
