package me.aflak.libraries.ui.chat.view;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.aflak.libraries.MyApp;
import me.aflak.libraries.R;
import me.aflak.libraries.data.GesstureAdapter;
import me.aflak.libraries.data.Gesture_my;
import me.aflak.libraries.ui.chat.data.ChatModule;
import me.aflak.libraries.ui.chat.data.DaggerChatComponent;
import me.aflak.libraries.ui.chat.interactor.ChatInteractor;
import me.aflak.libraries.ui.chat.presenter.ChatPresenter;
import me.aflak.libraries.ui.chat.view.Gesture_settings.Gesture_settings;
import me.aflak.libraries.ui.chat.view.Gesture_settings.Gesture_settings2;
import me.aflak.libraries.ui.chat.view.Gesture_settings.Gesture_settings3;
import me.aflak.libraries.ui.chat.view.Gripper_settings.GripperSettings;

/**
 * Created by Omar on 20/12/2017.
 */

public class ChatActivity extends AppCompatActivity implements ChatView, SensorEventListener, GesstureAdapter.OnGestureMyListener {
    @BindView(R.id.activity_chat_status) TextView state;
    @BindView(R.id.seekBarCH1on) SeekBar seekBarCH1on;
    @BindView(R.id.seekBarCH1off) SeekBar seekBarCH1off;
    @BindView(R.id.seekBarCH1sleep) SeekBar seekBarCH1sleep;
    @BindView(R.id.seekBarCH2on) SeekBar seekBarCH2on;
    @BindView(R.id.seekBarCH2off) SeekBar seekBarCH2off;
    @BindView(R.id.seekBarCH2sleep) SeekBar seekBarCH2sleep;
    @BindView(R.id.seekBarIstop) SeekBar seekBarIstop;
    @BindView(R.id.valueCH1on) TextView valueCH1on;
    @BindView(R.id.valueCH1off) TextView valueCH1off;
    @BindView(R.id.valueCH1sleep) TextView valueCH1sleep;
    @BindView(R.id.valueCH2on) TextView valueCH2on;
    @BindView(R.id.valueCH2off) TextView valueCH2off;
    @BindView(R.id.valueCH2sleep) TextView valueCH2sleep;
    @BindView(R.id.valueIstop) TextView valueIstop;
    @BindView(R.id.activity_chat_messages) TextView messages;
    @BindView(R.id.valueCH1) TextView valueCH1;
    @BindView(R.id.valueCH2) TextView valueCH2;
    @BindView(R.id.layout_sensors) RelativeLayout layoutSensors;
//    @BindView(R.id.gestures_list_relative) RelativeLayout layoutGestures;
    @BindView(R.id.activity_chat_hello_world) Button helloWorld;
    @BindView(R.id.activity_chat_hello_world2) Button helloWorld2;
//    @BindView(R.id.fab) FloatingActionButton fab;
    private int intValueCH1on = 2500;
    private int intValueCH1off = 100;
    private int intValueCH1sleep = 200;
    private int intValueCH2on = 2500;
    private int intValueCH2off = 100;
    private int intValueCH2sleep = 200;
    private byte indicatorTypeMessage;
    private byte numberChannel;
    public boolean isEnable = false;
    public boolean infinitAction = false;
    public boolean stateIsOpen = false;
    public boolean errorReception = false;
    private int i = 0;
    public byte[] TextByteTreeg = new byte[8];
    public byte[] TextByteTreegCurentSettingsAndInvert = new byte[4];
    public byte[] TextByteTreegMod = new byte[2];
    public byte[] TextByteSensorActivate = new byte[2];
//    for graph
    private SensorManager sensorManager;
    private Sensor mAccelerometer;
    private LineChart mChart;
    private boolean plotData = true;
    private LineChart mChart2;
    private Thread thread;
    private boolean plotData2 = true;
    String TAG = "thread";
//    for bluetooth controller restart error
    private boolean pervoe_vkluchenie_bluetooth = true;

    RecyclerView recyclerView;
    GesstureAdapter gestureAdapter;
    List<Gesture_my> gestureMyList;

    @Inject ChatPresenter presenter;

//    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
//            = new BottomNavigationView.OnNavigationItemSelectedListener() {
//
//        @Override
//        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//            switch (item.getItemId()) {
//                case R.id.navigation_home:
//                    Log.i(TAG, "oncliiiiick");
//                    layoutSensors.setVisibility(View.GONE);
//                    fab.show();
//                    layoutGestures.setVisibility(View.VISIBLE);
//                    return true;
//                case R.id.navigation_dashboard:
//                    Log.i(TAG, ":))");
//                    layoutSensors.setVisibility(View.VISIBLE);
//                    fab.hide();
//                    layoutGestures.setVisibility(View.GONE);
//                    return true;
//            }
//            return false;
//        }
//    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

//        gestureMyList = new ArrayList<>();
//        recyclerView = (RecyclerView) findViewById(R.id.gestures_list);
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //adding some items to our list
//        gestureMyList.add(
//                new Gesture_my(
//                        1,
//                        R.drawable.gesture1,
//                        "bla bla bla",
//                        "Нажмите для редактирования начального и конечного состояний",
//                        "Жест №1",
//                        2,
//                        600000));
//
//        gestureMyList.add(
//                new Gesture_my(
//                        1,
//                        R.drawable.gesture2,
//                        "bla bla bla",
//                        "Нажмите для редактирования начального и конечного состояний",
//                        "Жест №2",
//                        2,
//                        60000));
//
//        gestureMyList.add(
//                new Gesture_my(
//                        1,
//                        R.drawable.gesture3,
//                        "bla bla bla",
//                        "Нажмите для редактирования начального и конечного состояний",
//                        "Жест №3",
//                        2,
//                        60000));
//
//        gestureAdapter = new GesstureAdapter(this, gestureMyList, this);
//        recyclerView.setAdapter(gestureAdapter);

        DaggerChatComponent.builder()
            .bluetoothModule(MyApp.app().bluetoothModule())
            .chatModule(new ChatModule(this))
            .build().inject(this);
        ButterKnife.bind(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

//        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
//        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if(mAccelerometer != null){
            sensorManager.registerListener(ChatActivity.this,mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }

////////initialized graph for channel 1
        mChart = (LineChart) findViewById(R.id.chartCH1);

        mChart.getDescription().setEnabled(true);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(false);
        mChart.setDragXEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(false);
        mChart.setBackgroundColor(Color.BLACK);
        mChart.getHighlightByTouchPoint(1, 1);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);

        Legend legend = mChart.getLegend();

        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextColor(Color.WHITE);

        XAxis x1 = mChart.getXAxis();
        x1.setTextColor(Color.WHITE);
        x1.setDrawGridLines(false);
        x1.setAxisMaximum(4000000f);//x1.resetAxisMaximum();


        x1.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart.getAxisLeft();
        y1.setTextColor(Color.WHITE);
        y1.setAxisMaximum(2700f);
        y1.setAxisMinimum(-100f);
        y1.setDrawGridLines(true);

        YAxis y12 = mChart.getAxisRight();
        y12.setEnabled(false);

////////initialized graph for channel 2
        mChart2 = (LineChart) findViewById(R.id.chartCH2);

        mChart2.getDescription().setEnabled(true);
        mChart2.setTouchEnabled(true);
        mChart2.setDragEnabled(false);
        mChart2.setDragXEnabled(false);
        mChart2.setScaleEnabled(false);
        mChart2.setDrawGridBackground(false);
        mChart2.setPinchZoom(false);
        mChart2.setBackgroundColor(Color.BLACK);
        mChart2.getHighlightByTouchPoint(1, 1);

        LineData data2 = new LineData();
        data2.setValueTextColor(Color.WHITE);
        mChart2.setData(data2);

        Legend legend2 = mChart2.getLegend();

        legend2.setForm(Legend.LegendForm.LINE);
        legend2.setTextColor(Color.WHITE);

        XAxis x12 = mChart2.getXAxis();
        x12.setTextColor(Color.WHITE);
        x12.setDrawGridLines(false);
        x12.setAxisMaximum(4000000f);//x1.resetAxisMaximum();


        x12.setAvoidFirstLastClipping(true);

        YAxis y1_2 = mChart2.getAxisLeft();
        y1_2.setTextColor(Color.WHITE);
        y1_2.setAxisMaximum(2700f);
        y1_2.setAxisMinimum(-100f);
        y1_2.setDrawGridLines(true);

        YAxis y122 = mChart2.getAxisRight();
        y122.setEnabled(false);

//        startPlot();

        TextByteTreeg[2] = (byte) intValueCH1on;
        TextByteTreeg[3] = (byte) (intValueCH1on >> 8);
        TextByteTreeg[4] = (byte) intValueCH1off;
        TextByteTreeg[5] = (byte) (intValueCH1off >> 8);
        TextByteTreeg[6] = (byte) intValueCH1sleep;
        TextByteTreeg[7] = (byte) (intValueCH1sleep >> 8);

        presenter.onCreate(getIntent());
        seekBarCH1on.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueCH1on.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueCH1on.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH1on.setText(String.valueOf(seekBar.getProgress()));
                intValueCH1on = seekBarCH1on.getProgress();
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
            }
        });

        seekBarCH1off.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueCH1off.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueCH1off.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH1off.setText(String.valueOf(seekBar.getProgress()));
                intValueCH1off = seekBarCH1off.getProgress();
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
            }
        });

        seekBarCH1sleep.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueCH1sleep.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueCH1sleep.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH1sleep.setText(String.valueOf(seekBar.getProgress()));
                intValueCH1sleep = seekBarCH1sleep.getProgress();
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
            }
        });

        seekBarCH2on.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH2on.setText(String.valueOf(seekBar.getProgress()));
                intValueCH2on = seekBarCH2on.getProgress();
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
            }
        });

        seekBarCH2off.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueCH2off.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueCH2off.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH2off.setText(String.valueOf(seekBar.getProgress()));
                intValueCH2off = seekBarCH2off.getProgress();
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
            }
        });

        seekBarCH2sleep.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueCH2sleep.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valueCH2sleep.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                valueCH2sleep.setText(String.valueOf(seekBar.getProgress()));
                intValueCH2sleep = seekBarCH2sleep.getProgress();
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
                int Curent = seekBar.getProgress();
                byte Invert = 0x00;
                presenter.onHelloWorld(CompileMassegeCurentSettingsAndInvert(Curent, Invert));
            }
        });

        helloWorld2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
//                if(isEnable){
//                    isEnable = false;
//                } else {
//                    isEnable = true;
//                }
                int numberSensor = 0x07;
                presenter.onHelloWorld(CompileMassegeSensorActivate(numberSensor));
                addEntry(2500);
                addEntry2(2500);

            }
        });

        if(thread != null){
            thread.interrupt();
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if(plotData){
                        addEntry(2);
                        addEntry2(2);
                        plotData = false;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isEnable && !errorReception) {
                                indicatorTypeMessage = 0x02;
                                numberChannel = 0x01;
                                TextByteTreeg[0] = indicatorTypeMessage;
                                TextByteTreeg[1] = numberChannel;
                                presenter.onHelloWorld(TextByteTreeg);
                            }
                        }
                    });
                    try {
                        Thread.sleep(500);
                    }catch (Exception e){}
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isEnable && !errorReception) {
                                indicatorTypeMessage = 0x02;
                                numberChannel = 0x02;
                                TextByteTreeg[0] = indicatorTypeMessage;
                                TextByteTreeg[1] = numberChannel;
                                presenter.onHelloWorld(TextByteTreeg);
                            }
                        }
                    });
                    try {
                        Thread.sleep(500);
                    }catch (Exception e){}
                    if (isEnable && errorReception) {
                        errorReception = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {}
                        });
                        try {
                            Thread.sleep(500);
                        }catch (Exception e){}
                    }
                }
            }
        });
        thread.start();

//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Новый жест добавлен", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
//                gestureMyList.add(
//                        new Gesture_my(
//                                1,
//                                R.drawable.gesture4,
//                                "bla bla bla",
//                                "Нажмите для редактирования начального и конечного состояний",
//                                "Жест №"+4,
//                                2,
//                                123));
//                recyclerView.setAdapter(gestureAdapter);
//            }
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // получим идентификатор выбранного пункта меню
        int id = item.getItemId();
        // Операции для выбранного пункта меню
        switch (id) {
            case R.id.action_Trigger1:
                presenter.onHelloWorld(CompileMassegeTreegMod (1));
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
            case R.id.action_Trigger4:
                presenter.onHelloWorld(CompileMassegeTreegMod (4));
                infinitAction = false;
                return true;
            case R.id.action_Trigger5:
                presenter.onHelloWorld(CompileMassegeTreegMod (5));
                infinitAction = false;
                return true;
            case R.id.action_Trigger6:
                presenter.onHelloWorld(CompileMassegeTreegMod (6));
                infinitAction = false;
                return true;
            case R.id.action_Trigger7:
                presenter.onHelloWorld(CompileMassegeTreegMod (7));
                infinitAction = false;
                return true;
            case R.id.action_Trigger8:
                presenter.onHelloWorld(CompileMassegeTreegMod (8));
                infinitAction = false;
                return true;
            case R.id.action_Trigger9:
                final BluetoothDevice device = getIntent().getExtras().getParcelable("device");
                Intent intent = new Intent(this, InfinitySettings.class);
                intent.putExtra("device", device);
                startActivity(intent);
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

            mChart.setVisibleXRange(0, 50);
            mChart.setMaxVisibleValueCount(0);
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

            mChart2.setVisibleXRange(0, 50);
            mChart2.setMaxVisibleValueCount(0);
            mChart2.moveViewToX(set.getEntryCount()-50);//data.getEntryCount()

        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, null);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);//.AxisDependency.LEFT
        set.setLineWidth(2f);
        set.setColor(Color.GREEN);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);

        set.setCircleColor(Color.GREEN);
        set.setCircleHoleColor(Color.GREEN);
        set.setCircleSize(1f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 177));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);

        set.setHighLightColor(Color.YELLOW);
        return set;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pervoe_vkluchenie_bluetooth = false;
        try {
            thread.interrupt();
        } catch (Exception e){}
    }

    @OnClick(R.id.activity_chat_hello_world)
    public void onHelloWorld(){
        int numberSensor = 0x06;
        presenter.onHelloWorld(CompileMassegeSensorActivate(numberSensor));
        addEntry(20);
        addEntry2(20);
    }

    @Override
    public void setStatus(String status) {
        state.setText(status);
    }

    @Override
    public void setStatus(int resId) {
        state.setText(resId);
    }

    @Override
    public void setValueCH(int levelCH, int numberChannel) {
        String strlevelCH = new String(String.valueOf(levelCH));
        Integer numberOfChannel = new Integer(numberChannel);
        switch (numberOfChannel){
            case 1:
                valueCH1.setText(strlevelCH);
                addEntry(levelCH);
                break;
            case 2:
                valueCH2.setText(strlevelCH);
                addEntry2(levelCH);
                break;
        }
    }

    @Override
    public void setErrorReception (boolean incomeErrorReception) {
        errorReception = incomeErrorReception;
    }

//    @Override
//    public void setValueCH2(int levelCH2) {
//        String str = new String(String.valueOf(levelCH2));
//        valueCH2.setText(str);
//    }

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
        seekBarCH1on.setEnabled(enabled);
        seekBarCH1off.setEnabled(enabled);
        seekBarCH1sleep.setEnabled(enabled);
        seekBarCH2on.setEnabled(enabled);
        seekBarCH2off.setEnabled(enabled);
        seekBarCH2sleep.setEnabled(enabled);
        seekBarIstop.setEnabled(enabled);
    }

    public void GetPosition_My (int position, BluetoothDevice device){
        System.out.println("из ScanAct-------------> Передача position:" + position);
        System.out.println("из ScanAct-------------> выжимка из интента devise:" + device);
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
        presenter.disconnect();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        if(plotData){
//            addEntry(event);
//            plotData = false;
//        }
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onGestureClick(int position) {
        final BluetoothDevice device = getIntent().getExtras().getParcelable("device");
        switch (position){
            case 0:
                presenter.disconnect();
                Intent intent = new Intent(this, InfinitySettings.class);
                intent.putExtra("device", device);
                startActivity(intent);
                break;
            case 1:
                presenter.disconnect();
                Intent intent2 = new Intent(this, Gesture_settings2.class);
                intent2.putExtra("device", device);
                startActivity(intent2);
                break;
            case 2:
                presenter.disconnect();
                Intent intent3 = new Intent(this, Gesture_settings3.class);
                intent3.putExtra("device", device);
                startActivity(intent3);
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
            default:
                presenter.disconnect();
                Intent intent_b = new Intent(this, Gesture_settings.class);
                intent_b.putExtra("device", device);
                startActivity(intent_b);
                break;
        }
    }
}
