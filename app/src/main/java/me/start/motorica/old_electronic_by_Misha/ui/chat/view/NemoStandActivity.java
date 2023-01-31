package me.start.motorica.old_electronic_by_Misha.ui.chat.view;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import javax.inject.Inject;
import butterknife.ButterKnife;
import me.start.motorica.R;
import me.start.motorica.new_electronic_by_Rodeon.WDApplication;
import me.start.motorica.old_electronic_by_Misha.ui.chat.data.DaggerNemoStandComponent;
import me.start.motorica.old_electronic_by_Misha.ui.chat.data.NemoStandModule;
import me.start.motorica.old_electronic_by_Misha.ui.chat.presenter.NemoStandPresenter;
import timber.log.Timber;

public class NemoStandActivity extends AppCompatActivity implements NemoStandView {
    ImageView borderGray;
    ImageView borderGreen;
    ImageView borderRed;
    Button electrode1Btn;
    Button electrode2Btn;
    Button electrode3Btn;
    Button electrode4Btn;
    Button electrode5Btn;
    Button electrode6Btn;
    Button electrode7Btn;
    Button electrode8Btn;
    Button startStopBtn;
    View amplitudeDecBtn;
    View amplitudeAddBtn;
    NumberPicker amplitudeNp;
    NumberPicker _1Np;
    NumberPicker _2Np;
    NumberPicker _3Np;
    Timer timer = new Timer();
    public enum SelectStation {UNSELECTED_OBJECT, SELECT_MINUS, SELECT_PLUS}
    private static SelectStation selectStation1;
    private static SelectStation selectStation2;
    private static SelectStation selectStation3;
    private static SelectStation selectStation4;
    private static SelectStation selectStation5;
    private static SelectStation selectStation6;
    private static SelectStation selectStation7;
    private static SelectStation selectStation8;
    private static final int START_AMPLITUDE = 1;
    private static final int AMPLITUDE_MULTIPLIER = 20;
    private static int amplitude = START_AMPLITUDE;
    private int amplitudeR1 = START_AMPLITUDE;
    private int amplitudeG1 = START_AMPLITUDE;
    private int amplitudeR2 = START_AMPLITUDE;
    private int amplitudeG2 = START_AMPLITUDE;
    private int amplitudeR3 = START_AMPLITUDE;
    private int amplitudeG3 = START_AMPLITUDE;
    private int amplitudeR4 = START_AMPLITUDE;
    private int amplitudeG4 = START_AMPLITUDE;
    private int amplitudeR5 = START_AMPLITUDE;
    private int amplitudeG5 = START_AMPLITUDE;
    private int amplitudeR6 = START_AMPLITUDE;
    private int amplitudeG6 = START_AMPLITUDE;
    private int amplitudeR7 = START_AMPLITUDE;
    private int amplitudeG7 = START_AMPLITUDE;
    private int amplitudeR8 = START_AMPLITUDE;
    private int amplitudeG8 = START_AMPLITUDE;
    private final int test = 0;
    private boolean startTest = false;
    private boolean activeIncentive = true;
    private Menu myMenu;
    private Toolbar toolbar;

    public static String deviceName;
    private final boolean firstSendPass = true;
    //    for delay
    private boolean runOnUi;

    //	  for transfer
    Massages mMassages = new Massages();
    public byte[] TextByteSetGeneralParcel = new byte[2];

    //    save
    SharedPreferences sharedPreferences;


    @Inject
    public NemoStandPresenter presenter;



    @SuppressLint({"NewApi", "ClickableViewAccessibility", "UseCompatLoadingForDrawables", "SetTextI18n"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //changing statusbar
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.tool_bar));
        setContentView(R.layout.nemo_stand);
        final float scale = getResources().getDisplayMetrics().density;


        toolbar = (Toolbar) findViewById(R.id.nemo_toolbar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitle(deviceName);
        toolbar.setSubtitleTextColor(getColor(R.color.end2_gradient));
        toolbar.inflateMenu(R.menu.menu_main);
        myMenu = toolbar.getMenu();
        myMenu.setGroupVisible(R.id.nemo_logo, true);
        myMenu.setGroupVisible(R.id.modes, false);
        myMenu.setGroupVisible(R.id.service_settings, false);


        borderGray = findViewById(R.id.borderGray);
        borderGreen = findViewById(R.id.borderGreen);
        borderRed = findViewById(R.id.borderRed);
        electrode1Btn = findViewById(R.id.electrode_1_btn);
        electrode2Btn = findViewById(R.id.electrode_2_btn);
        electrode3Btn = findViewById(R.id.electrode_3_btn);
        electrode4Btn = findViewById(R.id.electrode_4_btn);
        electrode5Btn = findViewById(R.id.electrode_5_btn);
        electrode6Btn = findViewById(R.id.electrode_6_btn);
        electrode7Btn = findViewById(R.id.electrode_7_btn);
        electrode8Btn = findViewById(R.id.electrode_8_btn);
        startStopBtn = findViewById(R.id.start_stop_btn);
        amplitudeNp = findViewById(R.id.unit_amplitude);
        _1Np = findViewById(R.id.unit_burst);
        _2Np = findViewById(R.id.subunit_burst3);
        _3Np = findViewById(R.id.subunit_burstawd3);

        amplitudeDecBtn = findViewById(R.id.amplitude_dec_btn);
        amplitudeAddBtn = findViewById(R.id.amplitude_add_btn);

        selectStation1 = SelectStation.UNSELECTED_OBJECT;
        selectStation2 = SelectStation.UNSELECTED_OBJECT;
        selectStation3 = SelectStation.UNSELECTED_OBJECT;
        selectStation4 = SelectStation.UNSELECTED_OBJECT;
        selectStation5 = SelectStation.UNSELECTED_OBJECT;
        selectStation6 = SelectStation.UNSELECTED_OBJECT;
        selectStation7 = SelectStation.UNSELECTED_OBJECT;
        selectStation8 = SelectStation.UNSELECTED_OBJECT;

        _1Np.setMaxValue(40);
        _1Np.setMinValue(40);
        _2Np.setMaxValue(500);
        _2Np.setMinValue(500);
        _3Np.setMaxValue(1000);
        _3Np.setMinValue(1000);

        amplitudeNp.setMaxValue(10);
        amplitudeNp.setMinValue(0);
        amplitudeNp.setValue(START_AMPLITUDE);

        electrode1Btn.setOnClickListener(view -> {
            switch (selectStation1) {
                case UNSELECTED_OBJECT :
                    selectStation1 = SelectStation.SELECT_MINUS;
                    electrode1Btn.setBackground(getDrawable(R.drawable.border_nemo_minus));
                    electrode1Btn.setTextColor(getColor(R.color.white));
                    electrode1Btn.setText("-");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_MINUS :
                    selectStation1 = SelectStation.SELECT_PLUS;
                    electrode1Btn.setBackground(getDrawable(R.drawable.border_nemo_plus));
                    electrode1Btn.setTextColor(getColor(R.color.white));
                    electrode1Btn.setText("+");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_PLUS:
                    selectStation1 = SelectStation.UNSELECTED_OBJECT;
                    electrode1Btn.setBackground(getDrawable(R.drawable.border_nemo));
                    electrode1Btn.setTextColor(getColor(R.color.black));
                    electrode1Btn.setText("1");
                    if (startTest) {compileMassage();}
                    break;
            }
        });
        electrode2Btn.setOnClickListener(view -> {
            switch (selectStation2) {
                case UNSELECTED_OBJECT :
                    selectStation2 = SelectStation.SELECT_MINUS;
                    electrode2Btn.setBackground(getDrawable(R.drawable.border_nemo_minus));
                    electrode2Btn.setTextColor(getColor(R.color.white));
                    electrode2Btn.setText("-");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_MINUS :
                    selectStation2 = SelectStation.SELECT_PLUS;
                    electrode2Btn.setBackground(getDrawable(R.drawable.border_nemo_plus));
                    electrode2Btn.setTextColor(getColor(R.color.white));
                    electrode2Btn.setText("+");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_PLUS:
                    selectStation2 = SelectStation.UNSELECTED_OBJECT;
                    electrode2Btn.setBackground(getDrawable(R.drawable.border_nemo));
                    electrode2Btn.setTextColor(getColor(R.color.black));
                    electrode2Btn.setText("2");
                    if (startTest) {compileMassage();}
                    break;
            }
        });
        electrode3Btn.setOnClickListener(view -> {
            switch (selectStation3) {
                case UNSELECTED_OBJECT :
                    selectStation3 = SelectStation.SELECT_MINUS;
                    electrode3Btn.setBackground(getDrawable(R.drawable.border_nemo_minus));
                    electrode3Btn.setTextColor(getColor(R.color.white));
                    electrode3Btn.setText("-");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_MINUS :
                    selectStation3 = SelectStation.SELECT_PLUS;
                    electrode3Btn.setBackground(getDrawable(R.drawable.border_nemo_plus));
                    electrode3Btn.setTextColor(getColor(R.color.white));
                    electrode3Btn.setText("+");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_PLUS:
                    selectStation3 = SelectStation.UNSELECTED_OBJECT;
                    electrode3Btn.setBackground(getDrawable(R.drawable.border_nemo));
                    electrode3Btn.setTextColor(getColor(R.color.black));
                    electrode3Btn.setText("3");
                    if (startTest) {compileMassage();}
                    break;
            }
        });
        electrode4Btn.setOnClickListener(view -> {
            switch (selectStation4) {
                case UNSELECTED_OBJECT :
                    selectStation4 = SelectStation.SELECT_MINUS;
                    electrode4Btn.setBackground(getDrawable(R.drawable.border_nemo_minus));
                    electrode4Btn.setTextColor(getColor(R.color.white));
                    electrode4Btn.setText("-");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_MINUS :
                    selectStation4 = SelectStation.SELECT_PLUS;
                    electrode4Btn.setBackground(getDrawable(R.drawable.border_nemo_plus));
                    electrode4Btn.setTextColor(getColor(R.color.white));
                    electrode4Btn.setText("+");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_PLUS:
                    selectStation4 = SelectStation.UNSELECTED_OBJECT;
                    electrode4Btn.setBackground(getDrawable(R.drawable.border_nemo));
                    electrode4Btn.setTextColor(getColor(R.color.black));
                    electrode4Btn.setText("4");
                    if (startTest) {compileMassage();}
                    break;
            }
        });
        electrode5Btn.setOnClickListener(view -> {
            switch (selectStation5) {
                case UNSELECTED_OBJECT :
                    selectStation5 = SelectStation.SELECT_MINUS;
                    electrode5Btn.setBackground(getDrawable(R.drawable.border_nemo_minus));
                    electrode5Btn.setTextColor(getColor(R.color.white));
                    electrode5Btn.setText("-");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_MINUS :
                    selectStation5 = SelectStation.SELECT_PLUS;
                    electrode5Btn.setBackground(getDrawable(R.drawable.border_nemo_plus));
                    electrode5Btn.setTextColor(getColor(R.color.white));
                    electrode5Btn.setText("+");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_PLUS:
                    selectStation5 = SelectStation.UNSELECTED_OBJECT;
                    electrode5Btn.setBackground(getDrawable(R.drawable.border_nemo));
                    electrode5Btn.setTextColor(getColor(R.color.black));
                    electrode5Btn.setText("5");
                    if (startTest) {compileMassage();}
                    break;
            }
        });
        electrode6Btn.setOnClickListener(view -> {
            switch (selectStation6) {
                case UNSELECTED_OBJECT :
                    selectStation6 = SelectStation.SELECT_MINUS;
                    electrode6Btn.setBackground(getDrawable(R.drawable.border_nemo_minus));
                    electrode6Btn.setTextColor(getColor(R.color.white));
                    electrode6Btn.setText("-");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_MINUS :
                    selectStation6 = SelectStation.SELECT_PLUS;
                    electrode6Btn.setBackground(getDrawable(R.drawable.border_nemo_plus));
                    electrode6Btn.setTextColor(getColor(R.color.white));
                    electrode6Btn.setText("+");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_PLUS:
                    selectStation6 = SelectStation.UNSELECTED_OBJECT;
                    electrode6Btn.setBackground(getDrawable(R.drawable.border_nemo));
                    electrode6Btn.setTextColor(getColor(R.color.black));
                    electrode6Btn.setText("6");
                    if (startTest) {compileMassage();}
                    break;
            }
        });
        electrode7Btn.setOnClickListener(view -> {
            switch (selectStation7) {
                case UNSELECTED_OBJECT :
                    selectStation7 = SelectStation.SELECT_MINUS;
                    electrode7Btn.setBackground(getDrawable(R.drawable.border_nemo_minus));
                    electrode7Btn.setTextColor(getColor(R.color.white));
                    electrode7Btn.setText("-");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_MINUS :
                    selectStation7 = SelectStation.SELECT_PLUS;
                    electrode7Btn.setBackground(getDrawable(R.drawable.border_nemo_plus));
                    electrode7Btn.setTextColor(getColor(R.color.white));
                    electrode7Btn.setText("+");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_PLUS:
                    selectStation7 = SelectStation.UNSELECTED_OBJECT;
                    electrode7Btn.setBackground(getDrawable(R.drawable.border_nemo));
                    electrode7Btn.setTextColor(getColor(R.color.black));
                    electrode7Btn.setText("7");
                    if (startTest) {compileMassage();}
                    break;
            }
        });
        electrode8Btn.setOnClickListener(view -> {
            switch (selectStation8) {
                case UNSELECTED_OBJECT :
                    selectStation8 = SelectStation.SELECT_MINUS;
                    electrode8Btn.setBackground(getDrawable(R.drawable.border_nemo_minus));
                    electrode8Btn.setTextColor(getColor(R.color.white));
                    electrode8Btn.setText("-");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_MINUS :
                    selectStation8 = SelectStation.SELECT_PLUS;
                    electrode8Btn.setBackground(getDrawable(R.drawable.border_nemo_plus));
                    electrode8Btn.setTextColor(getColor(R.color.white));
                    electrode8Btn.setText("+");
                    if (startTest) {compileMassage();}
                    break;

                case SELECT_PLUS:
                    selectStation8 = SelectStation.UNSELECTED_OBJECT;
                    electrode8Btn.setBackground(getDrawable(R.drawable.border_nemo));
                    electrode8Btn.setTextColor(getColor(R.color.black));
                    electrode8Btn.setText("8");
                    if (startTest) {compileMassage();}
                    break;
            }
        });
        startStopBtn.setOnClickListener(view -> {
            if (startTest) {
                startTest = false;
                startStopBtn.setText("start");
                compileOffMassage();
            } else {
                if (checkConditions()) {
                    startTest = true;
                    startStopBtn.setText("stop");
                }
                if (startTest) {setTimer(1000);}
            }
        });
        amplitudeDecBtn.setOnClickListener(view -> {
            if (amplitude > 0) {
                amplitude -= 1;
                amplitudeNp.setValue(amplitude);
                if (startTest) {compileMassage();}
            }
        });
        amplitudeAddBtn.setOnClickListener(view -> {
            if (amplitude < 10) {
                amplitude += 1;
                amplitudeNp.setValue(amplitude);
                if (startTest) {compileMassage();}
            }
        });

        DaggerNemoStandComponent.builder()
                .bluetoothModule(Objects.requireNonNull(WDApplication.app()).bluetoothModule())
                .nemoStandModule(new NemoStandModule(this))
                .build().inject(this);
        ButterKnife.bind(this);

        presenter.onCreate(getIntent());
    }
    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        presenter.setOnPauseActivity(true);
    }
    @Override
    protected void onResume() {
        super.onResume();
        presenter.setOnPauseActivity(false);
    }
    @Override
    protected void onStop() {
        super.onStop();
        presenter.disconnect();
    }
    @Override
    public void onBackPressed() {
        openQuitDialog();
    }
    private void openQuitDialog() {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(
                NemoStandActivity.this);
        quitDialog.setTitle(R.string.leave);

        quitDialog.setPositiveButton(R.string.ok, (dialog, which) -> finish());

        quitDialog.setNegativeButton(R.string.no, (dialog, which) -> {
        });

        quitDialog.show();
    }
    private void compileMassage() {
        switch (selectStation1) {
            case UNSELECTED_OBJECT:
                amplitudeR1 = 0x00;
                amplitudeG1 = 0x00;
                break;

            case SELECT_MINUS:
                amplitudeR1 = amplitude*AMPLITUDE_MULTIPLIER;
                amplitudeG1 = 0x00;
                break;

            case SELECT_PLUS:
                amplitudeR1 = 0x00;
                amplitudeG1 = amplitude*AMPLITUDE_MULTIPLIER;
                break;
        }
        switch (selectStation2) {
            case UNSELECTED_OBJECT:
                amplitudeR2 = 0x00;
                amplitudeG2 = 0x00;
                break;

            case SELECT_MINUS:
                amplitudeR2 = amplitude*AMPLITUDE_MULTIPLIER;
                amplitudeG2 = 0x00;
                break;

            case SELECT_PLUS:
                amplitudeR2 = 0x00;
                amplitudeG2 = amplitude*AMPLITUDE_MULTIPLIER;
                break;
        }
        switch (selectStation3) {
            case UNSELECTED_OBJECT:
                amplitudeR3 = 0x00;
                amplitudeG3 = 0x00;
                break;

            case SELECT_MINUS:
                amplitudeR3 = amplitude*AMPLITUDE_MULTIPLIER;
                amplitudeG3 = 0x00;
                break;

            case SELECT_PLUS:
                amplitudeR3 = 0x00;
                amplitudeG3 = amplitude*AMPLITUDE_MULTIPLIER;
                break;
        }
        switch (selectStation4) {
            case UNSELECTED_OBJECT:
                amplitudeR4 = 0x00;
                amplitudeG4 = 0x00;
                break;

            case SELECT_MINUS:
                amplitudeR4 = amplitude*AMPLITUDE_MULTIPLIER;
                amplitudeG4 = 0x00;
                break;

            case SELECT_PLUS:
                amplitudeR4 = 0x00;
                amplitudeG4 = amplitude*AMPLITUDE_MULTIPLIER;
                break;
        }
        switch (selectStation5) {
            case UNSELECTED_OBJECT:
                amplitudeR5 = 0x00;
                amplitudeG5 = 0x00;
                break;

            case SELECT_MINUS:
                amplitudeR5 = amplitude*AMPLITUDE_MULTIPLIER;
                amplitudeG5 = 0x00;
                break;

            case SELECT_PLUS:
                amplitudeR5 = 0x00;
                amplitudeG5 = amplitude*AMPLITUDE_MULTIPLIER;
                break;
        }
        switch (selectStation6) {
            case UNSELECTED_OBJECT:
                amplitudeR6 = 0x00;
                amplitudeG6 = 0x00;
                break;

            case SELECT_MINUS:
                amplitudeR6 = amplitude*AMPLITUDE_MULTIPLIER;
                amplitudeG6 = 0x00;
                break;

            case SELECT_PLUS:
                amplitudeR6 = 0x00;
                amplitudeG6 = amplitude*AMPLITUDE_MULTIPLIER;
                break;
        }
        switch (selectStation7) {
            case UNSELECTED_OBJECT:
                amplitudeR7 = 0x00;
                amplitudeG7 = 0x00;
                break;

            case SELECT_MINUS:
                amplitudeR7 = amplitude*AMPLITUDE_MULTIPLIER;
                amplitudeG7 = 0x00;
                break;

            case SELECT_PLUS:
                amplitudeR7 = 0x00;
                amplitudeG7 = amplitude*AMPLITUDE_MULTIPLIER;
                break;
        }
        switch (selectStation8) {
            case UNSELECTED_OBJECT:
                amplitudeR8 = 0x00;
                amplitudeG8 = 0x00;
                break;

            case SELECT_MINUS:
                amplitudeR8 = amplitude*AMPLITUDE_MULTIPLIER;
                amplitudeG8 = 0x00;
                break;

            case SELECT_PLUS:
                amplitudeR8 = 0x00;
                amplitudeG8 = amplitude*AMPLITUDE_MULTIPLIER;
                break;
        }


        presenter.onHelloWorld(mMassages.CompileMessageSetLine(
                (byte) amplitudeR1, (byte) amplitudeG1, (byte) amplitudeR2, (byte) amplitudeG2,
                (byte) amplitudeR3, (byte) amplitudeG3, (byte) amplitudeR4, (byte) amplitudeG4,
                (byte) amplitudeR5, (byte) amplitudeG5, (byte) amplitudeR6, (byte) amplitudeG6,
                (byte) amplitudeR7, (byte) amplitudeG7, (byte) amplitudeR8, (byte) amplitudeG8));
    }
    private void compileOffMassage() {
        presenter.onHelloWorld(mMassages.CompileMessageSetLine(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00));
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                        работа с меню в апбаре                          **/
    //////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.setGroupVisible(R.id.modes, false);
        menu.setGroupVisible(R.id.service_settings, false);
        menu.setGroupVisible(R.id.nemo_logo, true);
        return true;
    }

    //////////////////////////////////////////////////////////////////////////////
    /**                           геттеры сеттеры                              **/
    //////////////////////////////////////////////////////////////////////////////
    @Override
    public void setStatus(String status) {}
    @Override
    public void setStatus(int resId) {
        if (resId == R.string.bluetooth_connect_in_3sec){borderGray.setVisibility(View.GONE); borderGreen.setVisibility(View.GONE); borderRed.setVisibility(View.VISIBLE);}
        if (resId == R.string.bluetooth_connected){borderGray.setVisibility(View.GONE); borderGreen.setVisibility(View.VISIBLE); borderRed.setVisibility(View.GONE);}
        if (resId == R.string.bluetooth_connecting){borderGray.setVisibility(View.VISIBLE); borderGreen.setVisibility(View.GONE); borderRed.setVisibility(View.GONE);}
    }
    public void setStartTrig(){ }
    @Override
    public void setFlagReceptionExpectation(Boolean flagReceptionExpectation) {}
    @Override
    public void setErrorReception (boolean incomeErrorReception) { }
    public void setReceiveRoughnessOfSensors(byte receiveRoughnessOfSensors) {}
    @SuppressLint("MissingPermission")
    public void getNameFromDevice(BluetoothDevice device){
        deviceName = device.getName();
    }
    public void getName(String deviceName){
        NemoStandActivity.deviceName = deviceName;
    }
    private boolean checkConditions() {
        boolean hasMinus = false;
        boolean hasPlus = false;

        switch (selectStation1) {
            case SELECT_MINUS:
                hasMinus = true;
                break;

            case SELECT_PLUS:
                hasPlus = true;
                break;
        }
        switch (selectStation2) {
            case SELECT_MINUS:
                hasMinus = true;
                break;

            case SELECT_PLUS:
                hasPlus = true;
                break;
        }
        switch (selectStation3) {
            case SELECT_MINUS:
                hasMinus = true;
                break;

            case SELECT_PLUS:
                hasPlus = true;
                break;
        }
        switch (selectStation4) {
            case SELECT_MINUS:
                hasMinus = true;
                break;

            case SELECT_PLUS:
                hasPlus = true;
                break;
        }
        switch (selectStation5) {
            case SELECT_MINUS:
                hasMinus = true;
                break;

            case SELECT_PLUS:
                hasPlus = true;
                break;
        }
        switch (selectStation6) {
            case SELECT_MINUS:
                hasMinus = true;
                break;

            case SELECT_PLUS:
                hasPlus = true;
                break;
        }
        switch (selectStation7) {
            case SELECT_MINUS:
                hasMinus = true;
                break;

            case SELECT_PLUS:
                hasPlus = true;
                break;
        }
        switch (selectStation8) {
            case SELECT_MINUS:
                hasMinus = true;
                break;

            case SELECT_PLUS:
                hasPlus = true;
                break;
        }

        if (!hasMinus && !hasPlus) {showToast("select \"+\" and \"-\" electrode"); return false;}
        if (!hasMinus) {showToast("select \"-\" electrode"); return false;}
        if (!hasPlus) {showToast("select \"+\" electrode"); return false;}
        return true;
    }
    private void setTimer(int timeMilisec) {
        if (activeIncentive) {
            compileMassage();
        } else {
            compileOffMassage();
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (activeIncentive) {
                    activeIncentive = false;
                    setTimer(700);
                } else {
                    activeIncentive = true;
                    if(startTest) {
                        setTimer(1000);
                    }
                }
            }
        }, timeMilisec);
    }


    @Override
    public void enableInterface(boolean enabled) {
        System.err.println("enableInterface NemoStandActivity");
        electrode1Btn.setEnabled(enabled);
        electrode2Btn.setEnabled(enabled);
        electrode3Btn.setEnabled(enabled);
        electrode4Btn.setEnabled(enabled);
        electrode5Btn.setEnabled(enabled);
        electrode6Btn.setEnabled(enabled);
        electrode7Btn.setEnabled(enabled);
        electrode8Btn.setEnabled(enabled);
        amplitudeDecBtn.setEnabled(enabled);
        amplitudeAddBtn.setEnabled(enabled);
        if (enabled) { presenter.onHelloWorld(mMassages.CompileMessagePass()); }
    }
    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    @Override
    public void showToastWithoutConnection() {
        Toast.makeText(this, R.string.connection_is_absent, Toast.LENGTH_SHORT).show();
    }
    @Override
    public void setStartParametersInNemoStandActivity() {}


    //////////////////////////////////////////////////////////////////////////////
    /**                           работа с памятью                             **/
    //////////////////////////////////////////////////////////////////////////////
    @SuppressLint("LogNotTimber")
    public void saveVariable (String nameVariableInt, Integer Variable) {
        Timber.e("saveVariable ----> " + nameVariableInt + " = " + Variable);
        sharedPreferences = getSharedPreferences("My_variables" ,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(nameVariableInt, Variable);
        editor.apply();
    }
    @SuppressLint("LogNotTimber")
    public Integer loadVariable (String nameVariableInt) {
        sharedPreferences = getSharedPreferences("My_variables",MODE_PRIVATE);
        Integer variable = sharedPreferences.getInt(nameVariableInt,0);
        Timber.e("loadVariable ----> " + nameVariableInt + " = " + variable);
        return variable;
    }


    private float pxFromDp() {
        return (float) 48 * getApplicationContext().getResources().getDisplayMetrics().density;
    }
}
