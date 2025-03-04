package com.bailout.stickk.ubi4.ui.fragments.customerServiceFragmentUBI4

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.fragments.customerServiceFragmentUBI4.AccountFragmentCustomerServiceUBI4.Companion.accountCustomerServiceList

class AccountCustomerServiceAdapterUbi4(private val onYourMangerClickListener: OnAccountCustomerServiceUBI4ClickListener
) : RecyclerView.Adapter<AccountCustomerServiceAdapterUbi4.AccountViewHolder>() {

    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val yourManagerBtn: View = view.findViewById(R.id.ubi4_your_manager_btn)

        val dateOfReceiptOfProsthesisStrTv: TextView = view.findViewById(R.id.ubi4_date_of_receipt_of_prosthesis_str_tv)
        val warrantyExpirationDateStrTv: TextView = view.findViewById(R.id.ubi4_warranty_expiration_date_str_tv)
        val yourManagerStrTv: TextView = view.findViewById(R.id.ubi4_your_manager_str_tv)
        val prosthesisStatusStrTv: TextView = view.findViewById(R.id.ubi4_prosthesis_status_str_tv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountCustomerServiceAdapterUbi4.AccountViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_account_customer_service, parent, false)
        return AccountViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AccountCustomerServiceAdapterUbi4.AccountViewHolder, position: Int) {
        holder.dateOfReceiptOfProsthesisStrTv.text = accountCustomerServiceList[position].getDateOfReceiptOfProsthesisUbi4()
        holder.warrantyExpirationDateStrTv.text = accountCustomerServiceList[position].getWarrantyExpirationDateUbi4()
        holder.yourManagerStrTv.text = accountCustomerServiceList[position].getYourManagerUbi4()
        holder.prosthesisStatusStrTv.text = accountCustomerServiceList[position].getProsthesisStatusUbi4()


        holder.yourManagerBtn.setOnClickListener {
            onYourMangerClickListener.onYourMangerClicked()
        }
    }
    override fun getItemCount(): Int {
        return accountCustomerServiceList.size
    }
}