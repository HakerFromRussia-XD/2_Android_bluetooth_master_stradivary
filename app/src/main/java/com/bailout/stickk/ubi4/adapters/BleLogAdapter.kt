package com.bailout.stickk.ubi4.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ItemBleLogEntryBinding
import com.bailout.stickk.ubi4.blelog.BleLogDirection
import com.bailout.stickk.ubi4.blelog.BleLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BleLogAdapter : RecyclerView.Adapter<BleLogAdapter.BleLogViewHolder>() {

    private val items = mutableListOf<BleLogEntry>()
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BleLogViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = Ubi4ItemBleLogEntryBinding.inflate(inflater, parent, false)
        return BleLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BleLogViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun replaceEntries(newItems: List<BleLogEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun appendEntries(newItems: List<BleLogEntry>) {
        if (newItems.isEmpty()) return
        val startPosition = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    inner class BleLogViewHolder(
        private val binding: Ubi4ItemBleLogEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BleLogEntry) = with(binding) {
            timeTv.text = timeFormatter.format(Date(item.timestampMillis))
            bytesTv.text = item.bytesHex

            if (item.direction == BleLogDirection.OUTGOING) {
                sourceIv.setImageResource(R.drawable.img_mobile)
                targetIv.setImageResource(R.drawable.robotic_hand)
            } else {
                sourceIv.setImageResource(R.drawable.robotic_hand)
                targetIv.setImageResource(R.drawable.img_mobile)
            }
        }
    }
}
