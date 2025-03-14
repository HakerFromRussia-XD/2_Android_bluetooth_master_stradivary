package com.bailout.stickk.ubi4.adapters.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.models.widgets.FileItem

class FileCheckpointAdapter(
    private val files: List<FileItem>,
    private val listener: OnFileActionListener
) : RecyclerView.Adapter<FileCheckpointAdapter.FileViewHolder>() {

    private val isLoading = MutableList(files.size) { false }

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.ubi4DialogTitleFileTv)
        val deleteButton: View = view.findViewById(R.id.deleteFileIv)
        val ubi4DialogFileItemBtn: View = view.findViewById(R.id.ubi4DialogFileItemBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_dialog_file, parent, false)
        return FileViewHolder(itemView)
    }

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileItem = files[position]
        holder.fileName.text = fileItem.name

        holder.deleteButton.setOnClickListener {
            listener.onDelete(position, fileItem)
        }
        holder.ubi4DialogFileItemBtn.setOnClickListener {
            listener.onSelect(position, fileItem) {}


        }
    }

    interface OnFileActionListener {
        fun onDelete(position: Int, fileItem: FileItem)
        fun onSelect(position: Int, fileItem: FileItem, onComplete: () -> Unit)
    }
}