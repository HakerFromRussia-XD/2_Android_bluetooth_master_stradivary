package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.account

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.BuildConfig
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.account.AccountFragment.Companion.accountList

class AccountAdapter(private val onAccountClickListener: OnAccountClickListener
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val customerServiceBtn: View = view.findViewById(R.id.customer_service_btn) as View
        val prosthesisInformationBtn: View = view.findViewById(R.id.prosthesis_information_btn) as View
        val fioTv: TextView = view.findViewById(R.id.fio_tv) as TextView
        val driverVersionNumTv: TextView = view.findViewById(R.id.driver_version_num_tv) as TextView
        val bmsVersionNumTv: TextView = view.findViewById(R.id.bms_version_num_tv) as TextView
        val sensorsVersionNumTv: TextView = view.findViewById(R.id.sensors_version_num_tv) as TextView
        val applicationVersionNumTv: TextView = view.findViewById(R.id.version_app_num_tv) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account, parent, false)
        return AccountViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.fioTv.text = accountList[position].getName() + " " + accountList[position].getPatronymic()


        holder.driverVersionNumTv.text = accountList[position].getVersionDriver()
        holder.bmsVersionNumTv.text = accountList[position].getVersionBms()
        holder.sensorsVersionNumTv.text = accountList[position].getVersionSensors()
        holder.applicationVersionNumTv.text = BuildConfig.VERSION_NAME


        holder.customerServiceBtn.setOnClickListener {
            onAccountClickListener.onCustomerServiceClicked()
        }

        holder.prosthesisInformationBtn.setOnClickListener {
            onAccountClickListener.onProsthesisInformationClicked()
        }
    }
    override fun getItemCount(): Int {
        return accountList.size
    }
}