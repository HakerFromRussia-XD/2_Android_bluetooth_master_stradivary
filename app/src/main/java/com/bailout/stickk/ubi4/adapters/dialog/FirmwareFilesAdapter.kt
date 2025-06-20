package com.bailout.stickk.ubi4.adapters.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.adapters.dialog.FileCheckpointAdapter.OnFileActionListener
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.models.widgets.FileItem

class FirmwareFilesAdapter(private val files: List<FirmwareFileItem>,
                           private val listener: OnFileActionListener
) : RecyclerView.Adapter<FirmwareFilesAdapter.FileViewHolder>()  {

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileNameTv: TextView = view.findViewById(R.id.ubi4DialogTitleFileTv)
        val deleteBtn: View = view.findViewById(R.id.deleteFileIv)
        val selectBtn: View = view.findViewById(R.id.ubi4DialogFileItemBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ubi4_item_dialog_file, parent, false)
        return FileViewHolder(itemView)
    }

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = files[position]

        holder.fileNameTv.text = item.name

        holder.deleteBtn.setOnClickListener {
            // удаляем через PlatformFile API
            if (item.file.delete()) {
                listener.onDelete(position, item)
            }
        }

        holder.selectBtn.setOnClickListener {
            listener.onSelect(position, item) {
                // вызываем onComplete, когда UI-действия внутри адаптера завершены
            }
        }
    }

    interface OnFileActionListener {
        /** вызывается после успешного удаления файла */
        fun onDelete(position: Int, fileItem: FirmwareFileItem)

        /** вызывается при выборе файла; onComplete() когда можно закрыть диалог */
        fun onSelect(position: Int, fileItem: FirmwareFileItem, onComplete: () -> Unit)
    }
}