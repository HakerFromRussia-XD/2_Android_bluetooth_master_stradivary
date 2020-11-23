package me.Romans.motorica.scan.view;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.Romans.motorica.old_electronic_by_Misha.MyApp;
import me.Romans.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity;
import me.Romans.motorica.R;
import me.Romans.motorica.scan.data.DaggerScanComponent;
import me.Romans.motorica.scan.data.ScanItem;
import me.Romans.motorica.scan.data.ScanListAdapter;
import me.Romans.motorica.scan.data.ScanModule;
import me.Romans.motorica.scan.presenter.ScanPresenter;

public class ScanActivity extends AppCompatActivity implements ScanView, ScanListAdapter.OnScanMyListener {
    RecyclerView pairedDeviceList;
    @BindView(R.id.activity_scan_list) ListView deviceList;
    @BindView(R.id.activity_scan_state) TextView state;
    @BindView(R.id.activity_scan_progress) ProgressBar progress;
    @BindView(R.id.activity_scan_button) Button scanButton;
    private boolean firstStart = true;

    @Inject
    ScanPresenter presenter;

    ScanListAdapter mScanListAdapter;
    ArrayList<ScanItem> scanList;

    @SuppressLint({"NewApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        scanList = new ArrayList<>();
        buildScanListView();

        DaggerScanComponent.builder()
                .bluetoothModule(MyApp.app().bluetoothModule())
                .scanModule(new ScanModule(this))
                .build().inject(this);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.activity_scan_button)
    public void onScan(){
        presenter.startScanning();
    }

    @Override
    public void showPairedList(List<String> items) {
        if(firstStart){
            for (int i = 0; i < items.size(); i++)
            {
                scanList.add(
                        new ScanItem(
                                R.drawable.circle_16_gray,
                                items.get(i),
                                true));
            }
            pairedDeviceList.setAdapter(mScanListAdapter);
            firstStart = false;
        } else {
            loadData();
            buildScanListView();
            pairedDeviceList.setAdapter(mScanListAdapter);
        }

    }

    @Override
    public void addDeviceToScanList(String item, BluetoothDevice device) {
        System.err.println(device+"  addDeviceToScanList");
        scanList.add(
                new ScanItem(
                        R.drawable.circle_16_blue,
                        item,
                        false));
        pairedDeviceList.setAdapter(mScanListAdapter);
    }

    @Override
    public void setScanStatus(String status, boolean enabled) {
        state.setVisibility(enabled?View.VISIBLE:View.GONE);
        state.setText(status);
    }



    @Override
    public void setScanStatus(int resId, boolean enabled) {
        state.setVisibility(enabled?View.VISIBLE:View.GONE);
        state.setText(resId);
    }

    @Override
    public void clearScanList() {
        int scanDeviceCount = 0;
        int scanListSize = scanList.size();
        //вычисление числа неспаренных устройств в списке
        for(ScanItem str: scanList){
            if(str.getTitle().split(":")[1].equals("s")){
                scanDeviceCount++;
            }
        }
        //удаление этого числа элементов из конца списка
        if (scanListSize > ((scanListSize - 1) - scanDeviceCount) + 1) {
            scanList.subList(((scanListSize - 1) - scanDeviceCount) + 1, scanListSize).clear();
        }
        pairedDeviceList.setAdapter(mScanListAdapter);
    }

    @Override
    public void clearPairedList() {
        scanList.clear();
        pairedDeviceList.setAdapter(mScanListAdapter);
    }

    @Override
    public void showProgress(boolean enabled) {
        progress.setVisibility(enabled?View.VISIBLE:View.GONE);
    }

    @Override
    public void enableScanButton(boolean enabled) {
        scanButton.setVisibility(enabled?View.VISIBLE:View.GONE);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateToChat(String extraName, BluetoothDevice extraDevice) {
        presenter.setStartFlags(extraDevice.getName());
        Intent intent = new Intent(ScanActivity.this, ChartActivity.class);
        intent.putExtra(extraName, extraDevice);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.setOnPauseActivity(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveData();
        presenter.setOnPauseActivity(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.setOnPauseActivity(true);
        presenter.onStop();
    }

    public boolean isFirstStart() {
        return firstStart;
    }

    @Override
    public void onScanClick(int position) {
        pairedDeviceList.setClickable(false);
        presenter.itemClick(position);
    }

    public void setNewStageCellScanList (int numberCell, int setImage, String setText){
        ScanItem cell = new ScanItem(
                setImage,
                setText,
                false);
        scanList.set(numberCell,cell);
        pairedDeviceList.setAdapter(mScanListAdapter);
    }

    public List<ScanItem> getMyScanList () {
        return scanList;
    }

    public void buildScanListView() {
        pairedDeviceList = findViewById(R.id.activity_scan_paired_list);
        pairedDeviceList.setHasFixedSize(true);
        pairedDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mScanListAdapter = new ScanListAdapter(this, scanList, this);
    }

    private void saveData(){
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        int scanDeviceCount = 0;
        int scanListSize = scanList.size();
        for(ScanItem str: scanList){
            if(str.getTitle().split(":")[1].equals("s")){
                scanDeviceCount++;
            }
        }
        for(int i = (scanListSize-1); i>((scanListSize-1)-scanDeviceCount); i--){
            scanList.remove(i);
        }
        mScanListAdapter = new ScanListAdapter(this, scanList, this);
        pairedDeviceList.setAdapter(mScanListAdapter);
        String json = gson.toJson(scanList);
        editor.putString("scan list", json);
        editor.apply();
    }

    @Override
    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("scan list", null);
        Type type = new TypeToken<ArrayList<ScanItem>>() {}.getType();
        scanList = gson.fromJson(json, type);
    }
}
