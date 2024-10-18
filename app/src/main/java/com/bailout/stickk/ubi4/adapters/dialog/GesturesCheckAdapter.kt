package com.bailout.stickk.ubi4.adapters.dialog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.adapters.models.DialogGestureItem

class GesturesCheckAdapter(
    private val gesturesList: ArrayList<DialogGestureItem>,
    private val onCheckGestureListener: OnCheckGestureListener,
) : RecyclerView.Adapter<GesturesCheckAdapter.ScanViewHolder>() {

    inner class ScanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val selectGestureBtn: View = view.findViewById(R.id.ubi4DialogGestureItemBtn)
        val gestureName: TextView = view.findViewById(R.id.ubi4DialogTitleItemTv)
        val gestureCheckImage: ImageView = view.findViewById(R.id.usedGestureCheckIv)
        //val titleText: TextView = view.findViewById(R.id.dialogTitleBindingTv)

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
        holder.gestureName.text = gesturesList[position].title
        holder.gestureCheckImage.setVisibility(if (gesturesList[position].check) View.VISIBLE else View.GONE)

        holder.selectGestureBtn.setOnClickListener {
            onCheckGestureListener.onGestureClicked(position, gesturesList[position].title)
        }
    }

    override fun getItemCount(): Int {
        return gesturesList.size
    }
}

interface OnCheckGestureListener {
    fun onGestureClicked(position: Int, title: String)
}