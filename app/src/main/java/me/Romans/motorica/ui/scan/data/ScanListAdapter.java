package me.Romans.motorica.ui.scan.data;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.view.ChartActivity;
import me.Romans.motorica.ui.scan.view.ScanView;

public class ScanListAdapter extends RecyclerView.Adapter<ScanListAdapter.ScanViewHolder> implements ScanView {

    private Context mCtx;
    private List<ScanItem> mScanList;
    private OnScanMyListener mOnScanMyListener;

    public ScanListAdapter(Context  mCtx, List<ScanItem> mScanList, OnScanMyListener onScanMyListener) {
        this.mCtx = mCtx;
        this.mScanList = mScanList;
        this.mOnScanMyListener = onScanMyListener;
    }

    public void setNewList(List<ScanItem> list){
        this.mScanList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mCtx);
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.cell_scan_list, null);
        ScanViewHolder holder = new ScanViewHolder(view, mOnScanMyListener);
        return new ScanViewHolder(view, mOnScanMyListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ScanViewHolder holder, int position) {
        ScanItem item = mScanList.get(position);

        holder.textViewTitle.setText(item.getTitle());
        holder.imageView.setImageDrawable(mCtx.getResources().getDrawable(item.getImage()));
        holder.checkProgress.setVisibility(item.getCheckProgress()?View.VISIBLE:View.GONE);
    }

    @Override
    public int getItemCount() {
        return mScanList.size();
    }

    public class ScanViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView imageView;
        TextView textViewTitle;
        ProgressBar checkProgress;
        OnScanMyListener onScanMyListener;


        public ScanViewHolder(View itemView, OnScanMyListener onScanMyListener) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            checkProgress = itemView.findViewById(R.id.activity_check_progress);

            this.onScanMyListener = onScanMyListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onScanMyListener.onScanClick(getAdapterPosition());
        }
    }

    public interface OnScanMyListener {
        void onScanClick(int position);
    }

    @Override
    public void showPairedList(List<String> items) {

    }

    @Override
    public void addDeviceToScanList(String item, BluetoothDevice device) {

    }

    @Override
    public void clearScanList() {

    }

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
    public void navigateToChat(String extraName, BluetoothDevice extraDevice) {

    }

    @Override
    public void setNewStageCellScanList(int numberCell, int setImage, String setText) {

    }

    @Override
    public List<ScanItem> getMyScanList() {
        return null;
    }

    @Override
    public void loadData() {

    }

    @Override
    public void buildScanListView() {

    }

    @Override
    public boolean isFirstStart() {
        return false;
    }
}
