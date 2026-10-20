package com.bailout.stickk.ubi4.adapters.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.network.RemoteFirmwareFile

/** Displays catalog metadata; an archive is not needed until its row is selected. */
class RemoteFirmwareFilesAdapter(
    private val files: List<RemoteFirmwareFile>,
    private val onSelect: (RemoteFirmwareFile) -> Unit
) : RecyclerView.Adapter<RemoteFirmwareFilesAdapter.FileViewHolder>() {
    var isSelectionEnabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.ubi4DialogTitleFileTv)
        val select: View = view.findViewById(R.id.ubi4DialogFileItemBtn)

        init {
            view.findViewById<View>(R.id.deleteFileIv).visibility = View.GONE
            view.findViewById<View>(R.id.deleteBtn).visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder =
        FileViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_dialog_file, parent, false))

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.name.text = file.name
        holder.select.isEnabled = isSelectionEnabled
        holder.itemView.alpha = if (isSelectionEnabled) 1f else 0.5f
        holder.select.setOnClickListener {
            if (isSelectionEnabled) onSelect(file)
        }
    }
}
