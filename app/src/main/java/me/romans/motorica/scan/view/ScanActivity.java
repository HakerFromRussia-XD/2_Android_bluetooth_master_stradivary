package me.romans.motorica.scan.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import me.romans.motorica.new_electronic_by_Rodeon.WDApplication;
import me.romans.motorica.new_electronic_by_Rodeon.ble.ConstantManager;
import me.romans.motorica.new_electronic_by_Rodeon.ui.activities.intro.DeviceScanActivity;
import me.romans.motorica.new_electronic_by_Rodeon.ui.activities.intro.StartActivity;
import me.romans.motorica.old_electronic_by_Misha.MyApp;
import me.romans.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity;
import me.romans.motorica.R;
import me.romans.motorica.scan.data.DaggerScanComponent;
import me.romans.motorica.scan.data.ScanItem;
import me.romans.motorica.scan.data.ScanListAdapter;
import me.romans.motorica.scan.data.ScanModule;
import me.romans.motorica.scan.presenter.ScanPresenter;

public class ScanActivity extends AppCompatActivity implements ScanView, ScanListAdapter.OnScanMyListener {
    /// BT
    RecyclerView pairedDeviceList;
    ListView deviceList;
    TextView state;
    LottieAnimationView progress;
    Button scanButton;
    private boolean firstStart = true;

    @Inject
    ScanPresenter presenter;

    ScanListAdapter mScanListAdapter;
    ArrayList<ScanItem> scanList;


    /// BLE
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
//    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private int scanListBLEPosition = 0;

    private ArrayList<BluetoothDevice> mLeDevices;


    @SuppressLint({"NewApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerScanComponent.builder()
                .bluetoothModule(Objects.requireNonNull(WDApplication.app()).bluetoothModule())
                .scanModule(new ScanModule(this))
                .build().inject(this);
        setContentView(R.layout.activity_scan);
        /////////////////////////////////////////
        deviceList = findViewById(R.id.activity_scan_list);
//        state = findViewById(R.id.activity_scan_state);
        progress = findViewById(R.id.activity_scan_progress);
        scanButton = findViewById(R.id.activity_scan_button);
        /////////////////////////////////////////

        scanList = new ArrayList<>();
        buildScanListView();
        scanButton.setOnClickListener(v -> {
            scanListBLEPosition = 0;
            mLeDevices.clear();
            pairedDeviceList.setAdapter(mScanListAdapter);
            scanLeDevice(true);
            presenter.startScanning();
        });

        /// BLE
        mLeDevices = new ArrayList<>();
        mHandler = new Handler();
        // Checks if Bluetooth is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE не завёлся", Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "BT не завёлся", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(() -> {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                invalidateOptionsMenu();
                progress.setVisibility(View.GONE);
                scanButton.setEnabled(true);
                scanButton.setText("SCAN AGAIN");
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            scanButton.setEnabled(false);
            scanButton.setText("SCANNING");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            scanButton.setEnabled(true);
            scanButton.setText("SCAN AGAIN");
        }
        progress.setVisibility(enable?View.VISIBLE:View.GONE);
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            (device, rssi, scanRecord) -> runOnUiThread(() -> {
                if(device.getName() != null){
//                    System.err.println("\n=======================================================================");
                    System.err.println("DeviceScanActivity ---------> device name:"+device.getName());
//                    System.err.println("DeviceScanActivity ---------> device type:"+device.getType());
//                    System.err.println("DeviceScanActivity ---------> device bluetooth class:"+device.getBluetoothClass());
//                    System.err.println("DeviceScanActivity ---------> device address:"+device.getAddress());
//                    System.err.println("DeviceScanActivity ---------> device bound state:"+device.getBondState());

                    addLEDeviceToScanList(device.getName()+":l:", device);
                }
            });

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
    public void addLEDeviceToScanList(String item, BluetoothDevice device) {
        boolean canAdd = true;
        for (int i = 0; i<scanList.size(); i++) {
            if(scanList.get(i).getTitle().split(":")[0].equals(item.split(":")[0])){
                canAdd = false;
            }
        }
        if (canAdd) {
            mLeDevices.add(device);
            scanList.add(
                    new ScanItem(
                            R.drawable.circle_16_blue,
                            item+scanListBLEPosition,
                            false));
            pairedDeviceList.setAdapter(mScanListAdapter);
            scanListBLEPosition++;
        }
    }

    @Override
    public void addDeviceToScanList(String item, BluetoothDevice device) {
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
//        state.setVisibility(enabled?View.VISIBLE:View.GONE);
//        state.setText(resId);
    }

    @Override
    public void clearScanList() {
        int scanDeviceCount = 0;
        int scanListSize = scanList.size();
        //вычисление числа неспаренных устройств в списке
        for(ScanItem str: scanList){
            if(str.getTitle().split(":")[1].equals("s") || str.getTitle().split(":")[1].equals("l")){
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
    public void navigateToLEChat(String extraName, BluetoothDevice extraDevice) {
        if (extraDevice == null) return;
        Intent intent = new Intent(ScanActivity.this, StartActivity.class);
        intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, extraDevice.getName());
        intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, extraDevice.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
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
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveData();
        presenter.setOnPauseActivity(true);
        scanLeDevice(false);
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
        //TODO дописать обработку нажатия на BLE устройства
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


    public ArrayList<BluetoothDevice> getLeDevices() {
        return mLeDevices;
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
