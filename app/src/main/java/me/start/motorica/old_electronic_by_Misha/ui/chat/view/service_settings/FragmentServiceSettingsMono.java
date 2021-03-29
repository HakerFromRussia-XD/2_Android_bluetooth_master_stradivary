package me.start.motorica.old_electronic_by_Misha.ui.chat.view.service_settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.Objects;

import butterknife.ButterKnife;
import me.start.motorica.new_electronic_by_Rodeon.WDApplication;
import me.start.motorica.R;
import me.start.motorica.old_electronic_by_Misha.ui.chat.data.ChatModule;
import me.start.motorica.old_electronic_by_Misha.ui.chat.data.DaggerChatComponent;
import me.start.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity;
import me.start.motorica.old_electronic_by_Misha.ui.chat.view.ChartView;
import me.start.motorica.old_electronic_by_Misha.ui.chat.view.Massages;

@SuppressLint("UseSwitchCompatOrMaterialCode")
public class FragmentServiceSettingsMono extends Fragment implements ChartView {
    Button save_service_settings;
    public SeekBar seekBarRoughness;
    public Switch switchInvert;
    public Switch switchNotUseInternalADC;
    public SeekBar seekBarIStop;
    TextView valueIStop;
    public TextView valueIStop2;
    public View view;
    private ChartActivity chartActivity;
    Massages mMassages = new Massages();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_service_settings_mono, container, false);

        DaggerChatComponent.builder()
                .bluetoothModule(Objects.requireNonNull(WDApplication.app()).bluetoothModule())
                .chatModule(new ChatModule(FragmentServiceSettingsMono.this))
                .build().inject(FragmentServiceSettingsMono.this);
        ButterKnife.bind(this, view);

        if (getActivity() != null) { chartActivity = (ChartActivity) getActivity();}
        chartActivity.graphThreadFlag = false;
        chartActivity.updateServiceSettingsThreadFlag = true;
        chartActivity.startUpdateThread();
        chartActivity.layoutSensors.setVisibility(View.GONE);
        valueIStop2 = view.findViewById(R.id.valueIstop2);
        valueIStop = view.findViewById(R.id.valueIstop);
        seekBarIStop = view.findViewById(R.id.seekBarIstopServiceSettings);
        save_service_settings = view.findViewById(R.id.save_service_settings);
        seekBarRoughness = view.findViewById(R.id.seekBarRoughness);
        switchInvert = view.findViewById(R.id.switchInvert);
        switchNotUseInternalADC = view.findViewById(R.id.switchNotUseInternalADC);
        seekBarIStop.setProgress(chartActivity.current);
        valueIStop.setText(String.valueOf(seekBarIStop.getProgress()));


        save_service_settings.setOnClickListener(v -> {
            if (getActivity() != null) {
                chartActivity.fragmentManager.beginTransaction()
                        .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                        .remove(chartActivity.fragmentServiceSettingsMono)
                        .commit();
                chartActivity.graphThreadFlag = true;
                chartActivity.startGraphEnteringDataThread();
                chartActivity.myMenu.setGroupVisible(R.id.service_settings, true);
                chartActivity.myMenu.setGroupVisible(R.id.modes, false);
                chartActivity.updateServiceSettingsThreadFlag = false;
                chartActivity.layoutSensors.setVisibility(View.VISIBLE);
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
                if(chartActivity.getFlagUseHDLCProtocol()){
                    chartActivity.presenter.onHelloWorld(mMassages.CompileMassageRoughnessHDLC(roughness));
                } else {
                    chartActivity.presenter.onHelloWorld(mMassages.CompileMassageRoughness(roughness));
                }
                chartActivity.setReceiveRoughnessOfSensors(roughness);
                chartActivity.saveVariable( ChartActivity.deviceName +"receiveRoughnessOfSensors", (int) roughness);
            }
        });

        switchInvert.setOnClickListener(v -> {
            System.err.println("FragmentServiceSettings-------------->");
            if (switchInvert.isChecked()){
                chartActivity.invert = 0x01;
                chartActivity.presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvert(chartActivity.current, chartActivity.invert));
                int temp = chartActivity.intValueCH1on;
                chartActivity.seekBarCH1on2.setProgress((int) (chartActivity.intValueCH2on/(chartActivity.multiplierSeekBar -0.1)));//-0.5
                chartActivity.seekBarCH2on2.setProgress((int) (temp/(chartActivity.multiplierSeekBar -0.1)));//-0.5
                chartActivity.invertChannel = true;
            } else {
                chartActivity.invert = 0x00;
                chartActivity.presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvert(chartActivity.current, chartActivity.invert));
                int temp = chartActivity.intValueCH1on;
                chartActivity.seekBarCH1on2.setProgress((int) (chartActivity.intValueCH2on/(chartActivity.multiplierSeekBar -0.1)));//-0.5
                chartActivity.seekBarCH2on2.setProgress((int) (temp/(chartActivity.multiplierSeekBar -0.1)));//-0.5
                chartActivity.invertChannel = false;
            }
        });

        seekBarIStop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueIStop.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                chartActivity.current = seekBar.getProgress();
                chartActivity.saveVariable( ChartActivity.deviceName +"current", chartActivity.current);
                if(chartActivity.getFlagUseHDLCProtocol()){
                    chartActivity.presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvertHDLC(chartActivity.current));
                } else {
                    chartActivity.presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvert(chartActivity.current, chartActivity.invert));
                }
            }
        });

        switchNotUseInternalADC.setOnClickListener(v -> {
            if (switchNotUseInternalADC.isChecked()){
                if(chartActivity.getFlagUseHDLCProtocol()){
                    System.err.println("FragmentServiceSettings--------------> CompileMassegeSettingsNotUseInternalADCHDLC 0");
                    chartActivity.presenter.onHelloWorld(mMassages.CompileMassageSettingsNotUseInternalADCHDLC((byte) 0x00));
                }
            } else {
                if(chartActivity.getFlagUseHDLCProtocol()){
                    System.err.println("FragmentServiceSettings--------------> CompileMassegeSettingsNotUseInternalADCHDLC 1");
                    chartActivity.presenter.onHelloWorld(mMassages.CompileMassageSettingsNotUseInternalADCHDLC((byte) 0x01));
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        seekBarIStop.setProgress(chartActivity.loadVariable((ChartActivity.deviceName +"current")));
        seekBarRoughness.setProgress(chartActivity.receiveRoughnessOfSensors);
    }

    @Override
    public void onPause() {
        super.onPause();
        chartActivity.seekBarIStop.setProgress(chartActivity.loadVariable((ChartActivity.deviceName +"current")));
    }


    public void backPressed() {
        if (getActivity() != null) {
            System.err.println("fragmentServiceSettingsMono----> выполнение  backPressed()");
            chartActivity.fragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                    .remove(chartActivity.fragmentServiceSettingsMono)
                    .commit();
            chartActivity.graphThreadFlag = true;
            chartActivity.startGraphEnteringDataThread();
            chartActivity.myMenu.setGroupVisible(R.id.service_settings, true);
            chartActivity.myMenu.setGroupVisible(R.id.modes, false);
            chartActivity.updateServiceSettingsThreadFlag = false;
            chartActivity.layoutSensors.setVisibility(View.VISIBLE);
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

//    @Override
//    public void setStartParametersBattery(Integer receiveBatteryTension) {
//
//    }

}
