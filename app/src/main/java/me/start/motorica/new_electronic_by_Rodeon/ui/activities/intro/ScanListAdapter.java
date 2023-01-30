package me.start.motorica.new_electronic_by_Rodeon.ui.activities.intro;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import me.start.motorica.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;



public class ScanListAdapter extends RecyclerView.Adapter<ScanListAdapter.ScanViewHolder> implements ScanView {

    private final Context mCtx;
    private final List<ScanItem> mScanList;
    private final OnScanMyListener mOnScanMyListener;


    public ScanListAdapter(Context  mCtx, List<ScanItem> mScanList, OnScanMyListener onScanMyListener) {
        this.mCtx = mCtx;
        this.mScanList = mScanList;
        this.mOnScanMyListener = onScanMyListener;
    }


    @NotNull
    @Override
    public ScanViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mCtx);
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.item_scanlist, null);
        return new ScanViewHolder(view, mOnScanMyListener);
    }

    @Override
    public void onBindViewHolder(@NotNull ScanViewHolder holder, int position) {
        ScanItem item = mScanList.get(position);

        holder.textViewTitle.setText(item.getTitle());
//        holder.imageView.setImageDrawable(mCtx.getResources().getDrawable(item.getImage()));
//        holder.checkProgress.setVisibility(item.getCheckProgress()?View.VISIBLE:View.GONE);
    }

    @Override
    public int getItemCount() {
        return mScanList.size();
    }

    public static class ScanViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView imageView;
        TextView textViewTitle;
//        ProgressBar checkProgress;
        OnScanMyListener onScanMyListener;


        public ScanViewHolder(View itemView, OnScanMyListener onScanMyListener) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
//            checkProgress = itemView.findViewById(R.id.activity_check_progress);

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
    public void addDeviceToScanList(String item, BluetoothDevice device) {  }
    @Override
    public void clearScanList() { }

}
