/*
 * Copyright 2014 Magnus Woxblom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bailout.stickk.R;
import com.woxthebox.draglistview.DragItemAdapter;

import java.util.ArrayList;

import kotlin.Pair;

class RotationGroupItemAdapterV3 extends DragItemAdapter<Pair<Long, String>, RotationGroupItemAdapterV3.ViewHolder> {

    private int mLayoutId;
    private int mGrabHandleId;
    private boolean mDragOnLongPress;
    OnCopyClickRotationGroupListener onCopyClickRotationGroupListener;
    OnDeleteClickRotationGroupListener onDeleteClickRotationGroupListener;
    OnSelectClickRotationGroupListener onSelectClickRotationGroupListener;
    private int activeGestureId = -1;
    private boolean interactionEnabled = true;

    RotationGroupItemAdapterV3(ArrayList<Pair<Long, String>> list, int layoutId, int grabHandleId, boolean dragOnLongPress, OnCopyClickRotationGroupListener onCopyClickRotationGroupListener, OnDeleteClickRotationGroupListener onDeleteClickRotationGroupListener, OnSelectClickRotationGroupListener onSelectClickRotationGroupListener) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
        setItemList(list);
        this.onCopyClickRotationGroupListener = onCopyClickRotationGroupListener;
        this.onDeleteClickRotationGroupListener = onDeleteClickRotationGroupListener;
        this.onSelectClickRotationGroupListener = onSelectClickRotationGroupListener;
    }

    public void setActiveGestureId(int gestureId) {
        this.activeGestureId = gestureId;
        notifyDataSetChanged();
    }

    public void setInteractionEnabled(boolean enabled) {
        this.interactionEnabled = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        String raw = mItemList.get(position).getSecond(); // format: "Name™id"
        String[] parts = raw.split("™");
        String text = parts.length > 0 ? parts[0] : "";
        int gestureId = 0;
        if (parts.length > 1) {
            try {
                gestureId = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        holder.gestureInRotationGroupTv.setText(text);
        holder.itemView.setTag(mItemList.get(position).getFirst());

        // По умолчанию текст белый, как раньше
        int inactiveColor = holder.itemView.getContext().getColor(R.color.white);
        // Активный жест подсвечиваем таким же цветом, как в биндингах
        int activeColor = holder.itemView.getContext().getColor(R.color.ubi4_active);

        if (interactionEnabled && gestureId == activeGestureId) {
            holder.gestureInRotationGroupTv.setTextColor(activeColor);
        } else {
            holder.gestureInRotationGroupTv.setTextColor(inactiveColor);
        }
    }

    @Override
    public long getUniqueItemId(int position) {
        System.err.println("setTag getUniqueItemId ==========================" + mItemList.get(position));
        return mItemList.get(position).getFirst();
    }

    class ViewHolder extends DragItemAdapter.ViewHolder {
        TextView gestureInRotationGroupTv;
        View deleteBtn;
        View copyBtn;

        ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);
            gestureInRotationGroupTv = itemView.findViewById(R.id.gestureInRotationGroupTv);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
            deleteBtn.setOnClickListener(v -> {
                if (!interactionEnabled) return;
                int position = getIndexItem(Long.parseLong(itemView.getTag().toString()));
                onDeleteClickRotationGroupListener.onDeleteClickCb(position);
            });
            copyBtn = itemView.findViewById(R.id.copyBtn);
            copyBtn.setOnClickListener(v -> {
                if (!interactionEnabled) return;
                int position = getIndexItem(Long.parseLong(itemView.getTag().toString()));
                Long setUniqueItemId = (long)mItemList.size();
                addItem(mItemList.size(), new Pair<>(setUniqueItemId, mItemList.get(position).getSecond()));
                onCopyClickRotationGroupListener.onCopyClick(position, mItemList.get(position).getSecond());
            });
            gestureInRotationGroupTv.setOnClickListener(v -> {
                if (!interactionEnabled) return;
                int position = getIndexItem(Long.parseLong(itemView.getTag().toString()));
                String raw = mItemList.get(position).getSecond(); // format: "Name™id"
                String[] parts = raw.split("™");
                String gestureName = parts.length > 0 ? parts[0] : "";
                int gestureId = 0;
                if (parts.length > 1) {
                    try {
                        gestureId = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (onSelectClickRotationGroupListener != null && gestureId != 0) {
                    onSelectClickRotationGroupListener.onRotationGestureClick(position, gestureName, gestureId);
                }
            });
        }

        @Override
        public void onItemClicked(View view) {}


        @Override
        public boolean onItemLongClicked(View view) {
            return interactionEnabled;
        }

        private int getIndexItem(long index) {
            int count = 0;
            int result = 0;
            for (Pair<Long, String> variable : mItemList) {
                if (index == variable.getFirst()) {
                    result = count;
                }
                count += 1;
            }
            return result;
        }
    }
    public interface OnCopyClickRotationGroupListener { void onCopyClick(int position, String gestureName); }
    public interface OnDeleteClickRotationGroupListener { void onDeleteClickCb(int position); }
    public interface OnSelectClickRotationGroupListener { void onRotationGestureClick(int position, String gestureName, int gestureId); }
}
