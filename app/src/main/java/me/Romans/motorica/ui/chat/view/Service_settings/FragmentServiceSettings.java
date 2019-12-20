package me.Romans.motorica.ui.chat.view.Service_settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.Romans.motorica.MyApp;
import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.data.ChatModule;
import me.Romans.motorica.ui.chat.data.DaggerChatComponent;
import me.Romans.motorica.ui.chat.view.ChartActivity;
import me.Romans.motorica.ui.chat.view.ChatView;

public class FragmentServiceSettings extends Fragment implements ChatView {
    @BindView(R.id.save_service_settings) Button save_service_settings;
    @BindView(R.id.seekBarRoughness) public SeekBar seekBarRoughness;
    @BindView(R.id.switchInvert) public Switch switchInvert;
    public View view;
    private ChartActivity chatActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_service_settings, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule((ChatView) FragmentServiceSettings.this))
                .build().inject(FragmentServiceSettings.this);
        ButterKnife.bind(this, view);

        if (getActivity() != null) {chatActivity = (ChartActivity) getActivity();}
        chatActivity.graphThreadFlag = false;
        chatActivity.updateSeviceSettingsThreadFlag = true;
        chatActivity.startUpdateThread();
        chatActivity.layoutSensors.setVisibility(View.GONE);

        if (chatActivity.invertChannel){
            switchInvert.setChecked(true);
        } else {
            switchInvert.setChecked(false);
        }

        save_service_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    chatActivity.fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .remove(chatActivity.fragmentServiceSettings)
                            .commit();
                    chatActivity.navigation.clearAnimation();
                    chatActivity.navigation.animate().translationY(0).setDuration(200);
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
                if(chatActivity.getFlagUseHDLCProcol()){
                    chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeRouhnessHDLC((byte) (((byte) seekBar.getProgress()) + 1)));
                } else {
                    chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeRouhness((byte) (((byte) seekBar.getProgress()) + 1)));
                }
            }
        });

        switchInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchInvert.isChecked()){
                    chatActivity.invert = 0x01;
                    if(chatActivity.getFlagUseHDLCProcol()){
                        chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvertHDLC(chatActivity.curent));
                    } else {
                        chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvert(chatActivity.curent, chatActivity.invert));
                    }
                    Integer temp = chatActivity.intValueCH1on;
                    chatActivity.seekBarCH1on2.setProgress((int) (chatActivity.intValueCH2on/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.seekBarCH2on2.setProgress((int) (temp/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.invertChannel = true;
                } else {
                    chatActivity.invert = 0x00;
                    if(chatActivity.getFlagUseHDLCProcol()){
                        chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvertHDLC(chatActivity.curent));
                    } else {
                        chatActivity.presenter.onHelloWorld(chatActivity.CompileMassegeCurentSettingsAndInvert(chatActivity.curent, chatActivity.invert));
                    }
                    Integer temp = chatActivity.intValueCH1on;
                    chatActivity.seekBarCH1on2.setProgress((int) (chatActivity.intValueCH2on/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.seekBarCH2on2.setProgress((int) (temp/(chatActivity.multiplierSeekbar-0.1)));//-0.5
                    chatActivity.invertChannel = false;
                }
            }
        });

        return view;
    }

    public void backPressed() {
        if (getActivity() != null) {
            chatActivity.fragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                    .remove(chatActivity.fragmentServiceSettings)
                    .commit();
            chatActivity.navigation.clearAnimation();
            chatActivity.navigation.animate().translationY(0).setDuration(200);
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
    public void setStartParametersCurrrent(Integer receiveСurrent) {

    }

}
