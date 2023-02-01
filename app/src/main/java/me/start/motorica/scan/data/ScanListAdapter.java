package me.start.motorica.scan.data;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.start.motorica.R;
import me.start.motorica.scan.view.ScanView;

public class ScanListAdapter extends RecyclerView.Adapter<ScanListAdapter.ScanViewHolder> implements ScanView {

    private final Context mCtx;
    private final List<ScanItem> mScanList;
    private final OnScanMyListener mOnScanMyListener;

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
        return new ScanViewHolder(view, mOnScanMyListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ScanViewHolder holder, int position) {
        ScanItem item = mScanList.get(position);

        holder.textViewTitle.setText(item.getTitle());

        if (position==(mScanList.size() - 1)) {
            holder.itemView.setOnClickListener(null);
            holder.divider.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mScanList.size();
    }

    public static class ScanViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView textViewTitle;
        View divider;
        OnScanMyListener onScanMyListener;


        public ScanViewHolder(View itemView, OnScanMyListener onScanMyListener) {
            super(itemView);

            textViewTitle = itemView.findViewById(R.id.title_tv);
            divider = itemView.findViewById(R.id.divider_v);

            this.onScanMyListener = onScanMyListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onScanMyListener.onScanClick(getAdapterPosition());
        }
    }

    public interface OnScanMyListener { void onScanClick(int position); }

    @Override
    public void showPairedList(List<String> items) { }
    @Override
    public void addDeviceToScanList(String item, String address, BluetoothDevice device) { }

    @Override
    public void addLEDeviceToScanList(String item, BluetoothDevice device, int rssi) { }

    @Override
    public void clearScanList() { }
    @Override
    public void clearPairedList() { }
    @Override
    public void setScanStatus(String status, boolean enabled) { }
    @Override
    public void setScanStatus(int resId, boolean enabled) { }
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
    public void setNewStageCellScanList(int numberCell, int setImage, String setText) { }
    @Override
    public List<ScanItem> getMyScanList() {
        return null;
    }
    @Override
    public void loadData() { }
    @Override
    public void buildScanListView() { }
    @Override
    public boolean isFirstStart() {
        return false;
    }

    @Override
    public ArrayList<BluetoothDevice> getLeDevices() { return null; }
}
