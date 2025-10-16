package com.bailout.stickk.ubi4.adapters.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.Ubi4ItemGestureEmptyCardBinding
import com.bailout.stickk.ubi4.models.GestureItem

class GesturesSelectionAdapter(
    private val gestures: List<GestureItem>,
    private val onGestureClick: (GestureItem) -> Unit
) : RecyclerView.Adapter<GesturesSelectionAdapter.GestureCardViewHolder>() {
    class GestureCardViewHolder(private val binding: Ubi4ItemGestureEmptyCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(gesture: GestureItem, onGestureClick: (GestureItem) -> Unit) {
            binding.gestureTv.text = gesture.name

            binding.gestureBtn.setOnClickListener {
                onGestureClick(gesture)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureCardViewHolder {
        val binding = Ubi4ItemGestureEmptyCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GestureCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GestureCardViewHolder, position: Int) {
        val gesture = gestures[position]
        holder.bind(gesture, onGestureClick)
    }

    override fun getItemCount(): Int = gestures.size
}