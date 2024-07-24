package com.bailout.stickk.ubi4.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.contract.OnChatClickListener
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val typeCellsList: ArrayList<String>,
                  private val massagesList: ArrayList<String>,
                  private val onChatClickListener: OnChatClickListener
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val typeOneCell: ConstraintLayout = view.findViewById(R.id.massage_type_1_cell) as ConstraintLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account_customer_service, parent, false)
        return ChatViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        println("onBindViewHolder selectedProfile = " + MainActivityUBI4.connectedDeviceName)
        val simpleDateFormat = SimpleDateFormat("dd MMMM  HH:mm", Locale.ROOT)
        fun getDateString(time: Long) : String = simpleDateFormat.format(time * 1000L)
        val typeCell = typeCellsList[position]



//        if (typeCell != "invalidate") {
//            if (position < massagesList.size) {
//                if (typeCell == "type_1") {
//                    holder.typeOneCell.visibility = View.VISIBLE
//                }
//                if (typeCell == "type_2") {
//                    holder.typeOneCell.visibility = View.GONE
//                }
//                if (typeCell == "type_3") {
//                    holder.typeOneCell.visibility = View.GONE
//                }
//            }
//        } else {
//            holder.typeOneCell.visibility = View.GONE
//        }
    }
    override fun getItemCount(): Int {
        return typeCellsList.size
    }

}