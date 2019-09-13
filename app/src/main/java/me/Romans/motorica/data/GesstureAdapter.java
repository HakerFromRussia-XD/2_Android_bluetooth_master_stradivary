package me.Romans.motorica.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import me.Romans.motorica.R;
import me.Romans.motorica.ui.chat.view.ChatActivity;
import me.Romans.motorica.ui.chat.view.ChatView;

public class GesstureAdapter extends RecyclerView.Adapter<GesstureAdapter.GestureViewHolder> implements ChatView {

    private Context mCtx;
    private List<Gesture_my> gesturesList;
    private OnGestureMyListener mOnGestureMyListener;
    private ChatActivity chatActivity;

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
        View view = inflater.inflate(R.layout.gestures_lest_layout, null);
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
    public void appendMessage(String message) {

    }

    @Override
    public void enableHWButton(boolean enabled) {

    }

    @Override
    public void showToast(String message) {

    }

    @Override
    public void onGestureClick(int position) {

    }

    @Override
    public void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension) {

    }

    @Override
    public void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication) {

    }

    @Override
    public void setStartParametersInGraphActivity() {

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
}
