package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.account.customerServiceFragment

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.account.customerServiceFragment.AccountFragmentCustomerService.Companion.accountCustomerServiceList

class AccountCustomerServiceAdapter(private val onYourMangerClickListener: OnAccountCustomerServiceClickListener
) : RecyclerView.Adapter<AccountCustomerServiceAdapter.AccountViewHolder>() {

    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val yourManagerBtn: View = view.findViewById(R.id.your_manager_btn) as View

        val dateOfReceiptOfProsthesisStrTv: TextView = view.findViewById(R.id.date_of_receipt_of_prosthesis_str_tv) as TextView
        val warrantyExpirationDateStrTv: TextView = view.findViewById(R.id.warranty_expiration_date_str_tv) as TextView
        val yourManagerStrTv: TextView = view.findViewById(R.id.your_manager_str_tv) as TextView
        val prosthesisStatusStrTv: TextView = view.findViewById(R.id.prosthesis_status_str_tv) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountCustomerServiceAdapter.AccountViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_account_customer_service, parent, false)
        return AccountViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AccountCustomerServiceAdapter.AccountViewHolder, position: Int) {
        holder.dateOfReceiptOfProsthesisStrTv.text = accountCustomerServiceList[position].getDateOfReceiptOfProsthesis()
        holder.warrantyExpirationDateStrTv.text = accountCustomerServiceList[position].getWarrantyExpirationDate()
        holder.yourManagerStrTv.text = accountCustomerServiceList[position].getYourManager()
        holder.prosthesisStatusStrTv.text = accountCustomerServiceList[position].getProsthesisStatus()


        holder.yourManagerBtn.setOnClickListener {
            onYourMangerClickListener.onYourMangerClicked()
        }
    }
    override fun getItemCount(): Int {
        return accountCustomerServiceList.size
    }
}