package com.bailout.stickk.ubi4.adapters.dialog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R

class GesturesCheckAdapter(
    private val gesturesList: ArrayList<String>,
    private val onCheckGestureListener: OnCheckGestureListener,
) : RecyclerView.Adapter<GesturesCheckAdapter.ScanViewHolder>() {

    inner class ScanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val selectGestureBtn: View = view.findViewById(R.id.ubi4DialogGestureItemBtn)
        val gestureName: TextView = view.findViewById(R.id.ubi4DialogTitleItemTv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_dialog_gesture, parent, false)
        return ScanViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        holder.gestureName.text = gesturesList[position]
        holder.selectGestureBtn.setOnClickListener {
            onCheckGestureListener.onGestureClicked(position)
        }
    }
    override fun getItemCount(): Int {
        return gesturesList.size
    }
}

interface OnCheckGestureListener {
    fun onGestureClicked(position : Int)
}