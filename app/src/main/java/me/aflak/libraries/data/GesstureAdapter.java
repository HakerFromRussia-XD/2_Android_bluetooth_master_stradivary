package me.aflak.libraries.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import me.aflak.libraries.R;

public class GesstureAdapter extends RecyclerView.Adapter<GesstureAdapter.GestureViewHolder> {

    private Context mCtx;
    private List<Gesture_my> gesturesList;
    private OnGestureMyListener mOnGestureMyListener;

    public GesstureAdapter(Context  mCtx, List<Gesture_my> gesturesList, OnGestureMyListener onGestureMyListener) {
        this.mCtx = mCtx;
        this.gesturesList = gesturesList;
        this.mOnGestureMyListener = onGestureMyListener;
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
