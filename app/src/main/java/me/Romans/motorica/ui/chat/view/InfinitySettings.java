package me.Romans.motorica.ui.chat.view;



import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.Romans.motorica.MyApp;
import me.Romans.motorica.ui.chat.data.ChatModule;
import me.Romans.motorica.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.data.DaggerChatComponent;


public class InfinitySettings extends AppCompatActivity implements ChatView {

    @BindView(R.id.seekBarTimeOpen) SeekBar seekBarTimeOpen;
    @BindView(R.id.seekBarTimeClose) SeekBar seekBarTimeClose;
    @BindView(R.id.seekBarSpeedOpen) SeekBar seekBarSpeedOpen;
    @BindView(R.id.seekBarSpeedClose) SeekBar seekBarSpeedClose;
    @BindView(R.id.valueTimeOpen) TextView valueTimeOpen;
    @BindView(R.id.valueTimeClose) TextView valueTimeClose;
    @BindView(R.id.valueSpeedOpen) TextView valueSpeedOpen;
    @BindView(R.id.valueSpeedClose) TextView valueSpeedClose;
    private int intValueTimeOpen = 1000;
    private int intValueTimeClose = 1000;
    private int intValueSpeedOpen = 999;
    private int intValueSpeedClose = 999;
    public byte[] TextByteTriggerInfinitySettings = new byte[9];

    @Inject
    ChatPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infinity_settings);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(InfinitySettings.this))
                .build().inject(InfinitySettings.this);
        ButterKnife.bind(InfinitySettings.this);

//        final BluetoothDevice device = getIntent().getExtras().getParcelable("device");
        presenter.onCreate(getIntent());

        seekBarTimeOpen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueTimeOpen.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueTimeOpen.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueTimeOpen.setText(String.valueOf(seekBar.getProgress()));
                intValueTimeOpen = seekBarTimeOpen.getProgress();
                presenter.onHelloWorld(CompileMassegeInfinitySettings(intValueTimeOpen, intValueTimeClose, intValueSpeedOpen, intValueSpeedClose));
            }
        });

        seekBarTimeClose.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueTimeClose.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueTimeClose.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueTimeClose.setText(String.valueOf(seekBar.getProgress()));
                intValueTimeClose = seekBarTimeClose.getProgress();
                presenter.onHelloWorld(CompileMassegeInfinitySettings(intValueTimeOpen, intValueTimeClose, intValueSpeedOpen, intValueSpeedClose));
            }
        });

        seekBarSpeedOpen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueSpeedOpen.setText("0." + seekBar.getProgress());
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueSpeedOpen.setText("0." + seekBar.getProgress());
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueSpeedOpen.setText("0." + seekBar.getProgress());
                intValueSpeedOpen = seekBarSpeedOpen.getProgress();
                presenter.onHelloWorld(CompileMassegeInfinitySettings(intValueTimeOpen, intValueTimeClose, intValueSpeedOpen, intValueSpeedClose));
            }
        });

        seekBarSpeedClose.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueSpeedClose.setText("0." + seekBar.getProgress());
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueSpeedClose.setText("0." + seekBar.getProgress());
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueSpeedClose.setText("0." + seekBar.getProgress());
                intValueSpeedClose = seekBarSpeedClose.getProgress();
                presenter.onHelloWorld(CompileMassegeInfinitySettings(intValueTimeOpen, intValueTimeClose, intValueSpeedOpen, intValueSpeedClose));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart(InfinitySettings.this);
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.disconnect();
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
        seekBarTimeOpen.setEnabled(enabled);
        seekBarTimeClose.setEnabled(enabled);
        seekBarSpeedOpen.setEnabled(enabled);
        seekBarSpeedClose.setEnabled(enabled);
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

    private byte[] CompileMassegeInfinitySettings (int waitOpenTime, int waitCloseTime, int openSpeed, int closeSpeed){
        TextByteTriggerInfinitySettings[0] = 0x0A;
        TextByteTriggerInfinitySettings[1] = (byte) waitOpenTime;
        TextByteTriggerInfinitySettings[2] = (byte) (waitOpenTime >> 8);
        TextByteTriggerInfinitySettings[3] = (byte) waitCloseTime;
        TextByteTriggerInfinitySettings[4] = (byte) (waitCloseTime >> 8);
        TextByteTriggerInfinitySettings[5] = (byte) openSpeed;
        TextByteTriggerInfinitySettings[6] = (byte) (openSpeed >> 8);
        TextByteTriggerInfinitySettings[7] = (byte) closeSpeed;
        TextByteTriggerInfinitySettings[8] = (byte) (closeSpeed >> 8);
        return TextByteTriggerInfinitySettings;
    }
}
