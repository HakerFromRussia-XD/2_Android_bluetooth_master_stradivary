package com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R

class BootloaderAdapterUBI4(
    private val listener: OnBootloaderClickListener
) : ListAdapter<BootloaderBoardItemUBI4, BootloaderAdapterUBI4.BoardViewHolder>(Diff) {

    interface OnBootloaderClickListener {
        fun onUpdateClick(item: BootloaderBoardItemUBI4)
    }
    object Diff : DiffUtil.ItemCallback<BootloaderBoardItemUBI4>() {
        override fun areItemsTheSame(o: BootloaderBoardItemUBI4, n: BootloaderBoardItemUBI4) =
            o.deviceCode == n.deviceCode
        override fun areContentsTheSame(o: BootloaderBoardItemUBI4, n: BootloaderBoardItemUBI4) = o == n
    }

    inner class BoardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title     : TextView = view.findViewById(R.id.board_name_tv)
        val updateBtn : TextView = view.findViewById(R.id.update_btn)   // ← было Button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_bootloader_board, parent, false)
        return BoardViewHolder(v)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val item = getItem(position)
        holder.title.text = item.boardName
        holder.updateBtn.isEnabled = item.canUpdate
        holder.updateBtn.setOnClickListener { listener.onUpdateClick(item) }
    }

    /** Передаём новый список плат в адаптер. */
    fun submitBoards(list: List<BootloaderBoardItemUBI4>) = submitList(list)
}