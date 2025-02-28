package com.bailout.stickk.ubi4.ui.fragments.account

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.BuildConfig
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.fragments.account.AccountFragmentMainUBI4.Companion.accountMainList

class AccountMainAdapterUBI4(private val onAccountClickListener: OnAccountMainUBI4ClickListener
) : RecyclerView.Adapter<AccountMainAdapterUBI4.AccountViewHolder>() {

    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val customerServiceBtn: View = view.findViewById(R.id.ubi4_customer_service_btn)
        val prosthesisInformationBtn: View = view.findViewById(R.id.ubi4_prosthesis_information_btn)
        val fioTv: TextView = view.findViewById(R.id.ubi4_fio_tv) as TextView
        val driverVersionNumTv: TextView = view.findViewById(R.id.ubi4_driver_version_num_tv) as TextView
        val bmsVersionNumTv: TextView = view.findViewById(R.id.ubi4_bms_version_num_tv) as TextView
        val sensorsVersionNumTv: TextView = view.findViewById(R.id.ubi4_sensors_version_num_tv) as TextView
        val applicationVersionNumTv: TextView = view.findViewById(R.id.ubi4_version_app_num_tv) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_account_main, parent, false)
        return AccountViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.fioTv.text = accountMainList[position].getName()+" "+accountMainList[position].getSurname()


        holder.driverVersionNumTv.text = accountMainList[position].getVersionDriver()
        holder.bmsVersionNumTv.text = accountMainList[position].getVersionBms()
        holder.sensorsVersionNumTv.text = accountMainList[position].getVersionSensors()
        holder.applicationVersionNumTv.text = BuildConfig.VERSION_NAME


        holder.customerServiceBtn.setOnClickListener {
            onAccountClickListener.onCustomerServiceClicked()
        }

        holder.prosthesisInformationBtn.setOnClickListener {
            onAccountClickListener.onProsthesisInformationClicked()
        }
    }
    override fun getItemCount(): Int {
        return accountMainList.size
    }
}