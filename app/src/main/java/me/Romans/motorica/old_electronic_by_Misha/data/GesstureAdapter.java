package me.Romans.motorica.old_electronic_by_Misha.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import me.Romans.motorica.R;
import me.Romans.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity;
import me.Romans.motorica.old_electronic_by_Misha.ui.chat.view.ChartView;

public class GesstureAdapter extends RecyclerView.Adapter<GesstureAdapter.GestureViewHolder> implements ChartView {

    private Context mCtx;
    private List<Gesture_my> gesturesList;
    private OnGestureMyListener mOnGestureMyListener;
    private ChartActivity chatActivity;

    public GesstureAdapter(Context  mCtx, List<Gesture_my> gesturesList, OnGestureMyListener onGestureMyListener) {
        this.mCtx = mCtx;
        this.gesturesList = gesturesList;
        this.mOnGestureMyListener = onGestureMyListener;
    }

    public void setNewList(List<Gesture_my> list){
        this.gesturesList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GestureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mCtx);
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.gestures_lest_layout, null);
        GestureViewHolder holder = new GestureViewHolder(view, mOnGestureMyListener);
        return new GestureViewHolder(view, mOnGestureMyListener);
    }

    @Override
    public void onBindViewHolder(@NonNull GestureViewHolder holder, int position) {
        Gesture_my gesture = gesturesList.get(position);

        holder.textViewTitle.setText(gesture.getTitle());
        holder.textViewInfo.setText(gesture.getInfo());
        holder.textViewRating.setText(String.valueOf(gesture.getRating()));
//        holder.textViewPrice.setText(String.valueOf(gesture.getPrise()));
        holder.imageView.setImageDrawable(mCtx.getResources().getDrawable(gesture.getImage()));
    }

    @Override
    public int getItemCount() {
        return gesturesList.size();
    }

    public class GestureViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView imageView;
        TextView textViewName, textViewInfo, textViewTitle, textViewRating, textViewPrice;
        OnGestureMyListener onGestureMyListener;


        public GestureViewHolder(View itemView, OnGestureMyListener onGestureMyListener) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewInfo = itemView.findViewById(R.id.textViewShortDesc);
            textViewRating = itemView.findViewById(R.id.textViewRating);
//            textViewPrice = itemView.findViewById(R.id.textViewPrice);

            this.onGestureMyListener = onGestureMyListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onGestureMyListener.onGestureClick(getAdapterPosition());
        }
    }

    public interface OnGestureMyListener{
        void onGestureClick(int position);
    }


    @Override
    public void setStatus(String status) {
    }

    @Override
    public void setStatus(int resId) {
    }

    @Override
    public void setValueCH(int levelCH, int numberChannel) {
    }

    @Override
    public void setErrorReception(boolean incomeErrorReception) {
    }


    @Override
    public void enableInterface(boolean enabled) {
    }

    @Override
    public void showToast(String message) {
    }

    @Override
    public void showToastWithoutConnection() {
    }

    @Override
    public void onGestureClick(int position) {
    }

    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {
    }

    @Override
    public void setStartParameters(Integer receiveCurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication, Byte receiveRoughnessOfSensors) {
    }

    @Override
    public void setStartParametersInChartActivity() {
    }

    @Override
    public boolean getFirstRead() {
        return false;
    }

    @Override
    public void setFlagReceptionExpectation(Boolean flagReceptionExpectation) {
    }

    @Override
    public void setStartParametersTrigCH1(Integer receiveLevelTrigCH1) {
    }

    @Override
    public void setStartParametersTrigCH2(Integer receiveLevelTrigCH2) {
    }

    @Override
    public void setStartParametersCurrent(Integer receiveCurrent) {
    }

    @Override
    public void setStartParametersBlock(Byte receiveBlockIndication) {
    }

    @Override
    public void setStartParametersRoughness(Byte receiveRoughnessOfSensors) {
    }

    @Override
    public void setStartParametersBattery(Integer receiveBatteryTension) {
    }
}
