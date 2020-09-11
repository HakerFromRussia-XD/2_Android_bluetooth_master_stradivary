package me.Romans.motorica.ui.chat.view.service_settings;

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
import me.Romans.motorica.ui.chat.view.ChartView;
import me.Romans.motorica.ui.chat.view.Massages;

public class FragmentServiceSettingsMono extends Fragment implements ChartView {
    @BindView(R.id.save_service_settings) Button save_service_settings;
    @BindView(R.id.seekBarRoughness) public SeekBar seekBarRoughness;
    @BindView(R.id.switchInvert) public Switch switchInvert;
    @BindView(R.id.switchNotUseInternalADC) public Switch switchNotUseInternalADC;
    public SeekBar seekBarIStop;
    TextView valueIStop;
    public TextView valueIStop2;
    public View view;
    private ChartActivity chatActivity;
    private int maxCurrent = 1500;
    Massages mMassages = new Massages();

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
        chatActivity.updateServiceSettingsThreadFlag = true;
        chatActivity.startUpdateThread();
        chatActivity.layoutSensors.setVisibility(View.GONE);
        valueIStop2 = view.findViewById(R.id.valueIstop2);
        valueIStop = view.findViewById(R.id.valueIstop);
        seekBarIStop = view.findViewById(R.id.seekBarIstopServiceSettings);


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
                    chatActivity.updateServiceSettingsThreadFlag = false;
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
                byte roughness = (byte) (((byte) seekBar.getProgress()) + 1);
                if(chatActivity.getFlagUseHDLCProtocol()){
                    chatActivity.presenter.onHelloWorld(mMassages.CompileMassageRoughnessHDLC(roughness));
                } else {
                    chatActivity.presenter.onHelloWorld(mMassages.CompileMassageRoughness(roughness));
                }
                chatActivity.setReceiveRoughnessOfSensors(roughness);
                chatActivity.saveVariable( chatActivity.deviceName+"receiveRoughnessOfSensors", (int) roughness);
            }
        });

        switchInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.err.println("FragmentServiceSettings-------------->");
                if (switchInvert.isChecked()){
                    chatActivity.invert = 0x01;
                    chatActivity.presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvert(chatActivity.current, chatActivity.invert));
                    int temp = chatActivity.intValueCH1on;
                    chatActivity.seekBarCH1on2.setProgress((int) (chatActivity.intValueCH2on/(chatActivity.multiplierSeekBar -0.1)));//-0.5
                    chatActivity.seekBarCH2on2.setProgress((int) (temp/(chatActivity.multiplierSeekBar -0.1)));//-0.5
                    chatActivity.invertChannel = true;
                } else {
                    chatActivity.invert = 0x00;
                    chatActivity.presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvert(chatActivity.current, chatActivity.invert));
                    int temp = chatActivity.intValueCH1on;
                    chatActivity.seekBarCH1on2.setProgress((int) (chatActivity.intValueCH2on/(chatActivity.multiplierSeekBar -0.1)));//-0.5
                    chatActivity.seekBarCH2on2.setProgress((int) (temp/(chatActivity.multiplierSeekBar -0.1)));//-0.5
                    chatActivity.invertChannel = false;
                }
            }
        });

        seekBarIStop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxCurrent = seekBar.getProgress();
                valueIStop.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                maxCurrent = seekBar.getProgress();
                chatActivity.maxCurrent = maxCurrent;
                valueIStop.setText(String.valueOf(seekBar.getProgress()));
                chatActivity.current = seekBar.getProgress();
                if(chatActivity.getFlagUseHDLCProtocol()){
                    chatActivity.presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvertHDLC(chatActivity.current));
                } else {
                    chatActivity.presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvert(chatActivity.current, chatActivity.invert));
                }
            }
        });

        switchNotUseInternalADC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchNotUseInternalADC.isChecked()){
                    if(chatActivity.getFlagUseHDLCProtocol()){
                        System.err.println("FragmentServiceSettings--------------> CompileMassegeSettingsNotUseInternalADCHDLC 0");
                        chatActivity.presenter.onHelloWorld(mMassages.CompileMassageSettingsNotUseInternalADCHDLC((byte) 0x00));
                    }
                } else {
                    if(chatActivity.getFlagUseHDLCProtocol()){
                        System.err.println("FragmentServiceSettings--------------> CompileMassegeSettingsNotUseInternalADCHDLC 1");
                        chatActivity.presenter.onHelloWorld(mMassages.CompileMassageSettingsNotUseInternalADCHDLC((byte) 0x01));
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        seekBarIStop.setProgress(chatActivity.maxCurrent);
        seekBarRoughness.setProgress(chatActivity.receiveRoughnessOfSensors);
    }

    @Override
    public void onPause() {
        super.onPause();
        chatActivity.seekBarIStop.setProgress(maxCurrent);
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
            chatActivity.updateServiceSettingsThreadFlag = false;
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
    public void enableInterface(boolean enabled) {

    }
    @Override
    public void showToast(String message) {

    }

    @Override
    public void showToastWithoutConnection() {

    }

    @Override
    public void onGestureClick(int position) {

    }

    @Override
    public void setGeneralValue(int receiveCurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {

    }
    @Override
    public void setStartParameters(Integer receiveCurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication, Byte receiveRoughnessOfSensors) {

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
    public void setStartParametersCurrent(Integer receiveCurrent) {

    }

    @Override
    public void setStartParametersBlock(Byte receiveBlockIndication) {

    }

    @Override
    public void setStartParametersRoughness(Byte receiveRoughnessOfSensors) {

    }

    @Override
    public void setStartParametersBattery(Integer receiveBatteryTension) {

    }

}
