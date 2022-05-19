package me.start.motorica.new_electronic_by_Rodeon.ui.activities.intro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.start.motorica.R;
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager;

import java.util.ArrayList;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppCompatActivity implements ScanView, ScanListAdapter.OnScanMyListener {
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    RecyclerView pairedDeviceList;
    ScanListAdapter mScanListAdapter;
    ArrayList<ScanItem> scanList;
    private ArrayList<BluetoothDevice> mLeDevices;
    ProgressBar progress;
    Button scanButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.err.println(" LOLOLOEFWEF --->  DeviceScanActivity onCreate");
        setContentView(R.layout.activity_scan);
        scanList = new ArrayList<>();
        buildScanListView();
        mLeDevices = new ArrayList<>();
        progress = findViewById(R.id.activity_scan_progress);
        scanButton = findViewById(R.id.activity_scan_button);

        mHandler = new Handler();
        // Checks if Bluetooth is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ошибка 1", Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        checkLocationPermission();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "ошибка 2", Toast.LENGTH_SHORT).show();
            finish();
        }

        scanButton.setOnClickListener(v -> {
            scanList.clear();
            mLeDevices.clear();
            pairedDeviceList.setAdapter(mScanListAdapter);
            scanLeDevice(true);
        });
    }


    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("ошибка 3")
                        .setMessage("ошибка 4")
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> ActivityCompat.requestPermissions(DeviceScanActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                MY_PERMISSIONS_REQUEST_LOCATION))
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }
//    private String[] getRequiredPermissions() {
//        System.err.println(" LOLOLOEFWEF ---> getRequiredPermissions()");
//        int targetSdkVersion = getApplicationInfo().targetSdkVersion;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetSdkVersion >= Build.VERSION_CODES.S) {
//            return new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
//            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
//        } else return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);//Request location updates:
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        scanLeDevice(false);
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


    @SuppressLint("SetTextI18n")
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
                    System.err.println("\n=======================================================================");
                    System.err.println("DeviceScanActivity ---------> device name:"+device.getName());
                    System.err.println("DeviceScanActivity ---------> device type:"+device.getType());
                    System.err.println("DeviceScanActivity ---------> device bluetooth class:"+device.getBluetoothClass());
                    System.err.println("DeviceScanActivity ---------> device address:"+device.getAddress());
                    System.err.println("DeviceScanActivity ---------> device bound state:"+device.getBondState());

                    addDeviceToScanList(device.getName(), device);
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
    public void addDeviceToScanList(String item, BluetoothDevice device) {
        boolean canAdd = true;
        for (int i = 0; i<scanList.size(); i++) {
            if(scanList.get(i).getTitle().equals(item)){
                canAdd = false;
            }
        }
        if (canAdd) {
            System.err.println(device+"  addDeviceToScanList");
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
    public void clearScanList() {

    }

//    static class ViewHolder {
//        TextView deviceName;
//        TextView deviceAddress;
//    }

    public void buildScanListView() {
        pairedDeviceList = findViewById(R.id.activity_scan_paired_list);
        pairedDeviceList.setHasFixedSize(true);
        pairedDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mScanListAdapter = new ScanListAdapter(this, scanList, (ScanListAdapter.OnScanMyListener) this);
    }
}
