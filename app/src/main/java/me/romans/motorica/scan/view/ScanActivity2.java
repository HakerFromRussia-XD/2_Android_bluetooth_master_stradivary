package me.romans.motorica.scan.view;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import me.romans.motorica.R;
import me.romans.motorica.new_electronic_by_Rodeon.WDApplication;
import me.romans.motorica.new_electronic_by_Rodeon.ble.ConstantManager;
import me.romans.motorica.new_electronic_by_Rodeon.ui.activities.intro.StartActivity;
import me.romans.motorica.scan.data.DaggerScanComponent;
import me.romans.motorica.scan.data.ScanItem;
import me.romans.motorica.scan.data.ScanListAdapter;
import me.romans.motorica.scan.data.ScanModule;
import me.romans.motorica.scan.presenter.ScanPresenter;

public class ScanActivity2 extends AppCompatActivity implements ScanView, ScanListAdapter.OnScanMyListener  {
    // BT
    RecyclerView pairedDeviceList;
    ListView deviceList;
    LottieAnimationView progress;
    Button scanButton;

    @Inject
    ScanPresenter presenter;

    ScanListAdapter mScanListAdapter;
    ArrayList<ScanItem> scanList;

    //    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private int scanListBLEPosition = 0;

    private ArrayList<BluetoothDevice> mLeDevices;



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerScanComponent.builder()
                .bluetoothModule(Objects.requireNonNull(WDApplication.app()).bluetoothModule())
                .scanModule(new ScanModule(this))
                .build().inject(this);
        setContentView(R.layout.activity_scan);
        //changing statusbar
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark));

        /////////////////////////////////////////
        deviceList = findViewById(R.id.activity_scan_list);
        progress = findViewById(R.id.activity_scan_progress);
        scanButton = findViewById(R.id.activity_scan_button);
        /////////////////////////////////////////

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

        scanList = new ArrayList<>();
        buildScanListView();
        scanButton.setOnClickListener(v -> {
            scanList.clear();
            mLeDevices.clear();
            pairedDeviceList.setAdapter(mScanListAdapter);
            scanLeDevice(true);
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //TODO работаем тут
            // Stops scanning after a pre-defined scan period.
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
                    System.err.println("\n=======================================================================");
                    System.err.println("DeviceScanActivity ---------> device name:"+device.getName());
                    System.err.println("DeviceScanActivity ---------> device type:"+device.getType());
                    System.err.println("DeviceScanActivity ---------> device bluetooth class:"+device.getBluetoothClass());
                    System.err.println("DeviceScanActivity ---------> device address:"+device.getAddress());
                    System.err.println("DeviceScanActivity ---------> device bound state:"+device.getBondState());

//                    addDeviceToScanList(device.getName(), device);
                }
            });

    @Override
    public void onScanClick(int position) {
        final BluetoothDevice device = mLeDevices.get(position);
        if (device == null) return;
        final Intent intent = new Intent(this, StartActivity.class);
        intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    @Override
    public void showPairedList(List<String> items) {

    }

    @Override
    public void addDeviceToScanList(String item, BluetoothDevice device) {
        boolean canAdd = true;
        for (int i = 0; i<scanList.size(); i++) {
            if(scanList.get(i).getTitle().equals(item)){
                canAdd = false;
            }
        }
        if (canAdd) {
            mLeDevices.add(device);
            scanList.add(
                    new ScanItem(
                            R.drawable.circle_16_blue,
                            item,
                            false));
            pairedDeviceList.setAdapter(mScanListAdapter);
        }
    }

    @Override
    public void addLEDeviceToScanList(String item, BluetoothDevice device) {

    }

    @Override
    public void clearScanList() { }

    @Override
    public void clearPairedList() {

    }

    @Override
    public void setScanStatus(String status, boolean enabled) {

    }

    @Override
    public void setScanStatus(int resId, boolean enabled) {

    }

    @Override
    public void showProgress(boolean enabled) {

    }

    @Override
    public void enableScanButton(boolean enabled) {

    }

    @Override
    public void showToast(String message) {

    }

    @Override
    public void navigateToChart(String extraName, BluetoothDevice extraDevice) {

    }

    @Override
    public void navigateToLEChart(String extraName, BluetoothDevice extraDevice) {

    }

    @Override
    public void setNewStageCellScanList(int numberCell, int setImage, String setText) {

    }

    public ArrayList<ScanItem> getMyScanList() {
        return scanList;
    }

    @Override
    public void loadData() {

    }

    public void buildScanListView() {
        pairedDeviceList = findViewById(R.id.activity_scan_paired_list);
        pairedDeviceList.setHasFixedSize(true);
        pairedDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mScanListAdapter = new ScanListAdapter(this, scanList,this);
    }

    @Override
    public boolean isFirstStart() {
        return false;
    }

    @Override
    public ArrayList<BluetoothDevice> getLeDevices() {
        return null;
    }
}
