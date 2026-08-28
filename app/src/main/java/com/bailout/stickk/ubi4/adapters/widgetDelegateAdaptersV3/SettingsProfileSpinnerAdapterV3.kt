package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.Ubi4ItemSettingsProfileSpinnerBinding
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import com.skydoves.powerspinner.PowerSpinnerInterface
import com.skydoves.powerspinner.PowerSpinnerView

internal class SettingsProfileSpinnerAdapterV3(
    override val spinnerView: PowerSpinnerView,
    private val isEditable: (position: Int) -> Boolean,
    private val onEditClick: (position: Int, name: String) -> Unit
) : RecyclerView.Adapter<SettingsProfileSpinnerAdapterV3.ViewHolder>(),
    PowerSpinnerInterface<CharSequence> {

    private val items = mutableListOf<CharSequence>()

    override var index: Int = spinnerView.selectedIndex
    override var onSpinnerItemSelectedListener: OnSpinnerItemSelectedListener<CharSequence>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = Ubi4ItemSettingsProfileSpinnerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(
            name = items[position].toString(),
            selected = position == index,
            editable = isEditable(position)
        )
    }

    override fun getItemCount(): Int = items.size

    override fun setItems(itemList: List<CharSequence>) {
        items.clear()
        items.addAll(itemList)
        index = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    override fun notifyItemSelected(index: Int) {
        if (index !in items.indices) return

        val previousIndex = this.index
        val previousItem = items.getOrNull(previousIndex)
        val selectedItem = items[index]
        this.index = index

        spinnerView.notifyItemSelected(index, selectedItem)
        notifyDataSetChanged()
        onSpinnerItemSelectedListener?.onItemSelected(
            previousIndex,
            previousItem,
            index,
            selectedItem
        )
    }

    inner class ViewHolder(
        private val binding: Ubi4ItemSettingsProfileSpinnerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?: return@setOnClickListener
                notifyItemSelected(position)
            }
            binding.ubi4SettingsProfileItemEdit.setOnClickListener {
                val position = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?: return@setOnClickListener
                val name = items.getOrNull(position)?.toString() ?: return@setOnClickListener
                onEditClick(position, name)
            }
        }

        fun bind(name: String, selected: Boolean, editable: Boolean) = with(binding) {
            root.layoutParams = root.layoutParams.apply {
                if (spinnerView.spinnerItemHeight != Int.MIN_VALUE) {
                    height = spinnerView.spinnerItemHeight
                }
            }
            root.background = spinnerView.spinnerSelectedItemBackground.takeIf { selected }

            ubi4SettingsProfileItemTitle.apply {
                text = name
                typeface = spinnerView.typeface
                setTextSize(TypedValue.COMPLEX_UNIT_PX, spinnerView.textSize)
                setTextColor(spinnerView.currentTextColor)
            }
            ubi4SettingsProfileItemEdit.visibility = if (editable) View.VISIBLE else View.GONE
        }
    }
}
