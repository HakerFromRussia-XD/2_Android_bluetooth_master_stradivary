package me.start.motorica.scan.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import me.start.motorica.new_electronic_by_Rodeon.WDApplication;
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager;
import me.start.motorica.new_electronic_by_Rodeon.presenters.Load3DModelNew;
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.intro.StartActivity;
import me.start.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity;
import me.start.motorica.R;
import me.start.motorica.old_electronic_by_Misha.ui.chat.view.Load3DModel;
import me.start.motorica.scan.data.DaggerScanComponent;
import me.start.motorica.scan.data.ScanItem;
import me.start.motorica.scan.data.ScanListAdapter;
import me.start.motorica.scan.data.ScanModule;
import me.start.motorica.scan.presenter.ScanPresenter;

import static me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager.MAX_NUMBER_DETAILS;


@SuppressWarnings("ALL")
public class ScanActivity extends AppCompatActivity implements ScanView, ScanListAdapter.OnScanMyListener {
    /// BT
    RecyclerView pairedDeviceList;
    ListView deviceList;
    TextView state;
    LottieAnimationView progress;
    Button scanButton;
    private boolean firstStart = true;
    // 3D
    Load3DModel mLoad3DModel = new Load3DModel(this);
    Load3DModelNew mLoad3DModelNew = new Load3DModelNew(this);
    public Thread[] threadFunction = new Thread[MAX_NUMBER_DETAILS+MAX_NUMBER_DETAILS];

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


    @SuppressLint({"NewApi", "ClickableViewAccessibility", "ObsoleteSdkInt"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerScanComponent.builder()
                .bluetoothModule(Objects.requireNonNull(WDApplication.app()).bluetoothModule())
                .scanModule(new ScanModule(this))
                .build().inject(this);
        setContentView(R.layout.activity_scan);
        //changing statusbar
        if (android.os.Build.VERSION.SDK_INT >= 21){
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark));
        }
        /////////////////////////////////////////
        deviceList = findViewById(R.id.activity_scan_list);
        progress = findViewById(R.id.activity_scan_progress);
        scanButton = findViewById(R.id.activity_scan_button);
        /////////////////////////////////////////

        scanList = new ArrayList<>();
        buildScanListView();
        scanButton.setOnClickListener(v -> {
            scanListBLEPosition = 0;
            mLeDevices.clear();
            pairedDeviceList.setAdapter(mScanListAdapter);
            if ((ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
                scanLeDevice(true);
                presenter.startScanning();
            }
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
        //getBluetoothLeAdvertiser()
//        mBluetoothAdapter = bluetoothManager.getAdapter().getBluetoothLeAdvertiser().startAdvertising();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "bluetooth нет на телефоне", Toast.LENGTH_SHORT).show();
            if(!mBluetoothAdapter.isMultipleAdvertisementSupported()){
                Toast.makeText(this, "bluetooth low energy нет на телефоне", Toast.LENGTH_SHORT).show();
            }
            finish();
        }

        checkLocationPermission();
        init3D();
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(() -> {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                invalidateOptionsMenu();
                progress.setVisibility(View.GONE);
                scanButton.setEnabled(true);
                scanButton.setText(R.string.scan_again);
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
//            mBluetoothAdapter.getBluetoothLeAdvertiser().startAdvertising(settingsBuilder.build(), dataBuilder.build(), mLeAdvertisingCallback);
            scanButton.setEnabled(false);
            scanButton.setText(R.string.bluetooth_scanning);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            scanButton.setEnabled(true);
            scanButton.setText(R.string.scan_again);
        }
        progress.setVisibility(enable?View.VISIBLE:View.GONE);
        invalidateOptionsMenu();
    }

    // Device scan callback.
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.M)
    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            (device, rssi, scanRecord) -> runOnUiThread(() -> {
                if(device.getName() != null){
                    System.err.println("===========================================");
//                    System.err.println("DeviceScanActivity ---------> device name: "+device.getName()+ "   Uuids: "+device.getUuids());
//                    System.err.println("DeviceScanActivity ---------> device rssi: "+rssi);
//                    System.err.println("DeviceScanActivity ---------> device: "+device.toString()+" | "+device.getAddress()+" | "+device.getUuids()+" | "+device.getName()
//                            +" | "+device.getType()+" | "+device.getClass().getCanonicalName()+" | "+device.getName()+" | "+device.getClass().getSimpleName()+" | "
//                            +device.getClass().getTypeName()+" | "+device.getClass().getAnnotations().length);
                    System.err.println("DeviceScanActivity ---------> device: "+ device.getName() + " | " + device.getAddress() +" | "+rssi);
                    System.err.println("===========================================");
                    addLEDeviceToScanList(device.getName()+":l:", device, rssi);
                }
            });
    private final BluetoothAdapter.LeScanCallback mLeAdvertisingCallback =
            (device, rssi, scanRecord) -> runOnUiThread(() -> { });
    @Override
    public void showPairedList(List<String> items) {
        if(firstStart){
            for (int i = 0; i < items.size(); i++)
            {
                scanList.add(
                        new ScanItem(
                                R.drawable.circle_16_gray,
                                items.get(i),
                                "00",
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
    public void addLEDeviceToScanList(String item, BluetoothDevice device, int rssi) {
        boolean canAdd = true;
        for (int i = 0; i<scanList.size(); i++) {
//            if(scanList.get(i).getTitle().split(":")[0].equals(item.split(":")[0])){
//                canAdd = false;
//            }
            if(scanList.get(i).getAddress().equals(device.getAddress())){
                canAdd = false;
            }
        }
        System.err.println("DeviceScanActivity addLEDeviceToScanList ---------> device address: " + device.getAddress());
        //TODO здесь мы принимаем решение добавлять ли новое устройство в список отсканированных
        if (canAdd) {
            if(checkOurLEName(item)){
                mLeDevices.add(device);
                scanList.add(
                        new ScanItem(
                                R.drawable.circle_16_blue,
                                item+scanListBLEPosition+":   "+rssi,
                                device.getAddress(),
                                false));
                pairedDeviceList.setAdapter(mScanListAdapter);
                scanListBLEPosition++;
            }
        }
    }

    @Override
    public void addDeviceToScanList(String item, String address, BluetoothDevice device) {
        scanList.add(
                new ScanItem(
                        R.drawable.circle_16_blue,
                        item,
                        address,
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
        scanButton.setText(resId);
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
    public void navigateToChart(String extraName, BluetoothDevice extraDevice) {
        for (int j = 0; j<MAX_NUMBER_DETAILS; j++) {
            final int finalJ = j;
            threadFunction[j] = new Thread(() -> mLoad3DModel.loadSTR2(finalJ));
            threadFunction[j].start();
        }
        presenter.setStartFlags(extraDevice.getName());
        Intent intent = new Intent(ScanActivity.this, ChartActivity.class);
        intent.putExtra(extraName, extraDevice);
        startActivity(intent);
        finish();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void navigateToLEChart(String extraName, BluetoothDevice extraDevice) {
        for (int k = 0; k<MAX_NUMBER_DETAILS; k++) {
            final int finalK = k;
            System.err.println("Запуск загрузки: " + finalK);
            threadFunction[k] = new Thread(() -> mLoad3DModelNew.loadSTR2(finalK));
            threadFunction[k].start();
        }

        if (extraDevice == null) return;
        Intent intent = new Intent(ScanActivity.this, StartActivity.class);
        intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, extraDevice.getName());
        intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, extraDevice.getAddress());
        intent.putExtra(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A, extraDevice.getName());
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
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
            presenter.onStart(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.setOnPauseActivity(false);
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        scanListBLEPosition = 0;
        mLeDevices.clear();
        pairedDeviceList.setAdapter(mScanListAdapter);
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
            scanLeDevice(true);
            presenter.startScanning();
        }
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
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
            presenter.onStop();
        }
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
                "0",
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

    private boolean checkOurLEName (@NotNull String deviceName){
//        return deviceName.split(":")[0].equals("HRSTM") ||
//                deviceName.split(":")[0].equals("BLE_test_service—•——");
        return true;
    }

    //  Инициализация зарузки 3D объектов
    private void init3D(){
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

        Load3DModelNew.model[0]  = mLoad3DModelNew.readData(ConstantManager.MODEDEL_0_NEW);
        Load3DModelNew.model[1]  = mLoad3DModelNew.readData(ConstantManager.MODEDEL_1_NEW);
        Load3DModelNew.model[2]  = mLoad3DModelNew.readData(ConstantManager.MODEDEL_2_NEW);
        Load3DModelNew.model[3]  = mLoad3DModelNew.readData(ConstantManager.MODEDEL_3_NEW);
        Load3DModelNew.model[4]  = mLoad3DModelNew.readData(ConstantManager.MODEDEL_4_NEW);
        Load3DModelNew.model[5]  = mLoad3DModelNew.readData(ConstantManager.MODEDEL_5_NEW);
        Load3DModelNew.model[6]  = mLoad3DModelNew.readData(ConstantManager.MODEDEL_6_NEW);
        Load3DModelNew.model[7]  = mLoad3DModelNew.readData(ConstantManager.MODEDEL_7_NEW);
        Load3DModelNew.model[8]  = mLoad3DModelNew.readData(ConstantManager.MODEDEL_8_NEW);
        Load3DModelNew.model[9]  = mLoad3DModelNew.readData(ConstantManager.MODEDEL_9_NEW);
        Load3DModelNew.model[10] = mLoad3DModelNew.readData(ConstantManager.MODEDEL_10_NEW);
        Load3DModelNew.model[11] = mLoad3DModelNew.readData(ConstantManager.MODEDEL_11_NEW);
        Load3DModelNew.model[12] = mLoad3DModelNew.readData(ConstantManager.MODEDEL_12_NEW);
        Load3DModelNew.model[13] = mLoad3DModelNew.readData(ConstantManager.MODEDEL_13_NEW);
        Load3DModelNew.model[14] = mLoad3DModelNew.readData(ConstantManager.MODEDEL_14_NEW);
        Load3DModelNew.model[15] = mLoad3DModelNew.readData(ConstantManager.MODEDEL_15_NEW);
        Load3DModelNew.model[16] = mLoad3DModelNew.readData(ConstantManager.MODEDEL_16_NEW);
        Load3DModelNew.model[17] = mLoad3DModelNew.readData(ConstantManager.MODEDEL_17_NEW);
        Load3DModelNew.model[18] = mLoad3DModelNew.readData(ConstantManager.MODEDEL_18_NEW);
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
        if (scanListSize > ((scanListSize - 1) - scanDeviceCount) + 1) {
            scanList.subList(((scanListSize - 1) - scanDeviceCount) + 1, scanListSize).clear();
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


    private void checkLocationPermission() {
        //проверка включена ли геолокация и если выключена, то показ предложения её включить
        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ignored) {}
        if(!gps_enabled) {
            // notify user
            new AlertDialog.Builder(this)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.open_location_settings, (paramDialogInterface, paramInt) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setCancelable(false)
                    .show();
        }
    }
}
