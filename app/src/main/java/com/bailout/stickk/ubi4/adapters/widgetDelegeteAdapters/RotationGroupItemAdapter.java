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

package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bailout.stickk.R;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import com.woxthebox.draglistview.DragItemAdapter;

import java.util.ArrayList;

class RotationGroupItemAdapter extends DragItemAdapter<Pair<Long, String>, RotationGroupItemAdapter.ViewHolder> {

    private int mLayoutId;
    private int mGrabHandleId;
    private boolean mDragOnLongPress;
    OnCopyClickRotationGroupListener onCopyClickRotationGroupListener;
    OnDeleteClickRotationGroupListener onDeleteClickRotationGroupListener;

    RotationGroupItemAdapter(ArrayList<Pair<Long, String>> list, int layoutId, int grabHandleId, boolean dragOnLongPress, OnCopyClickRotationGroupListener onCopyClickRotationGroupListener, OnDeleteClickRotationGroupListener onDeleteClickRotationGroupListener) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
        setItemList(list);
        this.onCopyClickRotationGroupListener = onCopyClickRotationGroupListener;
        this.onDeleteClickRotationGroupListener = onDeleteClickRotationGroupListener;
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
        String text = mItemList.get(position).second;
        holder.gestureInRotationGroupTv.setText(text);
        holder.itemView.setTag(mItemList.get(position).first);
//        System.err.println("setTag ==========================");
//        System.err.println("setTag = "+mItemList.get(position));
    }

    @Override
    public long getUniqueItemId(int position) {
        System.err.println("setTag getUniqueItemId ==========================" + mItemList.get(position));
        return mItemList.get(position).first;
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
//                Toast.makeText(v.getContext(), "DELETE! "+ itemView.getTag(), Toast.LENGTH_SHORT).show();
                int position = getIndexItem(Long.parseLong(itemView.getTag().toString()));
//                removeItem(getIndexItem(Long.parseLong(itemView.getTag().toString())));
                onDeleteClickRotationGroupListener.onDeleteClickCb(position);
            });
            copyBtn = itemView.findViewById(R.id.copyBtn);
            copyBtn.setOnClickListener(v -> {
//                Toast.makeText(v.getContext(), "COPY! " + itemView.getTag(), Toast.LENGTH_SHORT).show();
                int position = getIndexItem(Long.parseLong(itemView.getTag().toString()));
                Long setUniqueItemId = (long)mItemList.size();
                addItem(mItemList.size(), new Pair<>(setUniqueItemId, mItemList.get(position).second));
                onCopyClickRotationGroupListener.onCopyClick(position);
            });
        }

        @Override
        public void onItemClicked(View view) {}


        @Override
        public boolean onItemLongClicked(View view) {
            return true;
        }

        private int getIndexItem(long index) {
            int count = 0;
            int result = 0;
            for (Pair<Long, String> variable : mItemList) {
                if (index == variable.first) {
                    result = count;
                }
                count += 1;
            }
            return result;
        }
    }

    public interface OnCopyClickRotationGroupListener { void onCopyClick(int position); }
    public interface OnDeleteClickRotationGroupListener { void onDeleteClickCb(int position); }
}
