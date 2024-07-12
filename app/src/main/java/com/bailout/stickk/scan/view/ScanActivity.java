package com.bailout.stickk.scan.view;

import static com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.MAX_NUMBER_DETAILS;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bailout.stickk.BuildConfig;
import com.bailout.stickk.R;
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication;
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager;
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys;
import com.bailout.stickk.new_electronic_by_Rodeon.presenters.Load3DModelNew;
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.intro.StartActivity;
import com.bailout.stickk.new_electronic_by_Rodeon.utils.NameUtil;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.view.ChartActivity;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.view.Load3DModel;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.view.NemoStandActivity;
import com.bailout.stickk.scan.data.DaggerScanComponent;
import com.bailout.stickk.scan.data.PairedListAdapter;
import com.bailout.stickk.scan.data.ScanItem;
import com.bailout.stickk.scan.data.ScanListAdapter;
import com.bailout.stickk.scan.data.ScanModule;
import com.bailout.stickk.scan.presenter.ScanPresenter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;


@SuppressWarnings("ALL")
public class ScanActivity extends AppCompatActivity implements ScanView, ScanListAdapter.OnScanMyListener, PairedListAdapter.OnScanMyListener, TextSwitcher.ViewFactory {
    /// BT
    RecyclerView pairedDeviceList;
    RecyclerView scanDeviceList;
    TextView state;
    LottieAnimationView progress;
    View prosthesesButtonFilter;
    View allDevicesButtonFilter;
    View scanButton;
    View rssiButton;
    View selectView;
    View filterView;
    ImageView rescanImage;
    ImageView rssiOnImage;
    ImageView rssiOffImage;
    TextView prosthesesText;
    TextView allDevicesText;
    TextView versionAppText;
    TextSwitcher scanningTextSwitcher;
    private int filterWidth = 0;
    private boolean firstStart = true;
    private boolean firstNavigateToActivity = true;
    private boolean filteringOursDevices = true;
    private boolean acteveteRssiShow = false;
    // 3D
    Load3DModel mLoad3DModel = new Load3DModel(this);
    Load3DModelNew mLoad3DModelNew = new Load3DModelNew(this);
    public Thread[] threadFunction = new Thread[MAX_NUMBER_DETAILS+MAX_NUMBER_DETAILS];
    private SharedPreferences mSettings = null;
    private float scale = 0F;
    private int count = 0;
    private int ANIMATION_DURATION = 200;

    private final boolean isAndoird12 = Build.VERSION.SDK_INT>=Build.VERSION_CODES.S;
    @RequiresApi(api = Build.VERSION_CODES.S)
    private boolean isBluetoothScanNotOk() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)!= PackageManager.PERMISSION_GRANTED;
    }
    @RequiresApi(api = Build.VERSION_CODES.S)
    private boolean isBluetoothConnectNotOk() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)!= PackageManager.PERMISSION_GRANTED;
    }
    private interface check12 {
        void ongranted(boolean isgranted);
    }
//    private void checkandroid12BT(check12 c) {
//        if(isBluetoothScanNotOk()) {
//            startpermissionresult(isgranted -> {
//
//            },Manifest.permission.BLUETOOTH_SCAN);
//        } else {
//            if(isBluetoothConnectNotOk()){
//                startpermissionresult
//            } else {
//                c.ongranted(true);
//            }
//        }
//    }

    @Inject
    ScanPresenter presenter;

    PairedListAdapter mPairedListAdapter;
    ScanListAdapter mScanListAdapter;
    ArrayList<ScanItem> pairedList = new ArrayList<>();
    ArrayList<ScanItem> scanList  = new ArrayList<>();


    /// BLE
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler = new Handler();

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 20000;
    // public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private int scanListBLEPosition = 0;

    private ArrayList<BluetoothDevice> mLeDevices  = new ArrayList<>();
    private ArrayList<Integer> mRssisList  = new ArrayList<>();
    private ArrayList<BluetoothDevice> filteringLeDevices  = new ArrayList<>();


    @SuppressLint({"NewApi", "ClickableViewAccessibility", "ObsoleteSdkInt"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        System.err.println(" LOLOLOEFWEF --->  ScanActivity onCreate");
        DaggerScanComponent.builder()
                .bluetoothModule(Objects.requireNonNull(WDApplication.app()).bluetoothModule())
                .scanModule(new ScanModule(this))
                .build().inject(this);
        setContentView(R.layout.activity_scan_new);
        //changing statusbar
        if (android.os.Build.VERSION.SDK_INT >= 21){
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.blue_status_bar));
            window.setNavigationBarColor(this.getResources().getColor(R.color.color_primary));
        }
        scale = this.getResources().getDisplayMetrics().density;
        /////////////////////////////////////////
        scanDeviceList = findViewById(R.id.scan_list);
        progress = findViewById(R.id.scan_progress);
        scanButton = findViewById(R.id.scan_btn);
        rssiButton = findViewById(R.id.rssi_btn);
        rescanImage = findViewById(R.id.rescan_iv);
        rssiOnImage = findViewById(R.id.rssi_on_iv);
        rssiOffImage = findViewById(R.id.rssi_off_iv);
        prosthesesText = findViewById(R.id.prostheses_tv);
        allDevicesText = findViewById(R.id.all_devices_tv);
        versionAppText = findViewById(R.id.version_app_tv);
        prosthesesButtonFilter = findViewById(R.id.prostheses_select_btn);
        allDevicesButtonFilter = findViewById(R.id.all_devices_select_btn);
        selectView = findViewById(R.id.select_v);
        filterView = findViewById(R.id.filter_v);
        scanningTextSwitcher = (TextSwitcher) findViewById(R.id.scanning_ts);
        scanningTextSwitcher.setInAnimation(this, android.R.anim.slide_in_left);
        scanningTextSwitcher.setOutAnimation(this, android.R.anim.slide_out_right);
        scanningTextSwitcher.setFactory(this);


        /// BLE
        // Smart connection
        mSettings = getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE);
        initUI();
        buildPairedListView();
        buildScanListView();
        onProsthesesFilterClick();
        onAllDevicesFilterClick();

        String versionName = BuildConfig.VERSION_NAME;
        versionAppText.setText((this.getResources().getString(R.string.version_app)) + " " + versionName);

        scanButton.setOnClickListener(v -> {
            mLeDevices.clear();
            mRssisList.clear();
            animateScanList(0);
            showScanList(mLeDevices, mRssisList);
            scanLeDevice(true);
            presenter.startScanning();
        });

        rssiButton.setOnClickListener(v -> {
            acteveteRssiShow = !acteveteRssiShow;
            saveBool(PreferenceKeys.ACTIVATE_RSSI_SHOW, acteveteRssiShow);
            setScaleAnimation(rssiOnImage, acteveteRssiShow ? 1f : 0f);
            setScaleAnimation(rssiOffImage, !acteveteRssiShow ? 1f : 0f);
            showScanList(mLeDevices, mRssisList);
        });

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

            try {
                if(!mBluetoothAdapter.isMultipleAdvertisementSupported()){
                    Toast.makeText(this, "bluetooth low energy нет на телефоне", Toast.LENGTH_SHORT).show();
                }
                finish();
            } catch (IllegalStateException e) {
                // This can only happen on 4.1+, when we don't have a parent or a result set.
                // In that case we should just finish().
                finish();
            }
        }

        checkLocationPermission();
        init3D();


        //TODO закомментить быстрый вход после завершения экспериментов
//        testNavigate();
    }
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        filterWidth = (int) (filterView.getWidth()/displayMetrics.density);
    }
    @Override
    protected void onStart() {
        super.onStart();
//        if ((ContextCompat.checkSelfPermission(this,
//                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
        presenter.onStart(this);
//        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        presenter.setOnPauseActivity(false);
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mLeDevices.clear();
        mRssisList.clear();
        animateScanList(0);
        showScanList(mLeDevices, mRssisList);
        scanLeDevice(true);
        presenter.startScanning();
    }
    @Override
    protected void onPause() {
        super.onPause();
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


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(() -> {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                invalidateOptionsMenu();
                setScaleAnimation(progress, 0f);
                setScaleAnimation(rescanImage, 1f);
                scanButton.setEnabled(true);
                scanningTextSwitcher.setText(getResources().getString(R.string.availible_devices));
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            scanButton.setEnabled(false);
            scanningTextSwitcher.setText(getResources().getString(R.string.bluetooth_scanning));
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            scanButton.setEnabled(true);
            scanningTextSwitcher.setText(getResources().getString(R.string.scan_again));
        }
        setScaleAnimation(progress, enable ? 1f : 0f);
        setScaleAnimation(rescanImage, !enable ? 1f : 0f);
        invalidateOptionsMenu();
    }

    // Device scan callback.
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.M)
    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            (device, rssi, scanRecord) -> runOnUiThread(() -> {
                if(device.getName() != null){
                    addLEDeviceToLeDevicesList(device, rssi);
                }
            });
    private final BluetoothAdapter.LeScanCallback mLeAdvertisingCallback =
            (device, rssi, scanRecord) -> runOnUiThread(() -> { });
    @Override
    public void showPairedList(ArrayList<ScanItem> items) {
        updatePairedList(items);
        pairedDeviceList.setAdapter(mPairedListAdapter);
        animatePairedList(items.size());
    }
    private void updatePairedList(List<ScanItem> items) {
        pairedList.clear();
        for (int i = 0; i < items.size(); i++) {
            pairedList.add(items.get(i));
        }
    }
    private void showScanList(List<BluetoothDevice> items, List<Integer> rssis) {
        updateScanList(items, rssis);
        scanDeviceList.setAdapter(mScanListAdapter);
    }
    private void updateScanList(List<BluetoothDevice> items, List<Integer> rssis) {
        scanList.clear();
        filteringLeDevices.clear();
        System.err.println("--> my updateScanList ============= ");
        for (int i = 0; i < items.size(); i++) {
            System.err.println("--> my updateScanList проверяем " + items.get(i).getName() +"  "+checkOurLEName(items.get(i).getName()));
            if (items.get(i).getName() != null) {
                if (checkOurLEName(items.get(i).getName())) {
                    scanList.add(new ScanItem(
                            getProtocolType(items.get(i).getName()),
                            NameUtil.INSTANCE.getCleanName(items.get(i).getName()),
                            items.get(i).getAddress(),
                            i,
                            rssis.get(i)
                    ));
                    filteringLeDevices.add(items.get(i));
                    animateScanList(scanList.size());
                } else {
//                    System.err.println("--> scanList не прошёл по фильтру имени");
                }
            } else {
//                System.err.println("--> scanList не прошёл по ненулёвости имени");
            }
        }
    }
    @Override
    public void addLEDeviceToLeDevicesList(BluetoothDevice device, int rssi) {
        boolean canAdd = true;
        for (int i = 0; i<mLeDevices.size(); i++) {
            if (mLeDevices.get(i).getAddress().equals(device.getAddress())) {
                canAdd = false;
            }
        }

        //здесь мы принимаем решение добавлять ли новое устройство в список отсканированных

        if (canAdd) {
            System.err.println("--> my addLEDeviceToLeDevicesList name = "+device.getName() + " Address: " + device.getAddress());
            mLeDevices.add(device);
            mRssisList.add(rssi);
            showScanList(mLeDevices, mRssisList);
            scrollToEndList(scanDeviceList);
//            System.err.println("--> my addLEDeviceToLeDevicesList ============= "+device.getName());
//            for (int i = 0; i<mLeDevices.size(); i++) {
//                System.err.println("--> my addLEDeviceToLeDevicesList name = "+mLeDevices.get(i).getName());
//            }
        }
        smartConnection(device);
    }
    private void scrollToEndList(RecyclerView chatRv) {
        if (chatRv.getAdapter().getItemCount() >= 5) {
            chatRv.smoothScrollToPosition(chatRv.getAdapter().getItemCount() - 1);
        }
    }
    @Override
    public void addDeviceToScanList(String item, String address, BluetoothDevice device) {}
    @Override
    public void clearScanList() {
        scanList.clear();
        filteringLeDevices.clear();
        pairedDeviceList.setAdapter(mScanListAdapter);
    }
    private void smartConnection(BluetoothDevice device) {
        if (loadBool(PreferenceKeys.SET_MODE_SMART_CONNECTION)) {
            if (device.getAddress().toString().equals(loadString(PreferenceKeys.LAST_CONNECTION_MAC))) {
                navigateToLEChart("device", device);
            }
        }
    }
    @Override
    public void setScanStatus(String status, boolean enabled) {
        try {
            state.setVisibility(enabled?View.VISIBLE:View.GONE);
            state.setText(status);
        } catch (Exception e) {
            System.err.println("Exception setScanStatus: " + e);
        }
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
        if (extraDevice.getName().contains("NEMO") ||
            extraDevice.getName().contains("STAND") ) {
            intent = new Intent(ScanActivity.this, NemoStandActivity.class);
        }
        intent.putExtra(extraName, extraDevice);
        startActivity(intent);
        finish();
    }
    @SuppressLint("MissingPermission")
    @Override
    public void navigateToLEChart(String extraName, BluetoothDevice extraDevice) {
        if (firstNavigateToActivity) {
            firstNavigateToActivity = false;
            mHandler.postDelayed(() -> {
                for (int k = 0; k < MAX_NUMBER_DETAILS; k++) {
                    final int finalK = k;
                    System.err.println("Запуск загрузки: " + finalK);
                    threadFunction[k] = new Thread(() -> mLoad3DModelNew.loadSTR2(finalK));
                    threadFunction[k].start();
                }
            }, 500);


            if (extraDevice == null) return;
            Intent intent = new Intent(ScanActivity.this, StartActivity.class);
            intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, NameUtil.INSTANCE.getCleanName(extraDevice.getName()));//NameUtil.INSTANCE.getCleanName(
            intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, extraDevice.getAddress());
            intent.putExtra(ConstantManager.EXTRAS_DEVICE_TYPE, getProtocolType(extraDevice.getName()));//getProtocolType()


            if (mScanning) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
            }
            startActivity(intent);
            finish();
        }
    }
    private void testNavigate() {
        mHandler.postDelayed(() -> {
            for (int k = 0; k<MAX_NUMBER_DETAILS; k++) {
                final int finalK = k;
                System.err.println("Запуск загрузки: " + finalK);
                threadFunction[k] = new Thread(() -> mLoad3DModelNew.loadSTR2(finalK));
                threadFunction[k].start();
            }
        }, 500);

        Intent intent = new Intent(ScanActivity.this, StartActivity.class);
        intent.putExtra(ConstantManager.EXTRAS_DEVICE_NAME, "FEST-X");
        intent.putExtra(ConstantManager.EXTRAS_DEVICE_ADDRESS, "lol");
        intent.putExtra(ConstantManager.EXTRAS_DEVICE_TYPE, "FEST-X");//FEST-X INDY
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
        finish();
    }

    public boolean isFirstStart() {
        return firstStart;
    }
    private void onProsthesesFilterClick() {
        prosthesesButtonFilter.setOnClickListener(v -> {
            filteringOursDevices = true;
            saveBool(PreferenceKeys.FILTERING_OUR_DEVISES, true);
            moveFilterSelection(1);
        });
    }
    private void onAllDevicesFilterClick() {
        allDevicesButtonFilter.setOnClickListener(v -> {
            filteringOursDevices = false;
            saveBool(PreferenceKeys.FILTERING_OUR_DEVISES, false);
            moveFilterSelection(2);
        });
    }
    private void moveFilterSelection(int position) {
        System.err.println("kejhfuyewgfbasuklihf moveFilterSelection position "+position);
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        switch (position) {
            case(1):
                ObjectAnimator.ofFloat(selectView, "x", (18 * displayMetrics.density)).setDuration(ANIMATION_DURATION).start();//53
                ObjectAnimator colorAnim = ObjectAnimator.ofInt(prosthesesText, "textColor",
                        getColor(R.color.unselected_filter), getColor(R.color.selected_filter));
                        colorAnim.setEvaluator(new ArgbEvaluator());
                        colorAnim.start();
                ObjectAnimator colorAnim3 = ObjectAnimator.ofInt(allDevicesText, "textColor",
                        getColor(R.color.selected_filter), getColor(R.color.unselected_filter));
                colorAnim3.setEvaluator(new ArgbEvaluator());
                colorAnim3.start();
                break;
            case(2):
                ObjectAnimator.ofFloat(selectView, "x", ((filterWidth/2)+18) * displayMetrics.density).setDuration(ANIMATION_DURATION).start();//546
                ObjectAnimator colorAnim2 = ObjectAnimator.ofInt(prosthesesText, "textColor",
                        getColor(R.color.selected_filter), getColor(R.color.unselected_filter));
                        colorAnim2.setEvaluator(new ArgbEvaluator());
                        colorAnim2.start();
                ObjectAnimator colorAnim4 = ObjectAnimator.ofInt(allDevicesText, "textColor",
                        getColor(R.color.unselected_filter), getColor(R.color.selected_filter));
                colorAnim4.setEvaluator(new ArgbEvaluator());
                colorAnim4.start();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + position);
        }
        showPairedList(presenter.getPairedList());
        showScanList(mLeDevices, mRssisList);
    }
    private void animatePairedList(int countItems) {
        if (countItems <= 3) {
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            ValueAnimator anim = ValueAnimator.ofInt(pairedDeviceList.getMeasuredHeight(), (int) (57 * displayMetrics.density * countItems));
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int val = (Integer) valueAnimator.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = pairedDeviceList.getLayoutParams();
                    layoutParams.height = val;
                    pairedDeviceList.setLayoutParams(layoutParams);
                }
            });
            anim.setDuration(ANIMATION_DURATION);
            anim.start();
        }
    }
    private void animateScanList(int countItems) {
//        System.err.println("--> animateScanList: countItems "+countItems);
        if (countItems == 1) {
            scanDeviceList.animate()
                    .translationY(0)
                    .setDuration(ANIMATION_DURATION)
                    .alpha(1f)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
//        if (countItems <= 3) {
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            ValueAnimator anim = ValueAnimator.ofInt(scanDeviceList.getMeasuredHeight(), (int) (57 * displayMetrics.density * countItems));
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int val = (Integer) valueAnimator.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = scanDeviceList.getLayoutParams();
                    layoutParams.height = val;
                    scanDeviceList.setLayoutParams(layoutParams);
                }
            });
            anim.setDuration(ANIMATION_DURATION);
            anim.start();
//        }
    }
    @Override
    public void onPairedClick(int position) {
        pairedDeviceList.setClickable(false);
        presenter.pairedItemClick(position);
    }
    @Override
    public void onScanClick(int position) {
        scanDeviceList.setClickable(false);
        presenter.leItemClick(position);
    }
    public boolean getFilteringOursDevices () {
        return filteringOursDevices;
    }

    private String getProtocolType(@NotNull String deviceName) {
        String protocolType = "";
        if (deviceName.length() >= 6) {
            protocolType = deviceName.substring(0, 6);
        } // FEST-X
        if (protocolType.contains(ConstantManager.DEVICE_TYPE_FEST_A)
            || protocolType.contains(ConstantManager.DEVICE_TYPE_BT05)
            || protocolType.contains(ConstantManager.DEVICE_TYPE_MY_IPHONE)) {
//            showToast("getProtocolType:"+ConstantManager.DEVICE_TYPE_FEST_A+".");
            return ConstantManager.DEVICE_TYPE_FEST_A;
        } else {
            if (protocolType.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
//                showToast("getProtocolType: "+ConstantManager.DEVICE_TYPE_FEST_H+".");
                return ConstantManager.DEVICE_TYPE_FEST_H;
            } else {
                if (protocolType.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
//                    showToast("getProtocolType: "+ConstantManager.DEVICE_TYPE_FEST_X+".");
                    return ConstantManager.DEVICE_TYPE_FEST_X;
                } else {
//                    showToast("getProtocolType: "+ConstantManager.DEVICE_TYPE_INDY+".");
                    return ConstantManager.DEVICE_TYPE_INDY;
                }
            }
        }
    }

    public ArrayList<BluetoothDevice> getLeDevices() {
        if (filteringOursDevices) { return filteringLeDevices; }
        else { return mLeDevices; }
    }

    private void buildPairedListView() {
        pairedDeviceList = findViewById(R.id.paired_list);
        pairedDeviceList.setHasFixedSize(true);
        pairedDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mPairedListAdapter = new PairedListAdapter(this, pairedList, this);
    }
    private void buildScanListView() {
        scanDeviceList = findViewById(R.id.scan_list);
        scanDeviceList.setHasFixedSize(true);
        scanDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mScanListAdapter = new ScanListAdapter(this, scanList, this);
    }
    private boolean checkOurLEName (String deviceName){
        if (deviceName != null) {
            return deviceName.contains("HRSTM") ||
                    deviceName.contains("BLE_test_service") ||
                    deviceName.contains("MLT") ||
                    deviceName.contains("FNG") ||
                    deviceName.contains("FNS") ||
                    deviceName.contains("MLX") ||
                    deviceName.contains("FNX") ||
                    deviceName.contains("STR") ||
                    deviceName.contains("CBY") ||
                    deviceName.contains("IND") ||
                    deviceName.contains("HND") ||
                    deviceName.contains("NEMO") ||
                    deviceName.contains("STAND") ||
                    deviceName.contains("BT05") ||
                    deviceName.contains("FEST") ||
                    !filteringOursDevices;
        } else {
            return false;
        }
    }


    private void initUI() {
        filteringOursDevices = loadBool(PreferenceKeys.FILTERING_OUR_DEVISES);
        acteveteRssiShow = loadBool(PreferenceKeys.ACTIVATE_RSSI_SHOW);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setScaleAnimation(rssiOnImage, acteveteRssiShow ? 1f : 0f);
                setScaleAnimation(rssiOffImage, !acteveteRssiShow ? 1f : 0f);
                if (filteringOursDevices) {
                    moveFilterSelection(1);
                } else {
                    moveFilterSelection(2);
                }
            }
        }, 500);
    }
    private void setScaleAnimation(View view, float scale) {
        view.animate()
                .setDuration(ANIMATION_DURATION)
                .scaleX(scale)
                .scaleY(scale)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
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
    private void saveBool(String key, Boolean variable) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(key, variable);
        editor.apply();
    }
    private Boolean loadBool(String key) { return mSettings.getBoolean(key, false); }
    private void mySaveText(String key, String text) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(key, text);
        editor.apply();
    }
    private String loadString(String key) { return mSettings.getString(key, "null"); }

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


    @Override
    public View makeView() {
        TextView textView = new TextView(ScanActivity.this);
        textView.setTextSize(14);
        textView.setTextColor(Color.WHITE);
        textView.setGravity(Gravity.LEFT);
        textView.setTypeface(Typeface.createFromAsset(getAssets(),
                "fonts/font_open_sans.ttf"));
        return textView;
    }
}
