package me.aflak.libraries.ui.chat.view.Gripper_settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.ButterKnife;
import me.aflak.libraries.MyApp;
import me.aflak.libraries.R;
import me.aflak.libraries.ui.chat.data.ChatModule;
import me.aflak.libraries.ui.chat.data.DaggerChatComponent;
import me.aflak.libraries.ui.chat.presenter.ChatPresenter;
import me.aflak.libraries.ui.chat.view.ChatView;


public class GripperSettings extends AppCompatActivity implements ChatPresenter, ChatView {
    private SeekBar seekBarFinger1Angle;
    private SeekBar seekBarFinger2Angle;
    private SeekBar seekBarFinger3Angle;
    private SeekBar seekBarFinger4Angle;
    private SeekBar seekBarFinger5Angle;
    private SeekBar seekBarFinger1Speed;
    private SeekBar seekBarFinger2Speed;
    private SeekBar seekBarFinger3Speed;
    private SeekBar seekBarFinger4Speed;
    private SeekBar seekBarFinger5Speed;
    private TextView valueFinger1Angle;
    private TextView valueFinger2Angle;
    private TextView valueFinger3Angle;
    private TextView valueFinger4Angle;
    private TextView valueFinger5Angle;
    private TextView valueFinger1Speed;
    private TextView valueFinger2Speed;
    private TextView valueFinger3Speed;
    private TextView valueFinger4Speed;
    private TextView valueFinger5Speed;
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
    private byte GESTURE_SETTINGS = 0x21;
    private byte NUMBER_CELL = 0x00;
    public byte[] TextByteTreeg = new byte[8];
    private static final String TAG = "GripperSettings";
    
    @Inject ChatPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gripper_settings);
        seekBarFinger1Angle = findViewById(R.id.seekBarFinger1Angle);
        seekBarFinger2Angle = findViewById(R.id.seekBarFinger2Angle);
        seekBarFinger3Angle = findViewById(R.id.seekBarFinger3Angle);
        seekBarFinger4Angle = findViewById(R.id.seekBarFinger4Angle);
        seekBarFinger5Angle = findViewById(R.id.seekBarFinger5Angle);
        seekBarFinger1Speed = findViewById(R.id.seekBarFinger1Speed);
        seekBarFinger2Speed = findViewById(R.id.seekBarFinger2Speed);
        seekBarFinger3Speed = findViewById(R.id.seekBarFinger3Speed);
        seekBarFinger4Speed = findViewById(R.id.seekBarFinger4Speed);
        seekBarFinger5Speed = findViewById(R.id.seekBarFinger5Speed);
        valueFinger1Angle = findViewById(R.id.valueFinger1Angle);
        valueFinger2Angle = findViewById(R.id.valueFinger2Angle);
        valueFinger3Angle = findViewById(R.id.valueFinger3Angle);
        valueFinger4Angle = findViewById(R.id.valueFinger4Angle);
        valueFinger5Angle = findViewById(R.id.valueFinger5Angle);
        valueFinger1Speed = findViewById(R.id.valueFinger1Speed);
        valueFinger2Speed = findViewById(R.id.valueFinger2Speed);
        valueFinger3Speed = findViewById(R.id.valueFinger3Speed);
        valueFinger4Speed = findViewById(R.id.valueFinger4Speed);
        valueFinger5Speed = findViewById(R.id.valueFinger5Speed);

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(this))
                .build().inject(this);
        ButterKnife.bind(this);

        presenter.onCreate(getIntent());
        seekBarFinger1Angle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueFinger1Angle.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueFinger1Angle.setText(String.valueOf(seekBar.getProgress()));
                intValueFinger1Angle = seekBarFinger1Angle.getProgress();
                numberFinger = 0x01;
                TextByteTreeg[0] = indicatorTypeMessage;
                TextByteTreeg[1] = numberFinger;
                TextByteTreeg[2] = requestType;
                TextByteTreeg[3] = GESTURE_SETTINGS;
                TextByteTreeg[4] = NUMBER_CELL;
                TextByteTreeg[5] = (byte) intValueFinger1Angle;
                TextByteTreeg[6] = (byte) intValueFinger1Speed;
                for (int i = 0; i < TextByteTreeg.length-1; i++){
                    TextByteTreeg[7] += TextByteTreeg[i];
                    TextByteTreeg[7] = (byte) (TextByteTreeg[7] << 1);
                }
                presenter.onHelloWorld(TextByteTreeg);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueFinger1Angle.setText(String.valueOf(seekBar.getProgress()));
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
            }
        });
    }

    @Override
    public void onCreate(Intent intent) {

    }

    @Override
    public void onHelloWorld(byte[] textbyte) {

    }

    @Override
    public void onStart(Activity activity) {

    }
    
    @Override
    public void onStop() {
        super.onStop();
//        presenter.onStop();
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

//    @Override
//    public void onGestureClick(int position) {
//
//    }
}