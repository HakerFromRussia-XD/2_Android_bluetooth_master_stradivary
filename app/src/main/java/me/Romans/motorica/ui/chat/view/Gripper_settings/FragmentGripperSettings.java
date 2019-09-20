package me.Romans.motorica.ui.chat.view.Gripper_settings;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.Romans.motorica.MyApp;
import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.data.ChatModule;
import me.Romans.motorica.ui.chat.data.DaggerChatComponent;
import me.Romans.motorica.ui.chat.view.ChatActivity;
import me.Romans.motorica.ui.chat.view.ChatView;

public class FragmentGripperSettings extends Fragment implements ChatView {
    Button gripper_use;
    SeekBar seekBarSpeedFinger;
    public TextView textSpeedFinger;


    public View view;
    private ChatActivity chatActivity;
    private GripperSettingsGLSurfaceView glSurfaceView;
    private GripperSettingsRenderer renderer;

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

        gripper_use = view.findViewById(R.id.gripper_use);
        seekBarSpeedFinger = view.findViewById(R.id.seekBarSpeedFinger);
        textSpeedFinger = view.findViewById(R.id.textSpeedFinger);
//        textSpeedFinger.setText(""+90);
        glSurfaceView = view.findViewById(R.id.gl_surface_view);
        final ActivityManager activityManager = (ActivityManager) chatActivity.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            glSurfaceView.setEGLContextClientVersion(2);

            final DisplayMetrics displayMetrics = new DisplayMetrics();
            chatActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            renderer = new GripperSettingsRenderer(getActivity(), glSurfaceView);
            glSurfaceView.setRenderer(renderer, displayMetrics.density);
        } else {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
        }

        gripper_use.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    chatActivity.fragmentManager.beginTransaction()
                            .remove(chatActivity.fragmentGripperSettings)
                            .commit();
                    chatActivity.firstTapRcyclerView = true;
                    chatActivity.transferThreadFlag = false;
                }
            }
        });
        seekBarSpeedFinger.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(seekBarSpeedFinger.getProgress() < 10){textSpeedFinger.setText("0"+seekBarSpeedFinger.getProgress());}
                else {textSpeedFinger.setText(""+seekBarSpeedFinger.getProgress());}
//                System.err.println("FragmentGripperSettings--------> Progress: "+seekBarSpeedFinger.getProgress());
//                chatActivity.setSpeedFinger(seekBarSpeedFinger.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                chatActivity.speedFinger = seekBarSpeedFinger.getProgress();
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

    @Override
    public void setStartParametersInGraphActivity() {

    }
}
