package me.Romans.motorica.ui.chat.view.Service_settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.Romans.motorica.MyApp;
import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.data.ChatModule;
import me.Romans.motorica.ui.chat.data.DaggerChatComponent;
import me.Romans.motorica.ui.chat.view.ChartActivity;
import me.Romans.motorica.ui.chat.view.ChatView;

public class FragmentServiceSettingsMono extends Fragment implements ChatView {
    @BindView(R.id.save_service_settings) Button save_service_settings;
    @BindView(R.id.seekBarRoughness) public SeekBar seekBarRoughness;
    @BindView(R.id.switchInvert) public Switch switchInvert;
    public SeekBar seekBarIstop;
    TextView valueIstop;
    public TextView valueIstop2;
    public View view;
    private ChartActivity chatActivity;
    private int maxCurrent = 1500;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_service_settings_mono, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(FragmentServiceSettingsMono.this))
                .build().inject(FragmentServiceSettingsMono.this);
        ButterKnife.bind(this, view);

        if (getActivity() != null) {chatActivity = (ChartActivity) getActivity();}
        chatActivity.graphThreadFlag = false;
        chatActivity.updateSeviceSettingsThreadFlag = true;
        chatActivity.startUpdateThread();
        chatActivity.layoutSensors.setVisibility(View.GONE);
        valueIstop2 = view.findViewById(R.id.valueIstop2);
        valueIstop = view.findViewById(R.id.valueIstop);
        seekBarIstop = view.findViewById(R.id.seekBarIstopServiceSettings);


        save_service_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    chatActivity.fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .remove(chatActivity.fragmentServiceSettingsMono)
                            .commit();
                    chatActivity.graphThreadFlag = true;
                    chatActivity.startGraphEnteringDataThread();
                    chatActivity.myMenu.setGroupVisible(R.id.service_settings, true);
                    chatActivity.myMenu.setGroupVisible(R.id.modes, false);
                    chatActivity.updateSeviceSettingsThreadFlag = false;
                    chatActivity.layoutSensors.setVisibility(View.VISIBLE);
                }
            }
        });

        seekBarRoughness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeRouhness((byte) (((byte) seekBar.getProgress()) + 1)));
                chatActivity.receiveRoughnessOfSensors = (byte) seekBar.getProgress();
            }
        });

        switchInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchInvert.isChecked()){
                    chatActivity.invert = 0x01;
                    chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvert(chatActivity.curent, chatActivity.invert));
                    Integer temp = chatActivity.intValueCH1on;
                    chatActivity.seekBarCH1on2.setProgress((int) (chatActivity.intValueCH2on/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.seekBarCH2on2.setProgress((int) (temp/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.invertChannel = true;
                } else {
                    chatActivity.invert = 0x00;
                    chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvert(chatActivity.curent, chatActivity.invert));
                    Integer temp = chatActivity.intValueCH1on;
                    chatActivity.seekBarCH1on2.setProgress((int) (chatActivity.intValueCH2on/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.seekBarCH2on2.setProgress((int) (temp/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.invertChannel = false;
                }
            }
        });

        seekBarIstop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxCurrent = seekBar.getProgress();
                valueIstop.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                maxCurrent = seekBar.getProgress();
                chatActivity.maxCurrent = maxCurrent;
                valueIstop.setText(String.valueOf(seekBar.getProgress()));
                chatActivity.curent = seekBar.getProgress();
                chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvert(chatActivity.curent, chatActivity.invert));
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        seekBarIstop.setProgress(chatActivity.maxCurrent);
        seekBarRoughness.setProgress(chatActivity.receiveRoughnessOfSensors);
    }

    @Override
    public void onPause() {
        super.onPause();
        chatActivity.seekBarIstop.setProgress(maxCurrent);
    }


    public void backPressed() {
        if (getActivity() != null) {
            System.err.println("fragmentServiceSettingsMono----> выполнение  backPressed()");
            chatActivity.fragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                    .remove(chatActivity.fragmentServiceSettingsMono)
                    .commit();
            chatActivity.graphThreadFlag = true;
            chatActivity.startGraphEnteringDataThread();
            chatActivity.myMenu.setGroupVisible(R.id.service_settings, true);
            chatActivity.myMenu.setGroupVisible(R.id.modes, false);
            chatActivity.updateSeviceSettingsThreadFlag = false;
            chatActivity.layoutSensors.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setStatus(String status) {

    }
    @Override
    public void setStatus(int resId) {

    }
    @Override
    public void setValueCH(int levelCH, int numberChannel) {

    }
    @Override
    public void setErrorReception(boolean incomeErrorReception) {

    }
    @Override
    public void appendMessage(String message) {

    }
    @Override
    public void enableHWButton(boolean enabled) {

    }
    @Override
    public void showToast(String message) {

    }

    @Override
    public void onGestureClick(int position) {

    }

    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {

    }
    @Override
    public void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication, Byte receiveRoughnessOfSensors) {

    }

    @Override
    public void setStartParametersInChartActivity() {

    }

    @Override
    public boolean getFirstRead() {
        return false;
    }

}
