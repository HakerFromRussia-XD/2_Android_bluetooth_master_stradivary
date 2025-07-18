package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.BuildConfig
import com.bailout.stickk.R

class AccountMainAdapterUBI4(
    private val onAccountClickListener: OnAccountMainUBI4ClickListener
) : ListAdapter<AccountMainUBI4Item, AccountMainAdapterUBI4.AccountViewHolder>(Diff)  {

    object Diff : DiffUtil.ItemCallback<AccountMainUBI4Item>() {
        override fun areItemsTheSame(o: AccountMainUBI4Item, n: AccountMainUBI4Item) = true   // один элемент-хедер
        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(o: AccountMainUBI4Item, n: AccountMainUBI4Item) = o == n
    }
    inner class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val customerServiceBtn: View = view.findViewById(R.id.customerServiceClick)
        val prosthesisInformationBtn: View = view.findViewById(R.id.prosthesisInfoClick)
        val fioTv: TextView = view.findViewById(R.id.ubi4_fio_tv) as TextView
//        val applicationVersionNumTv: TextView = view.findViewById(R.id.ubi4_version_app_num_tv) as TextView
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_account_main, parent, false)
        return AccountViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val item = getItem(position)

        holder.fioTv.text               = "${item.getName()} ${item.getSurname()}"
//        holder.applicationVersionNumTv.text = BuildConfig.VERSION_NAME

        holder.customerServiceBtn.setOnClickListener {
            onAccountClickListener.onCustomerServiceClicked()
        }
        holder.prosthesisInformationBtn.setOnClickListener {
            onAccountClickListener.onProsthesisInformationClicked()
        }
    }


    fun submitProfile(item: AccountMainUBI4Item) = submitList(listOf(item))
}