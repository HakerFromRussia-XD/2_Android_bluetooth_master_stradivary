/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.romans.motorica.new_electronic_by_Rodeon.ui.activities.intro;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.romans.motorica.R;
import me.romans.motorica.new_electronic_by_Rodeon.ble.ConstantManager;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;


//import com.example.android.motorica.R;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppCompatActivity implements ScanView, ScanListAdapter.OnScanMyListener {
//    private LeDeviceListAdapter mLeDeviceListAdapter;
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

        setContentView(R.layout.activity_scan);
        scanList = new ArrayList<>();
        buildScanListView();
        mLeDevices = new ArrayList<>();
        progress = findViewById(R.id.activity_scan_progress);
        scanButton = findViewById(R.id.activity_scan_button);

        mHandler = new Handler();
        // Checks if Bluetooth is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        checkLocationPermission();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
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
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);//Request location updates:
            }
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        if (!mScanning) {
//            menu.findItem(R.id.menu_stop).setVisible(false);
//            menu.findItem(R.id.menu_scan).setVisible(true);
//            menu.findItem(R.id.menu_refresh).setActionView(null);
//        } else {
//            menu.findItem(R.id.menu_stop).setVisible(true);
//            menu.findItem(R.id.menu_scan).setVisible(false);
//            menu.findItem(R.id.menu_refresh).setActionView(
//                    R.layout.actionbar_indeterminate_progress);
//        }
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_scan:
////                mLeDeviceListAdapter.clear();
//                scanLeDevice(true);
//                break;
//            case R.id.menu_stop:
//                scanLeDevice(false);
//                break;
//        }
//        return true;
//    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // Initializes list view adapter.
//        mLeDeviceListAdapter = new LeDeviceListAdapter();
//        setListAdapter(mLeDeviceListAdapter);
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
//        mLeDeviceListAdapter.clear();
    }

//    @Override
//    protected void onListItemClick(ListView l, View v, int position, long id) {
//        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
//        if (device == null) return;
//        final Intent intent = new Intent(this, StartActivity.class);
//        intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, device.getAddress());
//        if (mScanning) {
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            mScanning = false;
//        }
//        startActivity(intent);
//    }

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

//    // Adapter for holding devices found through scanning.
////    private class LeDeviceListAdapter extends BaseAdapter {
////        private ArrayList<BluetoothDevice> mLeDevices;
////        private LayoutInflater mInflator;
////
////        public LeDeviceListAdapter() {
////            super();
////            mLeDevices = new ArrayList<>();
////            mInflator = DeviceScanActivity.this.getLayoutInflater();
////        }
////
////        public void addDevice(BluetoothDevice device) {
////            if(!mLeDevices.contains(device)) {
////                mLeDevices.add(device);
////            }
////        }
////
////        public BluetoothDevice getDevice(int position) {
////            return mLeDevices.get(position);
////        }
////
////        public void clear() {
////            mLeDevices.clear();
////        }
////
////        @Override
////        public int getCount() {
////            return mLeDevices.size();
////        }
////
////        @Override
////        public Object getItem(int i) {
////            return mLeDevices.get(i);
////        }
////
////        @Override
////        public long getItemId(int i) {
////            return i;
////        }
////
////        @SuppressLint("InflateParams")
////        @Override
////        public View getView(int i, View view, ViewGroup viewGroup) {
////            ViewHolder viewHolder;
////            // General ListView optimization code.
////            if (view == null) {
////                view = mInflator.inflate(R.layout.listitem_device, null);
////                viewHolder = new ViewHolder();
////                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
////                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
////                view.setTag(viewHolder);
////            } else {
////                viewHolder = (ViewHolder) view.getTag();
////            }
////
////            BluetoothDevice device = mLeDevices.get(i);
////            final String deviceName = device.getName();
////            if (deviceName != null && deviceName.length() > 0)
////                viewHolder.deviceName.setText(deviceName);
////            else
////                viewHolder.deviceName.setText(R.string.unknown_device);
////            viewHolder.deviceAddress.setText(device.getAddress());
////
////            return view;
////        }
////    }

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
