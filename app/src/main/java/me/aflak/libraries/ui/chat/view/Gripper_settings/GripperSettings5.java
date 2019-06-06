package me.aflak.libraries.ui.chat.view.Gripper_settings;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.aflak.libraries.MyApp;
import me.aflak.libraries.R;
import me.aflak.libraries.ui.chat.data.ChatModule;
import me.aflak.libraries.ui.chat.data.DaggerChatComponent;
import me.aflak.libraries.ui.chat.presenter.ChatPresenter;
import me.aflak.libraries.ui.chat.view.ChatView;
import me.aflak.libraries.ui.chat.view.Gesture_settings.Gesture_settings;


public class GripperSettings5 extends AppCompatActivity implements ChatView {
    @BindView(R.id.seekBarFinger1Angle) SeekBar seekBarFinger1Angle;
    @BindView(R.id.seekBarFinger2Angle) SeekBar seekBarFinger2Angle;
    @BindView(R.id.seekBarFinger3Angle) SeekBar seekBarFinger3Angle;
    @BindView(R.id.seekBarFinger4Angle) SeekBar seekBarFinger4Angle;
    @BindView(R.id.seekBarFinger5Angle) SeekBar seekBarFinger5Angle;
    @BindView(R.id.seekBarFinger1Speed) SeekBar seekBarFinger1Speed;
    @BindView(R.id.seekBarFinger2Speed) SeekBar seekBarFinger2Speed;
    @BindView(R.id.seekBarFinger3Speed) SeekBar seekBarFinger3Speed;
    @BindView(R.id.seekBarFinger4Speed) SeekBar seekBarFinger4Speed;
    @BindView(R.id.seekBarFinger5Speed) SeekBar seekBarFinger5Speed;
    @BindView(R.id.valueFinger1Angle) TextView valueFinger1Angle;
    @BindView(R.id.valueFinger2Angle) TextView valueFinger2Angle;
    @BindView(R.id.valueFinger3Angle) TextView valueFinger3Angle;
    @BindView(R.id.valueFinger4Angle) TextView valueFinger4Angle;
    @BindView(R.id.valueFinger5Angle) TextView valueFinger5Angle;
    @BindView(R.id.valueFinger1Speed) TextView valueFinger1Speed;
    @BindView(R.id.valueFinger2Speed) TextView valueFinger2Speed;
    @BindView(R.id.valueFinger3Speed) TextView valueFinger3Speed;
    @BindView(R.id.valueFinger4Speed) TextView valueFinger4Speed;
    @BindView(R.id.valueFinger5Speed) TextView valueFinger5Speed;
    @BindView(R.id.save_gripper_settings) Button save_gripper_settings;
    private int intValueFinger1Angle = 50;
    private int intValueFinger2Angle = 50;
    private int intValueFinger3Angle = 50;
    private int intValueFinger4Angle = 50;
    private int intValueFinger5Angle = 50;
    private int intValueFinger1Speed = 20;
    private int intValueFinger2Speed = 20;
    private int intValueFinger3Speed = 20;
    private int intValueFinger4Speed = 20;
    private int intValueFinger5Speed = 20;
    private byte indicatorTypeMessage = 0x03;
    private byte numberFinger;
    private byte requestType = 0x02;
    private byte NUMBER_CELL = 0x04;
    public byte[] TextByteTreegSettings = new byte[8];
    public byte[] TextByteTreegControl = new byte[6];
    private static final String TAG = "GripperSettings";
    
    @Inject ChatPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gripper_settings);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(GripperSettings5.this))
                .build().inject(GripperSettings5.this);
        ButterKnife.bind(GripperSettings5.this);

        final BluetoothDevice device = getIntent().getExtras().getParcelable("device");
        save_gripper_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                presenter.onStop();
                presenter.disconnect();
//                presenter.disable();
                Intent intent = new Intent(GripperSettings5.this, Gesture_settings.class);
                intent.putExtra("device", device);
                startActivity(intent);
                finish();
            }
        });

        presenter.onCreate(getIntent());
        seekBarFinger1Angle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger1Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger1Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger1Angle.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger1Angle = seekBarFinger1Angle.getProgress();
                numberFinger = 0x01;
                CompileMassege(numberFinger, intValueFinger1Angle, intValueFinger1Speed);
                presenter.onHelloWorld(TextByteTreegSettings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                    }
                }, GripperSettings.delay);
            }
        });

        seekBarFinger2Angle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger2Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger2Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger2Angle.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger2Angle = seekBarFinger2Angle.getProgress();
                numberFinger = 0x02;
                CompileMassege(numberFinger, intValueFinger2Angle, intValueFinger2Speed);
                presenter.onHelloWorld(TextByteTreegSettings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                    }
                }, GripperSettings.delay);
            }
        });

        seekBarFinger3Angle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger3Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger3Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger3Angle.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger3Angle = seekBarFinger3Angle.getProgress();
                numberFinger = 0x03;
                CompileMassege(numberFinger, intValueFinger3Angle, intValueFinger3Speed);
                presenter.onHelloWorld(TextByteTreegSettings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                    }
                }, GripperSettings.delay);
            }
        });

        seekBarFinger4Angle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger4Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger4Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger4Angle.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger4Angle = seekBarFinger4Angle.getProgress();
                numberFinger = 0x04;
                CompileMassege(numberFinger, intValueFinger4Angle, intValueFinger4Speed);
                presenter.onHelloWorld(TextByteTreegSettings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                    }
                }, GripperSettings.delay);
            }
        });

        seekBarFinger5Angle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger5Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger5Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger5Angle.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger5Angle = seekBarFinger5Angle.getProgress();
                numberFinger = 0x05;
                CompileMassege(numberFinger, intValueFinger5Angle, intValueFinger5Speed);
                presenter.onHelloWorld(TextByteTreegSettings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                    }
                }, GripperSettings.delay);
            }
        });

        seekBarFinger1Speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger1Speed.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger1Speed.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger1Speed.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger1Speed = seekBarFinger1Speed.getProgress();
                numberFinger = 0x01;
                CompileMassege(numberFinger, intValueFinger1Angle, intValueFinger1Speed);
                presenter.onHelloWorld(TextByteTreegSettings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                    }
                }, GripperSettings.delay);
            }
        });

        seekBarFinger2Speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger2Speed.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger2Speed.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger2Speed.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger2Speed = seekBarFinger2Speed.getProgress();
                numberFinger = 0x02;
                CompileMassege(numberFinger, intValueFinger2Angle, intValueFinger2Speed);
                presenter.onHelloWorld(TextByteTreegSettings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                    }
                }, GripperSettings.delay);
            }
        });

        seekBarFinger3Speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger3Speed.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger3Speed.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger3Speed.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger3Speed = seekBarFinger3Speed.getProgress();
                numberFinger = 0x03;
                CompileMassege(numberFinger, intValueFinger3Angle, intValueFinger3Speed);
                presenter.onHelloWorld(TextByteTreegSettings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                    }
                }, GripperSettings.delay);
            }
        });

        seekBarFinger4Speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger4Speed.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger4Speed.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger4Speed.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger4Speed = seekBarFinger4Speed.getProgress();
                numberFinger = 0x04;
                CompileMassege(numberFinger, intValueFinger4Angle, intValueFinger4Speed);
                presenter.onHelloWorld(TextByteTreegSettings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                    }
                }, GripperSettings.delay);
            }
        });

        seekBarFinger5Speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger5Speed.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger5Speed.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger5Speed.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger5Speed = seekBarFinger5Speed.getProgress();
                numberFinger = 0x05;
                CompileMassege(numberFinger, intValueFinger5Angle, intValueFinger5Speed);
                presenter.onHelloWorld(TextByteTreegSettings);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                    }
                }, GripperSettings.delay);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart(GripperSettings5.this);
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
    public void showToast(String message) {

    }

    @Override
    public void onGestureClick(int position) {

    }

    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {

    }

    @Override
    public void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication) {

    }

    @Override
    public void enableHWButton(boolean enabled) {
        seekBarFinger1Angle.setEnabled(enabled);
        seekBarFinger2Angle.setEnabled(enabled);
        seekBarFinger3Angle.setEnabled(enabled);
        seekBarFinger4Angle.setEnabled(enabled);
        seekBarFinger5Angle.setEnabled(enabled);
        seekBarFinger1Speed.setEnabled(enabled);
        seekBarFinger2Speed.setEnabled(enabled);
        seekBarFinger3Speed.setEnabled(enabled);
        seekBarFinger4Speed.setEnabled(enabled);
        seekBarFinger5Speed.setEnabled(enabled);
    }

    private byte[] CompileMassege(byte numberFinger, int intValueFingerAngle, int intValueFingerSpeed){
        TextByteTreegSettings[0] = indicatorTypeMessage;
        TextByteTreegSettings[1] = numberFinger;
        TextByteTreegSettings[2] = requestType;
        TextByteTreegSettings[3] = GripperSettings.GESTURE_SETTINGS;
        TextByteTreegSettings[4] = NUMBER_CELL;
        TextByteTreegSettings[5] = (byte) intValueFingerSpeed;
        TextByteTreegSettings[6] = (byte) intValueFingerAngle;
        TextByteTreegSettings[7] = presenter.calculationCRC(TextByteTreegSettings);
        return TextByteTreegSettings;
    }

    private byte[] CompileMassegeControl (byte numberFinger){
        TextByteTreegControl[0] = 0x05;
        TextByteTreegControl[1] = numberFinger;
        TextByteTreegControl[2] = 0x02;
        TextByteTreegControl[3] = 0x14;
        TextByteTreegControl[4] = NUMBER_CELL;
        TextByteTreegControl[5] = presenter.calculationCRC(TextByteTreegControl);
        return TextByteTreegControl;
    }
}