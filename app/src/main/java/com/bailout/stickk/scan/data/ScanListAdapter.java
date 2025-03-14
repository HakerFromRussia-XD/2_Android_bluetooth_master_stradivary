package com.bailout.stickk.scan.data;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.bailout.stickk.R;
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys;
import com.bailout.stickk.scan.view.ScanView;

public class ScanListAdapter extends RecyclerView.Adapter<ScanListAdapter.ScanViewHolder> implements ScanView {

    private final Context mCtx;
    private final List<ScanItem> mScanList;
    private List<Integer> realPosition;
    private final OnScanMyListener mOnScanMyListener;
    private final ArrayList<Long> percentAnimationPairedList = new ArrayList<>();
    private final int lastPosition = -1;
    private final boolean firstBind = true;
    private CountDownTimer timer;
    private final long time = 0;
    private SharedPreferences mSettings = null;

    public ScanListAdapter(Context  mCtx, List<ScanItem> mScanList, OnScanMyListener onScanMyListener) {
        this.mCtx = mCtx;
        this.mScanList = mScanList;
        this.mOnScanMyListener = onScanMyListener;
    }


    @NonNull
    @Override
    public ScanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mCtx);
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.item_scanlist, null);
        mSettings = mCtx.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE);
        return new ScanViewHolder(view, mOnScanMyListener);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ScanViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ScanItem item = mScanList.get(position);

        holder.textViewTitle.setText(item.getTitle());
        if (loadBool(PreferenceKeys.ACTIVATE_RSSI_SHOW)) holder.rssi.setText(""+item.getRssi());
    }

    @Override
    public int getItemCount() {
        if (mScanList != null) { return mScanList.size(); }
        else {return 0;}
    }

    public static class ScanViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView textViewTitle;
        TextView rssi;
        View divider;
        OnScanMyListener onScanMyListener;


        public ScanViewHolder(View itemView, OnScanMyListener onScanMyListener) {
            super(itemView);

            textViewTitle = itemView.findViewById(R.id.title_tv);
            rssi = itemView.findViewById(R.id.rssi_tv);
            divider = itemView.findViewById(R.id.divider_v);

            this.onScanMyListener = onScanMyListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) { onScanMyListener.onScanClick(getAdapterPosition()); }
    }

    public interface OnScanMyListener { void onScanClick(int position); }

    @Override
    public void showPairedList(ArrayList<ScanItem> items) {}

    @Override
    public void addDeviceToScanList(String item, String address, BluetoothDevice device) { }

    @Override
    public void addLEDeviceToLeDevicesList(BluetoothDevice device, int rssi) { }

    @Override
    public void clearScanList() { }
    @Override
    public void setScanStatus(String status, boolean enabled) { }

    @Override
    public void showProgress(boolean enabled) { }
    @Override
    public void enableScanButton(boolean enabled) { }
    @Override
    public void showToast(String message) { }
    @Override
    public void navigateToChart(String extraName, BluetoothDevice extraDevice) { }

    @Override
    public void navigateToLEChart(String extraName, BluetoothDevice extraDevice) { }

    @Override
    public boolean getFilteringOursDevices() {return false;}

    @Override
    public boolean isFirstStart() {
        return false;
    }

    @Override
    public ArrayList<BluetoothDevice> getLeDevices() { return null; }

    private Boolean loadBool(String key) { return mSettings.getBoolean(key, false); }
}
