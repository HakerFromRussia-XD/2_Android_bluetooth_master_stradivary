package me.romans.motorica.old_electronic_by_Misha.ui.chat.view;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.romans.bluetooth.BluetoothConstantManager;
import me.romans.motorica.new_electronic_by_Rodeon.WDApplication;
import me.romans.motorica.old_electronic_by_Misha.MyApp;
import me.romans.motorica.old_electronic_by_Misha.data.GesstureAdapter;
import me.romans.motorica.old_electronic_by_Misha.data.Gesture_my;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.data.ChatModule;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.presenter.ChatPresenter;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.gesture_settings.FragmentGestureSettings;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.gesture_settings.FragmentGestureSettings2;
import me.romans.bluetooth.ThreadHelper;
import me.romans.motorica.R;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.data.DaggerChatComponent;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.gesture_settings.FragmentGestureSettings3;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.gripper_settings.FragmentGripperSettings;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.service_settings.FragmentServiceSettings;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.service_settings.FragmentServiceSettingsMono;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.service_settings.SettingsDialog;
import me.romans.motorica.old_electronic_by_Misha.utils.ConstantManager;

import static me.romans.motorica.R.id.view_pager;

public class ChartActivity extends AppCompatActivity implements ChartView, GesstureAdapter.OnGestureMyListener, SettingsDialog.SettingsDialogListener {
    public static volatile boolean monograbVersion;
    public static volatile boolean flagUseHDLCProtocol = false;
    public static volatile boolean flagReceptionExpectation = false;
    private boolean flagPauseSending = false;
    private boolean flagOffUpdateGraphHDLC = false;
    public Thread pauseSendingThread;
    private boolean flagReadStartParametersHDLC = true;
    SeekBar seekBarCH1on;
    SeekBar seekBarCH2on;
    public SeekBar seekBarCH1on2;
    public SeekBar seekBarCH2on2;
    public SeekBar seekBarIStop;
    Switch switchBlockMode;
    Switch switchIlluminationMode;
    TextView valueCH2on;
    TextView valueBatteryTension;
    public RelativeLayout layoutSensors;
    RelativeLayout layoutGestures;
    Button openBtn;
    Button closeBtn;
    Button offUpdate;
    Button activity_chat_gesture1;
    Button activity_chat_gesture2;
    Button activity_chat_gesture3;
    Button activity_chat_gesture4;
    FloatingActionButton fab;
    ImageView imageViewStatus;
    ImageView imageViewStatusOpen;
    ImageView imageViewStatusClose;
    ImageView borderGray;
    ImageView borderGreen;
    ImageView borderRed;
    Massages mMassages = new Massages();
    Load3DModel mLoad3DModel = new Load3DModel(this);
    public BottomNavigationView navigation;
    public int numberOfChannel = 0;
    public int intValueCH1on = 2500;
    private int intValueCH1off = 100;
    private int intValueCH1sleep = 200;
    public int intValueCH2on = 2500;
    private int intValueCH2off = 100;
    private int intValueCH2sleep = 200;
    public int current = 0x00;
    private byte indicatorTypeMessage;
    private byte numberChannel;
    public byte invert = 0x00;
    public byte block = 0x00;
    public boolean isEnable = false;
    public boolean firstTapRecyclerView = true;
    public boolean errorReception = false;
    public static String deviceName;
    public double multiplierSeekBar = 13.6363;
    public byte[] TextByteTrigger = new byte[8];
    public byte[] TextByteHDLC6 = new byte[6];
    public byte[] TextByteReadStartParameterTrig1 = {ConstantManager.ADDR_MIO1, ConstantManager.READ, BluetoothConstantManager.MIO1_TRIG_HDLC, 0x00};
    public byte[] TextByteReadStartParameterTrig2 = {ConstantManager.ADDR_MIO2, ConstantManager.READ, BluetoothConstantManager.MIO2_TRIG_HDLC, 0x00};
    public byte[] TextByteReadStartParameterCurrent = {ConstantManager.ADDR_CUR_LIMIT, ConstantManager.READ, BluetoothConstantManager.CURR_LIMIT_HDLC, 0x00};
    public byte[] TextByteReadStartParameterPermissionBlock = {ConstantManager.ADDR_BLOCK, ConstantManager.READ, BluetoothConstantManager.BLOCK_PERMISSION_HDLC, 0x00};
    public byte[] TextByteReadStartParameterPermissionRoughness = {ConstantManager.ADDR_BUFF_CHOISES, ConstantManager.READ, BluetoothConstantManager.ADC_BUFF_CHOISES_HDLC, 0x00};
    public byte[] TextByteReadStartParameterBattery = {ConstantManager.ADDR_BATTERY, ConstantManager.READ, BluetoothConstantManager.CURR_BAT_HDLC, 0x00};
    public byte[] TextByteSetGeneralParcel = new byte[2];
    //    for graph
    public int receiveCurrent = 0;
    private int receiveLevelTrigCH1 = 14;
    private int receiveLevelTrigCH2 = 14;
    private int lastReceiveLevelCH1Chat;
    private int lastReceiveLevelCH2Chat;
    private byte receiveIndicationInvertMode = 0;
    private byte receiveBlockIndication = 0;
    public byte receiveRoughnessOfSensors = 0;
    private LineChart mChart;
    private boolean plotData = true;
    private LineChart mChart2;
    public Thread graphThread;
    public boolean graphThreadFlag = false;
    public boolean invertChannel = false;
    //    for general updates
    public int numberCycle = 0;
    public int receiveLevelCH1Chat = 0;
    public int receiveLevelCH2Chat = 0;
    public byte receiveIndicationStateChat = 0;
    public int receiveBatteryTensionChat = 0;
    String TAG = "thread";
    //    for animation limits
    ImageView limit_1;
    ImageView limit_2;
    ObjectAnimator objectAnimator;
    ObjectAnimator objectAnimator2;
    private int limit_sensor_open = 0;
    private int limit_sensor_close = 0;
    //    for delay
    private boolean runOnUi;
    //    for fragment gestures settings
    public FragmentGestureSettings fragmentGestureSettings;
    public FragmentGripperSettings fragmentGripperSettings;
    public FragmentGestureSettings2 fragmentGestureSettings2;
    public FragmentGestureSettings3 fragmentGestureSettings3;
    public FragmentServiceSettings fragmentServiceSettings;
    public FragmentServiceSettingsMono fragmentServiceSettingsMono;
    public FragmentManager fragmentManager = getSupportFragmentManager();
    public float heightBottomNavigation;
    //    public int indexCount;
    public static int MAX_NUMBER_DETAILS = 19;
    public Thread[] threadFunction = new Thread[MAX_NUMBER_DETAILS];
    //	  for transfer
    private byte numberFinger;
    public int speedFinger = 0;
    public int lastSpeedFinger = 0;
    public int SPEED = 99;
    public static int intValueFinger1Angle = 0;
    public static int intValueFinger2Angle = 0;
    public static int intValueFinger3Angle = 0;
    public static int intValueFinger4Angle = 0;
    public static int intValueFinger5Angle = 0;
    public static int intValueFinger6Angle = 0;
    private static int intValueFinger1AngleLast = 0;
    private static int intValueFinger2AngleLast = 0;
    private static int intValueFinger3AngleLast = 0;
    private static int intValueFinger4AngleLast = 0;
    private static int intValueFinger5AngleLast = 0;
    private static int intValueFinger6AngleLast = 0;
    private int intValueFinger1Speed = SPEED;
    private int intValueFinger2Speed = SPEED;
    private int intValueFinger3Speed = SPEED;
    private int intValueFinger4Speed = SPEED;
    private int intValueFinger5Speed = SPEED;
    private int intValueFinger6Speed = SPEED;
    public static byte GESTURE_SETTINGS = 0x15;
    public static byte NUMBER_CELL = 0x00;
    public static long delay = 200;
    private int delayPauseAfterSending = 200;
    public boolean transferThreadFlag = false;
    public boolean mainActivityStarted = false;
    public Thread transferThread;
    public Thread requestStartTrig1Thread;
    public Thread requestStartTrig2Thread;
    public Thread requestStartCurrentThread;
    public Thread requestStartBlockThread;
    public Thread requestStartRoughnessThread;
    public Thread requestBatteryTensionThread;

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public enum SelectStation {UNSELECTED_OBJECT, SELECT_FINGER_1, SELECT_FINGER_2, SELECT_FINGER_3, SELECT_FINGER_4, SELECT_FINGER_5}
    public static SelectStation selectStation;
    public boolean firstRead = true;
    //    for service menu
    public Menu myMenu;
    public Thread updateServiceSettingsThread;
    public boolean updateServiceSettingsThreadFlag = false;
    private boolean showMenu = true;
    private boolean lockServiceSettings = false;
    private int useGesture = 1;
    //    save
    SharedPreferences sharedPreferences;

    RecyclerView recyclerView;
    GesstureAdapter gestureAdapter;
    List<Gesture_my> gestureMyList;

    @Inject
    public ChatPresenter presenter;



    @SuppressLint({"NewApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //changing statusbar
        if (android.os.Build.VERSION.SDK_INT >= 21){
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark));
        }

        if(monograbVersion){
            //односхватная версия
            setContentView(R.layout.monograb);
            seekBarIStop = findViewById(R.id.seekBarIstop);
            fragmentServiceSettingsMono = new FragmentServiceSettingsMono();
        } else {
            //многосхватная версия
            setContentView(R.layout.multigrab);
            navigation = findViewById(R.id.navigation);
            navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
            heightBottomNavigation = pxFromDp();

            offUpdate = findViewById(R.id.activity_chat_off_update);
            activity_chat_gesture1 = findViewById(R.id.activity_chat_gesture1);
            activity_chat_gesture2 = findViewById(R.id.activity_chat_gesture2);
            activity_chat_gesture3 = findViewById(R.id.activity_chat_gesture3);
            activity_chat_gesture4 = findViewById(R.id.activity_chat_gesture4);
            fragmentGestureSettings = new FragmentGestureSettings();
            fragmentGripperSettings = new FragmentGripperSettings();
            fragmentGestureSettings2 = new FragmentGestureSettings2();
            fragmentGestureSettings3 = new FragmentGestureSettings3();
            fragmentServiceSettings = new FragmentServiceSettings();
        }
        seekBarCH1on = findViewById(R.id.seekBarCH1on);
        seekBarCH2on = findViewById(R.id.seekBarCH2on);
        seekBarCH1on2 = findViewById(R.id.seekBarCH1on2);
        seekBarCH2on2 = findViewById(R.id.seekBarCH2on2);
        switchBlockMode = findViewById(R.id.switchBlockMode);
        switchIlluminationMode = findViewById(R.id.switchIlluminationMode);
        valueCH2on = findViewById(R.id.valueCH2on);
        valueBatteryTension = findViewById(R.id.valueBatteryTension);
        layoutSensors = findViewById(R.id.layout_sensors);
        layoutGestures = findViewById(R.id.gestures_list_relative);
        openBtn = findViewById(R.id.open_btn);
        closeBtn = findViewById(R.id.close_btn);
        fab = findViewById(R.id.fab);
        imageViewStatus = findViewById(R.id.imageViewStatus);
        imageViewStatusOpen = findViewById(R.id.imageViewStatusOpen);
        imageViewStatusClose = findViewById(R.id.imageViewStatusClose);
        borderGray = findViewById(R.id.borderGray);
        borderGreen = findViewById(R.id.borderGreen);
        borderRed = findViewById(R.id.borderRed);

        if(monograbVersion){
            //односхватная версия
            seekBarIStop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    current = seekBar.getProgress();
                    saveVariable( deviceName+"current",current);
                    if(flagUseHDLCProtocol){
                        pauseSendingThread(mMassages.CompileMassageCurrentSettingsAndInvertHDLC(current));
                    } else {
                        presenter.onHelloWorld(mMassages.CompileMassageCurrentSettingsAndInvert(current, invert));
                    }
                }
            });
            current = loadVariable(deviceName +"current");
            seekBarIStop.setProgress(current);
        } else {
            //многосхватная версия
            activity_chat_gesture1.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (flagUseHDLCProtocol){
                            useGesture = 1;
                            gestureUseThread ((byte) 6, (byte) useGesture);
                        }else{
                            presenter.onHelloWorld(mMassages.CompileMassageSwitchGesture((byte) 0x00, (byte) 0x01));
                        }
                    }
                    return false;
                }
            });

            activity_chat_gesture2.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (flagUseHDLCProtocol){
                            useGesture = 2;
                            gestureUseThread ((byte) 7, (byte) useGesture);
                        }else{
                            presenter.onHelloWorld(mMassages.CompileMassageSwitchGesture((byte) 0x02, (byte) 0x03));
                        }
                    }
                    return false;
                }
            });

            activity_chat_gesture3.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (flagUseHDLCProtocol){
                            useGesture = 3;
                            gestureUseThread ((byte) 8, (byte) useGesture);
                        }else{
                            presenter.onHelloWorld(mMassages.CompileMassageSwitchGesture((byte) 0x04, (byte) 0x05));
                        }
                    }
                    return false;
                }
            });

            activity_chat_gesture4.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if (flagUseHDLCProtocol){
                        useGesture = 4;
                        gestureUseThread ((byte) 9, (byte) useGesture);
                    }else{
                        presenter.onHelloWorld(mMassages.CompileMassageSwitchGesture((byte) 0x06, (byte) 0x06));
                    }
                }
            });

            offUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (flagUseHDLCProtocol){
                        if(!flagOffUpdateGraphHDLC){
                            flagOffUpdateGraphHDLC = true;
                            offUpdate.setText(R.string.on_schedules);
                        } else {
                            flagOffUpdateGraphHDLC = false;
                            offUpdate.setText(R.string.off_schedules);
                        }
                    }
                }
            });
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(deviceName);
        mainActivityStarted = true;
        final float scale = getResources().getDisplayMetrics().density;


        limit_1 = findViewById(R.id.limit_1);
        limit_2 = findViewById(R.id.limit_2);
        objectAnimator =ObjectAnimator.ofFloat(limit_1, "y", limit_sensor_open);


        //////////////////////////////////////////
        //инициализация и заполнение списка жестов
        gestureMyList = new ArrayList<>();
        recyclerView = findViewById(R.id.gestures_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //adding some items to our list
        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.gesture1,
                        "bla bla bla",
                        getString(R.string.click_to_edit_start_and_end_states),
                        getString(R.string.gesture_1),
                        2,
                        600000));

        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.gesture2,
                        "bla bla bla",
                        getString(R.string.click_to_edit_start_and_end_states),
                        getString(R.string.gesture_2),
                        2,
                        60000));

        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.gesture3,
                        "bla bla bla",
                        getString(R.string.click_to_edit_start_and_end_states),
                        getString(R.string.gesture_3),
                        2,
                        60000));

        gestureAdapter = new GesstureAdapter(this, gestureMyList, this);
        recyclerView.setAdapter(gestureAdapter);
        /////////////////////////////////////////////////////////////////////////////////////////


        DaggerChatComponent.builder()
                .bluetoothModule(WDApplication.app().bluetoothModule())
                .chatModule(new ChatModule(this))
                .build().inject(this);
        ButterKnife.bind(this);


        ////////initialized graph for channel 1
        initializedGraphForChannel1();
        ////////initialized graph for channel 2
        initializedGraphForChannel2();


        seekBarCH1on.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                intValueCH1on = (int) (seekBarCH1on.getProgress()* multiplierSeekBar);
//                System.err.println("ChatActivity--------> seekBarCH1on : onProgressChanged - intValueCH1on=" + intValueCH1on);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                System.err.println("ChatActivity--------> seekBarCH1on : onStartTrackingTouch - intValueCH1on=" + intValueCH1on);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                intValueCH1on = (int) (seekBarCH1on.getProgress()* multiplierSeekBar);
                indicatorTypeMessage = 0x01;
                if (invertChannel){numberChannel = 0x02;} else {numberChannel = 0x01;}
                //TODO дописать инвертирование для хдлцешных посылок
                if(flagUseHDLCProtocol){
                    TextByteHDLC6[0] = ConstantManager.ADDR_MIO1;
                    TextByteHDLC6[1] = ConstantManager.WRITE;
                    TextByteHDLC6[2] = BluetoothConstantManager.MIO1_TRIG_HDLC;
                    TextByteHDLC6[3] = (byte) intValueCH1on;
                    TextByteHDLC6[4] = (byte) (intValueCH1on >> 8);
                    TextByteHDLC6[5] = presenter.calculationCRC_HDLC(TextByteHDLC6);
                    pauseSendingThread(TextByteHDLC6);
                } else {
                    TextByteTrigger[0] = indicatorTypeMessage;
                    TextByteTrigger[1] = numberChannel;
                    TextByteTrigger[2] = (byte) intValueCH1on;
                    TextByteTrigger[3] = (byte) (intValueCH1on >> 8);
                    TextByteTrigger[4] = (byte) intValueCH1off;
                    TextByteTrigger[5] = (byte) (intValueCH1off >> 8);
                    TextByteTrigger[6] = (byte) intValueCH1sleep;
                    TextByteTrigger[7] = (byte) (intValueCH1sleep >> 8);
                    presenter.onHelloWorld(TextByteTrigger);
                    //                    System.err.println("ChatActivity--------> seekBarCH1on : onStopTrackingTouch - intValueCH1on=" + intValueCH1on);
                }
                saveVariable( deviceName+"intValueCH1on",intValueCH1on);
                seekBarCH1on2.setProgress(seekBarCH1on.getProgress());
            }
        });
        seekBarCH1on2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                System.err.println("ChatActivity--------> seekBarCH1on : обновление порога СH1");
                intValueCH1on = (int) (seekBarCH1on2.getProgress()* multiplierSeekBar);
                limit_sensor_open = seekBar.getProgress();
                objectAnimator =ObjectAnimator.ofFloat(limit_1, "y", ((235*scale + 0.5f)-(limit_sensor_open*scale + 0.5f)));
                objectAnimator.setDuration(200);
                objectAnimator.start();
//                System.err.println("ChatActivity--------> seekBarCH1on2 : onProgressChanged - intValueCH1on=" + intValueCH1on);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                System.err.println("ChatActivity--------> seekBarCH1on2 : onStartTrackingTouch - intValueCH1on=" + intValueCH1on);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                intValueCH1on = (int) (seekBarCH1on2.getProgress()* multiplierSeekBar);
                indicatorTypeMessage = 0x01;
                if (invertChannel){numberChannel = 0x02;} else {numberChannel = 0x01;}
                if(flagUseHDLCProtocol){
                    TextByteHDLC6[0] = ConstantManager.ADDR_MIO1;
                    TextByteHDLC6[1] = ConstantManager.WRITE;
                    TextByteHDLC6[2] = BluetoothConstantManager.MIO1_TRIG_HDLC;
                    TextByteHDLC6[3] = (byte) intValueCH1on;
                    TextByteHDLC6[4] = (byte) (intValueCH1on >> 8);
                    TextByteHDLC6[5] = presenter.calculationCRC_HDLC(TextByteHDLC6);
                    pauseSendingThread(TextByteHDLC6);
                } else {
                    TextByteTrigger[0] = indicatorTypeMessage;
                    TextByteTrigger[1] = numberChannel;
                    TextByteTrigger[2] = (byte) intValueCH1on;
                    TextByteTrigger[3] = (byte) (intValueCH1on >> 8);
                    TextByteTrigger[4] = (byte) intValueCH1off;
                    TextByteTrigger[5] = (byte) (intValueCH1off >> 8);
                    TextByteTrigger[6] = (byte) intValueCH1sleep;
                    TextByteTrigger[7] = (byte) (intValueCH1sleep >> 8);
                    presenter.onHelloWorld(TextByteTrigger);
                }
                saveVariable( deviceName+"intValueCH1on",intValueCH1on);
                seekBarCH1on.setProgress(seekBarCH1on2.getProgress());
//                System.err.println("ChatActivity--------> seekBarCH1on2 : onStopTrackingTouch - intValueCH1on=" + intValueCH1on);
            }
        });
        seekBarCH2on.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                intValueCH2on = (int) (seekBarCH2on.getProgress()* multiplierSeekBar);
//                System.err.println("ChatActivity--------> seekBarCH2on : onProgressChanged - intValueCH2on=" + intValueCH2on);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                System.err.println("ChatActivity--------> seekBarCH2on : onStartTrackingTouch - intValueCH2on=" + intValueCH2on);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()* multiplierSeekBar));
                intValueCH2on = (int) (seekBarCH2on.getProgress()* multiplierSeekBar);
                indicatorTypeMessage = 0x01;
                if (invertChannel){numberChannel = 0x01;} else {numberChannel = 0x02;}
                if(flagUseHDLCProtocol){
                    TextByteHDLC6[0] = ConstantManager.ADDR_MIO2;
                    TextByteHDLC6[1] = ConstantManager.WRITE;
                    TextByteHDLC6[2] = BluetoothConstantManager.MIO2_TRIG_HDLC;
                    TextByteHDLC6[3] = (byte) intValueCH2on;
                    TextByteHDLC6[4] = (byte) (intValueCH2on >> 8);
                    TextByteHDLC6[5] = presenter.calculationCRC_HDLC(TextByteHDLC6);
                    pauseSendingThread(TextByteHDLC6);
                } else {
                    TextByteTrigger[0] = indicatorTypeMessage;
                    TextByteTrigger[1] = numberChannel;
                    TextByteTrigger[2] = (byte) intValueCH2on;
                    TextByteTrigger[3] = (byte) (intValueCH2on >> 8);
                    TextByteTrigger[4] = (byte) intValueCH2off;
                    TextByteTrigger[5] = (byte) (intValueCH2off >> 8);
                    TextByteTrigger[6] = (byte) intValueCH2sleep;
                    TextByteTrigger[7] = (byte) (intValueCH2sleep >> 8);
                    presenter.onHelloWorld(TextByteTrigger);
                }
                saveVariable( deviceName+"intValueCH2on",intValueCH2on);

                seekBarCH2on2.setProgress(seekBarCH2on.getProgress());
//                System.err.println("ChatActivity--------> seekBarCH2on : onStopTrackingTouch - intValueCH2on=" + intValueCH2on);
            }
        });
        seekBarCH2on2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                System.err.println("ChatActivity--------> seekBarCH1on : обновление порога СH2");
                intValueCH2on = (int) (seekBarCH2on2.getProgress()* multiplierSeekBar);
                limit_sensor_close = seekBar.getProgress();
                objectAnimator2 =ObjectAnimator.ofFloat(limit_2, "y", ((495*scale + 0.5f)-(limit_sensor_close*scale + 0.5f)));
                objectAnimator2.setDuration(200);
                objectAnimator2.start();
//                System.err.println("ChatActivity--------> seekBarCH2on2 : onProgressChanged - intValueCH2on=" + intValueCH2on);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                System.err.println("ChatActivity--------> seekBarCH2on2 : onStartTrackingTouch - intValueCH2on=" + intValueCH2on);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()* multiplierSeekBar));
                intValueCH2on = (int) (seekBarCH2on2.getProgress()* multiplierSeekBar);
                indicatorTypeMessage = 0x01;
                if (invertChannel){numberChannel = 0x01;} else {numberChannel = 0x02;}
                if(flagUseHDLCProtocol){
                    TextByteHDLC6[0] = ConstantManager.ADDR_MIO2;
                    TextByteHDLC6[1] = ConstantManager.WRITE;
                    TextByteHDLC6[2] = BluetoothConstantManager.MIO2_TRIG_HDLC;
                    TextByteHDLC6[3] = (byte) intValueCH2on;
                    TextByteHDLC6[4] = (byte) (intValueCH2on >> 8);
                    TextByteHDLC6[5] = presenter.calculationCRC_HDLC(TextByteHDLC6);
                    pauseSendingThread(TextByteHDLC6);
                } else {
                    TextByteTrigger[0] = indicatorTypeMessage;
                    TextByteTrigger[1] = numberChannel;
                    TextByteTrigger[2] = (byte) intValueCH2on;
                    TextByteTrigger[3] = (byte) (intValueCH2on >> 8);
                    TextByteTrigger[4] = (byte) intValueCH2off;
                    TextByteTrigger[5] = (byte) (intValueCH2off >> 8);
                    TextByteTrigger[6] = (byte) intValueCH2sleep;
                    TextByteTrigger[7] = (byte) (intValueCH2sleep >> 8);
                    presenter.onHelloWorld(TextByteTrigger);
                }
                saveVariable( deviceName+"intValueCH2on",intValueCH2on);
                seekBarCH2on.setProgress(seekBarCH2on2.getProgress());
//                System.err.println("ChatActivity--------> seekBarCH2on2 : onStopTrackingTouch - intValueCH2on=" + intValueCH2on);
            }
        });


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Новый жест добавлен", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                gestureMyList.add(
                        new Gesture_my(
                                1,
                                R.drawable.gesture4,
                                "bla bla bla",
                                "Нажмите для редактирования начального и конечного состояний",
                                "Жест №"+4,
                                2,
                                123));
                recyclerView.setAdapter(gestureAdapter);
            }
        });

        switchBlockMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchBlockMode.isChecked()){
                    block = 0x01;
                    if (flagUseHDLCProtocol){
                        presenter.onHelloWorld(mMassages.CompileMassageBlockModeHDLC(block));
                    } else {
                        presenter.onHelloWorld(mMassages.CompileMassageBlockMode(block));
                    }
                    receiveBlockIndication = 1;
                    imageViewStatus.setImageResource(R.drawable.unblock);
                } else {
                    block = 0x00;
                    if (flagUseHDLCProtocol){
                        presenter.onHelloWorld(mMassages.CompileMassageBlockModeHDLC(block));
                    } else {
                        presenter.onHelloWorld(mMassages.CompileMassageBlockMode(block));
                    }
                    receiveBlockIndication = 0;
                    imageViewStatus.setImageResource(R.drawable.block_not_use);
                }
            }
        });

        switchIlluminationMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchIlluminationMode.isChecked()){
                    presenter.onHelloWorld(mMassages.CompileMassageIlluminationMode(true));
                }else {
                    presenter.onHelloWorld(mMassages.CompileMassageIlluminationMode(false));
                }
            }
        });

        closeBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    lastReceiveLevelCH1Chat = receiveLevelCH1Chat;
                    lastReceiveLevelCH2Chat = receiveLevelCH2Chat;
                    int numberSensor = 0x07;
                    if(flagUseHDLCProtocol){
                        if(useGesture == 1){pauseSendingThread(mMassages.CompileMassageSensorActivate2HDLC((byte) 1));}
                        if(useGesture == 2){pauseSendingThread(mMassages.CompileMassageSensorActivate2HDLC((byte) 3));}
                        if(useGesture == 3){pauseSendingThread(mMassages.CompileMassageSensorActivate2HDLC((byte) 5));}
                    } else {
                        presenter.onHelloWorld(mMassages.CompileMassageSensorActivate(numberSensor));
                    }
                    receiveLevelCH1Chat = 20;
                    receiveLevelCH2Chat = 2500;
                    if(flagUseHDLCProtocol){System.out.println("ChatActivity----> flagReceptionExpectation " + flagReceptionExpectation);}
//                    else {showToast("not IND");}
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int numberSensor = 0x08;
                    if(flagUseHDLCProtocol){
                        pauseSendingThread(mMassages.CompileMassageSensorActivateHDLC());
                    } else {
                        presenter.onHelloWorld(mMassages.CompileMassageSensorActivate(numberSensor));
                    }

                    receiveLevelCH1Chat = lastReceiveLevelCH1Chat;
                    receiveLevelCH2Chat = lastReceiveLevelCH2Chat;
                }
                return false;
            }
        });

        openBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    lastReceiveLevelCH1Chat = receiveLevelCH1Chat;
                    lastReceiveLevelCH2Chat = receiveLevelCH2Chat;
                    int numberSensor = 0x06;
                    if(flagUseHDLCProtocol){
                        if(useGesture == 1){pauseSendingThread(mMassages.CompileMassageSensorActivate2HDLC((byte) 0));}
                        if(useGesture == 2){pauseSendingThread(mMassages.CompileMassageSensorActivate2HDLC((byte) 2));}
                        if(useGesture == 3){pauseSendingThread(mMassages.CompileMassageSensorActivate2HDLC((byte) 4));}
                    } else {
                        presenter.onHelloWorld(mMassages.CompileMassageSensorActivate(numberSensor));
                    }
                    receiveLevelCH1Chat = 2500;
                    receiveLevelCH2Chat = 20;
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int numberSensor = 0x08;
                    if(flagUseHDLCProtocol){
                        pauseSendingThread(mMassages.CompileMassageSensorActivateHDLC());
                    } else {
                        presenter.onHelloWorld(mMassages.CompileMassageSensorActivate(numberSensor));
                    }
                    receiveLevelCH1Chat = lastReceiveLevelCH1Chat;
                    receiveLevelCH2Chat = lastReceiveLevelCH2Chat;
                }
                return false;
            }
        });



        presenter.onCreate(getIntent());

        graphThreadFlag = true;
        startGraphEnteringDataThread();

        layoutSensors.setVisibility(View.VISIBLE);
        layoutGestures.setVisibility(View.GONE);
        fab.hide();

        ////////////////////////////////////////////////
        //**         3D initialization              **//
        ////////////////////////////////////////////////

        Load3DModel.model[0]  = mLoad3DModel.readData(ConstantManager.MODEDEL_0);
        Load3DModel.model[1]  = mLoad3DModel.readData(ConstantManager.MODEDEL_1);
        Load3DModel.model[2]  = mLoad3DModel.readData(ConstantManager.MODEDEL_2);
        Load3DModel.model[3]  = mLoad3DModel.readData(ConstantManager.MODEDEL_3);
        Load3DModel.model[4]  = mLoad3DModel.readData(ConstantManager.MODEDEL_4);
        Load3DModel.model[5]  = mLoad3DModel.readData(ConstantManager.MODEDEL_5);
        Load3DModel.model[6]  = mLoad3DModel.readData(ConstantManager.MODEDEL_6);
        Load3DModel.model[7]  = mLoad3DModel.readData(ConstantManager.MODEDEL_7);
        Load3DModel.model[8]  = mLoad3DModel.readData(ConstantManager.MODEDEL_8);
        Load3DModel.model[9]  = mLoad3DModel.readData(ConstantManager.MODEDEL_9);
        Load3DModel.model[10] = mLoad3DModel.readData(ConstantManager.MODEDEL_10);
        Load3DModel.model[11] = mLoad3DModel.readData(ConstantManager.MODEDEL_11);
        Load3DModel.model[12] = mLoad3DModel.readData(ConstantManager.MODEDEL_12);
        Load3DModel.model[13] = mLoad3DModel.readData(ConstantManager.MODEDEL_13);
        Load3DModel.model[14] = mLoad3DModel.readData(ConstantManager.MODEDEL_14);
        Load3DModel.model[15] = mLoad3DModel.readData(ConstantManager.MODEDEL_15);
        Load3DModel.model[16] = mLoad3DModel.readData(ConstantManager.MODEDEL_16);
        Load3DModel.model[17] = mLoad3DModel.readData(ConstantManager.MODEDEL_17);
        Load3DModel.model[18] = mLoad3DModel.readData(ConstantManager.MODEDEL_18);


        for (int j = 0; j<MAX_NUMBER_DETAILS; j++) {
            final int finalJ = j;
            threadFunction[j] = new Thread(new Runnable() {
                @Override
                public void run() {
                    mLoad3DModel.loadSTR2(finalJ);
                }
            });
            threadFunction[j].start();
        }
        ////////////////////////////////////////////////
    }
    @Override
    protected void onStart() {
        super.onStart();
        if(!firstRead){
            flagReadStartParametersHDLC = false;}
        presenter.onStart(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        presenter.setOnPauseActivity(true);
        try {
            graphThread.interrupt();
        } catch (Exception ignored){}
    }
    @Override
    protected void onResume() {
        super.onResume();
        presenter.setOnPauseActivity(false);
    }
    @Override
    protected void onStop() {
        super.onStop();
        if(flagUseHDLCProtocol){} else {
            if(isEnable){
                ThreadHelper.run(runOnUi, this, new Runnable() {
                    @Override
                    public void run() {
                        mMassages.CompileMessageSetGeneralParcel((byte) 0x00);
                        presenter.onHelloWorld(TextByteSetGeneralParcel);
                        try {
                            Thread.sleep(500);
                        }catch (Exception ignored){}
                    }
                });
                presenter.onHelloWorld(TextByteSetGeneralParcel);
            }
        }
        presenter.disconnect();
    }
    @Override
    public void onBackPressed() {
        openQuitDialog();
    }
    private void openQuitDialog() {
        if (fragmentServiceSettingsMono != null && fragmentServiceSettingsMono.isVisible()) {
            fragmentServiceSettingsMono.backPressed();
        } else {
            if (fragmentServiceSettings != null && fragmentServiceSettings.isVisible()) {
                fragmentServiceSettings.backPressed();
            } else {
                if (fragmentGripperSettings != null && fragmentGripperSettings.isVisible()) {
                    fragmentGripperSettings.backPressed();
                } else {
                    if (fragmentGestureSettings != null && fragmentGestureSettings.isVisible()) {
                        fragmentGestureSettings.backPressed();
                    } else {
                        if (fragmentGestureSettings2 != null && fragmentGestureSettings2.isVisible()) {
                            fragmentGestureSettings2.backPressed();
                        } else {
                            if (fragmentGestureSettings3 != null && fragmentGestureSettings3.isVisible()) {
                                fragmentGestureSettings3.backPressed();
                            } else {
                                AlertDialog.Builder quitDialog = new AlertDialog.Builder(
                                        ChartActivity.this);
                                quitDialog.setTitle(R.string.leave);

                                quitDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                });

                                quitDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });

                                quitDialog.show();
                            }
                        }
                    }
                }
            }
        }
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                             работа с 3D                                **/
    //////////////////////////////////////////////////////////////////////////////
    public void startTransferThread () {
        transferThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (transferThreadFlag){
                    // пальчики
                    if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){fragmentGripperSettings.seekBarSpeedFinger.setProgress(intValueFinger1Speed);}
                    if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){fragmentGripperSettings.seekBarSpeedFinger.setProgress(intValueFinger2Speed);}
                    if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){fragmentGripperSettings.seekBarSpeedFinger.setProgress(intValueFinger3Speed);}
                    if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){fragmentGripperSettings.seekBarSpeedFinger.setProgress(intValueFinger4Speed);}
                    if(String.valueOf(selectStation).equals("SELECT_FINGER_5")){fragmentGripperSettings.seekBarSpeedFinger.setProgress(intValueFinger6Speed);}
                    if(lastSpeedFinger != speedFinger && isEnable){
                        System.err.println("ChatActivity--------> speedFinger: "+ speedFinger);
                        String.valueOf(selectStation);
                        if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){intValueFinger1Speed = speedFinger;}
                        if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){intValueFinger2Speed = speedFinger;}
                        if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){intValueFinger3Speed = speedFinger;}
                        if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){intValueFinger4Speed = speedFinger;}
                        if(String.valueOf(selectStation).equals("SELECT_FINGER_5")){intValueFinger6Speed = speedFinger;}
                        lastSpeedFinger = speedFinger;
                    }
                    if(intValueFinger1AngleLast != intValueFinger1Angle && isEnable){
                        numberFinger = 1;
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(numberFinger, intValueFinger1Angle, intValueFinger1Speed));
                        }else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(numberFinger, intValueFinger1Angle, intValueFinger1Speed));
                        }
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger1Angle", intValueFinger1Angle);
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger1Speed", intValueFinger1Speed);

                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(numberFinger));
                        }else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)){
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(numberFinger, intValueFinger1Angle, 99));
                        }
                        intValueFinger1AngleLast = intValueFinger1Angle;
                    }
                    if(intValueFinger2AngleLast != intValueFinger2Angle && isEnable){
                        numberFinger = 2;
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(numberFinger, intValueFinger2Angle, intValueFinger2Speed));
                        }else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(numberFinger, intValueFinger2Angle, intValueFinger2Speed));
                        }
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger2Angle", intValueFinger2Angle);
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger2Speed", intValueFinger2Speed);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(numberFinger));
                        }else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)){
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(numberFinger, intValueFinger2Angle, 99));
                        }
                        intValueFinger2AngleLast = intValueFinger2Angle;
                    }
                    if(intValueFinger3AngleLast != intValueFinger3Angle && isEnable){
                        numberFinger = 3;
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(numberFinger, intValueFinger3Angle, intValueFinger3Speed));
                        }else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(numberFinger, intValueFinger3Angle, intValueFinger3Speed));
                        }
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger3Angle", intValueFinger3Angle);
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger3Speed", intValueFinger3Speed);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(numberFinger));
                        }else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)){
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(numberFinger, intValueFinger3Angle, 99));
                        }
                        intValueFinger3AngleLast = intValueFinger3Angle;
                    }
                    if(intValueFinger4AngleLast != intValueFinger4Angle && isEnable){
                        numberFinger = 4;
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(numberFinger, intValueFinger4Angle, intValueFinger4Speed));
                        }else {
                            presenter.onHelloWorld( mMassages.CompileMassageSettings(numberFinger, intValueFinger4Angle, intValueFinger4Speed));
                        }
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger4Angle", intValueFinger4Angle);
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger4Speed", intValueFinger4Speed);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(numberFinger));
                        }else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)){
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(numberFinger, intValueFinger4Angle, 99));
                        }
                        intValueFinger4AngleLast = intValueFinger4Angle;
                    }
                    if((intValueFinger5AngleLast != intValueFinger5Angle && isEnable)||(intValueFinger6AngleLast != intValueFinger6Angle && isEnable)){
                        numberFinger = 5;
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(numberFinger, intValueFinger5Angle, intValueFinger5Speed));
                        }else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(numberFinger, intValueFinger5Angle, intValueFinger5Speed));
                        }
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger5Angle", intValueFinger5Angle);
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger5Speed", intValueFinger5Speed);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(numberFinger));
                        }else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)){
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(numberFinger, intValueFinger5Angle, 99));
                        }
                        intValueFinger5AngleLast = intValueFinger5Angle;
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        numberFinger = 6;
                        if(flagUseHDLCProtocol){
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(numberFinger, intValueFinger6Angle, intValueFinger6Speed));
                        }else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(numberFinger, intValueFinger6Angle, intValueFinger6Speed));
                        }
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger6Angle", intValueFinger6Angle);
                        saveVariable(deviceName+NUMBER_CELL+"intValueFinger6Speed", intValueFinger6Speed);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(numberFinger));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        }catch (Exception ignored){}
                        if(flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)){
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(numberFinger, intValueFinger6Angle, 30));
                        }
                        intValueFinger6AngleLast = intValueFinger6Angle;
                    }
                    try {
                        Thread.sleep(10);
                    }catch (Exception ignored){}
                }
            }
        });
        transferThread.start();
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                        работа с меню в ботбаре                         **/
    //////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        myMenu = menu;
        return super.onPrepareOptionsMenu(myMenu);
    }
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    layoutSensors.setVisibility(View.GONE);
                    myMenu.setGroupVisible(R.id.service_settings, false);
                    showMenu = false;
//                    fab.show();
                    layoutGestures.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_dashboard:
                    layoutSensors.setVisibility(View.VISIBLE);
                    myMenu.setGroupVisible(R.id.service_settings, true);
                    showMenu = true;
                    fab.hide();
                    layoutGestures.setVisibility(View.GONE);
                    return true;
            }
            return false;
        }
    };


    //////////////////////////////////////////////////////////////////////////////
    /**                        работа с меню в апбаре                          **/
    //////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.setGroupVisible(R.id.modes, false);
        menu.setGroupVisible(R.id.service_settings, false);
        if(loadVariable(deviceName +"action_Trigger") == 0) {
            saveVariable( deviceName+"action_Trigger", 1);
        }
        menu.getItem(loadVariable(deviceName +"action_Trigger")).setChecked(true);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // ставим галочку напротив
        if(item.isChecked()){
            item.setChecked(true);
        } else {
            item.setChecked(false);
        }
        // получим идентификатор выбранного пункта меню
        int id = item.getItemId();
        // Операции для выбранного пункта меню
        switch (id) {
            case R.id.action_Trigger0:
                openSettingsDialog();
                return true;
            case R.id.action_Trigger1:
                if(flagUseHDLCProtocol){
                    pauseSendingThread(mMassages.CompileMassageTriggerModHDLC(1));
                } else {
                    presenter.onHelloWorld(mMassages.CompileMassageTriggerMod(1));
                }
                saveVariable( deviceName+"action_Trigger", 1);
                return true;
            case R.id.action_Trigger2:
                if(flagUseHDLCProtocol){
                    pauseSendingThread(mMassages.CompileMassageTriggerModHDLC(2));
                } else {
                    presenter.onHelloWorld(mMassages.CompileMassageTriggerMod(2));
                }
                saveVariable( deviceName+"action_Trigger", 2);
                return true;
            case R.id.action_Trigger3:
                if(flagUseHDLCProtocol){
                    pauseSendingThread(mMassages.CompileMassageTriggerModHDLC(3));
                } else {
                    presenter.onHelloWorld(mMassages.CompileMassageTriggerMod(3));
                }
                saveVariable( deviceName+"action_Trigger", 3);
                return true;
            case R.id.action_Trigger8:
                if(flagUseHDLCProtocol){
                    pauseSendingThread(mMassages.CompileMassageTriggerModHDLC(8));
                } else {
                    presenter.onHelloWorld(mMassages.CompileMassageTriggerMod(8));
                }
                saveVariable( deviceName+"action_Trigger", 4);
                return true;
            case R.id.action_Trigger9:
                if(flagUseHDLCProtocol){
                    pauseSendingThread(mMassages.CompileMassageTriggerModHDLC(9));
                } else {
                    presenter.onHelloWorld(mMassages.CompileMassageTriggerMod(9));
                }
                saveVariable( deviceName+"action_Trigger", 5);
                return true;
            case R.id.action_Trigger10:
                if(flagUseHDLCProtocol){
                    pauseSendingThread(mMassages.CompileMassageTriggerModHDLC(10));
                } else {
                    presenter.onHelloWorld(mMassages.CompileMassageTriggerMod(10));
                }
                saveVariable( deviceName+"action_Trigger", 6);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void openSettingsDialog() {
        SettingsDialog settingsDialog = new SettingsDialog();
        settingsDialog.show(getSupportFragmentManager(), "settings dialog");
    }
    public void openServiceSettings(){
        if(lockServiceSettings){
            if(monograbVersion){
                System.out.println("ChatActivity----> жмяк по onOptionsItemSelected в monograbVersion");
                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                        .add(view_pager, fragmentServiceSettingsMono)
                        .commit();
            } else {
                System.out.println("ChatActivity----> жмяк по onOptionsItemSelected в multigrabVersion");
                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                        .add(view_pager, fragmentServiceSettings)
                        .commit();
                navigation.clearAnimation();
                navigation.animate().translationY(heightBottomNavigation).setDuration(200);
            }
            myMenu.setGroupVisible(R.id.modes, true);
            myMenu.setGroupVisible(R.id.service_settings, false);
        } else {
            showToast(getString(R.string.not_the_right_password));
        }

    }
    @Override
    public void passwordServiceSettings(String password) {
        lockServiceSettings = password.equals("123");
    }
    public void startUpdateThread() {
        updateServiceSettingsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (updateServiceSettingsThreadFlag){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(monograbVersion){
                                if(isEnable){
                                    fragmentServiceSettingsMono.seekBarRoughness.setEnabled(true);
                                    fragmentServiceSettingsMono.switchInvert.setEnabled(true);
                                    fragmentServiceSettingsMono.seekBarIStop.setEnabled(true);
                                } else {
                                    fragmentServiceSettingsMono.seekBarRoughness.setEnabled(false);
                                    fragmentServiceSettingsMono.switchInvert.setEnabled(false);
                                    fragmentServiceSettingsMono.seekBarIStop.setEnabled(false);
                                }
                                if(invertChannel){
                                    fragmentServiceSettingsMono.switchInvert.setChecked(true);
                                } else {
                                    fragmentServiceSettingsMono.switchInvert.setChecked(false);
                                }
                            } else {
                                if(isEnable){
                                    fragmentServiceSettings.seekBarRoughness.setEnabled(true);
                                    fragmentServiceSettings.switchInvert.setEnabled(true);
                                    fragmentServiceSettings.switchNotUseInternalADC.setEnabled(true);
                                    fragmentServiceSettings.layout_calibration.setVisibility(View.VISIBLE);
                                } else {
                                    fragmentServiceSettings.seekBarRoughness.setEnabled(false);
                                    fragmentServiceSettings.switchInvert.setEnabled(false);
                                    fragmentServiceSettings.switchNotUseInternalADC.setEnabled(false);
                                    fragmentServiceSettings.layout_calibration.setVisibility(View.GONE);
                                }
                                if(invertChannel){
                                    fragmentServiceSettings.switchInvert.setChecked(true);
                                } else {
                                    fragmentServiceSettings.switchInvert.setChecked(false);
                                }
                            }
                            if((isEnable) && (!flagReceptionExpectation
                                    || ConstantManager.SKIP_GRAPH_СYCLE_FOR_SEND_UPDATE_REQUEST == numberCycle)
                                    && (flagUseHDLCProtocol) &&(!flagReadStartParametersHDLC) &&
                                    (!flagPauseSending) && (!flagOffUpdateGraphHDLC)){
                                System.err.println("запрос обновления сервисных настроек");
                                presenter.onHelloWorld(mMassages.CompileMassageMainDataHDLC());
                                flagReceptionExpectation = true;
                                if(numberCycle == ConstantManager.SKIP_GRAPH_СYCLE_FOR_SEND_UPDATE_REQUEST)
                                {showToast(getString(R.string.admission_of_inquiry_of_updating));}
                                numberCycle = 0;
                            }
//                            else {
//                                System.err.println(
//                                          "SKIP \n"+
//                                          "isEnable="+isEnable+" должно быть true \n"+
//                                          "flagReceptionExpectation="+flagReceptionExpectation+" должен быть false \n"+
//                                          "flagReadStartParametrsHDLC="+flagReadStartParametrsHDLC+" должен быть false \n"+
//                                          "flagUseHDLCProcol="+flagUseHDLCProcol+" должен быть true \n"+
//                                          "numberCycle="+numberCycle+" должен быть от 1 до 4 \n" );
//                            }
                            numberCycle++;
                            System.err.println("UpdateThread work");
                        }
                    });
                    try {
                        Thread.sleep(500);
                    }catch (Exception ignored){}
                }
            }
        });
        updateServiceSettingsThread.start();
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                           геттеры сеттеры                              **/
    //////////////////////////////////////////////////////////////////////////////
    @Override
    public void setStatus(String status) {}
    @Override
    public void setStatus(int resId) {
        System.out.println("ChatActivity----> resId setText:"+ resId + "   Айди строчки неподключения: "+R.string.bluetooth_connect_in_3sec);
        if (resId == R.string.bluetooth_connect_in_3sec){borderGray.setVisibility(View.GONE); borderGreen.setVisibility(View.GONE); borderRed.setVisibility(View.VISIBLE);}
        if (resId == R.string.bluetooth_connected){borderGray.setVisibility(View.GONE); borderGreen.setVisibility(View.VISIBLE); borderRed.setVisibility(View.GONE);}
        if (resId == R.string.bluetooth_connecting){borderGray.setVisibility(View.VISIBLE); borderGreen.setVisibility(View.GONE); borderRed.setVisibility(View.GONE);}
    }
    @Override
    public void setValueCH(int levelCH, int numberChannel) {
        if (invertChannel){
            switch (numberChannel){
                case 1:
                    receiveLevelCH2Chat = levelCH;
                    break;
                case 2:
                    receiveLevelCH1Chat = levelCH;
                    break;
            }
        } else {
            switch (numberChannel){
                case 1:
                    receiveLevelCH1Chat = levelCH;
                    break;
                case 2:
                    receiveLevelCH2Chat = levelCH;
                    break;
            }
        }
    }
    @SuppressLint("SetTextI18n")
    @Override
    public void setGeneralValue(int receiveCurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {
        if (invertChannel){
            receiveLevelCH1Chat = receiveLevelCH2;
            receiveLevelCH2Chat = receiveLevelCH1;
        } else {
            receiveLevelCH1Chat = receiveLevelCH1;
            receiveLevelCH2Chat = receiveLevelCH2;
        }

        receiveIndicationStateChat = receiveIndicationState;
        receiveBatteryTensionChat = receiveBatteryTension;

        valueBatteryTension.setText(""+receiveBatteryTensionChat); //(receiveBatteryTensionChat/1000 + "." + (receiveBatteryTensionChat%1000)/10) удаление знаков после запятой(показания напряжения)
        if (receiveIndicationStateChat == 0){
//            valueStatus.setText("покой");
            imageViewStatusOpen.setImageResource(R.drawable.circle_16_gray);
            imageViewStatusClose.setImageResource(R.drawable.circle_16_gray);
//            imageViewStatus.setImageResource(R.drawable.sleeping);
            if (receiveBlockIndication == 1){
                imageViewStatus.setImageResource(R.drawable.unblock);
//                System.err.println("ChatActivity----> разблокированно");
            } else {
                imageViewStatus.setImageResource(R.drawable.block_not_use);
//                System.err.println("ChatActivity----> блокировка не используется");
            }
        }
        if (receiveIndicationStateChat == 1){
            if(invertChannel){
//                valueStatus.setText("открытие");
                imageViewStatusOpen.setImageResource(R.drawable.circle_16_green);
                imageViewStatusClose.setImageResource(R.drawable.circle_16_gray);
//                imageViewStatus.setImageResource(R.drawable.opening);
            } else  {
//                valueStatus.setText("закрытие");
                imageViewStatusOpen.setImageResource(R.drawable.circle_16_gray);
                imageViewStatusClose.setImageResource(R.drawable.circle_16_green);
//                imageViewStatus.setImageResource(R.drawable.closing);
            }
            if (receiveBlockIndication == 1){
                imageViewStatus.setImageResource(R.drawable.unblock);
//                System.err.println("ChatActivity----> разблокированно");
            } else {
                imageViewStatus.setImageResource(R.drawable.block_not_use);
//                System.err.println("ChatActivity----> блокировка не используется");
            }
        }
        if (receiveIndicationStateChat == 2){
            if(invertChannel){
//                valueStatus.setText("закрытие");
                imageViewStatusOpen.setImageResource(R.drawable.circle_16_gray);
                imageViewStatusClose.setImageResource(R.drawable.circle_16_green);
//                imageViewStatus.setImageResource(R.drawable.closing);
            } else  {
//                valueStatus.setText("открытие");
                imageViewStatusOpen.setImageResource(R.drawable.circle_16_green);
                imageViewStatusClose.setImageResource(R.drawable.circle_16_gray);
//                imageViewStatus.setImageResource(R.drawable.opening);
            }
            if (receiveBlockIndication == 1){
                imageViewStatus.setImageResource(R.drawable.unblock);
//                System.err.println("ChatActivity----> разблокированно");
            } else {
                imageViewStatus.setImageResource(R.drawable.block_not_use);
//                System.err.println("ChatActivity----> блокировка не используется");
            }
        }
        if (receiveIndicationStateChat == 3){
//            valueStatus.setText("блок");
            imageViewStatusOpen.setImageResource(R.drawable.circle_16_green);
            imageViewStatusClose.setImageResource(R.drawable.circle_16_green);
//            switchBlockMode.setChecked(true);
            imageViewStatus.setImageResource(R.drawable.block);
//            System.err.println("ChatActivity----> заблокированно");
            receiveBlockIndication = 1;
        }
    }
    @Override
    public void setStartParameters(Integer receiveCurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication, Byte receiveRoughnessOfSensors)
    {
        invertChannel= receiveIndicationInvertMode == 1;//if (receiveIndicationInvertMode == 1) {invertChannel=true;} else {invertChannel=false;}
        if(invertChannel) {
            this.receiveLevelTrigCH1 = receiveLevelTrigCH2;
            this.receiveLevelTrigCH2 = receiveLevelTrigCH1;
            intValueCH1on = receiveLevelTrigCH1;
            intValueCH2on = receiveLevelTrigCH2;
        } else {
            this.receiveLevelTrigCH1 = receiveLevelTrigCH1;
            this.receiveLevelTrigCH2 = receiveLevelTrigCH2;
            intValueCH1on = receiveLevelTrigCH2;
            intValueCH2on = receiveLevelTrigCH1;
        }
        this.receiveCurrent = receiveCurrent;
        this.receiveIndicationInvertMode = receiveIndicationInvertMode;
        this.receiveBlockIndication = receiveBlockIndication;
        this.receiveRoughnessOfSensors = receiveRoughnessOfSensors;
    }
    @Override
    public void setStartParametersTrigCH1(Integer receiveLevelTrigCH1) {
        if(invertChannel) {
            this.receiveLevelTrigCH2 = receiveLevelTrigCH1;
            intValueCH1on = receiveLevelTrigCH1;
        } else {
            this.receiveLevelTrigCH1 = receiveLevelTrigCH1;
            intValueCH2on = receiveLevelTrigCH1;
        }
    }
    @Override
    public void setStartParametersTrigCH2(Integer receiveLevelTrigCH2) {
        if(invertChannel) {
            this.receiveLevelTrigCH1 = receiveLevelTrigCH2;
            intValueCH2on = receiveLevelTrigCH2;
        } else {
            this.receiveLevelTrigCH2 = receiveLevelTrigCH2;
            intValueCH1on = receiveLevelTrigCH2;
        }
    }
    @Override
    public void setStartParametersCurrent(Integer receiveCurrent) {
        if(receiveCurrent != 0) {
            this.receiveCurrent = receiveCurrent;
        } else {
            this.receiveCurrent = loadVariable(deviceName +"current");
        }
    }
    @Override
    public void setStartParametersBlock(Byte receiveBlockIndication) {
        this.receiveBlockIndication = receiveBlockIndication;
    }
    @Override
    public void setStartParametersRoughness(Byte receiveRoughnessOfSensors) {
        this.receiveRoughnessOfSensors = receiveRoughnessOfSensors;
    }
    @Override
    public void setStartParametersBattery(Integer receiveBatteryTension) {
        this.valueBatteryTension.setText(receiveBatteryTension);
    }
    public void setStartTrig(){
        seekBarCH1on2.setProgress((int) (receiveLevelTrigCH1 / (multiplierSeekBar - 0.25)));//-0.5
        seekBarCH2on2.setProgress((int) (receiveLevelTrigCH2 / (multiplierSeekBar - 0.25)));//-0.5
    }
    public void setStartParametersInChartActivity(){
        if (monograbVersion){ seekBarIStop.setProgress(receiveCurrent);}
        seekBarCH1on2.setProgress((int) (receiveLevelTrigCH1 / (multiplierSeekBar - 0.25)));//-0.5
        seekBarCH2on2.setProgress((int) (receiveLevelTrigCH2 / (multiplierSeekBar - 0.25)));//-0.5

        System.err.println("Параметры, полученные с кисти:\n" +
                "Trig CH1: "+receiveLevelTrigCH1+"\n"+
                "Trig CH2: "+receiveLevelTrigCH2+"\n"+
                "Current: "+ receiveCurrent +"\n"+
                "Indication mode: "+receiveIndicationInvertMode+"\n"+
                "Block indication: "+receiveBlockIndication+"\n"+
                "Roughness: "+receiveRoughnessOfSensors+"\n");
        firstRead = false;
        if(receiveBlockIndication == 0){switchBlockMode.setChecked(false); imageViewStatus.setImageResource(R.drawable.block_not_use);}
        if(receiveBlockIndication == 1){switchBlockMode.setChecked(true); imageViewStatus.setImageResource(R.drawable.unblock);}
        if(receiveBlockIndication == 2){switchBlockMode.setChecked(true); imageViewStatus.setImageResource(R.drawable.unblock);}
        if(receiveBlockIndication == 3){switchBlockMode.setChecked(true); imageViewStatus.setImageResource(R.drawable.block);}

    }
    @Override
    public void setFlagReceptionExpectation(Boolean flagReceptionExpectation) {
        ChartActivity.flagReceptionExpectation = flagReceptionExpectation;
    }
    @Override
    public void setErrorReception (boolean incomeErrorReception) {
        errorReception = incomeErrorReception;
    }
    public void setReceiveRoughnessOfSensors(byte receiveRoughnessOfSensors) {
        this.receiveRoughnessOfSensors = receiveRoughnessOfSensors;
    }
    public void getNameFromDevice(BluetoothDevice device){
        deviceName = device.getName();
    }
    public void getName(String deviceName){
        ChartActivity.deviceName =deviceName;
    }
    @Override
    public boolean getFirstRead() {
        return firstRead;
    }
    public boolean getFlagUseHDLCProtocol() {
        return flagUseHDLCProtocol;
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                схема запросов начальных параметров                     **/
    //  односхват:  ----> Trig1 ----> Trig2 ----> Current ----> Roughness       //
    //  многосхват: ----> Trig1 ----> Trig2 ----> Roughness                     //
    //////////////////////////////////////////////////////////////////////////////
    public void requestStartTrig1Thread () {
        requestStartTrig1Thread = new Thread(new Runnable() {
            @Override
            public void run() {
                TextByteReadStartParameterTrig1[3] = presenter.calculationCRC_HDLC(TextByteReadStartParameterTrig1);
                presenter.onHelloWorld(TextByteReadStartParameterTrig1);
                flagReceptionExpectation = true;
                System.out.println("ChartActivity--------------> отправка запроса Trig1= "+TextByteReadStartParameterTrig1[2]);
                try {
                    Thread.sleep(BluetoothConstantManager.TIME_RETURN_START_COMAND_HDLC_MS);
                }catch (Exception ignored){}
                if(!flagReceptionExpectation)
                {
//                    flagReadStartParametersHDLC = false;
//                    firstRead = false;
                    requestStartTrig2Thread ();
                    System.out.println("ChartActivity--------------> запуск запроса следующей функции Trig2");
                }
                if(isEnable){
                    while (flagReceptionExpectation){
                        System.out.println("ChartActivity--------------> рекурсивный запуск запроса Trig1");
                        flagReceptionExpectation = false;
                        requestStartTrig1Thread ();
                    }
                }
            }
        });
        requestStartTrig1Thread.start();
    }
    public void requestStartTrig2Thread () {
        requestStartTrig2Thread = new Thread(new Runnable() {
            @Override
            public void run() {
                TextByteReadStartParameterTrig2[3] = presenter.calculationCRC_HDLC(TextByteReadStartParameterTrig2);
                presenter.onHelloWorld(TextByteReadStartParameterTrig2);
                flagReceptionExpectation = true;
                System.out.println("ChartActivity--------------> отправка запроса Trig2= "+TextByteReadStartParameterTrig2[2]);
                try {
                    Thread.sleep(BluetoothConstantManager.TIME_RETURN_START_COMAND_HDLC_MS);
                }catch (Exception ignored){}
                if(!flagReceptionExpectation){
                    if(monograbVersion){requestStartCurrentThread();} else {requestStartRoughnessThread();}
                    System.out.println("ChartActivity--------------> запуск запроса следующей функции Curr");
                }
                if(isEnable) {
                    while (flagReceptionExpectation){
                        System.out.println("ChartActivity--------------> рекурсивный запуск запроса Trig2");
                        flagReceptionExpectation = false;
                        requestStartTrig2Thread();
                    }
                }
            }
        });
        requestStartTrig2Thread.start();
    }
    public void requestStartCurrentThread () {
        requestStartCurrentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                TextByteReadStartParameterCurrent[3] = presenter.calculationCRC_HDLC(TextByteReadStartParameterCurrent);
                presenter.onHelloWorld(TextByteReadStartParameterCurrent);
                flagReceptionExpectation = true;
                System.out.println("ChartActivity--------------> отправка запроса Current= "+TextByteReadStartParameterCurrent[2]);
                try {
                    Thread.sleep(BluetoothConstantManager.TIME_RETURN_START_COMAND_HDLC_MS);
                }catch (Exception ignored){}
                if(!flagReceptionExpectation){
//                    requestStartBlockThread();
                    //TODO вынесли запрос блокировки до лучших времён
                    requestStartRoughnessThread();
                    System.out.println("ChartActivity--------------> запуск запроса следующей функции Block");
                }
                if(isEnable) {
                    while (flagReceptionExpectation){
                        System.out.println("ChartActivity--------------> рекурсивный запуск запроса Current");
                        flagReceptionExpectation = false;
                        requestStartCurrentThread();
                    }
                }
            }
        });
        requestStartCurrentThread.start();
    }
    public void requestStartBlockThread () {
        requestStartBlockThread = new Thread(new Runnable() {
            @Override
            public void run() {
                TextByteReadStartParameterPermissionBlock[3] = presenter.calculationCRC_HDLC(TextByteReadStartParameterPermissionBlock);
                presenter.onHelloWorld(TextByteReadStartParameterPermissionBlock);
                flagReceptionExpectation = true;
                System.out.println("ChartActivity--------------> отправка запроса Block= "+TextByteReadStartParameterPermissionBlock[2]);
                try {
                    Thread.sleep(BluetoothConstantManager.TIME_RETURN_START_COMAND_HDLC_MS);
                }catch (Exception ignored){}
                if(!flagReceptionExpectation){
//                    requestStartRoughnessThread();
                    System.out.println("ChartActivity--------------> запуск запроса следующей функции Roughness");
                }
                if(isEnable) {
                    while (flagReceptionExpectation){
                        System.out.println("ChartActivity--------------> рекурсивный запуск запроса Block");
                        flagReceptionExpectation = false;
                        requestStartBlockThread();
                    }
                }
            }
        });
        requestStartBlockThread.start();
    }
    public void requestStartRoughnessThread () {
        requestStartRoughnessThread = new Thread(new Runnable() {
            @Override
            public void run() {
                TextByteReadStartParameterPermissionRoughness[3] = presenter.calculationCRC_HDLC(TextByteReadStartParameterPermissionRoughness);
                presenter.onHelloWorld(TextByteReadStartParameterPermissionRoughness);
                flagReceptionExpectation = true;
                System.out.println("ChartActivity--------------> отправка запроса Roughness= "+TextByteReadStartParameterPermissionRoughness[2]);
                try {
                    Thread.sleep(BluetoothConstantManager.TIME_RETURN_START_COMAND_HDLC_MS);
                }catch (Exception ignored){}
                if(!flagReceptionExpectation){
                    flagReadStartParametersHDLC = false;
                    firstRead = false;
                    System.out.println("ChartActivity--------------> конец запросов нач параметров");
                }
                if(isEnable) {
                    while (flagReceptionExpectation){
                        System.out.println("ChartActivity--------------> рекурсивный запуск запроса Roughness");
                        flagReceptionExpectation = false;
                        requestStartRoughnessThread();
                    }
                }
            }
        });
        requestStartRoughnessThread.start();
    }
    public void requestBatteryTensionThread () {
        requestBatteryTensionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                TextByteReadStartParameterBattery[3] = presenter.calculationCRC_HDLC(TextByteReadStartParameterBattery);
                presenter.onHelloWorld(TextByteReadStartParameterBattery);
                flagReceptionExpectation = true;
                System.out.println("ChartActivity--------------> отправка запроса Battery= "+TextByteReadStartParameterBattery[2]);
                try {
                    Thread.sleep(BluetoothConstantManager.TIME_RETURN_START_COMAND_HDLC_MS);
                }catch (Exception ignored){}
                if(!flagReceptionExpectation){
                    flagReadStartParametersHDLC = false;
                    firstRead = false;
                    System.out.println("ChartActivity--------------> конец запросов нач параметров");
                }
                if(isEnable) {
                    while (flagReceptionExpectation){
                        System.out.println("ChartActivity--------------> рекурсивный запуск запроса Battery");
                        flagReceptionExpectation = false;
                        requestBatteryTensionThread();
                    }
                }
            }
        });
        requestBatteryTensionThread.start();
    }


    //    часть отвечающая за установку флага паузы
    public void pauseSendingThread (final byte[] SendMassage) {
        pauseSendingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                flagPauseSending = true;
                System.out.println("ChartActivity--------------> ПАУЗЫ УСТАНОВКА ");
                try {
                    Thread.sleep(delayPauseAfterSending);
                }catch (Exception ignored){}
                System.out.println("ChartActivity--------------> ПАУЗЫ ОТПРАВКА ");
                presenter.onHelloWorld(SendMassage);
                try {
                    Thread.sleep(delayPauseAfterSending);
                }catch (Exception ignored){}
                System.out.println("ChartActivity--------------> ПАУЗЫ ОТМЕНА");
                flagPauseSending = false;
                if (numberCycle > ConstantManager.SKIP_GRAPH_СYCLE_FOR_SEND_UPDATE_REQUEST){
                    numberCycle = 0;
                }
            }
        });
        pauseSendingThread.start();
    }
    @Override
    public void enableInterface(boolean enabled) {
        isEnable = enabled;
        openBtn.setEnabled(enabled);
        closeBtn.setEnabled(enabled);
        seekBarCH1on.setEnabled(enabled);
        seekBarCH2on.setEnabled(enabled);
        seekBarCH1on2.setEnabled(enabled);
        seekBarCH2on2.setEnabled(enabled);
        switchBlockMode.setEnabled(enabled);
        switchIlluminationMode.setEnabled(enabled);

        if(!monograbVersion){
            offUpdate.setEnabled(enabled);
            activity_chat_gesture1.setEnabled(enabled);
            activity_chat_gesture2.setEnabled(enabled);
            activity_chat_gesture3.setEnabled(enabled);
            activity_chat_gesture4.setEnabled(enabled);
        } else {
            seekBarIStop.setEnabled(enabled);
        }
        this.runOnUi = true;
        if(isEnable){
            if(showMenu){ myMenu.setGroupVisible(R.id.service_settings, true); }
            transferThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    if(flagUseHDLCProtocol){
                        try {
                            Thread.sleep(20);
                        }catch (Exception ignored){}
                        if(firstRead){
                            setStartTrig();
//                            presenter.onHelloWorld(testCRC());
                            if(!ConstantManager.DISABLE_UPDATIONG_GRAPH){requestStartTrig1Thread ();}
                        }
                    } else {
                        try {
                            Thread.sleep(20);
                        }catch (Exception ignored){}
                        presenter.onHelloWorld(mMassages.CompileMassageReadStartParameters());
                        try {
                            Thread.sleep(500);
                        }catch (Exception ignored){}
                        presenter.onHelloWorld(mMassages.CompileMessageSetGeneralParcel((byte) 0x01));
                    }
                }
            });
            transferThread.start();
        }
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с графиками                            **/
    //////////////////////////////////////////////////////////////////////////////
    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, null);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);//.AxisDependency.LEFT
        set.setLineWidth(2f);
        set.setColor(Color.GREEN);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        set.setCircleColor(Color.GREEN);
        set.setCircleHoleColor(Color.GREEN);
        set.setCircleSize(1f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 177));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(1f);

        set.setHighLightColor(Color.YELLOW);
        return set;
    }
    private void addEntry(int event){

        LineData data = mChart.getData();

        if(data != null){
            ILineDataSet set = data.getDataSetByIndex(0);

            if(set == null){
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), event), 0);
            data.notifyDataChanged();

            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(50);
            mChart.moveViewToX(set.getEntryCount()-50);//data.getEntryCount()
        }
    }
    private void addEntry2(int event){

        LineData data = mChart2.getData();

        if(data != null){
            ILineDataSet set = data.getDataSetByIndex(0);

            if(set == null){
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), event), 0);
            data.notifyDataChanged();

            mChart2.notifyDataSetChanged();
            mChart2.setVisibleXRangeMaximum(50);
            mChart2.moveViewToX(set.getEntryCount()-50);//data.getEntryCount()

        }
    }
    public void initializedGraphForChannel1(){
        mChart = findViewById(R.id.chartCH1);

        mChart.getDescription().setEnabled(true);
        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setDragXEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(false);
        mChart.setBackgroundColor(Color.BLACK);
        mChart.getDescription().setEnabled(false);
        mChart.getHighlightByTouchPoint(1, 1);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);

        Legend legend = mChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextColor(Color.WHITE);
        legend.setForm(Legend.LegendForm.NONE);

        XAxis x1 = mChart.getXAxis();
        x1.setTextColor(Color.BLACK);
        x1.setDrawGridLines(false);
        x1.setAxisMaximum(4000000f);//x1.resetAxisMaximum();


        x1.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart.getAxisLeft();
        y1.setTextColor(Color.WHITE);
        y1.setAxisMaximum(3000f);
        y1.setAxisMinimum(-100f);
        y1.setGridColor(Color.BLACK);
        y1.setDrawGridLines(false);

        mChart.getAxisRight().setEnabled(false);
    }
    public void initializedGraphForChannel2(){
        mChart2 = findViewById(R.id.chartCH2);

        mChart2.getDescription().setEnabled(true);
        mChart2.setTouchEnabled(true);
        mChart2.setDragEnabled(false);
        mChart2.setDragXEnabled(false);
        mChart2.setScaleEnabled(false);
        mChart2.setDrawGridBackground(false);
        mChart2.setPinchZoom(false);
        mChart2.setBackgroundColor(Color.BLACK);
        mChart2.getDescription().setEnabled(false);
        mChart2.getHighlightByTouchPoint(1, 1);

        LineData data2 = new LineData();
        data2.setValueTextColor(Color.WHITE);
        mChart2.setData(data2);

        Legend legend2 = mChart2.getLegend();

        legend2.setForm(Legend.LegendForm.LINE);
        legend2.setTextColor(Color.WHITE);
        legend2.setForm(Legend.LegendForm.NONE);

        XAxis x12 = mChart2.getXAxis();
        x12.setTextColor(Color.BLACK);
        x12.setDrawGridLines(false);
        x12.setAxisMaximum(4000000f);//x1.resetAxisMaximum();
        x12.setAvoidFirstLastClipping(true);

        YAxis y1_2 = mChart2.getAxisLeft();

        // disable dual axis (only use LEFT axis)
        mChart2.getAxisRight().setEnabled(false);

        y1_2.setTextColor(Color.WHITE);
        y1_2.setAxisMaximum(3000f);
        y1_2.setAxisMinimum(-100f);
        y1_2.setGridColor(Color.BLACK);
        y1_2.setDrawGridLines(false);
    }
    public void startGraphEnteringDataThread() {
        graphThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (graphThreadFlag){
                    if(plotData){
                        addEntry(2);
                        addEntry2(2);
                        plotData = false;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isEnable && !errorReception) {
                                addEntry(receiveLevelCH1Chat);
                                addEntry2(receiveLevelCH2Chat);
                                if ((!flagReceptionExpectation ||
                                        ConstantManager.SKIP_GRAPH_СYCLE_FOR_SEND_UPDATE_REQUEST == numberCycle) &&
                                        (flagUseHDLCProtocol) &&(!flagReadStartParametersHDLC) &&
                                        (!flagPauseSending) && (!flagOffUpdateGraphHDLC)){
                                    if(!ConstantManager.DISABLE_UPDATIONG_GRAPH){
                                        System.err.println("запрос обновления графиков");
                                        presenter.onHelloWorld(mMassages.CompileMassageMainDataHDLC());
                                    }
                                    flagReceptionExpectation = true;
                                    numberCycle = 0;
                                }
                            }
                        }
                    });
                    numberCycle++;
                    if (numberCycle > ConstantManager.SKIP_GRAPH_СYCLE_FOR_SEND_UPDATE_REQUEST) {numberCycle = 0;}
                    try {
                        Thread.sleep(ConstantManager.GRAPH_UPDATE_DELAY);
                    }catch (Exception ignored){}
                }
            }
        });
        graphThread.start();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    @Override
    public void showToastWithoutConnection() {
        Toast.makeText(this, R.string.connection_is_absent, Toast.LENGTH_SHORT).show();
    }

    //////////////////////////////////////////////////////////////////////////////
    /**                      работа с меню выбора жеста                        **/
    //////////////////////////////////////////////////////////////////////////////
    @Override
    public void onGestureClick(int position) {
        switch (position){
            case 0:
                if(firstTapRecyclerView) {
                    fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .add(view_pager, fragmentGestureSettings)
                            .commit();
                    navigation.clearAnimation();
                    navigation.animate().translationY(heightBottomNavigation).setDuration(200);
                    NUMBER_CELL = 0x00;
                    firstTapRecyclerView = false;
                    showMenu = false;
                }
                break;
            case 1:
                if(firstTapRecyclerView) {
                    int commit = fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .add(view_pager, fragmentGestureSettings2)
//                            .add(R.id.view_pager, fragmentGestureSettings2)
//                            .add(R.id.view_pager, fragmentGestureSettings2)
                            .commit();
                    navigation.clearAnimation();
                    navigation.animate().translationY(heightBottomNavigation).setDuration(200);
                    NUMBER_CELL = 0x02;
                    firstTapRecyclerView = false;
                    showMenu = false;
                }
                break;
            case 2:
                if(firstTapRecyclerView) {
                    fragmentManager.beginTransaction()
                            .setCustomAnimations(R.animator.show_fr, R.animator.remove_fr)
                            .add(view_pager, fragmentGestureSettings3)
                            .commit();
                    navigation.clearAnimation();
                    navigation.animate().translationY(heightBottomNavigation).setDuration(200);
                    NUMBER_CELL = 0x04;
                    firstTapRecyclerView = false;
                    showMenu = false;
                }
                break;
        }
    }
    private void gestureUseThread (final byte fakeCellGesture, final byte cellNumGesture) {
        Thread gestureUseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                pauseSendingThread(mMassages.CompileMassageSensorActivate2HDLC(fakeCellGesture));
                System.out.println("ChartActivity--------------> выход на жест ");
                try {
                    Thread.sleep(delayPauseAfterSending);
                } catch (Exception ignored) {}
                pauseSendingThread(mMassages.CompileMassageSensorActivate2HDLC(fakeCellGesture));
                System.out.println("ChartActivity--------------> выход на жест ");
                try {
                    Thread.sleep(delayPauseAfterSending);
                } catch (Exception ignored) {}
                pauseSendingThread(mMassages.CompileMassageSwitchGestureHDLC(cellNumGesture));
                System.out.println("ChartActivity--------------> применение жеста");
            }
        });
        gestureUseThread.start();
    }

    //////////////////////////////////////////////////////////////////////////////
    /**                           работа с памятью                             **/
    //////////////////////////////////////////////////////////////////////////////
    public void saveVariable (String nameVariableInt, Integer Variable) {
        Log.e(TAG, "saveVariable ----> "+ nameVariableInt +" = "+ Variable);
        sharedPreferences = getSharedPreferences("My_variables" ,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(nameVariableInt, Variable);
        editor.apply();
    }
    public Integer loadVariable (String nameVariableInt) {
        sharedPreferences = getSharedPreferences("My_variables",MODE_PRIVATE);
        Integer variable = sharedPreferences.getInt(nameVariableInt,0);
        Log.e(TAG, "loadVariable ----> "+ nameVariableInt +" = "+ variable);
        return variable;
    }


    private float pxFromDp() {
        return (float) 48 * getApplicationContext().getResources().getDisplayMetrics().density;
    }
}
