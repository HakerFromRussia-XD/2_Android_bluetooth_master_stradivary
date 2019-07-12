package me.aflak.motorica.ui.chat.view.experimental;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import javax.inject.Inject;

import butterknife.ButterKnife;
import me.aflak.motorica.MyApp;
import me.aflak.motorica.R;
import me.aflak.motorica.ui.chat.data.ChatModule;
import me.aflak.motorica.ui.chat.data.DaggerChatComponent;
import me.aflak.motorica.ui.chat.presenter.ChatPresenter;
import me.aflak.motorica.ui.chat.view.ChatView;

public class DualChart extends AppCompatActivity implements ChatView, SensorEventListener {

//    @BindView(R.id.activity_chat_status) TextView state;
    public boolean isEnable = false;
//    for graph
    private SensorManager sensorManager;
    private Sensor mAccelerometer;
    private LineChart mChart;
    private boolean plotData = true;
    public boolean errorReception = false;
    private Thread thread;
    public float iterator = 0;
    public int dataSetIndex = 0;
//    public ILineDataSet set;
//    for massage
    private int intValueCH1on = 2500;
    private int intValueCH1off = 100;
    private int intValueCH1sleep = 200;
    private int intValueCH2on = 2500;
    private int intValueCH2off = 100;
    private int intValueCH2sleep = 200;
    private byte indicatorTypeMessage = 0x01;
    private byte numberChannel;
    public byte[] TextByteTreeg = new byte[8];
//    for general updates
    public int receiveСurrentChat = 0;
    public int receiveLevelCH1Chat = 0;
    public int receiveLevelCH2Chat = 0;
    public byte receiveIndicationStateChat = 0;

    @Inject
    ChatPresenter presenter;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activiti_experimental);

        getSupportActionBar().hide();

        if (android.os.Build.VERSION.SDK_INT >= 21){
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimary));
        }

        DaggerChatComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .chatModule(new ChatModule(DualChart.this))
                .build().inject(DualChart.this);
        ButterKnife.bind(DualChart.this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

//        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
//        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if(mAccelerometer != null){
            sensorManager.registerListener(DualChart.this,mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }

////////initialized graph for channel 1
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
        mChart.getHighlightByTouchPoint(10, 10);

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


        TextByteTreeg[2] = (byte) intValueCH1on;
        TextByteTreeg[3] = (byte) (intValueCH1on >> 8);
        TextByteTreeg[4] = (byte) intValueCH1off;
        TextByteTreeg[5] = (byte) (intValueCH1off >> 8);
        TextByteTreeg[6] = (byte) intValueCH1sleep;
        TextByteTreeg[7] = (byte) (intValueCH1sleep >> 8);

        presenter.onCreate(getIntent());

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
                            addEntry((int) iterator);
                            addEntry((int) (2500 - iterator));
                            addEntry((int) iterator);
                            addEntry((int) iterator);
                        }
                    });
                    try {
                        Thread.sleep(33);
                    }catch (Exception e){}
//                    if (isEnable && errorReception) { //обработчик пришедшей ошибки
//                        errorReception = false;
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {}
//                        });
//                        try {
//                            Thread.sleep(500000);
//                        }catch (Exception e){}
//                    }
                    iterator+=20;
                    if ( iterator == 2500) {iterator = 0;}
                }
            }
        });
        thread.start();
    }

    private void addEntry(int event){

        LineData data = mChart.getData();

        if(data == null){
            data = new LineData();
            mChart.setData(data);
        }

        ILineDataSet set = data.getDataSetByIndex(dataSetIndex);

        if((set == null)&&(dataSetIndex == 0)){
            set = createSet();
            data.addDataSet(set);
        }
        if((set == null)&&(dataSetIndex == 1)){
            set = createSet2();
            data.addDataSet(set);
        }

        data.addEntry(new Entry(set.getEntryCount(), event), dataSetIndex);
        data.notifyDataChanged();

        mChart.notifyDataSetChanged();

        mChart.setVisibleXRangeMaximum(1000);

        mChart.moveViewTo(data.getEntryCount()/2 - 1000, 50f, YAxis.AxisDependency.LEFT);

        if(dataSetIndex == 0){
            dataSetIndex = 1;
        } else {
            dataSetIndex = 0;
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
        set.setValueTextColor(Color.BLACK);
        set.setValueTextSize(0f);

        set.setHighLightColor(Color.YELLOW);
        return set;
    }

    private LineDataSet createSet2() {
        LineDataSet set = new LineDataSet(null, null);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);//.AxisDependency.LEFT
        set.setLineWidth(2f);
        set.setColor(Color.BLUE);
        set.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

        set.setCircleColor(Color.BLUE);
        set.setCircleHoleColor(Color.BLUE);
        set.setCircleSize(1f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 177));
        set.setValueTextColor(Color.BLACK);
        set.setValueTextSize(0f);

        set.setHighLightColor(Color.YELLOW);
        return set;
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
        errorReception = incomeErrorReception;
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

    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {

    }

    @Override
    public void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication) {

    }
}
