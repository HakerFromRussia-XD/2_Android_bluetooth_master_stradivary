package com.bailout.stickk.ubi4.ui.fragments.account.prosthesisInformationFragmentUBI4

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.fragments.account.prosthesisInformationFragmentUBI4.AccountFragmentProsthesisInformationUBI4.Companion.accountProsthesisInformationList

class AccountProsthesisInformationAdapterUBI4() : RecyclerView.Adapter<AccountProsthesisInformationAdapterUBI4.AccountViewHolder>() {

    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val prosthesisModelStrTv: TextView = view.findViewById(R.id.prosthesis_model_str_tv_ubi4) as TextView
        val prosthesisSizeStrTv: TextView = view.findViewById(R.id.prosthesis_size_str_tv_ubi4) as TextView
        val handSideStrTv: TextView = view.findViewById(R.id.hand_side_str_tv_ubi4) as TextView
        val rotatorTypeStrTv: TextView = view.findViewById(R.id.rotator_type_str_tv_ubi4) as TextView
        val touchscreenFingerPadsStrTv: TextView = view.findViewById(R.id.touchscreen_finger_pads_str_tv_ubi4) as TextView
        val batteryTypeStrTv: TextView = view.findViewById(R.id.battery_type_str_tv_ubi4) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_account_prosthesis_information, parent, false)
        return AccountViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.prosthesisModelStrTv.text = accountProsthesisInformationList[position].getProsthesisModel()
        holder.prosthesisSizeStrTv.text = accountProsthesisInformationList[position].getProsthesisSize()
        holder.handSideStrTv.text = accountProsthesisInformationList[position].getHandSide()
        holder.rotatorTypeStrTv.text = accountProsthesisInformationList[position].getRotatorType()
        holder.touchscreenFingerPadsStrTv.text = accountProsthesisInformationList[position].getTouchscreenFingerPads()
        holder.batteryTypeStrTv.text = accountProsthesisInformationList[position].getBatteryType()
    }
    override fun getItemCount(): Int {
        return accountProsthesisInformationList.size
    }
}