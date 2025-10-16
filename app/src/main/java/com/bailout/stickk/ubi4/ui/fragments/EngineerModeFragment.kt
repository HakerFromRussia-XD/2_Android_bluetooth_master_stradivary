package com.bailout.stickk.ubi4.ui.fragments

import android.app.Dialog
import android.content.Context
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
import com.bailout.stickk.ubi4.adapters.dialog.GesturesDragAdapter
import com.bailout.stickk.ubi4.adapters.dialog.GesturesSelectionAdapter
import com.bailout.stickk.ubi4.models.DataCollectionSettings
import com.bailout.stickk.ubi4.models.GestureItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class EngineerModeFragment : BaseWidgetsFragment() {
    private var dataCollectionDialog: Dialog? = null
    private var gesturesDialog: Dialog? = null
    private var gesturesListDialog: Dialog? = null
    private val selectedGestures = mutableListOf<GestureItem>()
    private val onDestroyParentCallbacks = mutableListOf<() -> Unit>()
    private lateinit var gesturesAdapter: GesturesDragAdapter
    private lateinit var gesturesSelectionAdapter: GesturesSelectionAdapter

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

        // СНАЧАЛА загружаем сохраненные жесты
        loadGesturesOrder()

        // ПОТОМ создаем адаптер с загруженными данными
        setupGesturesAdapter()

        dragListView.setDragListListener(object : DragListView.DragListListener {
            override fun onItemDragStarted(position: Int) {
                // Начало перетаскивания
            }

            override fun onItemDragEnded(fromPosition: Int, toPosition: Int) {
                if (fromPosition != toPosition) {
                    moveGestureItem(fromPosition, toPosition)
                }
            }

            override fun onItemDragging(itemPosition: Int, x: Float, y: Float) {
                // Процесс перетаскивания
            }
        })

        addBtn.setOnClickListener {
            showAddGestureDialog()
        }

        saveBtn.setOnClickListener {
            saveGesturesOrder()
            dialog.dismiss()
        }

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
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
        val prefs = requireContext().getSharedPreferences("gestures_order", Context.MODE_PRIVATE)
        val gesturesCount = prefs.getInt("gestures_count", 0)

        selectedGestures.clear()
        for (i in 0 until gesturesCount) {
            val id = prefs.getInt("gesture_${i}_id", -1)
            val name = prefs.getString("gesture_${i}_name", "")
            if (id != -1 && !name.isNullOrEmpty()) {
                selectedGestures.add(GestureItem(id, name))
            }
        }

        Log.d("LoadGestures", "Loaded ${selectedGestures.size} gestures: ${selectedGestures.map { it.name }}")
    }

    private fun saveGesturesOrder() {
        val prefs = requireContext().getSharedPreferences("gestures_order", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putInt("gestures_count", selectedGestures.size)
        selectedGestures.forEachIndexed { index, gesture ->
            editor.putInt("gesture_${index}_id", gesture.id)
            editor.putString("gesture_${index}_name", gesture.name)
        }
        editor.apply()

        Log.d("SaveGestures", "Saved ${selectedGestures.size} gestures: ${selectedGestures.map { it.name }}")
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

        gesturesSelectionAdapter = GesturesSelectionAdapter(gestures) { selectedGesture ->
            selectedGestures.add(selectedGesture)
            setupGesturesAdapter()
            dialog.dismiss()
        }

        recyclerView.adapter = gesturesSelectionAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun setupGesturesAdapter() {
        gesturesAdapter = GesturesDragAdapter(selectedGestures, ::onGestureDelete)
        val dragListView = gesturesDialog?.findViewById<DragListView>(R.id.gesturesDragLv)
        dragListView?.setLayoutManager(LinearLayoutManager(requireContext()))
        dragListView?.setAdapter(gesturesAdapter, true)
        dragListView?.setCanDragHorizontally(false)
        dragListView?.setCanDragVertically(true)
    }

    private fun onGestureDelete(gesture: GestureItem) {
        val index = selectedGestures.indexOf(gesture)
        selectedGestures.remove(gesture)
        setupGesturesAdapter()
    }
}