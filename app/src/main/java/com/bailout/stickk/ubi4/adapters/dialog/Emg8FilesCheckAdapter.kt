package com.bailout.stickk.ubi4.adapters.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.models.Emg8FileItem

// 2) Лисенер клика по элементу списка
interface OnCheckEmg8FileListener {
    fun onFileClicked(position: Int, item: Emg8FileItem)
}

// 3) Сам адаптер
class Emg8FilesCheckAdapter(
    private val items: List<Emg8FileItem>,
    private val listener: OnCheckEmg8FileListener
) : RecyclerView.Adapter<Emg8FilesCheckAdapter.Emg8ViewHolder>() {

    inner class Emg8ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTv: TextView = itemView.findViewById(R.id.ubi4DialogEmg8FileTitleItemTv)
        private val checkIv: ImageView = itemView.findViewById(R.id.usedEmg8FileCheckIv)
        private val container: View = itemView.findViewById(R.id.ubi4Emg8FilesItemBtn)

        fun bind(item: Emg8FileItem, pos: Int) {
            titleTv.text = item.file.name
            checkIv.visibility = if (item.isChecked) View.VISIBLE else View.GONE

            // можно кликать по любой области, но лучше – по всей View с фоновой селектируемой анимацией
            container.setOnClickListener {
                listener.onFileClicked(pos, item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Emg8ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_em8_files, parent, false)
        return Emg8ViewHolder(view)
    }

    override fun onBindViewHolder(holder: Emg8ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size
}