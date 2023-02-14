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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.start.motorica.R;
import me.start.motorica.scan.view.ScanView;

public class PairedListAdapter extends RecyclerView.Adapter<PairedListAdapter.ScanViewHolder> implements ScanView {

    private final Context mCtx;
    private final List<ScanItem> mPairedList;
    private final OnScanMyListener mOnScanMyListener;
    private final ArrayList<Long> percentAnimationPairedList = new ArrayList<>();
    private int lastPosition = -1;
    private final boolean firstBind = true;
    private CountDownTimer timer;
    private final long time = 0;
    private final static int ANIM_DURATION = 10000;

    public PairedListAdapter(Context  mCtx, List<ScanItem> mPairedList, OnScanMyListener onScanMyListener) {
        this.mCtx = mCtx;
        this.mPairedList = mPairedList;
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
        ScanItem item = mPairedList.get(position);

        holder.textViewTitle.setText(item.getTitle());
    }

    private void setFadeAnimation(View view) {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(1000);
        view.startAnimation(anim);
    }
    private void setScaleAnimation(View view, int percentAnimation) {
        ScaleAnimation anim = new ScaleAnimation(1.0f*percentAnimation, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(ANIM_DURATION - (long) ANIM_DURATION / 100 * percentAnimation);
        view.startAnimation(anim);
    }
    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            ScaleAnimation anim = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(10000);//to make duration random number between [0,501)
            viewToAnimate.startAnimation(anim);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return mPairedList.size();
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
            onScanMyListener.onPairedClick(getAdapterPosition());
        }
    }

    public interface OnScanMyListener { void onPairedClick(int position); }




    @Override
    public void showPairedList(ArrayList<ScanItem> items) { }

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
    public boolean getFilteringOursDevices() { return false; }

    @Override
    public boolean isFirstStart() {
        return false;
    }

    @Override
    public ArrayList<BluetoothDevice> getLeDevices() { return null; }

}
