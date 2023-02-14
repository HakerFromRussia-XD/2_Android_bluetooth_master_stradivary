package me.start.motorica.scan.data;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.start.motorica.R;
import me.start.motorica.scan.view.ScanActivity;
import me.start.motorica.scan.view.ScanView;

public class ScanListAdapter extends RecyclerView.Adapter<ScanListAdapter.ScanViewHolder> implements ScanView {

    private final Context mCtx;
    private final List<ScanItem> mScanList;
    private List<Integer> realPosition;
    private final OnScanMyListener mOnScanMyListener;
    private final ArrayList<Long> percentAnimationPairedList = new ArrayList<>();
    private int lastPosition = -1;
    private boolean firstBind = true;
    private CountDownTimer timer;
    private long time = 0;
    private final static int ANIM_DURATION = 10000;

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
    public void onBindViewHolder(@NonNull ScanViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ScanItem item = mScanList.get(position);

        holder.textViewTitle.setText(item.getTitle()+" "+item.getPosition());
    }

    @Override
    public int getItemCount() {
        if (mScanList != null) { return mScanList.size(); }
        else {return 0;}
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
    public boolean getFilteringOursDevices() {return false;}

    @Override
    public boolean isFirstStart() {
        return false;
    }

    @Override
    public ArrayList<BluetoothDevice> getLeDevices() { return null; }

}
