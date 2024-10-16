package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R

class UsedGestureDialogAdapter(
    private val gestures: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<UsedGestureDialogAdapter.DialogViewHolder>() {

    class DialogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gestureName: TextView = itemView.findViewById(R.id.dialog_gestureTvRv)
        val checkIcon: ImageView = itemView.findViewById(R.id.usedGestureCheckBtn)
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DialogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_dialog_gestures_binding_rv, parent, false)
        return DialogViewHolder(view)
    }

    override fun onBindViewHolder(
        holder:DialogViewHolder,
        position: Int
    ) {
       val gesture = gestures[position]
        holder.gestureName.text = gesture
        holder.checkIcon.setImageResource(R.drawable.check)
    }

    override fun getItemCount(): Int {
        return gestures.size
    }
}