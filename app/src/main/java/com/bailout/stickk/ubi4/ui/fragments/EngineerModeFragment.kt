package com.bailout.stickk.ubi4.ui.fragments

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.databinding.Ubi4ItemGestureCardBinding
import com.bailout.stickk.databinding.Ubi4ItemGestureEmptyCardBinding
import com.woxthebox.draglistview.DragItemAdapter
import com.woxthebox.draglistview.DragListView

data class DataCollectionSettings(
    val nCycles: Int,
    val baselineDuration: Int,
    val preGestDuration: Int,
    val atGestDuration: Int,
    val postGestDuration: Int
)

data class GestureItem(
    val id: Int,
    val name: String,
    val isSelected: Boolean = false
) {
    fun getItemId(): Any = id
}

class EngineerModeFragment : BaseWidgetsFragment() {
    private var dataCollectionDialog: Dialog? = null
    private var gesturesDialog: Dialog? = null
    private var gesturesListDialog: Dialog? = null
    private val selectedGestures = mutableListOf<GestureItem>()
    private val onDestroyParentCallbacks = mutableListOf<() -> Unit>()
    private lateinit var gesturesAdapter: DragItemAdapter<GestureItem, GestureViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ubi4_fragment_engineer_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dataCollectionBtn = view.findViewById<View>(R.id.dataCollectionBtn)
        dataCollectionBtn?.setOnClickListener {
            showDataCollectionDialog()
        }
    }

    private class GestureViewHolder(private val binding: Ubi4ItemGestureCardBinding) : DragItemAdapter.ViewHolder(
        binding.root,
        R.id.swapIv, // handleResId - ID элемента для перетаскивания
        true // dragOnLongPress - перетаскивание по долгому нажатию
    ) {
        fun bind(item: GestureItem, onDeleteClick: (GestureItem) -> Unit) {
            binding.gestureCardTv.text = item.name

            binding.deleteIv.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    private class GestureCardViewHolder(private val binding: Ubi4ItemGestureEmptyCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(gesture: GestureItem, onGestureClick: (GestureItem) -> Unit) {
            binding.gestureTv.text = gesture.name
            binding.gestureBtn.setOnClickListener {
                onGestureClick(gesture)
            }
        }
    }

    private fun showDataCollectionDialog() {
        if (dataCollectionDialog?.isShowing == true)
            return

        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_data_collection_settings, null)
        val dialog = Dialog(requireContext())

        dialog.setContentView(dialogBinding)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dataCollectionDialog = dialog
        dialog.show()

        setupDialogControls(dialogBinding)
        setupDialogButtons(dialogBinding, dialog)
    }

    private fun setupDialogControls(dialogBinding: View) {
        // N_CYCLES
        setupSlider(
            dialogBinding,
            R.id.nCyclesSliderSb,
            R.id.nCyclesSliderNumTv,
            R.id.nCyclesMinusBtnRipple,
            R.id.nCyclesPlusBtnRipple,
            minValue = 1,
            maxValue = 10,
            defaultValue = 3
        )

        // BASELINE_DURATION
        setupSlider(
            dialogBinding,
            R.id.baselineDurationSliderSb,
            R.id.baselineDurationSliderNumTv,
            R.id.baselineDurationMinusBtnRipple,
            R.id.baselineDurationPlusBtnRipple,
            minValue = 1,
            maxValue = 10,
            defaultValue = 5
        )

        // PRE_GEST_DURATION
        setupSlider(
            dialogBinding,
            R.id.preGestDurationSliderSb,
            R.id.preGestDurationSliderNumTv,
            R.id.preGestDurationMinusBtnRipple,
            R.id.preGestDurationPlusBtnRipple,
            minValue = 1,
            maxValue = 5,
            defaultValue = 2
        )

        // AT_GEST_DURATION
        setupSlider(
            dialogBinding,
            R.id.atGestDurationSliderSb,
            R.id.atGestDurationSliderNumTv,
            R.id.atGestDurationMinusBtnRipple,
            R.id.atGestDurationPlusBtnRipple,
            minValue = 1,
            maxValue = 5,
            defaultValue = 2
        )

        // POST_GEST_DURATION
        setupSlider(
            dialogBinding,
            R.id.postGestDurationSliderSb,
            R.id.postGestDurationSliderNumTv,
            R.id.postGestDurationMinusBtnRipple,
            R.id.postGestDurationPlusBtnRipple,
            minValue = 1,
            maxValue = 5,
            defaultValue = 2
        )
    }

    private fun setupSlider(
        dialogBinding: View,
        seekBarId: Int,
        textViewId: Int,
        minusBtnId: Int,
        plusBtnId: Int,
        minValue: Int,
        maxValue: Int,
        defaultValue: Int
    ) {
        val seekBar = dialogBinding.findViewById<SeekBar>(seekBarId)
        val textView = dialogBinding.findViewById<TextView>(textViewId)
        val minusBtn = dialogBinding.findViewById<View>(minusBtnId)
        val plusBtn = dialogBinding.findViewById<View>(plusBtnId)

        seekBar.max = maxValue - minValue
        seekBar.progress = defaultValue - minValue
        textView.text = defaultValue.toString()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + minValue
                textView.text = value.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        minusBtn.setOnClickListener {
            val currentValue = seekBar.progress + minValue
            if (currentValue > minValue)
                seekBar.progress = currentValue - 1 - minValue
        }

        plusBtn.setOnClickListener {
            val currentValue = seekBar.progress + minValue
            if (currentValue < maxValue)
                seekBar.progress = currentValue + 1 - minValue
        }
    }

    private fun setupDialogButtons(dialogBinding: View, dialog: Dialog) {
        val gesturesBtn = dialogBinding.findViewById<View>(R.id.dialogDataCollectionSettingsGesturesBtn)
        gesturesBtn.setOnClickListener {
            showGesturesSelectionDialog()
        }

        val saveBtn = dialogBinding.findViewById<View>(R.id.dialogDataCollectionSettingsSaveBtn)
        saveBtn.setOnClickListener {
            val settings = collectDialogSettings(dialogBinding)
//            saveDataCollectionSettings(settings)
            dialog.dismiss()
        }

        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogDataCollectionSettingsCancelBtn)
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun getSliderValue(dialogBinding: View, seekBarId: Int, minValue: Int = 1): Int {
        val seekBar = dialogBinding.findViewById<SeekBar>(seekBarId)
        return seekBar.progress + minValue
    }

    private fun collectDialogSettings(dialogBinding: View): DataCollectionSettings {
        return DataCollectionSettings(
            nCycles = getSliderValue(dialogBinding, R.id.nCyclesSliderSb),
            baselineDuration = getSliderValue(dialogBinding, R.id.baselineDurationSliderSb),
            preGestDuration = getSliderValue(dialogBinding, R.id.preGestDurationSliderSb),
            atGestDuration = getSliderValue(dialogBinding, R.id.atGestDurationSliderSb),
            postGestDuration = getSliderValue(dialogBinding, R.id.postGestDurationSliderSb)
        )
    }

    private fun showGesturesSelectionDialog() {
        if (gesturesDialog?.isShowing == true)
            return

        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_select_gestures_for_data_collection, null)
        val dialog = Dialog(requireContext())

        dialog.setContentView(dialogBinding)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        gesturesDialog = dialog
        dialog.show()

        setupGesturesDialog(dialogBinding, dialog)
    }

    private fun setupGesturesDialog(dialogBinding: View, dialog: Dialog) {
        val dragListView = dialogBinding.findViewById<DragListView>(R.id.gesturesDragLv)
        val addBtn = dialogBinding.findViewById<View>(R.id.addGestureBtn)
        val saveBtn = dialogBinding.findViewById<View>(R.id.saveGesturesBtn)
        val cancelBtn = dialogBinding.findViewById<View>(R.id.cancelGesturesBtn)

        // Настройка DragListView с DragItemAdapter
        val adapter = object : DragItemAdapter<GestureItem, GestureViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureViewHolder {
                val binding = Ubi4ItemGestureCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return GestureViewHolder(binding)
            }

            override fun onBindViewHolder(holder: GestureViewHolder, position: Int) {
                super.onBindViewHolder(holder, position)
                val item = selectedGestures[position]
                Log.d("DragItemAdapter", "Binding item at position $position: ${item.name}")
                Log.d("DragItemAdapter", "Total items: ${selectedGestures.size}")
                holder.bind(item) { gesture ->
                    val index = selectedGestures.indexOf(gesture)
                    Log.d("DragItemAdapter", "Removing gesture: ${gesture.name} at index: $index")
                    selectedGestures.remove(gesture)
                    notifyItemRemoved(index)
                    Log.d("DragItemAdapter", "After removal - Total items: ${selectedGestures.size}")
                }
            }

            override fun getItemCount(): Int = selectedGestures.size

            override fun getUniqueItemId(position: Int): Long {
                return selectedGestures[position].id.toLong()
            }
        }

        dragListView.setAdapter(adapter, true)
        gesturesAdapter = adapter

        // Настройка перетаскивания для DragListView
        dragListView.setDragListListener(object : DragListView.DragListListener {
            override fun onItemDragStarted(position: Int) {
                // Начало перетаскивания
            }

            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                // Конец перетаскивания - обновляем порядок элементов
                if (fromPosition != toPosition) {
                    moveGestureItem(fromPosition, toPosition)
                }
            }

            override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {
                // Процесс перетаскивания
            }
        })

        // Кнопка добавления жеста
        addBtn.setOnClickListener {
            showAddGestureDialog()
        }

        // Кнопка сохранения порядка
        saveBtn.setOnClickListener {
            saveGesturesOrder()
            dialog.dismiss()
        }

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        // Загружаем текущий порядок жестов
        loadGesturesOrder()
    }

    private fun moveGestureItem(fromPosition: Int, toPosition: Int) {
        val item = selectedGestures.removeAt(fromPosition)
        selectedGestures.add(toPosition, item)
        gesturesAdapter.notifyItemMoved(fromPosition, toPosition)
    }

    private fun updateGesturesList() {
        gesturesAdapter.notifyDataSetChanged()
    }

    private fun loadGesturesOrder() {
        // selectedGestures = loadFromPreferences()
        updateGesturesList()
    }

    private fun saveGesturesOrder() {
        // saveToPreferences(selectedGestures)
    }

    private fun showAddGestureDialog() {
        if (gesturesListDialog?.isShowing == true)
            return

        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_add_to_gestures_list, null)
        val dialog = Dialog(requireContext())

        dialog.setContentView(dialogBinding)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        gesturesListDialog = dialog
        dialog.show()

        setupGesturesListDialog(dialogBinding, dialog)
    }

    private fun setupGesturesListDialog(dialogBinding: View, dialog: Dialog) {
        val recyclerView = dialogBinding.findViewById<RecyclerView>(R.id.gesturesRv)
        val cancelBtn = dialogBinding.findViewById<View>(R.id.cancelGesturesBtn)

        val gestures = listOf(
            GestureItem(0, "Neutral"),
            GestureItem(1, "ThumbFingers"),
            GestureItem(2, "Close"),
            GestureItem(3, "Open"),
            GestureItem(4, "Pinch"),
            GestureItem(5, "Indication"),
            GestureItem(6, "Wrist_Flex"),
            GestureItem(7, "Wrist_Extend")
        )

        val adapter = object : RecyclerView.Adapter<GestureCardViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureCardViewHolder {
                val binding = Ubi4ItemGestureEmptyCardBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return GestureCardViewHolder(binding)
            }

            override fun onBindViewHolder(holder: GestureCardViewHolder, position: Int) {
                val gesture = gestures[position]
                holder.bind(gesture) { selectedGesture ->
                    selectedGestures.add(selectedGesture)
                    gesturesAdapter.notifyItemInserted(selectedGestures.size - 1)
                    dialog.dismiss()
                }
            }

            override fun getItemCount(): Int = gestures.size
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
    }
}