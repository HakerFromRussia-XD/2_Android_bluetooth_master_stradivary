package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.BuildConfig
import com.bailout.stickk.R

class BootloaderCardAdapter(
    private val innerAdapter: BootloaderAdapterUBI4
) : RecyclerView.Adapter<BootloaderCardAdapter.CardVH>() {

    private var lastCount = innerAdapter.itemCount

    /** Следим, когда во вложенном списке стало пусто / не пусто. */
    private val dataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged()                    = refresh()
        override fun onItemRangeInserted(s: Int, c: Int) = refresh()
        override fun onItemRangeRemoved(s: Int, c: Int)  = refresh()

        private fun refresh() {
            val newCount = innerAdapter.itemCount
            if ((lastCount == 0 && newCount > 0) || (lastCount > 0 && newCount == 0)) {
                lastCount = newCount
                notifyDataSetChanged()      // пересоздать карточку (0 → 1 или 1 → 0)
            }
        }
    }

    init { innerAdapter.registerAdapterDataObserver(dataObserver) }

    inner class CardVH(v: View) : RecyclerView.ViewHolder(v) {
        val rv: RecyclerView = v.findViewById(R.id.bootloader_rv)
        val applicationVersionNumTv: TextView = v.findViewById(R.id.ubi4_version_app_num_tv) as TextView

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bootloader_card, parent, false)
        return CardVH(v)
    }

    override fun onBindViewHolder(holder: CardVH, position: Int) {
        if (holder.rv.adapter == null) {
            holder.rv.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.rv.adapter       = innerAdapter
        }
        holder.applicationVersionNumTv.text = BuildConfig.VERSION_NAME

    }

    override fun getItemCount(): Int = if (innerAdapter.itemCount == 0) 0 else 1
}