package me.aflak.libraries.ui.chat.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import me.aflak.libraries.ui.chat.presenter.ChatPresenter;
import me.aflak.libraries.ui.chat.view.Gesture_settings.Gesture_settings;
import me.aflak.libraries.ui.chat.view.Gesture_settings.Gesture_settings2;

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
    @BindView(R.id.valueCH1on) TextView valueCH1on;
    @BindView(R.id.valueCH1off) TextView valueCH1off;
    @BindView(R.id.valueCH1sleep) TextView valueCH1sleep;
    @BindView(R.id.valueCH2on) TextView valueCH2on;
    @BindView(R.id.valueCH2off) TextView valueCH2off;
    @BindView(R.id.valueCH2sleep) TextView valueCH2sleep;
    @BindView(R.id.activity_chat_messages) TextView messages;
    @BindView(R.id.valueCH1) TextView valueCH1;
    @BindView(R.id.valueCH2) TextView valueCH2;
    @BindView(R.id.layout_sensors) RelativeLayout layoutSensors;
    @BindView(R.id.gestures_list_relative) RelativeLayout layoutGestures;
    @BindView(R.id.activity_chat_hello_world) Button helloWorld;
    @BindView(R.id.activity_chat_hello_world2) Button helloWorld2;
    @BindView(R.id.fab) FloatingActionButton fab;
    private int intValueCH1on = 2500;
    private int intValueCH1off = 100;
    private int intValueCH1sleep = 200;
    private int intValueCH2on = 2500;
    private int intValueCH2off = 100;
    private int intValueCH2sleep = 200;
    private byte indicatorTypeMessage;
    private byte numberChannel;
    public boolean isEnable = false;
    private int i = 0;
    public byte[] TextByteTreeg = new byte[8];
//    for graph
    private SensorManager sensorManager;
    private Sensor mAccelerometer;
    private LineChart mChart;
    private boolean plotData = true;
//    private LineChart mChart2;
    private Thread thread;
    private boolean plotData2 = true;
    String TAG = "thread";
//    for bluetooth controller restart error
    private boolean pervoe_vkluchenie_bluetooth = true;

    RecyclerView recyclerView;
    GesstureAdapter gestureAdapter;
    List<Gesture_my> gestureMyList;

    @Inject ChatPresenter presenter;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    Log.i(TAG, "oncliiiiick");
                    layoutSensors.setVisibility(View.GONE);
                    fab.show();
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        gestureMyList = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.gestures_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //adding some items to our list
        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.gesture1,
                        "bla bla bla",
                        "Нажмите для редактированиея начального и конечного состояний",
                        "Жест №1",
                        2,
                        600000));

        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.gesture2,
                        "bla bla bla",
                        "Нажмите для редактированиея начального и конечного состояний",
                        "Жест №2",
                        2,
                        60000));

        gestureMyList.add(
                new Gesture_my(
                        1,
                        R.drawable.gesture3,
                        "bla bla bla",
                        "Нажмите для редактированиея начального и конечного состояний",
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

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

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

        LineData data2 = new LineData();
        data2.setValueTextColor(Color.WHITE);
        mChart.setData(data2);

        Legend legend2 = mChart.getLegend();

        legend2.setForm(Legend.LegendForm.LINE);
        legend2.setTextColor(Color.WHITE);

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

        helloWorld2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(isEnable){
                    isEnable = false;
                } else {
                    isEnable = true;
                }
                indicatorTypeMessage = 0x02;
                numberChannel = 0x02;
                TextByteTreeg[0] = indicatorTypeMessage;
                TextByteTreeg[1] = numberChannel;
                presenter.onHelloWorld(TextByteTreeg);
                addEntry(2500);
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
                        plotData = false;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isEnable) {
                                indicatorTypeMessage = 0x02;
                                numberChannel = 0x01;
                                TextByteTreeg[0] = indicatorTypeMessage;
                                TextByteTreeg[1] = numberChannel;
                                presenter.onHelloWorld(TextByteTreeg);
                            }
                        }
                    });
                    try {
                        Thread.sleep(100);
                    }catch (Exception e){}
                }
            }
        });
        thread.start();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Новый жест добавлен", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                gestureMyList.add(
                        new Gesture_my(
                                1,
                                R.drawable.gesture4,
                                "bla bla bla",
                                "Нажмите для редактированиея начального и конечного состояний",
                                "Жест №"+4,
                                2,
                                123));
//                            gestureAdapter = new GesstureAdapter(ChatActivity.this, gestureMyList, this);
                recyclerView.setAdapter(gestureAdapter);
            }
        });
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

    private LineDataSet createSet() {
        LineDataSet set2 = new LineDataSet(null, null);
        set2.setAxisDependency(YAxis.AxisDependency.LEFT);//.AxisDependency.LEFT
        set2.setLineWidth(2f);
        set2.setColor(Color.GREEN);
        set2.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set2.setCubicIntensity(0.2f);

        set2.setCircleColor(Color.GREEN);
        set2.setCircleHoleColor(Color.GREEN);
        set2.setCircleSize(1f);
        set2.setFillAlpha(65);
        set2.setFillColor(ColorTemplate.getHoloBlue());
        set2.setHighLightColor(Color.rgb(244, 117, 177));
        set2.setValueTextColor(Color.WHITE);
        set2.setValueTextSize(10f);

        set2.setHighLightColor(Color.YELLOW);
        return set2;
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
        indicatorTypeMessage = 0x02;
        numberChannel = 0x01;
        TextByteTreeg[0] = indicatorTypeMessage;
        TextByteTreeg[1] = numberChannel;
        presenter.onHelloWorld(TextByteTreeg);
        addEntry(20);
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
                addEntry(levelCH);
                break;
        }
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
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(pervoe_vkluchenie_bluetooth){
            presenter.onStart(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(pervoe_vkluchenie_bluetooth) {
//            presenter.onStop();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        if(plotData){
//            addEntry(event);
//            plotData = false;
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onGestureClick(int position) {
        switch (position){
            case 0:
                Intent intent = new Intent(this, Gesture_settings.class);
                startActivity(intent);
                break;
            case 1:
                Intent intent2 = new Intent(this, Gesture_settings2.class);
                startActivity(intent2);
                break;
//            case 2:
//                Intent intent3 = new Intent(this, Gesture_settings3.class);
//                startActivity(intent3);
//                break;
//            case 3:
//                Intent intent4 = new Intent(this, Gesture_settings4.class);
//                startActivity(intent4);
//                break;
//            case 4:
//                Intent intent5 = new Intent(this, Gesture_settings5.class);
//                startActivity(intent5);
//                break;
            default:
                Intent intent_b = new Intent(this, Gesture_settings.class);
                startActivity(intent_b);
                break;
        }
    }
}
