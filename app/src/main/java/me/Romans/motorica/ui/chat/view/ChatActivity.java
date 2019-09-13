package me.Romans.motorica.ui.chat.view;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.Romans.motorica.MyApp;
import me.Romans.motorica.data.GesstureAdapter;
import me.Romans.motorica.data.Gesture_my;
import me.Romans.motorica.ui.chat.data.ChatModule;
import me.Romans.motorica.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.ui.chat.view.Gesture_settings.FragmentGestureSettings;
import me.Romans.motorica.ui.chat.view.Gesture_settings.FragmentGestureSettings2;
import me.Romans.bluetooth.ThreadHelper;
import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.data.DaggerChatComponent;
import me.Romans.motorica.ui.chat.view.Gesture_settings.FragmentGestureSettings3;
import me.Romans.motorica.ui.chat.view.Gripper_settings.FragmentGripperSettings;

/**
 * Created by Omar on 20/12/2017.
 */

public class ChatActivity extends AppCompatActivity implements ChatView, GesstureAdapter.OnGestureMyListener {
    @BindView(R.id.seekBarCH1on) SeekBar seekBarCH1on;
    @BindView(R.id.seekBarCH2on) SeekBar seekBarCH2on;
    @BindView(R.id.seekBarCH1on2) SeekBar seekBarCH1on2;
    @BindView(R.id.seekBarCH2on2) SeekBar seekBarCH2on2;
    @BindView(R.id.seekBarIstop) SeekBar seekBarIstop;
    @BindView(R.id.seekBarRoughness) SeekBar seekBarRoughness;
    @BindView(R.id.switchInvert) Switch switchInvert;
    @BindView(R.id.switchBlockMode) Switch switchBlockMode;
    @BindView(R.id.valueStatus) TextView valueStatus;
    @BindView(R.id.valueCH1on) TextView valueCH1on;
    @BindView(R.id.valueCH2on) TextView valueCH2on;
    @BindView(R.id.valueIstop) TextView valueIstop;
    @BindView(R.id.valueIstop2) TextView valueIstop2;
    @BindView(R.id.activity_chat_messages) TextView messages;
    @BindView(R.id.valueBatteryTension) TextView valueBatteryTension;
    @BindView(R.id.layout_sensors) RelativeLayout layoutSensors;
    @BindView(R.id.gestures_list_relative) RelativeLayout layoutGestures;
    @BindView(R.id.activity_chat_hello_world) Button helloWorld;
    @BindView(R.id.activity_chat_hello_world2) Button helloWorld2;
    @BindView(R.id.activity_chat_gesture1) Button activity_chat_gesture1;
    @BindView(R.id.activity_chat_gesture2) Button activity_chat_gesture2;
    @BindView(R.id.activity_chat_gesture3) Button activity_chat_gesture3;
    @BindView(R.id.activity_chat_gesture4) Button activity_chat_gesture4;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.imageViewStatus) ImageView imageViewStatus;
    @BindView(R.id.borderGray) ImageView borderGray;
    @BindView(R.id.borderGreen) ImageView borderGreen;
    @BindView(R.id.borderRed) ImageView borderRed;
    public BottomNavigationView navigation;
    private int intValueCH1on = 2500;
    private int intValueCH1off = 100;
    private int intValueCH1sleep = 200;
    private int intValueCH2on = 2500;
    private int intValueCH2off = 100;
    private int intValueCH2sleep = 200;
    private byte indicatorTypeMessage;
    private byte numberChannel;
    public byte invert = 0x00;
    public byte block = 0x00;
    public byte roughness = 0x00;
    public int curent = 0x00;
    public boolean isEnable = false;
    public boolean infinitAction = false;
    public boolean firstTapRcyclerView = true;
    public boolean stateIsOpen = false;
    public boolean errorReception = false;
    private int i = 0;
    public int multiplierSeekbar = 14;
    public byte[] TextByteTreeg = new byte[8];
    public byte[] TextByteTreegCurentSettingsAndInvert = new byte[4];
    public byte[] TextByteTreegMod = new byte[2];
    public byte[] TextByteSensorActivate = new byte[2];
    public byte[] TextByteSetGeneralParcel = new byte[2];
    public byte[] TextByteReadStartParameters = new byte [2];
    public byte[] TextByteSetBlockMode = new byte [2];
    public byte[] TextByteSetSwitchGesture = new byte [3];
    public byte[] TextByteSetRouhness = new byte [2];
//    for graph
    public int receiveСurrent = 0;
    public int receiveLevelTrigCH1 = 0;
    public int receiveLevelTrigCH2 = 0;
    public byte receiveIndicationInvertMode = 0;
    public byte receiveBlockIndication = 0;
    private boolean firstSetStartParametersFlag = true;
    private boolean isSetStartParametersActivityActiveFlag = false;
    public Thread delayThread;
    private LineChart mChart;
    private boolean plotData = true;
    private LineChart mChart2;
    public Thread graphThread;
    public boolean graphThreadFlag = false;
    public float iterator = 0;
//    for general updates
    public int receiveСurrentChat = 0;
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
    public FragmentManager fragmentManager = getSupportFragmentManager();
    public float heightBottomNavigation;
//    for 3D part
    private static String[][] model = new String[19][];
    private static String text[];
    private static String line;
    public int indexCount;
    public static int MAX_NUMBER_DETAILS = 19;
    public volatile float[][] coordArrey = new float[MAX_NUMBER_DETAILS][];
    public volatile float[][] texturessArrey = new float[MAX_NUMBER_DETAILS][];
    public volatile float[][] normalsArrey = new float[MAX_NUMBER_DETAILS][];
    public volatile static float[][] verticesArrey = new float[MAX_NUMBER_DETAILS][1];
    public volatile static int[][] indicesArreyVerteces = new int[MAX_NUMBER_DETAILS][1];
    public Thread[] threadFanction = new Thread[MAX_NUMBER_DETAILS];
//	for transfer
    private byte numberFinger;
    private int SPEED = 98;
    private static int intValueFinger1Angle = 0;
    private static int intValueFinger2Angle = 0;
    private static int intValueFinger3Angle = 0;
    private static int intValueFinger4Angle = 0;
    private static int intValueFinger5Angle = 0;
    private static int intValueFinger6Angle = 0;
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
    private byte requestType = 0x02;
    public static byte GESTURE_SETTINGS = 0x15;
    public byte NUMBER_CELL = 0x00;
    public static long delay = 200;
    public byte[] TextByteTreegSettings = new byte[8];
    public byte[] TextByteTreegComplexGestureSettings = new byte[15];
    public byte[] TextByteTreegControlComplexGesture = new byte[2];
    public byte[] TextByteTreegControl = new byte[6];
    public boolean transferThreadFlag = false;
    public Thread transferThread;

//    public ImageView imageViewStatus;

    RecyclerView recyclerView;
    GesstureAdapter gestureAdapter;
    List<Gesture_my> gestureMyList;

    @Inject
    ChatPresenter presenter;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    Log.i(TAG, "oncliiiiick");
                    layoutSensors.setVisibility(View.GONE);
//                    fab.show();
                    isSetStartParametersActivityActiveFlag = false;
                    layoutGestures.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_dashboard:
                    Log.i(TAG, ":))");
                    layoutSensors.setVisibility(View.VISIBLE);
                    fab.hide();
                    layoutGestures.setVisibility(View.GONE);
                    return true;
            }
            return false;
        }
    };

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final float scale = getResources().getDisplayMetrics().density;

        limit_1 = findViewById(R.id.limit_1);
        limit_2 = findViewById(R.id.limit_2);
        objectAnimator =ObjectAnimator.ofFloat(limit_1, "y", limit_sensor_open);

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
                        "Нажмите для редактирования начального и конечного состояний",
                        "Жест №1",
                        2,
                        600000));

        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.gesture2,
                        "bla bla bla",
                        "Нажмите для редактирования начального и конечного состояний",
                        "Жест №2",
                        2,
                        60000));

        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.gesture3,
                        "bla bla bla",
                        "Нажмите для редактирования начального и конечного состояний",
                        "Жест №3",
                        2,
                        60000));

        gestureAdapter = new GesstureAdapter(this, gestureMyList, this);
        recyclerView.setAdapter(gestureAdapter);

        DaggerChatComponent.builder()
            .bluetoothModule(MyApp.app().bluetoothModule())
            .chatModule(new ChatModule(this))
            .build().inject(this);
        ButterKnife.bind(this);

        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        heightBottomNavigation = pxFromDp(48);

        ////////initialized graph for channel 1
        initializedGraphForChannel1();
        ////////initialized graph for channel 2
        initializedGraphForChannel2();

        TextByteTreeg[2] = (byte) intValueCH1on;
        TextByteTreeg[3] = (byte) (intValueCH1on >> 8);
        TextByteTreeg[4] = (byte) intValueCH1off;
        TextByteTreeg[5] = (byte) (intValueCH1off >> 8);
        TextByteTreeg[6] = (byte) intValueCH1sleep;
        TextByteTreeg[7] = (byte) (intValueCH1sleep >> 8);

        seekBarCH1on.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueCH1on.setText(String.valueOf(seekBar.getProgress()));
                limit_sensor_open = seekBar.getProgress();
                objectAnimator =ObjectAnimator.ofFloat(limit_1, "y", ((240*scale + 0.5f)-(limit_sensor_open*scale + 0.5f)));
                objectAnimator.setDuration(200);
                objectAnimator.start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueCH1on.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH1on.setText(String.valueOf(seekBar.getProgress()*multiplierSeekbar));
                intValueCH1on = seekBarCH1on.getProgress()*multiplierSeekbar;
                indicatorTypeMessage = 0x01;
                numberChannel = 0x01;
                TextByteTreeg[0] = indicatorTypeMessage;
                TextByteTreeg[1] = numberChannel;
                TextByteTreeg[2] = (byte) intValueCH1on;
                TextByteTreeg[3] = (byte) (intValueCH1on >> 8);
                TextByteTreeg[4] = (byte) intValueCH1off;
                TextByteTreeg[5] = (byte) (intValueCH1off >> 8);
                TextByteTreeg[6] = (byte) intValueCH1sleep;
                TextByteTreeg[7] = (byte) (intValueCH1sleep >> 8);
                presenter.onHelloWorld(TextByteTreeg);
                seekBarCH1on2.setProgress(seekBarCH1on.getProgress());
            }
        });

        seekBarCH1on2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueCH1on.setText(String.valueOf(seekBar.getProgress()));
                limit_sensor_open = seekBar.getProgress();
                objectAnimator =ObjectAnimator.ofFloat(limit_1, "y", ((240*scale + 0.5f)-(limit_sensor_open*scale + 0.5f)));
                objectAnimator.setDuration(200);
                objectAnimator.start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueCH1on.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH1on.setText(String.valueOf(seekBar.getProgress()*multiplierSeekbar));
                intValueCH1on = seekBarCH1on.getProgress()*multiplierSeekbar;
                indicatorTypeMessage = 0x01;
                numberChannel = 0x01;
                TextByteTreeg[0] = indicatorTypeMessage;
                TextByteTreeg[1] = numberChannel;
                TextByteTreeg[2] = (byte) intValueCH1on;
                TextByteTreeg[3] = (byte) (intValueCH1on >> 8);
                TextByteTreeg[4] = (byte) intValueCH1off;
                TextByteTreeg[5] = (byte) (intValueCH1off >> 8);
                TextByteTreeg[6] = (byte) intValueCH1sleep;
                TextByteTreeg[7] = (byte) (intValueCH1sleep >> 8);
                presenter.onHelloWorld(TextByteTreeg);
                seekBarCH1on.setProgress(seekBarCH1on2.getProgress());
            }
        });

        seekBarCH2on.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()));
                limit_sensor_close = seekBar.getProgress();
                objectAnimator2 =ObjectAnimator.ofFloat(limit_2, "y", ((500*scale + 0.5f)-(limit_sensor_close*scale + 0.5f)));
                objectAnimator2.setDuration(200);
                objectAnimator2.start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()*multiplierSeekbar));
                intValueCH2on = seekBarCH2on.getProgress()*multiplierSeekbar;
                indicatorTypeMessage = 0x01;
                numberChannel = 0x02;
                TextByteTreeg[0] = indicatorTypeMessage;
                TextByteTreeg[1] = numberChannel;
                TextByteTreeg[2] = (byte) intValueCH2on;
                TextByteTreeg[3] = (byte) (intValueCH2on >> 8);
                TextByteTreeg[4] = (byte) intValueCH2off;
                TextByteTreeg[5] = (byte) (intValueCH2off >> 8);
                TextByteTreeg[6] = (byte) intValueCH2sleep;
                TextByteTreeg[7] = (byte) (intValueCH2sleep >> 8);
                presenter.onHelloWorld(TextByteTreeg);
                seekBarCH2on2.setProgress(seekBarCH2on.getProgress());
            }
        });

        seekBarCH2on2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()));
                limit_sensor_close = seekBar.getProgress();
                objectAnimator2 =ObjectAnimator.ofFloat(limit_2, "y", ((500*scale + 0.5f)-(limit_sensor_close*scale + 0.5f)));
                objectAnimator2.setDuration(200);
                objectAnimator2.start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()*multiplierSeekbar));
                intValueCH2on = seekBarCH2on.getProgress()*multiplierSeekbar;
                indicatorTypeMessage = 0x01;
                numberChannel = 0x02;
                TextByteTreeg[0] = indicatorTypeMessage;
                TextByteTreeg[1] = numberChannel;
                TextByteTreeg[2] = (byte) intValueCH2on;
                TextByteTreeg[3] = (byte) (intValueCH2on >> 8);
                TextByteTreeg[4] = (byte) intValueCH2off;
                TextByteTreeg[5] = (byte) (intValueCH2off >> 8);
                TextByteTreeg[6] = (byte) intValueCH2sleep;
                TextByteTreeg[7] = (byte) (intValueCH2sleep >> 8);
                presenter.onHelloWorld(TextByteTreeg);
                seekBarCH2on.setProgress(seekBarCH2on2.getProgress());
            }
        });

        seekBarIstop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueIstop.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueIstop.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueIstop.setText(String.valueOf(seekBar.getProgress()));
                curent = seekBar.getProgress();
                presenter.onHelloWorld(CompileMassegeCurentSettingsAndInvert(curent, invert));
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
                roughness = (byte) seekBar.getProgress();
                presenter.onHelloWorld(CompileMassegeRouhness(roughness));
            }
        });

        fragmentGestureSettings = new FragmentGestureSettings();
        fragmentGripperSettings = new FragmentGripperSettings();
        fragmentGestureSettings2 = new FragmentGestureSettings2();
        fragmentGestureSettings3 = new FragmentGestureSettings3();

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

        switchInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchInvert.isChecked()){
                    System.out.println("Invert mod");
                    invert = 0x01;
                    presenter.onHelloWorld(CompileMassegeCurentSettingsAndInvert(curent, invert));
                } else {
                    System.out.println("Invert Invert mod");
                    invert = 0x00;
                    presenter.onHelloWorld(CompileMassegeCurentSettingsAndInvert(curent, invert));
                }
            }
        });

        switchBlockMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchBlockMode.isChecked()){
                    System.out.println("Block mod");
                    block = 0x01;
                    presenter.onHelloWorld(CompileMassegeBlockMode(block));
                } else {
                    System.out.println("Invert Block mod");
                    block = 0x00;
                    presenter.onHelloWorld(CompileMassegeBlockMode(block));
                }
            }
        });

        helloWorld2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                int numberSensor = 0x07;
                presenter.onHelloWorld(CompileMassegeSensorActivate(numberSensor));
                addEntry(20);
                addEntry2(2500);
            }
        });

        activity_chat_gesture1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                presenter.onHelloWorld(CompileMassegeSwitchGesture((byte) 0x00, (byte) 0x01));
            }
        });

        activity_chat_gesture2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                presenter.onHelloWorld(CompileMassegeSwitchGesture((byte) 0x02, (byte) 0x03));
            }
        });

        activity_chat_gesture3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                presenter.onHelloWorld(CompileMassegeSwitchGesture((byte) 0x04, (byte) 0x05));
            }
        });

        activity_chat_gesture4.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                presenter.onHelloWorld(CompileMassegeSwitchGesture((byte) 0x06, (byte) 0x06));
            }
        });

        presenter.onCreate(getIntent());

        //TODO запускать тут поток вывода графической информации
        graphThreadFlag = true;
        startGraphEnteringDataThread();

        layoutSensors.setVisibility(View.VISIBLE);
        layoutGestures.setVisibility(View.GONE);
        fab.hide();

        ////////////////////////////////////////////////
/**                 3D initialization                        **/
        ////////////////////////////////////////////////

        model[0] = readData("STR2_big_finger_part18.obj");
        model[1] = readData("STR2_big_finger_part19.obj");
        model[2] = readData("STR2_big_finger_part1.obj");
        model[3] = readData("STR2_part3.obj");
        model[4] = readData("STR2_part9.obj");
        model[5] = readData("STR2_part13.obj");
        model[6] = readData("STR2_part14.obj");
        model[7] = readData("STR2_ukazatelnii_part15.obj");
        model[8] = readData("STR2_ukazatelnii_part4.obj");
        model[9] = readData("STR2_ukazatelnii_part17.obj");
        model[10] = readData("STR2_srednii_part8.obj");
        model[11] = readData("STR2_srednii_part6.obj");
        model[12] = readData("STR2_srednii_part16.obj");
        model[13] = readData("STR2_bezimiannii_part10.obj");
        model[14] = readData("STR2_bezimiannii_part7.obj");
        model[15] = readData("STR2_bezimiannii_part11.obj");
        model[16] = readData("STR2_mizinec_part12.obj");
        model[17] = readData("STR2_mizinec_part2.obj");
        model[18] = readData("STR2_mizinec_part5.obj");

        for (int j = 0; j<MAX_NUMBER_DETAILS; j++) {
            final int finalJ = j;
            threadFanction[j] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        loadSTR2(finalJ);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            threadFanction[j].start();
        }
    }

    public String[] readData(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            line = new String(buffer);
            text = line.split("#");
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        return text;
    }

    public static String[] getStringBuffer1()  { return model[0];  }
    public static String[] getStringBuffer2()  { return model[1];  }
    public static String[] getStringBuffer3()  { return model[2];  }
    public static String[] getStringBuffer4()  { return model[3];  }
    public static String[] getStringBuffer5()  { return model[4];  }
    public static String[] getStringBuffer6()  { return model[5];  }
    public static String[] getStringBuffer7()  { return model[6];  }
    public static String[] getStringBuffer8()  { return model[7];  }
    public static String[] getStringBuffer9()  { return model[8];  }
    public static String[] getStringBuffer10() { return model[9];  }
    public static String[] getStringBuffer11() { return model[10]; }
    public static String[] getStringBuffer12() { return model[11]; }
    public static String[] getStringBuffer13() { return model[12]; }
    public static String[] getStringBuffer14() { return model[13]; }
    public static String[] getStringBuffer15() { return model[14]; }
    public static String[] getStringBuffer16() { return model[15]; }
    public static String[] getStringBuffer17() { return model[16]; }
    public static String[] getStringBuffer18() { return model[17]; }
    public static String[] getStringBuffer19() { return model[18]; }

    public static  float[] getVertexArray(int i){
        return verticesArrey[i];
    }
    public static  int[] getIndicesArray(int i){
        return indicesArreyVerteces[i];
    }

    public void loadSTR2(final int i) throws InterruptedException {
        parserDataVertices(i);
        parserDataTextures(i);
        parserDataNormals(i);
        parserDataFacets(i);
    }

    public void parserDataVertices(int number){
        String text = "";
        if      (number ==  0) {text = "#" + getStringBuffer1() [1];}
        else if (number ==  1) {text = "#" + getStringBuffer2() [1];}
        else if (number ==  2) {text = "#" + getStringBuffer3() [1];}
        else if (number ==  3) {text = "#" + getStringBuffer4() [1];}
        else if (number ==  4) {text = "#" + getStringBuffer5() [1];}
        else if (number ==  5) {text = "#" + getStringBuffer6() [1];}
        else if (number ==  6) {text = "#" + getStringBuffer7() [1];}
        else if (number ==  7) {text = "#" + getStringBuffer8() [1];}
        else if (number ==  8) {text = "#" + getStringBuffer9() [1];}
        else if (number ==  9) {text = "#" + getStringBuffer10()[1];}
        else if (number == 10) {text = "#" + getStringBuffer11()[1];}
        else if (number == 11) {text = "#" + getStringBuffer12()[1];}
        else if (number == 12) {text = "#" + getStringBuffer13()[1];}
        else if (number == 13) {text = "#" + getStringBuffer14()[1];}
        else if (number == 14) {text = "#" + getStringBuffer15()[1];}
        else if (number == 15) {text = "#" + getStringBuffer16()[1];}
        else if (number == 16) {text = "#" + getStringBuffer17()[1];}
        else if (number == 17) {text = "#" + getStringBuffer18()[1];}
        else if (number == 18) {text = "#" + getStringBuffer19()[1];}
        String line = "";
        int coordNumber = 0;
        for (char msg : text.toCharArray()) {
            line = line + msg;
            if (msg == 10) {
                String[] currentLine = line.split(" ");
                if (line.startsWith("# ")) {
                    if (currentLine[2].equals("vertices\n")) {//\r
                        coordNumber = Integer.parseInt(currentLine[1]);
                        coordArrey[number] = new float[coordNumber * 3];
                        System.out.println("Количество вершин: " + coordNumber);
                        coordNumber = 0;
                    }
                } else if (line.startsWith("v ")){
                    coordArrey[number][coordNumber++] = Float.parseFloat(currentLine[1]);
                    coordArrey[number][coordNumber++] = Float.parseFloat(currentLine[2]);
                    coordArrey[number][coordNumber++] = Float.parseFloat(currentLine[3]);
                }
                line = "";
            }
        }
    }
    public void parserDataTextures(int number){
        String text = "";
        if      (number ==  0) {text = "#" + getStringBuffer1() [2];}
        else if (number ==  1) {text = "#" + getStringBuffer2() [2];}
        else if (number ==  2) {text = "#" + getStringBuffer3() [2];}
        else if (number ==  3) {text = "#" + getStringBuffer4() [2];}
        else if (number ==  4) {text = "#" + getStringBuffer5() [2];}
        else if (number ==  5) {text = "#" + getStringBuffer6() [2];}
        else if (number ==  6) {text = "#" + getStringBuffer7() [2];}
        else if (number ==  7) {text = "#" + getStringBuffer8() [2];}
        else if (number ==  8) {text = "#" + getStringBuffer9() [2];}
        else if (number ==  9) {text = "#" + getStringBuffer10()[2];}
        else if (number == 10) {text = "#" + getStringBuffer11()[2];}
        else if (number == 11) {text = "#" + getStringBuffer12()[2];}
        else if (number == 12) {text = "#" + getStringBuffer13()[2];}
        else if (number == 13) {text = "#" + getStringBuffer14()[2];}
        else if (number == 14) {text = "#" + getStringBuffer15()[2];}
        else if (number == 15) {text = "#" + getStringBuffer16()[2];}
        else if (number == 16) {text = "#" + getStringBuffer17()[2];}
        else if (number == 17) {text = "#" + getStringBuffer18()[2];}
        else if (number == 18) {text = "#" + getStringBuffer19()[2];}
        String line = "";

        int texturesNumber = 0;
        for (char msg : text.toCharArray()){
            line = line + msg;
            if (msg == 10){
                String[] currentLine = line.split(" ");
                if(line.startsWith("# ")){
                    if(currentLine[2].equals("texture")){
                        texturesNumber = Integer.parseInt(currentLine[1]);
                        texturessArrey[number] = new float[texturesNumber*2];
                        System.out.println("Количество текстурных координат: " + texturesNumber);
                        texturesNumber = 0;
                    }
                }else if (line.startsWith("vt ")){
                    texturessArrey[number][texturesNumber] = Float.parseFloat(currentLine[1]);
                    texturessArrey[number][texturesNumber + 1] = Float.parseFloat(currentLine[2]);
                    texturesNumber += 2;
                }
                line = "";
            }
        }
    }
    public void parserDataNormals(int number){
        String text = "";
        if      (number ==  0) {text = "#" + getStringBuffer1() [3];}
        else if (number ==  1) {text = "#" + getStringBuffer2() [3];}
        else if (number ==  2) {text = "#" + getStringBuffer3() [3];}
        else if (number ==  3) {text = "#" + getStringBuffer4() [3];}
        else if (number ==  4) {text = "#" + getStringBuffer5() [3];}
        else if (number ==  5) {text = "#" + getStringBuffer6() [3];}
        else if (number ==  6) {text = "#" + getStringBuffer7() [3];}
        else if (number ==  7) {text = "#" + getStringBuffer8() [3];}
        else if (number ==  8) {text = "#" + getStringBuffer9() [3];}
        else if (number ==  9) {text = "#" + getStringBuffer10()[3];}
        else if (number == 10) {text = "#" + getStringBuffer11()[3];}
        else if (number == 11) {text = "#" + getStringBuffer12()[3];}
        else if (number == 12) {text = "#" + getStringBuffer13()[3];}
        else if (number == 13) {text = "#" + getStringBuffer14()[3];}
        else if (number == 14) {text = "#" + getStringBuffer15()[3];}
        else if (number == 15) {text = "#" + getStringBuffer16()[3];}
        else if (number == 16) {text = "#" + getStringBuffer17()[3];}
        else if (number == 17) {text = "#" + getStringBuffer18()[3];}
        else if (number == 18) {text = "#" + getStringBuffer19()[3];}
        String line = "";

        int normalsNumber = 0;

        for (char msg : text.toCharArray()){
            line = line + msg;
            if (msg == 10) {
                String[] currentLine = line.split(" ");
                if (line.startsWith("# ")) {
                    if (currentLine[2].equals("vertex")) {
                        normalsNumber = Integer.parseInt(currentLine[1]);
                        normalsArrey[number] = new float[normalsNumber * 3];
                        System.out.println("Количество координат нормалей: " + normalsNumber);
                        normalsNumber = 0;
                    }
                } else if (line.startsWith("vn ")) {
                    normalsArrey[number][normalsNumber] = Float.parseFloat(currentLine[1]);
                    normalsArrey[number][normalsNumber + 1] = Float.parseFloat(currentLine[2]);
                    normalsArrey[number][normalsNumber + 2] = Float.parseFloat(currentLine[3]);
                    normalsNumber += 3;
                }
                line = "";
            }
        }
    }
    public void parserDataFacets (int number){
        String text = "";
        if      (number ==  0) {text = "#" + getStringBuffer1() [4];}
        else if (number ==  1) {text = "#" + getStringBuffer2() [4];}
        else if (number ==  2) {text = "#" + getStringBuffer3() [4];}
        else if (number ==  3) {text = "#" + getStringBuffer4() [4];}
        else if (number ==  4) {text = "#" + getStringBuffer5() [4];}
        else if (number ==  5) {text = "#" + getStringBuffer6() [4];}
        else if (number ==  6) {text = "#" + getStringBuffer7() [4];}
        else if (number ==  7) {text = "#" + getStringBuffer8() [4];}
        else if (number ==  8) {text = "#" + getStringBuffer9() [4];}
        else if (number ==  9) {text = "#" + getStringBuffer10()[4];}
        else if (number == 10) {text = "#" + getStringBuffer11()[4];}
        else if (number == 11) {text = "#" + getStringBuffer12()[4];}
        else if (number == 12) {text = "#" + getStringBuffer13()[4];}
        else if (number == 13) {text = "#" + getStringBuffer14()[4];}
        else if (number == 14) {text = "#" + getStringBuffer15()[4];}
        else if (number == 15) {text = "#" + getStringBuffer16()[4];}
        else if (number == 16) {text = "#" + getStringBuffer17()[4];}
        else if (number == 17) {text = "#" + getStringBuffer18()[4];}
        else if (number == 18) {text = "#" + getStringBuffer19()[4];}
        String line = "";

        int indicesVretices = 0;
        int indecesCoordinateV = 0;
        int indecesNormalsV = 0;
        int indecesTextureV = 0;

        for (char msg : text.toCharArray()){
            line = line + msg;
            if (msg == 10){
                String[] currentLine = line.split(" ");
                if(line.startsWith("# ")){
                    if(currentLine[2].equals("facets\n")){//\r
                        indicesVretices = Integer.parseInt(currentLine[1]);
                        verticesArrey[number] = new float[indicesVretices*12*3];
                        indicesArreyVerteces[number] = new int [indicesVretices*3];
                        System.out.println("Количество треугольников: " + indicesVretices);
                        indicesVretices = 0;
                    }
                } else if (line.startsWith("f ")){
                    //первая тройка
                    //координаты вершины
                    indecesCoordinateV = (Integer.parseInt(currentLine[1].split("/")[0]) - 1);
                    verticesArrey[number][indicesVretices * 12 + 0] = coordArrey[number][indecesCoordinateV * 3 + 0];
                    verticesArrey[number][indicesVretices * 12 + 1] = coordArrey[number][indecesCoordinateV * 3 + 1];
                    verticesArrey[number][indicesVretices * 12 + 2] = coordArrey[number][indecesCoordinateV * 3 + 2];
                    //нормали
                    indecesNormalsV = (Integer.parseInt(currentLine[1].split("/")[2]) - 1);
                    verticesArrey[number][indicesVretices * 12 + 3] = normalsArrey[number][indecesNormalsV * 3 + 0];
                    verticesArrey[number][indicesVretices * 12 + 4] = normalsArrey[number][indecesNormalsV * 3 + 1];
                    verticesArrey[number][indicesVretices * 12 + 5] = normalsArrey[number][indecesNormalsV * 3 + 2];
                    //цвета
                    verticesArrey[number][indicesVretices * 12 + 6] = 1.0f;
                    verticesArrey[number][indicesVretices * 12 + 7] = 1.0f;
                    verticesArrey[number][indicesVretices * 12 + 8] = 0.0f;
                    verticesArrey[number][indicesVretices * 12 + 9] = 0.0f;
                    //текстурные координаты
                    indecesTextureV = (Integer.parseInt(currentLine[1].split("/")[1]) - 1);
                    verticesArrey[number][indicesVretices * 12 + 10] = texturessArrey[number][indecesTextureV * 2 + 0];
                    verticesArrey[number][indicesVretices * 12 + 11] = texturessArrey[number][indecesTextureV * 2 + 1];

                    indicesArreyVerteces[number][indicesVretices] = indicesVretices++;

                    //вторая тройка
                    //координаты вершины
                    indecesCoordinateV = (Integer.parseInt(currentLine[2].split("/")[0]) - 1);
                    verticesArrey[number][indicesVretices * 12 + 0] = coordArrey[number][indecesCoordinateV * 3 + 0];
                    verticesArrey[number][indicesVretices * 12 + 1] = coordArrey[number][indecesCoordinateV * 3 + 1];
                    verticesArrey[number][indicesVretices * 12 + 2] = coordArrey[number][indecesCoordinateV * 3 + 2];
                    //нормали
                    verticesArrey[number][indicesVretices * 12 + 3] = normalsArrey[number][(Integer.parseInt(currentLine[2].split("/")[2]) - 1) * 3 + 0];
                    verticesArrey[number][indicesVretices * 12 + 4] = normalsArrey[number][(Integer.parseInt(currentLine[2].split("/")[2]) - 1) * 3 + 1];
                    verticesArrey[number][indicesVretices * 12 + 5] = normalsArrey[number][(Integer.parseInt(currentLine[2].split("/")[2]) - 1) * 3 + 2];
                    //цвета
                    verticesArrey[number][indicesVretices * 12 + 6] = 1.0f;
                    verticesArrey[number][indicesVretices * 12 + 7] = 1.0f;
                    verticesArrey[number][indicesVretices * 12 + 8] = 0.0f;
                    verticesArrey[number][indicesVretices * 12 + 9] = 0.0f;
                    //текстурные координаты
                    indecesTextureV = (Integer.parseInt(currentLine[2].split("/")[1]) - 1);
                    verticesArrey[number][indicesVretices * 12 + 10] = texturessArrey[number][indecesTextureV * 2 + 0];
                    verticesArrey[number][indicesVretices * 12 + 11] = texturessArrey[number][indecesTextureV * 2 + 1];

                    indicesArreyVerteces[number][indicesVretices] = indicesVretices++;

                    //третья тройка
                    //координаты вершины
                    indecesCoordinateV = (Integer.parseInt(currentLine[3].split("/")[0]) - 1);
                    verticesArrey[number][indicesVretices * 12 + 0] = coordArrey[number][indecesCoordinateV * 3 + 0];
                    verticesArrey[number][indicesVretices * 12 + 1] = coordArrey[number][indecesCoordinateV * 3 + 1];
                    verticesArrey[number][indicesVretices * 12 + 2] = coordArrey[number][indecesCoordinateV * 3 + 2];
                    //нормали
                    indecesNormalsV = (Integer.parseInt(currentLine[3].split("/")[2].split("\n")[0]) - 1);//.split("\r")[0]
                    verticesArrey[number][indicesVretices * 12 + 3] = normalsArrey[number][indecesNormalsV * 3 + 0];
                    verticesArrey[number][indicesVretices * 12 + 4] = normalsArrey[number][indecesNormalsV * 3 + 1];
                    verticesArrey[number][indicesVretices * 12 + 5] = normalsArrey[number][indecesNormalsV * 3 + 2];
                    //цвета
                    verticesArrey[number][indicesVretices * 12 + 6] = 1.0f;
                    verticesArrey[number][indicesVretices * 12 + 7] = 1.0f;
                    verticesArrey[number][indicesVretices * 12 + 8] = 0.0f;
                    verticesArrey[number][indicesVretices * 12 + 9] = 0.0f;
                    //текстурные координаты
                    indecesTextureV = (Integer.parseInt(currentLine[3].split("/")[1]) - 1);
                    verticesArrey[number][indicesVretices * 12 + 10] = texturessArrey[number][indecesTextureV * 2 + 0];
                    verticesArrey[number][indicesVretices * 12 + 11] = texturessArrey[number][indecesTextureV * 2 + 1];

                    indicesArreyVerteces[number][indicesVretices] = indicesVretices++;
                }
                line = "";
            }
        }
    }

    public void startTransferThread () {
        transferThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (transferThreadFlag){
                    if(intValueFinger1AngleLast != intValueFinger1Angle && isEnable){
                        numberFinger = 0x01;
                        CompileMassegeSettings(numberFinger, intValueFinger1Angle, intValueFinger1Speed);
                        presenter.onHelloWorld(TextByteTreegSettings);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception e){}
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                        intValueFinger1AngleLast = intValueFinger1Angle;
                    }
                    if(intValueFinger2AngleLast != intValueFinger2Angle && isEnable){
//                        System.err.println("ChatActivity--------> angleRingFingerTransfer: "+ intValueFinger2Angle);
                        numberFinger = 0x02;
                        CompileMassegeSettings(numberFinger, intValueFinger2Angle, intValueFinger2Speed);
                        presenter.onHelloWorld(TextByteTreegSettings);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception e){}
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                        intValueFinger2AngleLast = intValueFinger2Angle;
                    }
                    if(intValueFinger3AngleLast != intValueFinger3Angle && isEnable){
//                        System.err.println("ChatActivity--------> angleMiddleFingerTransfer: "+ intValueFinger3Angle);
                        numberFinger = 0x03;
                        CompileMassegeSettings(numberFinger, intValueFinger3Angle, intValueFinger3Speed);
                        presenter.onHelloWorld(TextByteTreegSettings);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception e){}
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                        intValueFinger3AngleLast = intValueFinger3Angle;
                    }
                    if(intValueFinger4AngleLast != intValueFinger4Angle && isEnable){
//                        System.err.println("ChatActivity--------> angleForeFingerTransfer: "+ intValueFinger4Angle);
                        numberFinger = 0x04;
                        CompileMassegeSettings(numberFinger, intValueFinger4Angle, intValueFinger4Speed);
                        presenter.onHelloWorld(TextByteTreegSettings);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception e){}
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                        intValueFinger4AngleLast = intValueFinger4Angle;
                    }
                    if((intValueFinger5AngleLast != intValueFinger5Angle && isEnable)||(intValueFinger6AngleLast != intValueFinger6Angle && isEnable)){
//                        System.err.println("ChatActivity--------> angleBigFingerTransfer1: "+ intValueFinger5Angle);
                        numberFinger = 0x05;
                        CompileMassegeSettings(numberFinger, intValueFinger5Angle, intValueFinger5Speed);
                        presenter.onHelloWorld(TextByteTreegSettings);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception e){}
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                        intValueFinger5AngleLast = intValueFinger5Angle;
                        try {
                            Thread.sleep(delay);
                        }catch (Exception e){}
//                        System.err.println("ChatActivity--------> angleBigFingerTransfer2: "+ intValueFinger6Angle);
                        numberFinger = 0x06;
                        CompileMassegeSettings(numberFinger, intValueFinger6Angle, intValueFinger6Speed);
                        presenter.onHelloWorld(TextByteTreegSettings);
                        try {
                            Thread.sleep(delay);
                        }catch (Exception e){}
                        CompileMassegeControl(numberFinger);
                        presenter.onHelloWorld(TextByteTreegControl);
                        intValueFinger6AngleLast = intValueFinger6Angle;
                    }
                    try {
                        Thread.sleep(10);
                    }catch (Exception e){}
                }
            }
        });
        transferThread.start();
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
                            }
                        }
                    });
                    try {
                        Thread.sleep(50);
                    }catch (Exception e){}
                }
            }
        });
        graphThread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public static void transferFinger1Static (int angleFinger1){ intValueFinger1Angle = angleFinger1; }
    public static void transferFinger2Static (int angleFinger2){ intValueFinger2Angle = angleFinger2; }
    public static void transferFinger3Static (int angleFinger3){ intValueFinger3Angle = angleFinger3; }
    public static void transferFinger4Static (int angleFinger4){ intValueFinger4Angle = angleFinger4; }
    public static void transferFinger5Static (int angleFinger5){ intValueFinger5Angle = angleFinger5; }
    public static void transferFinger6Static (int angleFinger6){ intValueFinger6Angle = angleFinger6; }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // получим идентификатор выбранного пункта меню
        int id = item.getItemId();
        // Операции для выбранного пункта меню
        switch (id) {
            case R.id.action_Trigger1:
                presenter.onHelloWorld(CompileMassegeTreegMod (1));
                if (transferThread.isAlive()) {
                    transferThread.interrupt();
                }
                infinitAction = false;
                return true;
            case R.id.action_Trigger2:
                presenter.onHelloWorld(CompileMassegeTreegMod (2));
                infinitAction = false;
                return true;
            case R.id.action_Trigger3:
                presenter.onHelloWorld(CompileMassegeTreegMod (3));
                infinitAction = false;
                return true;
//            case R.id.action_Trigger4:
//                presenter.onHelloWorld(CompileMassegeTreegMod (4));
//                infinitAction = false;
//                return true;
//            case R.id.action_Trigger5:
//                presenter.onHelloWorld(CompileMassegeTreegMod (5));
//                infinitAction = false;
//                return true;
//            case R.id.action_Trigger6:
//                presenter.onHelloWorld(CompileMassegeTreegMod (6));
//                infinitAction = false;
//                return true;
//            case R.id.action_Trigger7:
//                presenter.onHelloWorld(CompileMassegeTreegMod (7));
//                infinitAction = false;
//                return true;
            case R.id.action_Trigger8:
                presenter.onHelloWorld(CompileMassegeTreegMod (8));
                infinitAction = false;
                return true;
            case R.id.action_Trigger9:
                presenter.onHelloWorld(CompileMassegeTreegMod (9));
                infinitAction = false;
                return true;
            case R.id.action_Trigger10:
                presenter.onHelloWorld(CompileMassegeTreegMod (10));
                infinitAction = false;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, null);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);//.AxisDependency.LEFT
        set.setLineWidth(2f);
        set.setColor(Color.GREEN);
        set.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

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

    @Override
    protected void onResume() { super.onResume(); }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            graphThread.interrupt();
        } catch (Exception e){}
    }

    @OnClick(R.id.activity_chat_hello_world)
    public void onHelloWorld(){
        int numberSensor = 0x06;
        presenter.onHelloWorld(CompileMassegeSensorActivate(numberSensor));
        addEntry(2500);
        addEntry2(20);
    }

    @Override
    public void setStatus(String status) {

    }

    @Override
    public void setStatus(int resId) {
        System.out.println("ChatActivity----> resId setText:"+ resId);
        if (resId == 2131689516){borderGray.setVisibility(View.GONE); borderGreen.setVisibility(View.GONE); borderRed.setVisibility(View.VISIBLE);}
        if (resId == 2131689517){borderGray.setVisibility(View.GONE); borderGreen.setVisibility(View.VISIBLE); borderRed.setVisibility(View.GONE);}
        if (resId == 2131689518){borderGray.setVisibility(View.VISIBLE); borderGreen.setVisibility(View.GONE); borderRed.setVisibility(View.GONE);}
    }

    @Override
    public void setValueCH(int levelCH, int numberChannel) {
        String strlevelCH = new String(String.valueOf(levelCH));
        Integer numberOfChannel = new Integer(numberChannel);
        switch (numberOfChannel){
            case 1:
                receiveLevelCH1Chat = levelCH;
                break;
            case 2:
                receiveLevelCH2Chat = levelCH;
                break;
        }
    }

    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {
        receiveСurrentChat = new Integer(receiveСurrent);
        receiveLevelCH1Chat = new Integer(receiveLevelCH1);
        receiveLevelCH2Chat = new Integer(receiveLevelCH2);
        receiveIndicationStateChat = new Byte(receiveIndicationState);
        receiveBatteryTensionChat = new Integer(receiveBatteryTension);

        valueIstop2.setText(String.valueOf(receiveСurrentChat));
        valueBatteryTension.setText(receiveBatteryTensionChat/1000 + "." + (receiveBatteryTensionChat%1000)/10); //(receiveBatteryTensionChat%1000)/10 удаление знаков после запятой(показания напряжения)
        if (receiveIndicationStateChat == 0){valueStatus.setText("покой"); imageViewStatus.setImageResource(R.drawable.sleeping);}
        if (receiveIndicationStateChat == 1){valueStatus.setText("закрытие"); imageViewStatus.setImageResource(R.drawable.closing);}
        if (receiveIndicationStateChat == 2){valueStatus.setText("открытие"); imageViewStatus.setImageResource(R.drawable.opening);}
        if (receiveIndicationStateChat == 3){valueStatus.setText("блок"); imageViewStatus.setImageResource(R.drawable.block);}

//        System.out.println("ChatActivity----> Сurrent:"+ receiveСurrentChat);
//        System.out.println("ChatActivity----> Level CH1:"+ receiveLevelCH1Chat);
//        System.out.println("ChatActivity----> Level CH2:"+ receiveLevelCH2Chat);
//        System.out.println("ChatActivity----> Indication State:"+ receiveIndicationStateChat);
//        System.out.println("ChatActivity----> Battery Tension:"+ receiveBatteryTensionChat);
    }

    @Override
    public void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication) {
        this.receiveСurrent = receiveСurrent;
        this.receiveLevelTrigCH1 = receiveLevelTrigCH1;
        this.receiveLevelTrigCH2 = receiveLevelTrigCH2;
        this.receiveIndicationInvertMode = receiveIndicationInvertMode;
        this.receiveBlockIndication = receiveBlockIndication;
    }

    public void setStartParametersInGraphActivity(){
            seekBarIstop.setProgress(receiveСurrent);
            seekBarCH1on.setProgress((int) (receiveLevelTrigCH1/(multiplierSeekbar-0.5)));
            seekBarCH2on.setProgress((int) (receiveLevelTrigCH2/(multiplierSeekbar-0.5)));
            if (receiveIndicationInvertMode == 1){
                switchInvert.setChecked(true);
            } else {
//                switchInvert.setChecked(false);
            }
            if (receiveBlockIndication == 1){
                switchBlockMode.setChecked(true);
            } else {
//                switchBlockMode.setChecked(false);
            }
    }

    @Override
    public void setErrorReception (boolean incomeErrorReception) {
        errorReception = incomeErrorReception;
    }

    @Override
    public void appendMessage(String message) {
        String str = message + " C-->" + i;//messages.getText()+"\n"+
        messages.setText(str);
        i++;
    }

    @Override
    public void enableHWButton(boolean enabled) {
        isEnable = enabled;
        helloWorld.setEnabled(enabled);
        helloWorld2.setEnabled(enabled);
        activity_chat_gesture1.setEnabled(enabled);
        activity_chat_gesture2.setEnabled(enabled);
        activity_chat_gesture3.setEnabled(enabled);
        activity_chat_gesture4.setEnabled(enabled);
        seekBarCH1on.setEnabled(enabled);
//        seekBarCH1off.setEnabled(enabled);
//        seekBarCH1sleep.setEnabled(enabled);
        seekBarCH2on.setEnabled(enabled);
        switchInvert.setEnabled(enabled);
        switchBlockMode.setEnabled(enabled);
//        seekBarCH2off.setEnabled(enabled);
//        seekBarCH2sleep.setEnabled(enabled);
        seekBarIstop.setEnabled(enabled);
        seekBarRoughness.setEnabled(enabled);
//        this.activity = Activity;
        this.runOnUi = true;
        if(isEnable){
            transferThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    CompileMassegeReadStartParameters();
                    presenter.onHelloWorld(TextByteReadStartParameters);
                    try {
                        Thread.sleep(300);
                    }catch (Exception e){}
                    CompileMessageSetGeneralParcel((byte) 0x01);
                    presenter.onHelloWorld(TextByteSetGeneralParcel);
                }
            });
            transferThread.start();
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

    public void GetPosition_My (int position, BluetoothDevice device){
        System.out.println("из ChatAct-------------> Передача position:" + position);
        System.out.println("из ChatAct-------------> выжимка из интента devise:" + device);
    }

    private byte[] CompileMassegeSettings(byte numberFinger, int intValueFingerAngle, int intValueFingerSpeed){
        TextByteTreegSettings[0] = 0x03;
        TextByteTreegSettings[1] = numberFinger;
        TextByteTreegSettings[2] = requestType;
        TextByteTreegSettings[3] = GESTURE_SETTINGS;
        TextByteTreegSettings[4] = NUMBER_CELL;
        TextByteTreegSettings[5] = (byte) intValueFingerSpeed;
        TextByteTreegSettings[6] = (byte) intValueFingerAngle;
        TextByteTreegSettings[7] = presenter.calculationCRC(TextByteTreegSettings);
        return TextByteTreegSettings;
    }

    public byte[] CompileMassegeComplexGestureSettings(int GESTURE_NUMBER, int GripperNumberStart1, int mySensorEvent1, int GripperNumberEnd1, int GripperNumberStart2, int mySensorEvent2, int GripperNumberEnd2, int indicatorTypeMessage){
        TextByteTreegComplexGestureSettings[0] = (byte) indicatorTypeMessage;
        TextByteTreegComplexGestureSettings[1] = (byte) (GESTURE_NUMBER >> 8);
        TextByteTreegComplexGestureSettings[2] = (byte) GESTURE_NUMBER;
        TextByteTreegComplexGestureSettings[3] = (byte) (GripperNumberStart1 >> 8);
        TextByteTreegComplexGestureSettings[4] = (byte) GripperNumberStart1;
        TextByteTreegComplexGestureSettings[5] = (byte) (mySensorEvent1 >> 8);
        TextByteTreegComplexGestureSettings[6] = (byte) mySensorEvent1;
        TextByteTreegComplexGestureSettings[7] = (byte) (GripperNumberEnd1 >> 8);
        TextByteTreegComplexGestureSettings[8] = (byte) GripperNumberEnd1;
        TextByteTreegComplexGestureSettings[9] = (byte) (GripperNumberStart2 >> 8);;
        TextByteTreegComplexGestureSettings[10] = (byte) GripperNumberStart2;
        TextByteTreegComplexGestureSettings[11] = (byte) (mySensorEvent2 >> 8);
        TextByteTreegComplexGestureSettings[12] = (byte) mySensorEvent2;
        TextByteTreegComplexGestureSettings[13] = (byte) (GripperNumberEnd2 >> 8);
        TextByteTreegComplexGestureSettings[14] = (byte) GripperNumberEnd2;

        return TextByteTreegComplexGestureSettings;
    }

    public byte[] CompileMassegeControlComplexGesture(int GESTURE_NUMBER){
        TextByteTreegControlComplexGesture[0] = 0x06;
        TextByteTreegControlComplexGesture[1] = (byte) GESTURE_NUMBER;
        return TextByteTreegControlComplexGesture;
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

    private byte[] CompileMassegeTreegMod (int Treeg_id){
        TextByteTreegMod[0] = 0x08;
        TextByteTreegMod[1] = (byte) Treeg_id;
        System.out.println("Treeg mod:" + Treeg_id);
        return TextByteTreegMod;
    }

    private byte[] CompileMassegeSensorActivate (int numberSensor){
        TextByteSensorActivate[0] = 0x09;
        TextByteSensorActivate[1] = (byte) numberSensor;
        return TextByteSensorActivate;
    }

    private byte[] CompileMassegeCurentSettingsAndInvert (int Curent, byte Invert) {
        TextByteTreegCurentSettingsAndInvert[0] = 0x0B;
        TextByteTreegCurentSettingsAndInvert[1] = (byte) Curent;
        TextByteTreegCurentSettingsAndInvert[2] = (byte) (Curent >> 8);
        TextByteTreegCurentSettingsAndInvert[3] = Invert;
        return TextByteTreegCurentSettingsAndInvert;
    }

    private byte[] CompileMessageSetGeneralParcel (byte turningOn){
        TextByteSetGeneralParcel[0] = 0x0C;
        TextByteSetGeneralParcel[1] = turningOn;
        return TextByteSetGeneralParcel;
    }

    private byte[] CompileMassegeReadStartParameters () {
        TextByteReadStartParameters[0] = 0x0D;
        TextByteReadStartParameters[1] = 0x00;
        return TextByteReadStartParameters;
    }

    private byte[] CompileMassegeBlockMode (byte onBlockMode) {
        TextByteSetBlockMode[0] = 0x0E;
        TextByteSetBlockMode[1] = onBlockMode; // 0x01 on     0x00 off
        return TextByteSetBlockMode;
    }

    public byte[] CompileMassegeSwitchGesture(byte openGesture, byte closeGesture) {
        System.err.println("ChatActivity----> укомпановали байт массив");
        TextByteSetSwitchGesture[0] = 0x0F;
        TextByteSetSwitchGesture[1] = openGesture;
        TextByteSetSwitchGesture[2] = closeGesture;
        return TextByteSetSwitchGesture;
    }

    public void TranslateMassegeControlComplexGesture(){
        presenter.onHelloWorld(TextByteTreegControlComplexGesture);
    }

    public void TranslateMassegeComplexGestureSettings(){
        presenter.onHelloWorld(TextByteTreegComplexGestureSettings);
    }

    public void TranslateMassegeSwitchGesture(){
        System.err.println("ChatActivity----> отправили байт массив");
        presenter.onHelloWorld(TextByteSetSwitchGesture);
    }

    private byte[] CompileMassegeRouhness (byte roughness) {
        TextByteSetRouhness[0] = 0x10;
        TextByteSetRouhness[1] = roughness; // 0x01 on     0x00 off
        return TextByteSetRouhness;
    }


    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(isEnable){
            ThreadHelper.run(runOnUi, this, new Runnable() {
                @Override
                public void run() {
                    CompileMessageSetGeneralParcel((byte) 0x00);
                    presenter.onHelloWorld(TextByteSetGeneralParcel);
                    try {
                        Thread.sleep(500);
                    }catch (Exception e){}
                }
            });
            presenter.onHelloWorld(TextByteSetGeneralParcel);
        }
        presenter.disconnect();
    }

    public void setInfinitAction () {
        if (infinitAction){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (stateIsOpen){
                        int numberSensor = 0x06;
                        presenter.onHelloWorld(CompileMassegeSensorActivate(numberSensor));
                        stateIsOpen = false;
                        setInfinitAction ();
                    } else {
                        int numberSensor = 0x07;
                        presenter.onHelloWorld(CompileMassegeSensorActivate(numberSensor));
                        stateIsOpen = true;
                        setInfinitAction ();
                    }
                }
            }, 3000);
        } else {}
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onGestureClick(int position) {
        final BluetoothDevice device = getIntent().getExtras().getParcelable("device");
        switch (position){
            case 0:
                if(firstTapRcyclerView) {
                    fragmentManager.beginTransaction()
                            .add(R.id.view_pager, fragmentGestureSettings)
                            .commit();
                    navigation.clearAnimation();
                    navigation.animate().translationY(heightBottomNavigation).setDuration(200);
                    NUMBER_CELL = 0x00;
                    firstTapRcyclerView = false;
                }
                break;
            case 1:
                if(firstTapRcyclerView) {
                    fragmentManager.beginTransaction()
                            .add(R.id.view_pager, fragmentGestureSettings2)
                            .commit();
                    navigation.clearAnimation();
                    navigation.animate().translationY(heightBottomNavigation).setDuration(200);
                    NUMBER_CELL = 0x02;
                    firstTapRcyclerView = false;
                }
                break;
            case 2:
               if(firstTapRcyclerView) {
                    fragmentManager.beginTransaction()
                            .add(R.id.view_pager, fragmentGestureSettings3)
                            .commit();
                    navigation.clearAnimation();
                    navigation.animate().translationY(heightBottomNavigation).setDuration(200);
                    NUMBER_CELL = 0x04;
                    firstTapRcyclerView = false;
                }
                break;
//            case 3:
//                presenter.disconnect();
//                Intent intent4 = new Intent(this, Gesture_settings4.class);
//                intent4.putExtra("device", device);
//                startActivity(intent4);
//                break;
//            case 4:
//                presenter.disconnect();
//                Intent intent5 = new Intent(this, Gesture_settings5.class);
//                intent5.putExtra("device", device);
//                startActivity(intent5);
//                break;
        }
    }

    private float pxFromDp(float dp) {
        return dp * getApplicationContext().getResources().getDisplayMetrics().density;
    }

}
