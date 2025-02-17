package com.bailout.stickk.ubi4.adapters.dialog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.models.SprDialogCollectionGestureItem

class SprGesturesCheckAdapter(
    private val gesturesList: ArrayList<SprDialogCollectionGestureItem>,
    private val onCheckSprGestureListener: OnCheckSprGestureListener2,
) : RecyclerView.Adapter<SprGesturesCheckAdapter.ScanViewHolder>() {

    inner class ScanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val selectGestureBtn: View = view.findViewById(R.id.ubi4DialogGestureItemBtn)
        val gestureName: TextView = view.findViewById(R.id.ubi4DialogTitleItemTv)
        val gestureCheckImage: ImageView = view.findViewById(R.id.usedGestureCheckIv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_dialog_gesture, parent, false)
        return ScanViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ScanViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        holder.gestureName.text = gesturesList[position].gesture.title
        holder.gestureCheckImage.setVisibility(if (gesturesList[position].check) View.VISIBLE else View.GONE)
        holder.selectGestureBtn.setOnClickListener {
            onCheckSprGestureListener.onSprGestureClicked2(position, gesturesList[position])
        }

    }
    override fun getItemCount(): Int {
        return gesturesList.size
    }
}

interface OnCheckSprGestureListener2 {
    fun onSprGestureClicked2(position : Int, gesture: SprDialogCollectionGestureItem)
}