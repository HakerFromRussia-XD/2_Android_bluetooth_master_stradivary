package com.bailout.stickk.ubi4.adapters.dialog

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ItemGestureCardBinding
import com.bailout.stickk.ubi4.models.GestureItem
import com.woxthebox.draglistview.DragItemAdapter

class GesturesDragAdapter(
    private val selectedGestures: MutableList<GestureItem>,
    private val onDeleteClick: (GestureItem) -> Unit
) : DragItemAdapter<GestureItem, GesturesDragAdapter.GestureViewHolder>() {

    class GestureViewHolder(private val binding: Ubi4ItemGestureCardBinding) : DragItemAdapter.ViewHolder(
        binding.root,
        R.id.swapIv,
        true
    ) {
        fun bind(item: GestureItem, onDeleteClick: (GestureItem) -> Unit) {
            Log.d("GesturesDragAdapter", "Binding item: ${item.name}")
            binding.gestureCardTv.text = item.name

            binding.deleteIv.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureViewHolder {
        Log.d("GesturesDragAdapter", "onCreateViewHolder called")
        val binding = Ubi4ItemGestureCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GestureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GestureViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val item = selectedGestures[position]
        holder.bind(item, onDeleteClick)
    }

    override fun getItemCount(): Int {
        return selectedGestures.size
    }

    override fun getUniqueItemId(position: Int): Long {
        return selectedGestures[position].id.toLong()
    }
}