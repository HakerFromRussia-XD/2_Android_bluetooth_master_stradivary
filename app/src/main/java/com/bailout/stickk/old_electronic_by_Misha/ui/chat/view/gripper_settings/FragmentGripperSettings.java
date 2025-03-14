package com.bailout.stickk.old_electronic_by_Misha.ui.chat.view.gripper_settings;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.Objects;

import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication;
import com.bailout.stickk.R;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.data.ChatModule;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.data.DaggerChatComponent;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.view.ChartActivity;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.view.ChartView;

public class FragmentGripperSettings extends Fragment implements ChartView {
    Button gripper_use;
    public SeekBar seekBarSpeedFinger;
    public TextView textSpeedFinger;


    public View view;
    private ChartActivity chatActivity;
    private GripperSettingsGLSurfaceView glSurfaceView;
    private GripperSettingsRenderer renderer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_gripper_settings, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(Objects.requireNonNull(WDApplication.app()).bluetoothModule())
                .chatModule(new ChatModule(FragmentGripperSettings.this))
                .build().inject(FragmentGripperSettings.this);
//        ButterKnife.bind(this, view);
        if (getActivity() != null) {chatActivity = (ChartActivity) getActivity();}

        gripper_use = view.findViewById(R.id.gripper_use);
        seekBarSpeedFinger = view.findViewById(R.id.seekBarSpeedFinger);
        textSpeedFinger = view.findViewById(R.id.textSpeedFinger);
        seekBarSpeedFinger.setProgress(99);
        glSurfaceView = view.findViewById(R.id.gl_surface_view);
        final ActivityManager activityManager = (ActivityManager) chatActivity.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x00020000;

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

        renderer.angleLittleFingerFloat =  chatActivity.loadVariable(ChartActivity.deviceName +chatActivity.NUMBER_CELL+"intValueFinger1Angle");
        renderer.angleRingFingerFloat =  chatActivity.loadVariable(ChartActivity.deviceName +chatActivity.NUMBER_CELL+"intValueFinger2Angle");
        renderer.angleMiddleFingerFloat =  chatActivity.loadVariable(ChartActivity.deviceName +chatActivity.NUMBER_CELL+"intValueFinger3Angle");
        renderer.angleForeFingerFloat =  chatActivity.loadVariable(ChartActivity.deviceName +chatActivity.NUMBER_CELL+"intValueFinger4Angle");
        renderer.angleBigFingerFloat1 =  84 - (int)(((float)chatActivity.loadVariable(ChartActivity.deviceName +chatActivity.NUMBER_CELL+"intValueFinger5Angle")+60)/100*90);
        renderer.angleBigFingerFloat2 =  144 - (int)(((float)chatActivity.loadVariable(ChartActivity.deviceName +chatActivity.NUMBER_CELL+"intValueFinger6Angle")+60)/100*90);
//        Log.e(TAG, "NUMBER_CELL = "+String.valueOf(chatActivity.NUMBER_CELL));

        gripper_use.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    chatActivity.fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .remove(chatActivity.fragmentGripperSettings)
                            .commit();
                    chatActivity.firstTapRecyclerView = true;
                    chatActivity.transferThreadFlag = false;
                }
            }
        });
        seekBarSpeedFinger.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(seekBarSpeedFinger.getProgress() < 10){textSpeedFinger.setText("0"+seekBarSpeedFinger.getProgress());}
                else {textSpeedFinger.setText(""+seekBarSpeedFinger.getProgress());}
                chatActivity.speedFinger = seekBarSpeedFinger.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        return view;
    }

    public void backPressed() {
        if (getActivity() != null) {
            chatActivity.fragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                    .remove(chatActivity.fragmentGripperSettings)
                    .commit();
            chatActivity.firstTapRecyclerView = true;
            chatActivity.transferThreadFlag = false;
        }
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
    public void enableInterface(boolean enabled) { }
    @Override
    public void showToast(String message) { }

    @Override
    public void showToastWithoutConnection() {

    }

    @Override
    public void onGestureClick(int position) { }
    @Override
    public void setGeneralValue(int receiveCurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) { }
    @Override
    public void setStartParameters(Integer receiveCurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication, Byte receiveRoughnessOfSensors) { }

    @Override
    public void setStartParametersInChartActivity() {

    }

    @Override
    public boolean getFirstRead() {
        return false;
    }

    @Override
    public void setFlagReceptionExpectation(Boolean flagReceptionExpectation) {

    }

    @Override
    public void setStartParametersTrigCH1(Integer receiveLevelTrigCH1) {

    }

    @Override
    public void setStartParametersTrigCH2(Integer receiveLevelTrigCH2) {

    }

    @Override
    public void setStartParametersCurrent(Integer receiveCurrent) {

    }

    @Override
    public void setStartParametersBlock(Byte receiveBlockIndication) {

    }

    @Override
    public void setStartParametersRoughness(Byte receiveRoughnessOfSensors) {

    }

//    @Override
//    public void setStartParametersBattery(Integer receiveBatteryTension) {
//
//    }

}
